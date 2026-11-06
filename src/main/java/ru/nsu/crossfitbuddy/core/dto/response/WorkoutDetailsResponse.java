package ru.nsu.crossfitbuddy.core.dto.response;

import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;

import java.util.List;
import java.util.UUID;

public record WorkoutDetailsResponse(
    UUID id,
    String title,
    String description,
    Integer durationSec,
    WorkoutStatus status,
    List<StageResponse> stages,
    List<WorkoutMaterialResponse> materials
){}
