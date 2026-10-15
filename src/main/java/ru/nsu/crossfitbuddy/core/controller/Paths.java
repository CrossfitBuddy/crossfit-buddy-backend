package ru.nsu.crossfitbuddy.core.controller;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Paths {
    public static final String USER = "/v1/users";
    public static final String USER_BY_ID = "/{id}";

    public static final String WORKOUTS = "/api/v1/workouts";
}
