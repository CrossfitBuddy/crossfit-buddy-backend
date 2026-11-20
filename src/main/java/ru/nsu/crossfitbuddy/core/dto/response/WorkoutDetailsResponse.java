package ru.nsu.crossfitbuddy.core.dto.response;

import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;

import java.util.List;
import java.util.UUID;

public record WorkoutDetailsResponse(
    UUID id,
    String title,
    String description,
    WorkoutType type,
    WorkoutStatus status,
    Integer durationSec,
    List<ComplexResponse> complexes,
    List<WorkoutMaterialResponse> materials
){}
