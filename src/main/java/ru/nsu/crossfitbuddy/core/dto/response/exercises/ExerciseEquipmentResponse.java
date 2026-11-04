package ru.nsu.crossfitbuddy.core.dto.response.exercises;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;

public record ExerciseEquipmentResponse(
        UUID id,

        @NotBlank String name,
        MultipartFile image,
        List<EquipmentPropertiesResponse> equipmentProperties

) {
}
