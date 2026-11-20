package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import ru.nsu.crossfitbuddy.core.entity.MetricsValueType;

public record EquipmentPropertiesRequest(
        @NotBlank(message = "Необходимо указать название свойства оборудования") String name,

        @NotNull(message = "Необходимо указать категорию") MetricsValueType metricsValueType,

        @NotBlank(message = " Необходимо указать еденицу измерения") @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ ]+$", message = "Допускаются только буквы и пробелы") String unit

) {
}
