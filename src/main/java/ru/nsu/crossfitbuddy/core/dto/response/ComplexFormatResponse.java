package ru.nsu.crossfitbuddy.core.dto.response;

import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;

public record ComplexFormatResponse(
        ComplexFormatType type,
        String name,
        String description
) {}