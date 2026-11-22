package ru.nsu.crossfitbuddy.core.dto.response.exercises;

import java.util.UUID;
import ru.nsu.crossfitbuddy.core.entity.MetricsValueType;

public record EquipmentPropertiesResponse(
    UUID id,
    String name,
    MetricsValueType metricsValueType,
    String unit
) {}
