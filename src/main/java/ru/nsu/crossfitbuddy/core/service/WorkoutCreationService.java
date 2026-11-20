package ru.nsu.crossfitbuddy.core.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import ru.nsu.crossfitbuddy.core.dto.workout.OwnWorkoutRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFieldError;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFormatType;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutResponse;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.exception.WorkoutValidationException;
import ru.nsu.crossfitbuddy.core.mapper.WorkoutCreationMapper;
import ru.nsu.crossfitbuddy.core.repository.WorkoutCreationRepository;
import ru.nsu.crossfitbuddy.core.validation.WorkoutFormatParametersValidator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
        validateReferences(user, request);
        var workout = mapper.toModel(user.id(), request);
        workouts.save(workout);
        return mapper.toResponse(workout);
    }

    private void validateReferences(CurrentUser user, OwnWorkoutRequest request) {
        Set<Integer> stageIds = new HashSet<>();
        Set<WorkoutFormatType> formats = new HashSet<>();
        Set<UUID> exerciseIds = new HashSet<>();
        for (var stage : request.stages()) {
            stageIds.add(stage.stageFormatId());
            formats.add(stage.format());
            for (var round : stage.rounds()) {
                for (var exercise : round.exercises()) {
                    exerciseIds.add(exercise.exerciseId());
                }
            }
        }
        var availableStages = workouts.lockStageFormats(stageIds);
        var availableFormats = workouts.lockWorkoutFormats(formats);
        var availableExercises = workouts.lockAvailableExercises(exerciseIds, user.id());
        var errors = new ArrayList<WorkoutFieldError>();
        for (int s = 0; s < request.stages().size(); s++) {
            var stage = request.stages().get(s);
            String stagePath = "stages[" + s + "]";
            if (!availableStages.contains(stage.stageFormatId())) {
                errors.add(unknown(stagePath + ".stageFormatId", "Stage format does not exist."));
            }
            if (!availableFormats.containsKey(stage.format())) {
                errors.add(unknown(stagePath + ".format", "Complex format does not exist."));
            } else {
                var definition = availableFormats.get(stage.format());
                parametersValidator.validate(stage.params(), definition.durationRequired(),
                    definition.intervalRequired(), stagePath, errors);
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
