package ru.nsu.crossfitbuddy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;

import java.util.UUID;
import java.util.stream.Stream;

class WorkoutCreationValidationIntegrationTest extends AbstractWorkoutCreationIntegrationTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFields")
    @DisplayName("Некорректное поле")
    void rejectsInvalidField(String label, String pointer, String value, String field, String code) throws Exception {
        var request = request(ComplexFormatType.EMOM);
        int separator = pointer.lastIndexOf('/');
        var parent = request.at(pointer.substring(0, separator));
        var property = pointer.substring(separator + 1);
        if (parent instanceof ObjectNode object) {
            object.set(property, json.readTree(value));
        } else {
            ((ArrayNode) parent).set(Integer.parseInt(property), json.readTree(value));
        }
        var before = counts();
        create(request, token).andExpect(status().isBadRequest())
            .andExpect(jsonPath("code").value(code))
            .andExpect(jsonPath("path").value(ENDPOINT))
            .andExpect(jsonPath("fieldErrors[*].field", hasItem(field)));
        assertThat(counts()).isEqualTo(before);
    }

    static Stream<Arguments> invalidFields() {
        return Stream.of(
            Arguments.of("Дата обязательна", "/datetime",
                "null", "datetime", "VALIDATION_ERROR"),
            Arguments.of("Дата без зоны", "/datetime",
                "\"2026-11-20T18:00:00\"", "datetime", "INVALID_REQUEST"),
            Arguments.of("Несуществующая дата", "/datetime",
                "\"2026-02-30T18:00:00Z\"", "datetime", "INVALID_REQUEST"),
            Arguments.of("Нулевая длительность", "/durationSeconds",
                "0", "durationSeconds", "VALIDATION_ERROR"),
            Arguments.of("Отрицательная длительность", "/durationSeconds",
                "-1", "durationSeconds", "VALIDATION_ERROR"),
            Arguments.of("Строка вместо времени", "/durationSeconds",
                "\"60\"", "durationSeconds", "INVALID_REQUEST"),
            Arguments.of("Дробные секунды", "/durationSeconds",
                "0.5", "durationSeconds", "INVALID_REQUEST"),
            Arguments.of("Переполнение времени", "/durationSeconds",
                "2147483648", "durationSeconds", "INVALID_REQUEST"),
            Arguments.of("Пустые этапы", "/stages",
                "[]", "stages", "VALIDATION_ERROR"),
            Arguments.of("Отсутствуют этапы", "/stages",
                "null", "stages", "VALIDATION_ERROR"),
            Arguments.of("Пустой элемент этапов", "/stages/0",
                "null", "stages[0]", "VALIDATION_ERROR"),
            Arguments.of("Неверный ID стадии", "/stages/0/workoutStageId",
                "0", "stages[0].workoutStageId", "VALIDATION_ERROR"),
            Arguments.of("Формат обязателен", "/stages/0/format",
                "null", "stages[0].format", "VALIDATION_ERROR"),
            Arguments.of("Неизвестный формат", "/stages/0/format",
                "\"TABATA\"", "stages[0].format", "INVALID_REQUEST"),
            Arguments.of("Число вместо формата", "/stages/0/format",
                "1", "stages[0].format", "INVALID_REQUEST"),
            Arguments.of("Старый params", "/stages/0/params",
                "{}", "stages[0].params", "INVALID_REQUEST"),
            Arguments.of("Нулевой лимит", "/stages/0/timeLimitSeconds",
                "0", "stages[0].timeLimitSeconds", "VALIDATION_ERROR"),
            Arguments.of("Строка вместо интервала", "/stages/0/intervalSeconds",
                "\"60\"", "stages[0].intervalSeconds", "INVALID_REQUEST"),
            Arguments.of("Нулевой интервал", "/stages/0/intervalSeconds",
                "0", "stages[0].intervalSeconds", "VALIDATION_ERROR"),
            Arguments.of("Дробный интервал", "/stages/0/intervalSeconds",
                "1.5", "stages[0].intervalSeconds", "INVALID_REQUEST"),
            Arguments.of("Пустые раунды", "/stages/0/rounds",
                "[]", "stages[0].rounds", "VALIDATION_ERROR"),
            Arguments.of("Пустой элемент раундов", "/stages/0/rounds/0",
                "null", "stages[0].rounds[0]", "VALIDATION_ERROR"),
            Arguments.of("Нулевой повтор", "/stages/0/rounds/0/repeatCount",
                "0", "stages[0].rounds[0].repeatCount", "VALIDATION_ERROR"),
            Arguments.of("Дробный повтор", "/stages/0/rounds/0/repeatCount",
                "1.5", "stages[0].rounds[0].repeatCount", "INVALID_REQUEST"),
            Arguments.of("Пустые упражнения", "/stages/0/rounds/0/exercises",
                "[]", "stages[0].rounds[0].exercises", "VALIDATION_ERROR"),
            Arguments.of("Пустое упражнение", "/stages/0/rounds/0/exercises/0",
                "null", "stages[0].rounds[0].exercises[0]", "VALIDATION_ERROR"),
            Arguments.of("Отсутствует ID упражнения", "/stages/0/rounds/0/exercises/0/exerciseId",
                "null", "stages[0].rounds[0].exercises[0].exerciseId", "VALIDATION_ERROR"),
            Arguments.of("Некорректный UUID", "/stages/0/rounds/0/exercises/0/exerciseId",
                "\"bad-uuid\"", "stages[0].rounds[0].exercises[0].exerciseId", "INVALID_REQUEST"),
            Arguments.of("Нулевые повторения", "/stages/0/rounds/0/exercises/0/repetitions",
                "0", "stages[0].rounds[0].exercises[0].repetitions", "VALIDATION_ERROR"),
            Arguments.of("Отрицательный вес", "/stages/0/rounds/0/exercises/0/weightKg",
                "-0.01", "stages[0].rounds[0].exercises[0].weightKg", "VALIDATION_ERROR"),
            Arguments.of("Точность веса", "/stages/0/rounds/0/exercises/0/weightKg",
                "1.001", "stages[0].rounds[0].exercises[0].weightKg", "VALIDATION_ERROR"),
            Arguments.of("Диапазон веса", "/stages/0/rounds/0/exercises/0/weightKg",
                "100000", "stages[0].rounds[0].exercises[0].weightKg", "VALIDATION_ERROR"),
            Arguments.of("Нулевое время упражнения", "/stages/0/rounds/0/exercises/0/durationSeconds",
                "0", "stages[0].rounds[0].exercises[0].durationSeconds", "VALIDATION_ERROR"),
            Arguments.of("Дробное время упражнения", "/stages/0/rounds/0/exercises/0/durationSeconds",
                "0.5", "stages[0].rounds[0].exercises[0].durationSeconds", "INVALID_REQUEST"),
            Arguments.of("Отрицательная дистанция", "/stages/0/rounds/0/exercises/0/distanceMeters",
                "-1", "stages[0].rounds[0].exercises[0].distanceMeters", "VALIDATION_ERROR"),
            Arguments.of("Нулевая высота", "/stages/0/rounds/0/exercises/0/heightCm",
                "0", "stages[0].rounds[0].exercises[0].heightCm", "VALIDATION_ERROR"),
            Arguments.of("Лишнее поле", "/stages/0/rounds/0/exercises/0/unexpected",
                "true", "stages[0].rounds[0].exercises[0].unexpected", "INVALID_REQUEST")
        );
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("requiredParameters")
    @DisplayName("Обязательный параметр формата")
    void requiresFormatParameter(ComplexFormatType format, String field) throws Exception {
        var request = request(format);
        ((ObjectNode) request.at("/stages/0")).remove(field);
        var before = counts();
        create(request, token).andExpect(status().isBadRequest())
            .andExpect(jsonPath("fieldErrors[*].field", hasItem("stages[0]." + field)))
            .andExpect(jsonPath("fieldErrors[*].code", hasItem("REQUIRED_PARAMETER")));
        assertThat(counts()).isEqualTo(before);
    }

    static Stream<Arguments> requiredParameters() {
        return Stream.of(
            Arguments.of(ComplexFormatType.AMRAP, "timeLimitSeconds"),
            Arguments.of(ComplexFormatType.EMOM, "timeLimitSeconds"),
            Arguments.of(ComplexFormatType.EMOM, "intervalSeconds"),
            Arguments.of(ComplexFormatType.FOR_TIME, "targetRounds")
        );
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("forbiddenParameters")
    @DisplayName("Лишний параметр формата")
    void rejectsForbiddenFormatParameter(ComplexFormatType format, String field) throws Exception {
        var request = request(format);
        ((ObjectNode) request.at("/stages/0")).put(field, 1);
        var before = counts();
        create(request, token).andExpect(status().isBadRequest())
            .andExpect(jsonPath("fieldErrors[*].field", hasItem("stages[0]." + field)))
            .andExpect(jsonPath("fieldErrors[*].code", hasItem("UNSUPPORTED_PARAMETER")));
        assertThat(counts()).isEqualTo(before);
    }

    static Stream<Arguments> forbiddenParameters() {
        return Stream.of(
            Arguments.of(ComplexFormatType.AMRAP, "intervalSeconds"),
            Arguments.of(ComplexFormatType.AMRAP, "targetRounds"),
            Arguments.of(ComplexFormatType.EMOM, "targetRounds"),
            Arguments.of(ComplexFormatType.FOR_TIME, "timeLimitSeconds"),
            Arguments.of(ComplexFormatType.FOR_TIME, "intervalSeconds"),
            Arguments.of(ComplexFormatType.FREE, "timeLimitSeconds"),
            Arguments.of(ComplexFormatType.FREE, "intervalSeconds"),
            Arguments.of(ComplexFormatType.FREE, "targetRounds")
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("Неверное количество раундов")
    void rejectsNonPositiveTargetRounds(int value) throws Exception {
        var request = request(ComplexFormatType.FOR_TIME);
        ((ObjectNode) request.at("/stages/0")).put("targetRounds", value);
        assertInvalid(request, "stages[0].targetRounds");
    }

    @Test
    @DisplayName("Ошибка второго комплекса")
    void rejectsLaterInvalidReferenceWithoutWrites() throws Exception {
        var request = request(ComplexFormatType.AMRAP);
        var invalid = ((ObjectNode) request.at("/stages/0")).deepCopy();
        ((ObjectNode) invalid.at("/rounds/0/exercises/0")).put("exerciseId", UUID.randomUUID().toString());
        request.withArray("stages").add(invalid);
        var before = counts();
        create(request, token).andExpect(status().isBadRequest())
            .andExpect(jsonPath("fieldErrors[*].field", hasItem("stages[1].rounds[0].exercises[0].exerciseId")));
        assertThat(counts()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/plain", "application/xml", "application/octet-stream"})
    @DisplayName("Неверный Content-Type")
    void rejectsUnsupportedMediaType(String type) throws Exception {
        var before = counts();
        mvc.perform(post(ENDPOINT).contextPath("/api")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(type)
                .content(json.writeValueAsBytes(request(ComplexFormatType.AMRAP))))
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("code").value("UNSUPPORTED_MEDIA_TYPE"));
        assertThat(counts()).isEqualTo(before);
    }
}
