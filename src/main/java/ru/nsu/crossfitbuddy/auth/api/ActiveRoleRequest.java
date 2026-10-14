package ru.nsu.crossfitbuddy.auth.api;

import jakarta.validation.constraints.NotNull;

import ru.nsu.crossfitbuddy.core.user.domain.UserRole;

public record ActiveRoleRequest(@NotNull(message = "Active role is required.") UserRole role) {
}
