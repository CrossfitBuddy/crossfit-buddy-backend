package ru.nsu.crossfitbuddy.core.dto.response.exercises;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;

public record GeneralExerciseResponse(
        UUID id,

        @NotBlank @Size(max = 255) String name,
        @NotBlank String description,
        List<ExercisesMetricsResponse> exercisesMetrics,
        List<ExerciseEquipmentResponse> exerciseEquipments,

        ExerciseCategory category,

        MultipartFile image,
        MultipartFile videoExample

) {
}
