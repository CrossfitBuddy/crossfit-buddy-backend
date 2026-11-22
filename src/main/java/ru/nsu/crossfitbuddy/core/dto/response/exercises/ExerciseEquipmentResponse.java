package ru.nsu.crossfitbuddy.core.dto.response.exercises;

import java.util.List;
import java.util.UUID;

public record ExerciseEquipmentResponse(
    UUID id,
    String name,
    String imageUrl,
    List<EquipmentPropertiesResponse> equipmentProperties
) {}
