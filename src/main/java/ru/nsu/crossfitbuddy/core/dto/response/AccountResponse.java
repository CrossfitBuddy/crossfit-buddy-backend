package ru.nsu.crossfitbuddy.core.dto.response;

import ru.nsu.crossfitbuddy.core.entity.UserAccount;
import ru.nsu.crossfitbuddy.core.entity.UserRole;

import java.util.Set;
import java.util.UUID;

public record AccountResponse(UUID id, String email, String displayName, Set<UserRole> roles, UserRole activeRole) {
    public static AccountResponse from(UserAccount user, String displayName, UserRole activeRole) {
        return new AccountResponse(user.getId(), user.getEmail(), displayName, Set.copyOf(user.getRoles()), activeRole);
    }
}
