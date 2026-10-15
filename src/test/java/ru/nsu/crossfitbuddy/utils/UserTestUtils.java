package ru.nsu.crossfitbuddy.utils;

import ru.nsu.crossfitbuddy.core.dto.request.UserRequest;
import ru.nsu.crossfitbuddy.core.entity.AppUser;
import ru.nsu.crossfitbuddy.core.entity.UserRole;

import java.time.Instant;

public class UserTestUtils {

    public static AppUser getAppUser() {
        return new AppUser(
            null,
            "test@example.com",
            "password",
            "displayName",
            UserRole.ATHLETE,
            true,
            Instant.now(),
            Instant.now()
        );
    }

    public static UserRequest getUserRequest() {
        return new UserRequest(
            "test@example.com",
            "password",
            "displayName",
            UserRole.ATHLETE,
            true,
            Instant.now(),
            Instant.now()
        );
    }

    public static UserRequest getUpdatedUserRequest() {
        return new UserRequest(
            "new@example.com",
            "newpassword",
            "displayName",
            UserRole.ATHLETE,
            true,
            Instant.now(),
            Instant.now()
        );
    }
}
