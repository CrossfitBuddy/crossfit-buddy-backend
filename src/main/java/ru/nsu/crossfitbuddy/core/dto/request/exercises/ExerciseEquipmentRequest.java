package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;

public record ExerciseEquipmentRequest(
        @NotBlank String name,
        MultipartFile image,
        List<EquipmentPropertiesRequest> propertiesRequests

) {
}
