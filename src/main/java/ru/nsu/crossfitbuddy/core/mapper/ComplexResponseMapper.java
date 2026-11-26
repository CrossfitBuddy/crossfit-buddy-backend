package ru.nsu.crossfitbuddy.core.mapper;

import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.dto.response.ComplexResponse;
import ru.nsu.crossfitbuddy.core.dto.response.ExerciseResponse;
import ru.nsu.crossfitbuddy.core.dto.response.RoundResponse;
import ru.nsu.crossfitbuddy.core.entity.Complex;
import ru.nsu.crossfitbuddy.core.entity.RoundExercise;
import ru.nsu.crossfitbuddy.core.entity.ComplexRound;

import java.util.Comparator;

@Component
public class ComplexResponseMapper {
    public ComplexResponse toResponse(Complex s) {
        var rounds = s.getRounds().stream()
            .sorted(Comparator.comparing(ComplexRound::getPosition))
            .map(this::toRound)
            .toList();

        return new ComplexResponse(
            s.getId(),
            s.getWorkoutStage().getName(),
            s.getComplexFormat().getType().toString(),
            s.getTimeLimitSeconds(),
            s.getTargetRounds(),
            s.getIntervalSeconds(),
            rounds
        );
    }

    private RoundResponse toRound(ComplexRound r) {
        var exercises = r.getExercises().stream()
            .sorted(Comparator.comparing(RoundExercise::getPosition))
            .map(this::toExercise)
            .toList();

        return new RoundResponse(r.getId(), r.getPosition(), r.getRepeatCount(), exercises);
    }

    private ExerciseResponse toExercise(RoundExercise e) {
        return new ExerciseResponse(
            e.getExercise().getId(),
            e.getExercise().getName(),
            e.getPosition(),
            e.getRepetitions(),
            e.getWeightKg(),
            e.getDurationSeconds(),
            e.getDistanceMeters(),
            e.getHeightCm(),
            e.getNotes()
        );
    }
}
