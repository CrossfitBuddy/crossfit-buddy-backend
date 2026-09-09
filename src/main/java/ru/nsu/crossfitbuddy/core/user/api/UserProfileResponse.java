package ru.nsu.crossfitbuddy.core.user.api;

import java.util.UUID;

public record UserProfileResponse(UUID id, String email, String displayName) {
}
