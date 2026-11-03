package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;

public record GeneralExerciseRequest(

        @NotBlank @Size(max = 255) String name,
        @NotBlank String description,
        List<ExercisesMetricsRequest> exercisesMetrics,
        List<ExerciseEquipmentRequest> exercisesEquipments,

        ExerciseCategory category,

        MultipartFile image,
        MultipartFile videoExample

) {
}
