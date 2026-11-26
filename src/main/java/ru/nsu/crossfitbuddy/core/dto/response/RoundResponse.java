package ru.nsu.crossfitbuddy.core.dto.response;

import java.util.List;
import java.util.UUID;

public record RoundResponse(
    UUID id,
    int position,
    int repeatCount,
    List<ExerciseResponse> exercises
){}
