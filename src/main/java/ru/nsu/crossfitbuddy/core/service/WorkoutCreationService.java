package ru.nsu.crossfitbuddy.core.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.nsu.crossfitbuddy.core.dto.workout.OwnWorkoutRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFieldError;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutResponse;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormat;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.Exercise;
import ru.nsu.crossfitbuddy.core.entity.WorkoutStage;
import ru.nsu.crossfitbuddy.core.exception.WorkoutValidationException;
import ru.nsu.crossfitbuddy.core.mapper.WorkoutCreationMapper;
import ru.nsu.crossfitbuddy.core.repository.WorkoutCreationRepository;
import ru.nsu.crossfitbuddy.core.validation.WorkoutFormatParametersValidator;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
public class WorkoutCreationService {
    private final WorkoutCreationRepository workouts;
    private final WorkoutPolicy policy;
    private final WorkoutCreationMapper mapper;
    private final WorkoutFormatParametersValidator parametersValidator;

    @Transactional
    public WorkoutResponse createOwn(CurrentUser user, @NotNull @Valid OwnWorkoutRequest request) {
        policy.requireOwnWorkoutCreation(user);
        var stages = workouts.lockStages(request.stages().stream()
            .map(stage -> stage.workoutStageId()).collect(Collectors.toSet())).stream()
            .collect(Collectors.toMap(WorkoutStage::getId, Function.identity()));
        var formats = workouts.lockFormats(request.stages().stream()
            .map(stage -> stage.format()).collect(Collectors.toSet())).stream()
            .collect(Collectors.toMap(ComplexFormat::getType, Function.identity()));
        var exerciseIds = request.stages().stream().flatMap(stage -> stage.rounds().stream())
            .flatMap(round -> round.exercises().stream()).map(exercise -> exercise.exerciseId())
            .collect(Collectors.toSet());
        var exercises = workouts.lockAvailableExercises(exerciseIds, user.id()).stream()
            .collect(Collectors.toMap(Exercise::getId, Function.identity()));
        validateReferences(request, stages.keySet(), formats.keySet(), exercises.keySet());
        var workout = mapper.toEntity(user.id(), request, stages, formats, exercises);
        workouts.save(workout);
        return mapper.toResponse(workout);
    }

    private void validateReferences(OwnWorkoutRequest request, Set<Integer> availableStages,
        Set<ComplexFormatType> availableFormats, Set<UUID> availableExercises) {
        var errors = new ArrayList<WorkoutFieldError>();
        for (int s = 0; s < request.stages().size(); s++) {
            var stage = request.stages().get(s);
            String stagePath = "stages[" + s + "]";
            if (!availableStages.contains(stage.workoutStageId())) {
                errors.add(unknown(stagePath + ".workoutStageId", "Stage format does not exist."));
            }
            if (!availableFormats.contains(stage.format())) {
                errors.add(unknown(stagePath + ".format", "Complex format does not exist."));
            } else {
                parametersValidator.validate(stage, stagePath, errors);
            }
            for (int r = 0; r < stage.rounds().size(); r++) {
                var round = stage.rounds().get(r);
                for (int e = 0; e < round.exercises().size(); e++) {
                    if (!availableExercises.contains(round.exercises().get(e).exerciseId())) {
                        String path = stagePath + ".rounds[" + r + "].exercises[" + e + "].exerciseId";
                        errors.add(unknown(path, "Exercise does not exist or is unavailable."));
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new WorkoutValidationException(errors);
        }
    }

    private static WorkoutFieldError unknown(String path, String message) {
        return new WorkoutFieldError(path, "UNKNOWN_REFERENCE", message);
    }
}
