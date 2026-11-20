package ru.nsu.crossfitbuddy.core.dto.workout;

import io.swagger.v3.oas.annotations.media.Schema;

public record WorkoutFieldError(
    @Schema(example = "stages[0].rounds[0].exercises[0].exerciseId") String field,

    @Schema(example = "UNKNOWN_REFERENCE") String code,

    @Schema(example = "Exercise does not exist or is unavailable") String message
) {}
