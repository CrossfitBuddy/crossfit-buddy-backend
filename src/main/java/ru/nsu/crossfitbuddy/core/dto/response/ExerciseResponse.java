package ru.nsu.crossfitbuddy.core.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record ExerciseResponse(
    UUID id,
    String name,
    int position,
    Integer repetitions,
    BigDecimal weightKg,
    Integer durationSeconds,
    Integer distanceMeters,
    Integer heightCm,
    String notes
){}
