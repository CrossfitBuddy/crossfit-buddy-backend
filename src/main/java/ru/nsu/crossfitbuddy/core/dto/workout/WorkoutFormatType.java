package ru.nsu.crossfitbuddy.core.dto.workout;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum WorkoutFormatType {
    EMOM,
    FOR_TIME,
    AMRAP,
    TABATA;

    @JsonCreator
    public static WorkoutFormatType fromValue(String value) {
        return WorkoutFormatType.valueOf(value);
    }
}
