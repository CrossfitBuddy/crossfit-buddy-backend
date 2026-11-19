package ru.nsu.crossfitbuddy.core.dto.workout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class OwnWorkoutContractTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();
    private final ObjectMapper json =
    new ObjectMapper()
        .findAndRegisterModules()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @Test
    @DisplayName("Параметры формата")
    void validatesMapValuesWithoutHardCodingDictionaryRequirements() {
        assertThat(VALIDATOR.validate(request(Map.of("duration", 20, "interval", 60)))).isEmpty();
        assertThat(VALIDATOR.validate(request(Map.of("interval", 60)))).isEmpty();
        assertThat(VALIDATOR.validate(request(Map.of("duration", 0)))).isNotEmpty();
    }

    @Test
    @DisplayName("Обязательные поля")
    void rejectsMissingDateAndNonPositiveOptionalDuration() {
        var source = request(Map.of("duration", 20));
        var request = new OwnWorkoutRequest(null, null, null, BigDecimal.ZERO, source.stages());
        assertThat(VALIDATOR.validate(request).stream().map(v -> v.getPropertyPath().toString()))
            .containsExactlyInAnyOrder("datetime", "durationMinutes");
    }

    @Test
    @DisplayName("Передача параметров")
    void preservesParamsDuringJsonRoundTrip() throws Exception {
        var original = request(Map.of("duration", 20, "interval", 60));
        var restored = json.readValue(json.writeValueAsBytes(original), OwnWorkoutRequest.class);
        assertThat(restored.stages()).isEqualTo(original.stages());
        assertThat(restored.datetime().toInstant()).isEqualTo(original.datetime().toInstant());
    }

    @ParameterizedTest
    @ValueSource(strings = {"createdBy", "created_by", "type", "status", "participants", "coach"})
    @DisplayName("Защита служебных полей")
    void rejectsServerOwnedOrUnagreedPropertiesEvenWithLenientGlobalMapper(String property) {
        assertThatThrownBy(
            () ->
            json.readValue(
            "{\"" + property + "\":\"arbitrary\"}",
            OwnWorkoutRequest.class))
            .isInstanceOf(JsonMappingException.class)
            .hasMessageContaining("Unsupported workout field: " + property);
    }

    @Test
    @DisplayName("Примеры запросов")
    void documentedExamplesMatchTheTypedContract() throws Exception {
        var examples =
        json.readTree(
            Path.of("src/test/resources/workout/own-workout-examples.json").toFile());
        assertThat(examples.size()).isEqualTo(WorkoutFormatType.values().length);
        for (WorkoutFormatType format : WorkoutFormatType.values()) {
            var example = examples.required(format.name());
            var request = json.treeToValue(example.required("request"), OwnWorkoutRequest.class);
            var response = json.treeToValue(example.required("response"), WorkoutResponse.class);
            assertThat(VALIDATOR.validate(request)).as(format.name()).isEmpty();
            assertThat(request.stages().getFirst().format()).isEqualTo(format);
            assertThat(response.stages().getFirst().format()).isEqualTo(format);
            assertThat(response.stages().getFirst().params())
                .isEqualTo(request.stages().getFirst().params());
        }
    }

    private static OwnWorkoutRequest request(Map<String, Integer> params) {
        var exercise =
        new WorkoutExerciseRequest(UUID.randomUUID(), 10, null, null, null, null, null);
        var stage =
        new WorkoutStageRequest(
            1,
            WorkoutFormatType.EMOM,
            params.entrySet().stream()
            .collect(
            java.util.stream.Collectors.toMap(
            Map.Entry::getKey,
            entry -> BigDecimal.valueOf(entry.getValue()))),
            List.of(new WorkoutRoundRequest(1, List.of(exercise))));
        return new OwnWorkoutRequest(
            null,
            null,
            OffsetDateTime.parse("2026-11-16T18:30:00+07:00"),
            null,
            List.of(stage));
    }
}
