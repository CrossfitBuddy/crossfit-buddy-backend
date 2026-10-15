package ru.nsu.crossfitbuddy.core.dto.response;

import java.util.UUID;

public record UserProfileResponse(UUID id, String email, String displayName) {
}
