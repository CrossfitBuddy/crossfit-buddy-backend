package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpHeaders;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;

import java.math.BigDecimal;
import java.util.UUID;

class WorkoutCreationIntegrationTest extends AbstractWorkoutCreationIntegrationTest {
    @ParameterizedTest
    @EnumSource(ComplexFormatType.class)
    @DisplayName("Создание формата")
    void createsAndReadsCommittedWorkout(ComplexFormatType format) throws Exception {
        var request = request(format);
        request.put("name", "Тренировка").put("description", "Описание")
            .put("coach", "Иван Иванов").put("durationSeconds", 1800);
        var result = create(request, token)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("createdBy").value(owner.toString()))
            .andExpect(jsonPath("type").value("OWN"))
            .andExpect(jsonPath("status").value("READY"))
            .andExpect(jsonPath("coach").value("Иван Иванов"))
            .andExpect(jsonPath("durationSeconds").value(1800))
            .andExpect(jsonPath("stages[0].format").value(format.name()))
            .andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        assertCommittedResponse(body);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
            .endsWith(ENDPOINT + "/" + body.required("id").asText());
        String id = body.required("id").asText();
        String item = "items[?(@.id == '" + id + "')]";
        mvc.perform(get(ENDPOINT).contextPath("/api")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath(item + ".source", contains("OWN")))
            .andExpect(jsonPath(item + ".status", contains("READY")));
        assertThat(database.queryForObject("select count(*) from workout_participant where workout_id = ?",
            Integer.class, UUID.fromString(body.required("id").asText()))).isZero();
    }

    @Test
    @DisplayName("Из каталога в карточку")
    void createsWorkoutWithCatalogExerciseAndReadsCard() throws Exception {
        database.update("update exercise set is_common = true where id = ?", exercise);
        var catalog = mvc.perform(get("/api/v1/general-exercises/catalog").contextPath("/api")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("name", "Own squat"))
            .andExpect(status().isOk()).andReturn();
        var items = json.readTree(catalog.getResponse().getContentAsString());
        String catalogId = null;
        for (var item : items) {
            if (exercise.toString().equals(item.path("id").asText())) {
                catalogId = item.required("id").asText();
                break;
            }
        }
        assertThat(catalogId).isNotNull();

        var request = request(ComplexFormatType.EMOM).put("name", "Карточка");
        ((ObjectNode) request.at("/stages/0/rounds/0/exercises/0")).put("exerciseId", catalogId);
        var result = create(request, token).andExpect(status().isCreated()).andReturn();
        var id = json.readTree(result.getResponse().getContentAsString()).required("id").asText();
        mvc.perform(get(ENDPOINT + "/" + id).contextPath("/api")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("id").value(id))
            .andExpect(jsonPath("title").value("Карточка"))
            .andExpect(jsonPath("durationSec").value(1200))
            .andExpect(jsonPath("complexes[0].complexFormat").value("EMOM"))
            .andExpect(jsonPath("complexes[0].intervalSeconds").value(60))
            .andExpect(jsonPath("complexes[0].rounds[0].repeatCount").value(1))
            .andExpect(jsonPath("complexes[0].rounds[0].exercises[0].id").value(catalogId));
    }

    @Test
    @DisplayName("Порядок и отдельные вхождения")
    void preservesOrderMetricsAndRepeatedExercises() throws Exception {
        var request = request(ComplexFormatType.FOR_TIME);
        var first = (ObjectNode) request.at("/stages/0");
        first.put("workoutStageId", 1002).put("targetRounds", 5);
        var round = (ObjectNode) first.at("/rounds/0");
        round.put("repeatCount", 3);
        var metric = (ObjectNode) round.at("/exercises/0");
        metric.put("weightKg", BigDecimal.ZERO).put("durationSeconds", 45)
            .put("distanceMeters", 400).put("heightCm", 50).put("notes", "Первое вхождение");
        round.withArray("exercises").add(metric.deepCopy().put("repetitions", 5)
            .put("weightKg", new BigDecimal("40.25")));
        var secondRound = round.deepCopy().put("repeatCount", 2);
        ((ObjectNode) secondRound.at("/exercises/0")).put("repetitions", 15);
        first.withArray("rounds").add(secondRound);
        var second = first.deepCopy().put("workoutStageId", 1001).put("format", "EMOM");
        second.remove("targetRounds");
        second.put("timeLimitSeconds", 1200).put("intervalSeconds", 60);
        request.withArray("stages").add(second);
        var result = create(request, token)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("stages[0].targetRounds").value(5))
            .andExpect(jsonPath("stages[0].rounds[0].repeatCount").value(3))
            .andExpect(jsonPath("stages[0].rounds[1].repeatCount").value(2))
            .andExpect(jsonPath("stages[1].position").value(2)).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        assertCommittedResponse(body);
        var rows = database.queryForList("""
            select e.id, e.exercise_id, e.repetitions
            from complex c join complex_round r on r.complex_id = c.id
            join round_exercise e on e.round_id = r.id
            where c.workout_id = ? order by c.position, r.position, e.position
            """, UUID.fromString(body.required("id").asText()));
        assertThat(rows).hasSize(8);
        assertThat(rows.stream().map(row -> row.get("id")).distinct()).hasSize(8);
        assertThat(rows.stream().map(row -> row.get("exercise_id"))).containsOnly(exercise);
        assertThat(rows.stream().map(row -> row.get("repetitions")))
            .containsExactly(10, 5, 15, 5, 10, 5, 15, 5);
    }

    @Test
    @DisplayName("Необязательные поля")
    void acceptsAbsentAndExplicitNullOptionalFields() throws Exception {
        var request = request(ComplexFormatType.FREE);
        request.putNull("name").putNull("description").putNull("coach").putNull("durationSeconds");
        var result = create(request, token).andExpect(status().isCreated())
            .andExpect(jsonPath("coach").doesNotExist()).andReturn();
        assertCommittedResponse(json.readTree(result.getResponse().getContentAsString()));
    }

    @Test
    @DisplayName("Схема API")
    void exposesSecondsAndExplicitFormatFields() throws Exception {
        mvc.perform(get("/api/v3/api-docs").contextPath("/api"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("paths['/v1/workouts'].post.responses['201']").exists())
            .andExpect(jsonPath("paths['/v1/workouts'].post.responses['400']").exists())
            .andExpect(jsonPath("components.schemas.OwnWorkoutRequest.properties.datetime.format").value("date-time"))
            .andExpect(jsonPath("components.schemas.OwnWorkoutRequest.properties.durationSeconds").exists())
            .andExpect(jsonPath("components.schemas.WorkoutStageRequest.properties.params").doesNotExist())
            .andExpect(jsonPath("components.schemas.WorkoutStageRequest.properties.targetRounds").exists());
    }
}
