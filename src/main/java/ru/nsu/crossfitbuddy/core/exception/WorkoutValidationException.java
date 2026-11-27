package ru.nsu.crossfitbuddy.core.exception;

import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFieldError;

import java.util.List;

public final class WorkoutValidationException extends RuntimeException {
    private final List<WorkoutFieldError> fieldErrors;

    public WorkoutValidationException(List<WorkoutFieldError> fieldErrors) {
        super("The workout contains invalid data.");
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<WorkoutFieldError> fieldErrors() {
        return fieldErrors;
    }
}
