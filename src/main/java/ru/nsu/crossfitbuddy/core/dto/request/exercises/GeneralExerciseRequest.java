package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeneralExerciseRequest(

        @NotBlank @Size(max = 255) String name,
        @NotBlank String description,
        List<ExercisesMetricsRequest> exercises,
        MultipartFile image

) {
}
