package ru.nsu.crossfitbuddy.core.dto.response.exercises;

import java.util.List;
import java.util.UUID;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;

public record GeneralExerciseResponse(
    UUID id,
    String name,
    String description,
    List<ExercisesMetricsResponse> exercisesMetrics,
    List<ExerciseEquipmentResponse> exerciseEquipments,
    ExerciseCategory category,
    String imageUrl,
    String videoUrl
) {}
