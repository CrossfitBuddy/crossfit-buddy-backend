package ru.nsu.crossfitbuddy.core.dto.workout;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import ru.nsu.crossfitbuddy.core.dto.workout.json.MinutesDeserializer;
import ru.nsu.crossfitbuddy.core.dto.workout.json.UnsupportedWorkoutFieldException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(
    additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
    description = "Own workout request. Array order is significant; owner, type and status are server-owned."
)
public record OwnWorkoutRequest(
    String name,
    String description,

    @NotNull
    @Schema(description = "Scheduled datetime with a UTC offset, e.g. 2026-11-18T18:00:00+07:00")
    OffsetDateTime datetime,

    @Positive
    @Digits(integer = 9, fraction = 20)
    @JsonDeserialize(using = MinutesDeserializer.class)
    @Schema(description = "Optional duration in minutes", example = "60", minimum = "0", exclusiveMinimum = true)
    BigDecimal durationMinutes,

    @NotEmpty
    List<@NotNull @Valid WorkoutStageRequest> stages
) {
    @JsonAnySetter
    public void rejectUnknownProperty(String name, Object value) {
        throw new UnsupportedWorkoutFieldException(name);
    }
}
