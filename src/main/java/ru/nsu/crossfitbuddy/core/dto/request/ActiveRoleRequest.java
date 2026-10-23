package ru.nsu.crossfitbuddy.core.dto.request;

import jakarta.validation.constraints.NotNull;
import ru.nsu.crossfitbuddy.core.entity.UserRole;

public record ActiveRoleRequest(@NotNull(message = "Active role is required.") UserRole role) {
}
