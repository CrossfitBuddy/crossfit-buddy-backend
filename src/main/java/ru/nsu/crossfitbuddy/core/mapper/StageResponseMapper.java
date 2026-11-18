package ru.nsu.crossfitbuddy.core.mapper;

import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.dto.response.ExerciseResponse;
import ru.nsu.crossfitbuddy.core.dto.response.RoundResponse;
import ru.nsu.crossfitbuddy.core.dto.response.StageResponse;
import ru.nsu.crossfitbuddy.core.entity.RoundExercise;
import ru.nsu.crossfitbuddy.core.entity.Stage;
import ru.nsu.crossfitbuddy.core.entity.StageRound;

import java.util.Comparator;
import java.util.List;

@Component
public class StageResponseMapper {

    public StageResponse toResponse(Stage s) {
        var rounds = s.getRounds().stream()
            .sorted(Comparator.comparing(StageRound::getPosition))
            .map(this::toRound)
            .toList();

        return new StageResponse(
            s.getId(),
            s.getStageFormat().getName(),
            s.getComplexFormat().getName(),
            s.getTimeLimitSeconds(),
            s.getTargetRounds(),
            s.getIntervalSeconds(),
            rounds
        );
    }

    private RoundResponse toRound(StageRound r) {
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
