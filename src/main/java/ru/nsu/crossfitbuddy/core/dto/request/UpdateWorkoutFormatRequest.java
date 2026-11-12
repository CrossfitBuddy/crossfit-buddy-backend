package ru.nsu.crossfitbuddy.core.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateWorkoutFormatRequest(
        @NotBlank
        String description
) {}