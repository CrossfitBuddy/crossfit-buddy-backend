package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFormatType;

class WorkoutCreationPersistenceIntegrationTest extends AbstractWorkoutCreationIntegrationTest {
    @Test
    @DisplayName("Формат не найден")
    void rejectsSupportedFormatMissingFromTheDictionary() throws Exception {
        var original = database.queryForMap("""
        select *
        from workout_format
        where type = 'TABATA'
        """);
        database.update(
            """
            insert into workout_format(type, name, description, duration_required, interval_required)
            values ('TEST_UNAVAILABLE', 'Test', '', true, true)
            """);
        database.update(
            """
            update stage
            set workout_format_type = 'TEST_UNAVAILABLE'
            where workout_format_type = 'TABATA'
            """);
        database.update("""
        delete
        from workout_format
        where type = 'TABATA'
        """);
        try {
            assertInvalid(request(WorkoutFormatType.TABATA), "stages[0].format");
        } finally {
            database.update(
                """
                insert into workout_format(type, name, description, duration_required, interval_required)
                values ('TABATA', ?, ?, ?, ?)
                """,
                original.get("name"), original.get("description"), original.get("duration_required"),
                original.get("interval_required"));
            database.update(
                """
                update stage
                set workout_format_type = 'TABATA'
                where workout_format_type = 'TEST_UNAVAILABLE'
                """);
            database.update("""
            delete
            from workout_format
            where type = 'TEST_UNAVAILABLE'
            """);
        }
    }

    @Test
    @DisplayName("Откат сохранения")
    void rollsBackAllInsertedRowsWhenDatabaseRejectsAnExerciseBatch() throws Exception {
        var existing = create(request(WorkoutFormatType.FOR_TIME), token)
            .andExpect(status().isCreated())
            .andReturn();
        var existingBody = json.readTree(existing.getResponse().getContentAsString());
        var before = counts();
        database.execute(
            """
            alter table round_exercise add constraint ck_test_reject_note
            check (notes is distinct
            from 'test-persistence-failure')
            """);
        try {
            var request = request(WorkoutFormatType.AMRAP);
            var valid = (ObjectNode) request.at("/stages/0/rounds/0/exercises/0");
            var invalid = valid.deepCopy().put("notes", "test-persistence-failure");
            ((ObjectNode) request.at("/stages/0/rounds/0")).withArray("exercises").add(invalid);
            create(request, token)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("code").value("INTERNAL_ERROR"));
            assertThat(counts()).isEqualTo(before);
            assertCommittedResponse(existingBody);
        } finally {
            database.execute("""
            alter table round_exercise drop constraint ck_test_reject_note
            """);
        }
    }

    @Test
    @DisplayName("Правила справочника")
    void followsDictionaryFlagsForIntervalOnlyFormatAndRejectsEveryExtraKey() throws Exception {
        database.update("""
        update workout_format
        set duration_required = false
        where type = 'EMOM'
        """);
        try {
            var intervalOnly = request(WorkoutFormatType.EMOM);
            ((ObjectNode) intervalOnly.at("/stages/0/params")).remove("duration");
            var result = create(intervalOnly, token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("stages[0].params.duration").doesNotExist())
                .andReturn();
            assertCommittedResponse(json.readTree(result.getResponse().getContentAsString()));
            assertInvalid(request(WorkoutFormatType.EMOM), "stages[0].params.duration");
            ((ObjectNode) intervalOnly.at("/stages/0/params")).put("rounds", 5);
            assertInvalid(intervalOnly, "stages[0].params.rounds");
        } finally {
            database.update(
                """
                update workout_format
                set duration_required = true
                where type = 'EMOM'
                """);
        }
    }
}
