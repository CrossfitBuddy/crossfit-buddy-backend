package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;

import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFormatType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

class WorkoutCreationIntegrationTest extends AbstractWorkoutCreationIntegrationTest {
    @ParameterizedTest
    @EnumSource(WorkoutFormatType.class)
    @DisplayName("Создание тренировки")
    void createsSelectedFormatWithServerOwnedMetadataAndPersistedContents(WorkoutFormatType format) throws Exception {
        var result = create(request(format), token)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("createdBy").value(owner.toString()))
            .andExpect(jsonPath("type").value("OWN"))
            .andExpect(jsonPath("status").value("READY"))
            .andExpect(jsonPath("name").doesNotExist())
            .andExpect(jsonPath("durationMinutes").doesNotExist())
            .andExpect(jsonPath("stages[0].format").value(format.name()))
            .andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        UUID id = UUID.fromString(body.required("id").asText());
        assertCommittedResponse(body);
        mvc.perform(get(ENDPOINT).contextPath("/api")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("items[*].id", hasItem(id.toString())))
            .andExpect(jsonPath("items[*].source", hasItem("OWN")))
            .andExpect(jsonPath("items[*].status", hasItem("READY")));

        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
            .endsWith(ENDPOINT + "/" + id);
        var row = database.queryForMap("""
        select *
        from workout
        where id = ?
        """, id);
        assertThat(row.get("created_by")).isEqualTo(owner);
        assertThat(row.get("type").toString()).isEqualTo("OWN");
        assertThat(row.get("status").toString()).isEqualTo("READY");
        assertThat(database.queryForObject(
            """
            select scheduled_at
            from workout
            where id = ?
            """,
            OffsetDateTime.class, id)
            .toInstant())
            .isEqualTo(OffsetDateTime.parse("2026-11-16T18:30:00+07:00").toInstant());
        assertThat(database.queryForObject(
            """
            select count(*)
            from stage s
            join stage_round r on r.stage_id = s.id
            join round_exercise e on e.round_id = r.id
            where s.workout_id = ?
            """,
            Integer.class, id))
            .isEqualTo(1);
        if (format == WorkoutFormatType.FOR_TIME) {
            assertThat(database.queryForObject(
                """
                select params::text
                from stage
                where workout_id = ?
                """,
                String.class, id))
                .isEqualTo("{}");
        } else {
            assertThat(database.queryForList(
                """
                select (params->>'duration')::integer
                from stage
                where workout_id = ?
                """,
                Integer.class, id))
                .containsExactly(90);
        }
        assertThat(database.queryForObject(
            """
            select count(*)
            from stage
            where workout_id = ? and
            (complex_format_id is not null or time_limit_seconds is not null
            or interval_seconds is not null)
            """,
            Integer.class, id))
            .isZero();
        assertThat(database.queryForObject(
            """
            select count(*)
            from workout_participant
            where workout_id = ?
            """,
            Integer.class, id))
            .isZero();
        assertThat(database.queryForObject(
            """
            select count(*)
            from stage_result result
            join stage s on s.id = result.stage_id
            where s.workout_id = ?
            """,
            Integer.class, id))
            .isZero();
    }

    @Test
    @DisplayName("Порядок и повторы")
    void preservesMultipleStageRoundAndExercisePositionsAndRepeatedOccurrences() throws Exception {
        var request = request(WorkoutFormatType.FOR_TIME);
        request.put("name", "Strength session");
        request.put("description", "Full header");
        request.put("durationMinutes", new BigDecimal("60.5"));
        var stages = request.withArray("stages");
        ObjectNode first = (ObjectNode) stages.get(0);
        first.put("stageFormatId", 1002);
        var round = (ObjectNode) first.withArray("rounds").get(0);
        round.put("repeatCount", 3);
        var exercises = round.withArray("exercises");
        ((ObjectNode) exercises.get(0))
            .put("weightKg", BigDecimal.ZERO)
            .put("durationMinutes", new BigDecimal("0.75"))
            .put("distanceMeters", 400)
            .put("heightCm", 50)
            .put("notes", "First occurrence");
        var repeated = exercises.get(0).deepCopy();
        ((ObjectNode) repeated).put("repetitions", 5).put("weightKg", new BigDecimal("40.25"));
        exercises.add(repeated);
        var secondRound = round.deepCopy();
        secondRound.put("repeatCount", 2);
        ((ObjectNode) secondRound.at("/exercises/0")).put("repetitions", 15);
        first.withArray("rounds").add(secondRound);
        ObjectNode second = first.deepCopy();
        second.put("stageFormatId", 1001).put("format", "EMOM");
        ((ObjectNode) second.get("params")).put("duration", 90).put("interval", 30);
        stages.add(second);
        var response = create(request, token)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("durationMinutes").value(60.5))
            .andExpect(jsonPath("name").value("Strength session"))
            .andExpect(jsonPath("description").value("Full header"))
            .andExpect(jsonPath("stages[0].stageFormatId").value(1002))
            .andExpect(jsonPath("stages[1].stageFormatId").value(1001))
            .andExpect(jsonPath("stages[1].format").value("EMOM"))
            .andExpect(jsonPath("stages[1].position").value(2))
            .andExpect(jsonPath("stages[0].rounds[1].position").value(2))
            .andExpect(jsonPath("stages[0].rounds[1].repeatCount").value(2))
            .andExpect(jsonPath("stages[0].rounds[0].repeatCount").value(3))
            .andExpect(jsonPath("stages[0].rounds[0].exercises[1].parameters.weightKg")
            .value(40.25))
            .andReturn();
        var body = json.readTree(response.getResponse().getContentAsString());
        UUID id = UUID.fromString(body.required("id").asText());
        assertCommittedResponse(body);
        var occurrences = database.queryForList(
            """
            select e.id, e.exercise_id, e.repetitions, e.weight_kg
            from stage s
            join stage_round r on r.stage_id = s.id
            join round_exercise e on e.round_id = r.id
            where s.workout_id = ?
            order by s.position, r.position, e.position
            """,
            id);
        assertThat(occurrences).hasSize(8);
        assertThat(occurrences.stream().map(row -> row.get("id")).distinct()).hasSize(8);
        assertThat(occurrences.stream().map(row -> row.get("exercise_id"))).containsOnly(exercise);
        assertThat(occurrences.stream().map(row -> row.get("repetitions")))
            .containsExactly(10, 5, 15, 5, 10, 5, 15, 5);
    }

    @Test
    @DisplayName("Дробные минуты")
    void acceptsExplicitNullOptionalHeaderAndPreservesFractionalMinutes() throws Exception {
        var request = request(WorkoutFormatType.EMOM);
        request.putNull("name").putNull("description").putNull("durationMinutes");
        ((ObjectNode) request.at("/stages/0/params"))
            .put("duration", new BigDecimal("1.5"))
            .put("interval", new BigDecimal("0.5"));
        var result = create(request, token)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("stages[0].params.duration").value(1.5))
            .andExpect(jsonPath("stages[0].params.interval").value(0.5))
            .andReturn();
        assertCommittedResponse(json.readTree(result.getResponse().getContentAsString()));
    }

    @Test
    @DisplayName("Параметры TABATA")
    void acceptsYaroslavExampleWithLongerInterval() throws Exception {
        var request = request(WorkoutFormatType.TABATA);
        ((ObjectNode) request.at("/stages/0/params")).put("duration", 20).put("interval", 60);
        var result = create(request, token)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("stages[0].params.duration").value(20))
            .andExpect(jsonPath("stages[0].params.interval").value(60))
            .andReturn();
        assertCommittedResponse(json.readTree(result.getResponse().getContentAsString()));
    }

    @Test
    @DisplayName("Описание API")
    void publishesCreateOperationInOpenApi() throws Exception {
        mvc.perform(get("/api/v3/api-docs").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("paths['/v1/workouts'].post.responses['201']").exists())
            .andExpect(jsonPath("paths['/v1/workouts'].post.responses['400']").exists())
            .andExpect(jsonPath("components.schemas.OwnWorkoutRequest.required", hasItem("datetime")))
            .andExpect(jsonPath("components.schemas.OwnWorkoutRequest.required", hasItem("stages")))
            .andExpect(jsonPath("components.schemas.OwnWorkoutRequest.properties.datetime.format")
            .value("date-time"))
            .andExpect(jsonPath("components.schemas.WorkoutErrorResponse.properties.fieldErrors")
            .exists());
    }
}
