package ru.nsu.crossfitbuddy.core.dto.response;

import java.util.List;
import java.util.UUID;

public record StageResponse(
    UUID id,
    String format,
    Integer timeLimitSeconds,
    Integer targetRounds,
    Integer intervalSeconds,
    List<RoundResponse> rounds
) {}
