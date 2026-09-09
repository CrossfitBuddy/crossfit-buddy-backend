package ru.nsu.crossfitbuddy.core.user.domain;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserProfile {
    private final UUID id;
    private final String email;
    private final String displayName;
}
