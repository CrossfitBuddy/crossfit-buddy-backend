package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;

public record GeneralExerciseRequest(

        @NotBlank(message = "Необходимо указать название упражнения") @Size(max = 255, message = "Длинна названия упражнения не должна превышать 255 символов") String name,
        @NotBlank(message = "Необходимо указать описание упражнения") String description,
        @Valid List<ExercisesMetricsRequest> exercisesMetrics,
        @Valid List<ExerciseEquipmentRequest> exercisesEquipments,

        @NotNull(message = "Необходимо указать категорию упражнения") ExerciseCategory category,

        MultipartFile image,
        MultipartFile videoExample

) {
}
