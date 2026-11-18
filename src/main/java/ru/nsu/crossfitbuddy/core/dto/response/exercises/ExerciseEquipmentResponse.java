package ru.nsu.crossfitbuddy.core.dto.response.exercises;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;

public record ExerciseEquipmentResponse(
        UUID id,

        @NotBlank String name,

        UUID imageFileId,

        List<EquipmentPropertiesResponse> equipmentProperties) {
}
