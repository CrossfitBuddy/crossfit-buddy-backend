package ru.nsu.crossfitbuddy.core.dto.workout;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Workout creation response and proposed detail contract")
public record WorkoutResponse(
    UUID id,
    String name,
    String description,
    OffsetDateTime datetime,
    BigDecimal durationMinutes,
    UUID createdBy,
    WorkoutType type,
    WorkoutStatus status,
    List<Stage> stages
) {

@Schema(name = "WorkoutStageResponse")
    public record Stage(
        UUID id,
        int position,
        int stageFormatId,
        WorkoutFormatType format,
        Map<String, BigDecimal> params,
        List<Round> rounds
    ) {}

    @Schema(name = "WorkoutRoundResponse")
    public record Round(
        UUID id,
        int position,
        int repeatCount,
        List<Exercise> exercises
    ) {}

    @Schema(name = "WorkoutExerciseResponse")
    public record Exercise(
        UUID id,
        int position,
        WorkoutExerciseRequest parameters
    ) {}
}
