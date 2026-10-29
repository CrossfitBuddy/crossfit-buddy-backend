package ru.nsu.crossfitbuddy.core.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Paths {
    public static final String USER = "/v1/users";
    public static final String USER_BY_ID = "/{id}";

    public static final String WORKOUTS = "/v1/workouts";

    public static final String AUTH = "/v1/auth";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_REGISTER = "/register";
    public static final String AUTH_ME = "/me";
    public static final String AUTH_ACTIVE_ROLE = "/active-role";
    public static final String AUTH_EMAIL_CONFIRM = "/email/confirm";
    public static final String AUTH_EMAIL_RESEND = "/email/resend";
}
