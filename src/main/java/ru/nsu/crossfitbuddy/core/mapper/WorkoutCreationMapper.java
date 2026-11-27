package ru.nsu.crossfitbuddy.core.mapper;

import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.dto.workout.OwnWorkoutRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutResponse;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutRoundRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutStageRequest;
import ru.nsu.crossfitbuddy.core.entity.Complex;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormat;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.entity.ComplexRound;
import ru.nsu.crossfitbuddy.core.entity.Exercise;
import ru.nsu.crossfitbuddy.core.entity.RoundExercise;
import ru.nsu.crossfitbuddy.core.entity.Workout;
import ru.nsu.crossfitbuddy.core.entity.WorkoutStage;
import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;

import java.util.Map;
import java.util.UUID;

@Component
public class WorkoutCreationMapper {
    public Workout toEntity(UUID owner, OwnWorkoutRequest request, Map<Integer, WorkoutStage> stages,
        Map<ComplexFormatType, ComplexFormat> formats, Map<UUID, Exercise> exercises) {
        var workout = Workout.builder()
            .title(request.name()).description(request.description()).coach(request.coach())
            .datetime(request.datetime()).durationSeconds(request.durationSeconds())
            .createdBy(owner).type(WorkoutType.OWN).status(WorkoutStatus.READY).build();
        for (var stage : request.stages()) {
            var complex = toComplex(stage, workout.getComplexes().size() + 1,
                stages.get(stage.workoutStageId()), formats.get(stage.format()), exercises);
            workout.addComplex(complex);
        }
        return workout;
    }

    private Complex toComplex(WorkoutStageRequest request, int position, WorkoutStage stage,
        ComplexFormat format, Map<UUID, Exercise> exercises) {
        var complex = Complex.builder()
            .position(position)
            .workoutStage(stage)
            .complexFormat(format)
            .timeLimitSeconds(request.timeLimitSeconds())
            .intervalSeconds(request.intervalSeconds())
            .targetRounds(request.targetRounds())
            .build();

        for (var round : request.rounds()) {
            complex.addRound(toRound(round, complex.getRounds().size() + 1, exercises));
        }
        return complex;
    }

    private ComplexRound toRound(WorkoutRoundRequest request, int position, Map<UUID, Exercise> exercises) {
        var round = ComplexRound.builder()
            .position(position)
            .repeatCount(request.repeatCount())
            .build();

        for (var exercise : request.exercises()) {
            round.addExercise(toExercise(exercise, round.getExercises().size() + 1,
                exercises.get(exercise.exerciseId())));
        }
        return round;
    }

    private RoundExercise toExercise(WorkoutExerciseRequest request, int position, Exercise exercise) {
        return RoundExercise.builder()
            .position(position)
            .exercise(exercise)
            .repetitions(request.repetitions())
            .weightKg(request.weightKg())
            .durationSeconds(request.durationSeconds())
            .distanceMeters(request.distanceMeters())
            .heightCm(request.heightCm())
            .notes(request.notes())
            .build();
    }

    public WorkoutResponse toResponse(Workout workout) {
        var stages = workout.getComplexes().stream().map(this::toStageResponse).toList();
        return new WorkoutResponse(workout.getId(), workout.getTitle(), workout.getDescription(), workout.getCoach(),
            workout.getDatetime(), workout.getDurationSeconds(), workout.getCreatedBy(), workout.getType(),
            workout.getStatus(), stages);
    }

    private WorkoutResponse.Stage toStageResponse(Complex complex) {
        var rounds = complex.getRounds().stream().map(this::toRoundResponse).toList();
        return new WorkoutResponse.Stage(complex.getId(), complex.getPosition(), complex.getWorkoutStage().getId(),
            complex.getComplexFormat().getType(), complex.getTimeLimitSeconds(), complex.getIntervalSeconds(),
            complex.getTargetRounds(), rounds);
    }

    private WorkoutResponse.Round toRoundResponse(ComplexRound round) {
        var exercises = round.getExercises().stream().map(this::toExerciseResponse).toList();
        return new WorkoutResponse.Round(round.getId(), round.getPosition(), round.getRepeatCount(), exercises);
    }

    private WorkoutResponse.Exercise toExerciseResponse(RoundExercise exercise) {
        var parameters = new WorkoutExerciseRequest(exercise.getExercise().getId(), exercise.getRepetitions(),
            exercise.getWeightKg(), exercise.getDurationSeconds(), exercise.getDistanceMeters(),
            exercise.getHeightCm(), exercise.getNotes());
        return new WorkoutResponse.Exercise(exercise.getId(), exercise.getPosition(), parameters);
    }
}
