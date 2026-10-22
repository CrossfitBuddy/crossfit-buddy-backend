package ru.nsu.crossfitbuddy.core.dto.response;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, AccountResponse user) {
    @Override
    public String toString() {
        return "AuthResponse[token=REDACTED]";
    }
}
