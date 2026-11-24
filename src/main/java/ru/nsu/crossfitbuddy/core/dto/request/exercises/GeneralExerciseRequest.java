package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Singular;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;

@Builder
public record GeneralExerciseRequest(

    UUID id,

    @NotBlank(message = "Необходимо указать название упражнения") @Size(max = 255, message = "Длинна названия упражнения не должна превышать 255 символов") String name,
    @NotBlank(message = "Необходимо указать описание упражнения") String description,
    @Singular @Valid List<ExercisesMetricsRequest> exercisesMetrics,
    @Singular @Valid List<ExerciseEquipmentRequest> exercisesEquipments,

    @NotNull(message = "Необходимо указать категорию упражнения") ExerciseCategory category,

    UUID imageFileId,
    UUID videoFileId) {
}
