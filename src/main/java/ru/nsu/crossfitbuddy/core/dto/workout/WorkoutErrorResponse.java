package ru.nsu.crossfitbuddy.core.dto.workout;

import java.time.Instant;
import java.util.List;

public record WorkoutErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,

    List<WorkoutFieldError> fieldErrors
) {}
