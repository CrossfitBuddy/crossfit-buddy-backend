package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFormatType;

import java.util.UUID;
import java.util.stream.Stream;

class WorkoutCreationValidationIntegrationTest extends AbstractWorkoutCreationIntegrationTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("missingFields")
    @DisplayName("Обязательные поля")
    void rejectsMissingField(WorkoutFormatType format, String pointer, String property, String field)
        throws Exception {
        var request = request(format);
        ((ObjectNode) request.at(pointer)).remove(property);
        assertInvalid(request, field);
    }

    static Stream<Arguments> missingFields() {
        return Stream.of(
            Arguments.of(WorkoutFormatType.AMRAP, "", "datetime", "datetime"),
            Arguments.of(WorkoutFormatType.AMRAP, "/stages/0/params", "duration", "stages[0].params.duration"),
            Arguments.of(WorkoutFormatType.EMOM, "/stages/0/params", "interval", "stages[0].params.interval")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"90\"", "1000000000"})
    @DisplayName("Неверная длительность")
    void rejectsStringOrOutOfRangeMinutes(String value) throws Exception {
        var request = request(WorkoutFormatType.AMRAP);
        request.set("durationMinutes", json.readTree(value));
        var before = counts();
        create(request, token)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("fieldErrors[*].field", hasItem("durationMinutes")));
        assertThat(counts()).isEqualTo(before);
    }

    @Test
    @DisplayName("Неверный формат")
    void rejectsNumericEnumOrdinal() throws Exception {
        var request = request(WorkoutFormatType.AMRAP);
        ((ObjectNode) request.at("/stages/0")).put("format", 1);
        var before = counts();
        create(request, token)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("fieldErrors[*].field", hasItem("stages[0].format")));
        assertThat(counts()).isEqualTo(before);
    }

    @Test
    @DisplayName("Лишние поля")
    void rejectsNestedUnknownPropertiesWithTheirFullPath() throws Exception {
        var request = request(WorkoutFormatType.AMRAP);
        ((ObjectNode) request.at("/stages/0/rounds/0/exercises/0")).put("unexpected", true);
        var before = counts();
        create(request, token)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("fieldErrors[*].field", hasItem("stages[0].rounds[0].exercises[0].unexpected")));
        assertThat(counts()).isEqualTo(before);
    }

    @ParameterizedTest
    @EnumSource(value = WorkoutFormatType.class, names = {"EMOM", "AMRAP", "TABATA"})
    @DisplayName("Обязательная длительность")
    void requiresDurationForSelectedFormat(WorkoutFormatType format) throws Exception {
        var request = request(format);
        ((ObjectNode) request.at("/stages/0/params")).remove("duration");
        assertInvalid(request, "stages[0].params.duration");
    }

    @ParameterizedTest
    @EnumSource(WorkoutFormatType.class)
    @DisplayName("Обязательные раунды")
    void requiresNonEmptyRounds(WorkoutFormatType format) throws Exception {
        var request = request(format);
        ((ObjectNode) request.at("/stages/0")).putArray("rounds");
        assertInvalid(request, "stages[0].rounds");
    }

    @ParameterizedTest
    @EnumSource(value = WorkoutFormatType.class, names = {"EMOM", "TABATA"})
    @DisplayName("Обязательный интервал")
    void requiresIntervalForEveryIntervalFormat(WorkoutFormatType format) throws Exception {
        var request = request(format);
        ((ObjectNode) request.at("/stages/0/params")).remove("interval");
        assertInvalid(request, "stages[0].params.interval");
    }

    @ParameterizedTest
    @EnumSource(value = WorkoutFormatType.class, names = {"AMRAP", "FOR_TIME"})
    @DisplayName("Лишний интервал")
    void rejectsIntervalsForFormatsWithoutIntervals(WorkoutFormatType format) throws Exception {
        var request = request(format);
        ((ObjectNode) request.at("/stages/0/params")).put("interval", 30);
        assertInvalid(request, "stages[0].params.interval");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFields")
    @DisplayName("Неверные значения")
    void rejectsInvalidFieldsWithoutPartialWrites(
        String label, String pointer, String value, String field, String code) throws Exception {
        var request = request(WorkoutFormatType.EMOM);
        int separator = pointer.lastIndexOf('/');
        JsonNode parent = request.at(pointer.substring(0, separator));
        String property = pointer.substring(separator + 1);
        if (parent instanceof ObjectNode object) {
            object.set(property, json.readTree(value));
        } else {
            ((com.fasterxml.jackson.databind.node.ArrayNode) parent)
                .set(Integer.parseInt(property), json.readTree(value));
        }
        var before = counts();
        create(request, token)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value(code))
            .andExpect(jsonPath("path").value(ENDPOINT))
            .andExpect(jsonPath("fieldErrors[*].field", hasItem(field)));
        assertThat(counts()).isEqualTo(before);
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
            invalid("null date", "/datetime", "null", "datetime"),
            malformed("date without offset", "/datetime", "\"2026-11-16T18:30:00\"", "datetime"),
            malformed("invalid date", "/datetime", "\"2026-02-30T18:30:00Z\"", "datetime"),
            invalid("zero total duration", "/durationMinutes", "0", "durationMinutes"),
            invalid("negative total duration", "/durationMinutes", "-1", "durationMinutes"),
            invalid("no stages", "/stages", "[]", "stages"),
            invalid("null stages", "/stages", "null", "stages"),
            invalid("null stage", "/stages/0", "null", "stages[0]"),
            invalid("zero dictionary id", "/stages/0/stageFormatId", "0", "stages[0].stageFormatId"),
            invalid("zero interval", "/stages/0/params/interval", "0", "stages[0].params.interval"),
            invalid("null format", "/stages/0/format", "null", "stages[0].format"),
            invalid("null params", "/stages/0/params", "null", "stages[0].params"),
            malformed("array params", "/stages/0/params", "[]", "stages[0].params"),
            malformed("legacy stage duration", "/stages/0/durationSeconds", "20", "stages[0].durationSeconds"),
            invalid("null duration", "/stages/0/params/duration", "null", "stages[0].params.duration"),
            malformed("string parameter", "/stages/0/params/duration", "\"20\"", "stages[0].params.duration"),
            malformed("unknown format", "/stages/0/format", "\"UNSUPPORTED\"", "stages[0].format"),
            invalid("zero format duration", "/stages/0/params/duration", "0", "stages[0].params.duration"),
            invalid("negative interval", "/stages/0/params/interval", "-1", "stages[0].params.interval"),
            invalid("excess interval precision", "/stages/0/params/interval",
                "0.000000000000000000001", "stages[0].params.interval"),
            invalid("null round", "/stages/0/rounds/0", "null", "stages[0].rounds[0]"),
            invalid("zero repeat count", "/stages/0/rounds/0/repeatCount", "0", "stages[0].rounds[0].repeatCount"),
            invalid("no exercises", "/stages/0/rounds/0/exercises", "[]", "stages[0].rounds[0].exercises"),
            invalid("null exercise", "/stages/0/rounds/0/exercises/0", "null", "stages[0].rounds[0].exercises[0]"),
            invalid("missing exercise id", "/stages/0/rounds/0/exercises/0/exerciseId",
                "null", "stages[0].rounds[0].exercises[0].exerciseId"),
            malformed("invalid exercise uuid", "/stages/0/rounds/0/exercises/0/exerciseId",
                "\"bad-uuid\"", "stages[0].rounds[0].exercises[0].exerciseId"),
            invalidMetric("zero repetitions", "repetitions", "0"),
            invalidMetric("negative load", "weightKg", "-0.01"),
            invalidMetric("excess load precision", "weightKg", "1.001"),
            invalidMetric("load outside database range", "weightKg", "100000"),
            invalidMetric("zero exercise duration", "durationMinutes", "0"),
            invalidMetric("negative distance", "distanceMeters", "-1"),
            invalidMetric("zero height", "heightCm", "0")
        );
    }

    private static Arguments invalidMetric(String label, String metric, String value) {
        return invalid(label, "/stages/0/rounds/0/exercises/0/" + metric, value,
            "stages[0].rounds[0].exercises[0]." + metric);
    }

    private static Arguments invalid(String label, String pointer, String value, String field) {
        return Arguments.of(label, pointer, value, field, "VALIDATION_ERROR");
    }

    private static Arguments malformed(String label, String pointer, String value, String field) {
        return Arguments.of(label, pointer, value, field, "INVALID_REQUEST");
    }

    @Test
    @DisplayName("Ошибка второго этапа")
    void rejectsInvalidLaterStageWithoutPersistingEarlierValidStage() throws Exception {
        var request = request(WorkoutFormatType.AMRAP);
        var invalid = ((ObjectNode) request.at("/stages/0")).deepCopy();
        ((ObjectNode) invalid.at("/rounds/0/exercises/0"))
            .put("exerciseId", UUID.randomUUID().toString());
        request.withArray("stages").add(invalid);
        var before = counts();
        create(request, token)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("fieldErrors[*].field", hasItem("stages[1].rounds[0].exercises[0].exerciseId")))
            .andExpect(jsonPath("fieldErrors[*].code", hasItem("UNKNOWN_REFERENCE")));
        assertThat(counts()).isEqualTo(before);
    }

    @Test
    @DisplayName("Правила FOR_TIME")
    void forTimeRejectsPlannedTimingAndStillRequiresRounds() throws Exception {
        var withDuration = request(WorkoutFormatType.FOR_TIME);
        ((ObjectNode) withDuration.get("stages").get(0).get("params")).put("duration", 10);
        assertInvalid(withDuration, "stages[0].params.duration");
        var withInterval = request(WorkoutFormatType.FOR_TIME);
        ((ObjectNode) withInterval.get("stages").get(0).get("params")).put("interval", 1);
        assertInvalid(withInterval, "stages[0].params.interval");
        var withoutRounds = request(WorkoutFormatType.FOR_TIME);
        ((ObjectNode) withoutRounds.get("stages").get(0)).putArray("rounds");
        assertInvalid(withoutRounds, "stages[0].rounds");
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/xml", "application/octet-stream"})
    @DisplayName("Неверный Content-Type")
    void rejectsUnsupportedContentTypeWithoutWrites(String contentType) throws Exception {
        var before = counts();
        mvc.perform(post(ENDPOINT).contextPath("/api")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(contentType)
                .content(json.writeValueAsBytes(request(WorkoutFormatType.AMRAP))))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("status").value(415))
            .andExpect(jsonPath("code").value("UNSUPPORTED_MEDIA_TYPE"))
            .andExpect(jsonPath("path").value(ENDPOINT));
        assertThat(counts()).isEqualTo(before);
    }
}
