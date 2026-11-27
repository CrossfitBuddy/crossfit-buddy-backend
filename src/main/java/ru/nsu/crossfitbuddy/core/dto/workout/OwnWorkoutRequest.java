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

import java.time.OffsetDateTime;
import java.util.List;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record OwnWorkoutRequest(
    String name,
    String description,
    String coach,

    @NotNull
    OffsetDateTime datetime,

    @Positive
    @JsonDeserialize(using = WholeNumberDeserializer.class)
    Integer durationSeconds,

    @NotEmpty
    List<@NotNull @Valid WorkoutStageRequest> stages
) {
    @JsonAnySetter
    public void rejectUnknownProperty(String name, Object value) {
        throw new UnsupportedWorkoutFieldException(name);
    }
}
