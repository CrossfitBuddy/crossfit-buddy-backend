package ru.nsu.crossfitbuddy.core.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Paths {
    public static final String USER = "/v1/users";
    public static final String USER_BY_ID = "/{id}";

    public static final String WORKOUTS = "/api/v1/workouts";

    public static final String AUTH = "/api/v1/auth";
    public static final String AUTH_LOGIN = "/login";
    public static final String AUTH_REGISTER = "/register";
    public static final String AUTH_ME = "/me";
    public static final String AUTH_ACTIVE_ROLE = "/active-role";

    public static final String GENERAL_EXERCISES = "v1/general_exercises";
    public static final String GENERAL_EXERCISES_SET = "/set";
}
