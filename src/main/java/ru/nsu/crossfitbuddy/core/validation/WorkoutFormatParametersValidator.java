package ru.nsu.crossfitbuddy.core.validation;

import org.springframework.stereotype.Component;

import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFieldError;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class WorkoutFormatParametersValidator {
    public void validate(Map<String, BigDecimal> params, boolean durationRequired,
        boolean intervalRequired, String stagePath, List<WorkoutFieldError> errors) {
        for (String key : params.keySet()) {
            boolean allowed = switch (key) {
                case "duration" -> durationRequired;
                case "interval" -> intervalRequired;
                default -> false;
            };
            if (!allowed) {
                errors.add(new WorkoutFieldError(stagePath + ".params." + key, "UNSUPPORTED_PARAMETER",
                    "This parameter is not supported by the selected format."));
            }
        }
        if (durationRequired && !params.containsKey("duration")) {
            errors.add(new WorkoutFieldError(stagePath + ".params.duration", "REQUIRED_PARAMETER",
                "Duration is required by the selected format."));
        }
        if (intervalRequired && !params.containsKey("interval")) {
            errors.add(new WorkoutFieldError(stagePath + ".params.interval", "REQUIRED_PARAMETER",
                "Interval is required by the selected format."));
        }
    }
}
