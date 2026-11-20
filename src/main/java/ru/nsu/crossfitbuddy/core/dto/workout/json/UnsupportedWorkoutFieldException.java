package ru.nsu.crossfitbuddy.core.dto.workout.json;

public final class UnsupportedWorkoutFieldException extends IllegalArgumentException {
    private final String field;

    public UnsupportedWorkoutFieldException(String field) {
        super("Unsupported workout field: " + field);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
