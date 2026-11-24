package ru.nsu.crossfitbuddy.core.dto.workout;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record WorkoutResponse(
    UUID id,
    String name,
    String description,
    String coach,
    OffsetDateTime datetime,
    Integer durationSeconds,
    UUID createdBy,
    WorkoutType type,
    WorkoutStatus status,
    List<Stage> stages
) {
    @Schema(name = "WorkoutStageResponse")
    public record Stage(
        UUID id,
        int position,
        int workoutStageId,
        ComplexFormatType format,
        Integer timeLimitSeconds,
        Integer intervalSeconds,
        Integer targetRounds,
        List<Round> rounds
    ) {}

    @Schema(name = "WorkoutRoundResponse")
    public record Round(UUID id, int position, int repeatCount, List<Exercise> exercises) {}

    @Schema(name = "WorkoutExerciseResponse")
    public record Exercise(UUID id, int position, WorkoutExerciseRequest parameters) {}
}
