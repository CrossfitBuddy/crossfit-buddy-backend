package ru.nsu.crossfitbuddy.core.dto.response.exercises;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import ru.nsu.crossfitbuddy.core.entity.MetricsValueType;

public record EquipmentPropertiesResponse(
        UUID id,

        @NotBlank String name,

        @NotBlank MetricsValueType metricsValueType,

        @NotBlank @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ ]+$", message = "Допускаются только буквы и пробелы") String unit

) {
}
