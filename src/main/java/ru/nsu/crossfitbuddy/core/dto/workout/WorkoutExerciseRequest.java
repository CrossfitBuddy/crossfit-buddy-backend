package ru.nsu.crossfitbuddy.core.dto.workout;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import ru.nsu.crossfitbuddy.core.dto.workout.json.MinutesDeserializer;
import ru.nsu.crossfitbuddy.core.dto.workout.json.UnsupportedWorkoutFieldException;
import ru.nsu.crossfitbuddy.core.dto.workout.json.WholeNumberDeserializer;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
    description = "A separate exercise occurrence. Metrics are optional."
)
public record WorkoutExerciseRequest(
    @NotNull
    UUID exerciseId,

    @Positive
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer repetitions,

    @DecimalMin("0.00")
    @Digits(integer = 5, fraction = 2)
    BigDecimal weightKg,

    @Positive
    @Digits(integer = 9, fraction = 20)
    @JsonDeserialize(using = MinutesDeserializer.class)
    @Schema(description = "Exercise duration in minutes; fractional values are supported")
    BigDecimal durationMinutes,

    @Positive
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer distanceMeters,

    @Positive
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer heightCm,

    String notes
) {
    @JsonAnySetter
    public void rejectUnknownProperty(String name, Object value) {
        throw new UnsupportedWorkoutFieldException(name);
    }
}
