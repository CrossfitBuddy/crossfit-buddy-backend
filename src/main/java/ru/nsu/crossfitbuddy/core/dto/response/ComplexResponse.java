package ru.nsu.crossfitbuddy.core.dto.response;

import java.util.List;
import java.util.UUID;

public record ComplexResponse(
    UUID id,
    String workoutStage,
    String complexFormat,
    Integer timeLimitSeconds,
    Integer targetRounds,
    Integer intervalSeconds,
    List<RoundResponse> rounds
) {}
