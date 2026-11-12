package ru.nsu.crossfitbuddy.core.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Paths {
    // workouts
    public static final String WORKOUTS = "/v1/workouts";
    public static final String WORKOUTS_FORMATS = "/formats";
    public static final String WORKOUTS_FORMATS_BY_TYPE = "/{type}";


    // auth
    public static final String AUTH = "/v1/auth";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_REGISTER = "/register";
    public static final String AUTH_ME = "/me";
    public static final String AUTH_ACTIVE_ROLE = "/active-role";
    public static final String AUTH_EMAIL_CONFIRM = "/email/confirm";
    public static final String AUTH_EMAIL_RESEND = "/email/resend";
    public static final String AUTH_PASSWORD_RECOVERY_REQUEST = "/password/request";
    public static final String AUTH_PASSWORD_RECOVERY_CONFIRM = "/password/confirm";

    // profile
    public static final String PROFILE = "/v1/users/me";

    // avatar
    public static final String AVATAR = "/v1/users/me/avatar";

    // email (тестовый)
    public static final String EMAIL = "/send-email";
}
