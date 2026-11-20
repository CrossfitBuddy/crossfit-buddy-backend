package ru.nsu.crossfitbuddy.core.workout.model;

import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFormatType;
import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorkoutToCreate(
    UUID id,
    UUID createdBy,
    String name,
    String description,
    OffsetDateTime datetime,
    BigDecimal durationMinutes,
    WorkoutType type,
    WorkoutStatus status,
    List<Stage> stages
) {
    public WorkoutToCreate {
        stages = List.copyOf(stages);
    }

    public record Stage(
        UUID id,
        int position,
        int stageFormatId,
        WorkoutFormatType format,
        Map<String, BigDecimal> params,
        List<Round> rounds
    ) {
        public Stage {
            params = Map.copyOf(params);
            rounds = List.copyOf(rounds);
        }
    }

    public record Round(UUID id, int position, int repeatCount, List<Exercise> exercises) {
        public Round {
            exercises = List.copyOf(exercises);
        }
    }

    public record Exercise(
        UUID id,
        int position,
        UUID exerciseId,
        Integer repetitions,
        BigDecimal weightKg,
        BigDecimal durationMinutes,
        Integer distanceMeters,
        Integer heightCm,
        String notes
    ) {}
}
