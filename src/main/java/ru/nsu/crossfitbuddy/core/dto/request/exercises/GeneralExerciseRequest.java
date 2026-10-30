package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GeneralExerciseRequest(

        @NotBlank @Size(max = 255) String name,
        @NotBlank String description,
        List<ExercisesMetricsRequest> exercises

) {
}
