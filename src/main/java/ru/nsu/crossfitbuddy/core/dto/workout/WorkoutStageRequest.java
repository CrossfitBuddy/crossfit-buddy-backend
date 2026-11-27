package ru.nsu.crossfitbuddy.core.dto.workout;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.nsu.crossfitbuddy.core.dto.workout.json.UnsupportedWorkoutFieldException;
import ru.nsu.crossfitbuddy.core.dto.workout.json.WholeNumberDeserializer;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;

import java.util.List;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record WorkoutStageRequest(
    @NotNull
    @Positive
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer workoutStageId,

    @NotNull
    ComplexFormatType format,

    @Positive
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer timeLimitSeconds,

    @Positive
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer intervalSeconds,

    @Positive
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer targetRounds,

    @NotEmpty
    List<@NotNull @Valid WorkoutRoundRequest> rounds
) {
    @JsonAnySetter
    public void rejectUnknownProperty(String name, Object value) {
        throw new UnsupportedWorkoutFieldException(name);
    }
}
