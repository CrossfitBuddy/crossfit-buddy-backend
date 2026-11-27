package ru.nsu.crossfitbuddy.core.dto.workout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;

class OwnWorkoutContractTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @ParameterizedTest
    @ValueSource(strings = {"createdBy", "created_by", "type", "status", "participants", "durationMinutes"})
    @DisplayName("Служебные и старые поля")
    void rejectsUnsupportedRootFields(String property) {
        assertThatThrownBy(() -> json.readValue("{\"" + property + "\":null}", OwnWorkoutRequest.class))
            .isInstanceOf(JsonMappingException.class)
            .hasMessageContaining("Unsupported workout field: " + property);
    }

    @Test
    @DisplayName("Примеры запросов")
    void fixturesMatchRequestAndResponse() throws Exception {
        try (var stream = getClass().getResourceAsStream("/workout/own-workout-examples.json");
             var factory = Validation.buildDefaultValidatorFactory()) {
            var examples = json.readTree(stream);
            assertThat(examples.size()).isEqualTo(ComplexFormatType.values().length);
            for (var format : ComplexFormatType.values()) {
                var example = examples.required(format.name());
                var request = json.treeToValue(example.required("request"), OwnWorkoutRequest.class);
                var response = json.treeToValue(example.required("response"), WorkoutResponse.class);
                assertThat(factory.getValidator().validate(request)).as(format.name()).isEmpty();
                assertThat(request.stages().getFirst().format()).isEqualTo(format);
                assertThat(response.stages().getFirst().format()).isEqualTo(format);
                var restored = json.readValue(json.writeValueAsBytes(request), OwnWorkoutRequest.class);
                assertThat(restored).isEqualTo(request);
            }
        }
    }
}
