package ru.nsu.crossfitbuddy.core.mapper;

import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.dto.workout.OwnWorkoutRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutResponse;
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
            var complex = Complex.builder().build();
            complex.setPosition(workout.getComplexes().size() + 1);
            complex.setWorkoutStage(stages.get(stage.workoutStageId()));
            complex.setComplexFormat(formats.get(stage.format()));
            complex.setTimeLimitSeconds(stage.timeLimitSeconds());
            complex.setIntervalSeconds(stage.intervalSeconds());
            complex.setTargetRounds(stage.targetRounds());
            for (var roundRequest : stage.rounds()) {
                var round = ComplexRound.builder().build();
                round.setPosition(complex.getRounds().size() + 1);
                round.setRepeatCount(roundRequest.repeatCount());
                for (var requestExercise : roundRequest.exercises()) {
                    var occurrence = RoundExercise.builder().build();
                    occurrence.setPosition(round.getExercises().size() + 1);
                    occurrence.setExercise(exercises.get(requestExercise.exerciseId()));
                    occurrence.setRepetitions(requestExercise.repetitions());
                    occurrence.setWeightKg(requestExercise.weightKg());
                    occurrence.setDurationSeconds(requestExercise.durationSeconds());
                    occurrence.setDistanceMeters(requestExercise.distanceMeters());
                    occurrence.setHeightCm(requestExercise.heightCm());
                    occurrence.setNotes(requestExercise.notes());
                    round.addExercise(occurrence);
                }
                complex.addRound(round);
            }
            workout.addComplex(complex);
        }
        return workout;
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
