package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;

class WorkoutCreationPersistenceIntegrationTest extends AbstractWorkoutCreationIntegrationTest {
    @Test
    @DisplayName("Откат сохранения")
    void rollsBackAggregateWhenSecondExerciseFails() throws Exception {
        var saved = create(request(ComplexFormatType.FOR_TIME), token)
            .andExpect(status().isCreated()).andReturn();
        var savedBody = json.readTree(saved.getResponse().getContentAsString());
        var before = counts();
        database.execute("""
            alter table round_exercise add constraint ck_test_reject_note
            check (notes is distinct from 'test-persistence-failure')
            """);
        try {
            var request = request(ComplexFormatType.AMRAP);
            var round = (ObjectNode) request.at("/stages/0/rounds/0");
            var invalid = ((ObjectNode) round.at("/exercises/0")).deepCopy()
                .put("notes", "test-persistence-failure");
            round.withArray("exercises").add(invalid);
            create(request, token).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("code").value("INTERNAL_ERROR"));
            assertThat(counts()).isEqualTo(before);
            assertCommittedResponse(savedBody);
        } finally {
            database.execute("alter table round_exercise drop constraint ck_test_reject_note");
        }
    }

    @Test
    @DisplayName("Формат отсутствует в справочнике")
    void rejectsMissingDictionaryEntry() throws Exception {
        var original = database.queryForMap("select * from complex_format where type = 'FREE'");
        database.update("insert into complex_format(type, name, description) values ('TEST_FREE', 'Test', '')");
        database.update("update complex set complex_format_type = 'TEST_FREE' where complex_format_type = 'FREE'");
        database.update("delete from complex_format where type = 'FREE'");
        try {
            var before = counts();
            create(request(ComplexFormatType.FREE), token).andExpect(status().isBadRequest())
                .andExpect(jsonPath("fieldErrors[*].field", hasItem("stages[0].format")))
                .andExpect(jsonPath("fieldErrors[*].code", hasItem("UNKNOWN_REFERENCE")));
            assertThat(counts()).isEqualTo(before);
        } finally {
            database.update("insert into complex_format(type, name, description) values ('FREE', ?, ?)",
                original.get("name"), original.get("description"));
            database.update("update complex set complex_format_type = 'FREE' where complex_format_type = 'TEST_FREE'");
            database.update("delete from complex_format where type = 'TEST_FREE'");
        }
    }
}
