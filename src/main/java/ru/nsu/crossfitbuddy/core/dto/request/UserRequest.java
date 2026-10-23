package ru.nsu.crossfitbuddy.core.dto.request;

import ru.nsu.crossfitbuddy.core.entity.UserRole;

import java.time.Instant;

// TODO: заглушка
public record UserRequest(
    String email,
    String passwordHash,
    String displayName,
    UserRole activeRole,
    boolean enabled,
    Instant createdAt,
    Instant updatedAt
) {
}
