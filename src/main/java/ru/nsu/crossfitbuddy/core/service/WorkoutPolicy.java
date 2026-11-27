package ru.nsu.crossfitbuddy.core.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.UserRole;

@Component
public class WorkoutPolicy {
    public void requireOwnWorkoutCreation(CurrentUser user) {
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication is required.");
        }
        if (!UserRole.ATHLETE.name().equals(user.activeRole())) {
            throw new AccessDeniedException("Creating an own workout requires the active ATHLETE role.");
        }
    }
}
