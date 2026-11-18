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
import ru.nsu.crossfitbuddy.core.dto.workout.json.WholeNumberDeserializer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record WorkoutStageRequest(
    @NotNull
    @Positive
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    @Schema(description = "Existing stage_format dictionary ID")
    Integer stageFormatId,

    @NotNull
    @Schema(description = "Existing workout_format.type")
    WorkoutFormatType format,

    @NotNull
    @JsonDeserialize(contentUsing = MinutesDeserializer.class)
    @Schema(
        description = "Format parameters in minutes. Required and allowed keys follow workout_format flags.",
        example = "{\"duration\":10,\"interval\":0.5}"
    )
    Map<String, @NotNull @Positive @Digits(integer = 9, fraction = 20) BigDecimal> params,

    @NotEmpty
    List<@NotNull @Valid WorkoutRoundRequest> rounds
) {
    @JsonAnySetter
    public void rejectUnknownProperty(String name, Object value) {
        throw new UnsupportedWorkoutFieldException(name);
    }
}
