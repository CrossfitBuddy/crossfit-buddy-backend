package ru.nsu.crossfitbuddy.core.dto.response;

import ru.nsu.crossfitbuddy.core.entity.WorkoutFormatType;

public record WorkoutFormatResponse(
        WorkoutFormatType type,
        String name,
        String description,
        boolean durationRequired,
        boolean intervalRequired
) {}