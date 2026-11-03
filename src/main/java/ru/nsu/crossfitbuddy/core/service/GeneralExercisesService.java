package ru.nsu.crossfitbuddy.core.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;
import ru.nsu.crossfitbuddy.core.entity.GeneralExercises;
import ru.nsu.crossfitbuddy.core.repository.GeneralExercisesRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneralExercisesService {
    private final GeneralExercisesRepository generalExercisesRepository;

    public void setExercise (GeneralExerciseRequest request) {
       
        var exercises = new GeneralExercises();
        exercises.setName(request.name());
        exercises.setDescription(request.description());

        if (!request.exercisesMetrics().isEmpty()) {
            List<ExercisesMetrics> metrics;
            for (var metricReq : request.exercisesMetrics()){
                var metrics = new ExercisesMetrics();
                metrics.setName(request.name());
                metrics.setMetricsValueType(metricReq.metricsValueType());
               metrics.add(new ExercisesMetrics()) 
            }
        }

    }
}
