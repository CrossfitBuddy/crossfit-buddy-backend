package ru.nsu.crossfitbuddy.shared.security;

import java.security.Principal;
import java.util.UUID;

public record CurrentUser(UUID id, String email, String activeRole) implements Principal {
    @Override
    public String getName() {
        return id.toString();
    }
}
