package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import ru.nsu.crossfitbuddy.core.entity.MetricsValueType;

public record EquipmentPropertiesRequest(
        @NotBlank String name,

        @NotBlank MetricsValueType metricsValueType,

        @NotBlank @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ ]+$", message = "Допускаются только буквы и пробелы") String unit,

        @NotBlank float value

) {
}
