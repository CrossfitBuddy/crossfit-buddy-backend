package ru.nsu.crossfitbuddy.core.mapper;

import org.springframework.stereotype.Component;

import ru.nsu.crossfitbuddy.core.dto.workout.OwnWorkoutRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutResponse;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutRoundRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutStageRequest;
import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;
import ru.nsu.crossfitbuddy.core.workout.model.WorkoutToCreate;

import java.util.ArrayList;
import java.util.UUID;

@Component
public class WorkoutCreationMapper {
    public WorkoutToCreate toModel(UUID owner, OwnWorkoutRequest request) {
        var stages = new ArrayList<WorkoutToCreate.Stage>();
        for (int index = 0; index < request.stages().size(); index++) {
            stages.add(toStage(request.stages().get(index), index + 1));
        }
        return new WorkoutToCreate(UUID.randomUUID(), owner, request.name(), request.description(),
            request.datetime(), request.durationMinutes(), WorkoutType.OWN, WorkoutStatus.READY, stages);
    }

    public WorkoutResponse toResponse(WorkoutToCreate workout) {
        var stages = workout.stages().stream().map(this::toStageResponse).toList();
        return new WorkoutResponse(workout.id(), workout.name(), workout.description(), workout.datetime(),
            workout.durationMinutes(), workout.createdBy(), workout.type(), workout.status(), stages);
    }

    private WorkoutToCreate.Stage toStage(WorkoutStageRequest stage, int position) {
        var rounds = new ArrayList<WorkoutToCreate.Round>();
        for (int index = 0; index < stage.rounds().size(); index++) {
            rounds.add(toRound(stage.rounds().get(index), index + 1));
        }
        return new WorkoutToCreate.Stage(UUID.randomUUID(), position, stage.stageFormatId(),
            stage.format(), stage.params(), rounds);
    }

    private WorkoutToCreate.Round toRound(WorkoutRoundRequest round, int position) {
        var exercises = new ArrayList<WorkoutToCreate.Exercise>();
        for (int index = 0; index < round.exercises().size(); index++) {
            exercises.add(toExercise(round.exercises().get(index), index + 1));
        }
        return new WorkoutToCreate.Round(UUID.randomUUID(), position, round.repeatCount(), exercises);
    }

    private WorkoutToCreate.Exercise toExercise(WorkoutExerciseRequest exercise, int position) {
        return new WorkoutToCreate.Exercise(UUID.randomUUID(), position, exercise.exerciseId(),
            exercise.repetitions(), exercise.weightKg(), exercise.durationMinutes(),
            exercise.distanceMeters(), exercise.heightCm(), exercise.notes());
    }

    private WorkoutResponse.Stage toStageResponse(WorkoutToCreate.Stage stage) {
        var rounds = stage.rounds().stream().map(this::toRoundResponse).toList();
        return new WorkoutResponse.Stage(stage.id(), stage.position(), stage.stageFormatId(),
            stage.format(), stage.params(), rounds);
    }

    private WorkoutResponse.Round toRoundResponse(WorkoutToCreate.Round round) {
        var exercises = round.exercises().stream().map(this::toExerciseResponse).toList();
        return new WorkoutResponse.Round(round.id(), round.position(), round.repeatCount(), exercises);
    }

    private WorkoutResponse.Exercise toExerciseResponse(WorkoutToCreate.Exercise exercise) {
        var parameters = new WorkoutExerciseRequest(exercise.exerciseId(), exercise.repetitions(),
            exercise.weightKg(), exercise.durationMinutes(), exercise.distanceMeters(),
            exercise.heightCm(), exercise.notes());
        return new WorkoutResponse.Exercise(exercise.id(), exercise.position(), parameters);
    }
}
