package ru.nsu.crossfitbuddy.core.validation;

import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFieldError;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutStageRequest;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;

import java.util.List;

@Component
public class WorkoutFormatParametersValidator {
    public void validate(WorkoutStageRequest stage, String path, List<WorkoutFieldError> errors) {
        boolean time = switch (stage.format()) {
            case AMRAP, EMOM -> true;
            case FOR_TIME, FREE -> false;
        };
        check(stage.timeLimitSeconds(), time, path + ".timeLimitSeconds", errors);
        check(stage.intervalSeconds(), stage.format() == ComplexFormatType.EMOM,
            path + ".intervalSeconds", errors);
        check(stage.targetRounds(), stage.format() == ComplexFormatType.FOR_TIME,
            path + ".targetRounds", errors);
    }

    private void check(Integer value, boolean required, String path, List<WorkoutFieldError> errors) {
        if (required && value == null) {
            errors.add(new WorkoutFieldError(path, "REQUIRED_PARAMETER", "This format requires the parameter."));
        } else if (!required && value != null) {
            errors.add(new WorkoutFieldError(path, "UNSUPPORTED_PARAMETER", "This format does not allow the parameter."));
        }
    }
}
