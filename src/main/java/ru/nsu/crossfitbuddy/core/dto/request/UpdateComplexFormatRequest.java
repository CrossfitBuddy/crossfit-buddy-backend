package ru.nsu.crossfitbuddy.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateComplexFormatRequest(
        @NotBlank
        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description
) {}