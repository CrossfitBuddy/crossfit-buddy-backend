package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ru.nsu.crossfitbuddy.core.entity.MetricsValueType;

public record ExercisesMetricsRequest(
        UUID id,

        @NotBlank(message = "Необходимо указать название метрики") String name,

        @NotNull MetricsValueType metricsValueType,

        @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ ]+$", message = "Допускаются только буквы и пробелы") String unit) {
}
