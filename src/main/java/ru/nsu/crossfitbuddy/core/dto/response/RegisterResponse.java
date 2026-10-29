package ru.nsu.crossfitbuddy.core.dto.response;

import java.util.UUID;

public record RegisterResponse(
    UUID userId,
    String message
) {
}
