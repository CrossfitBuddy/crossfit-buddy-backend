package ru.nsu.crossfitbuddy.core.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExerciseEquipmentResponse;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExercisesMetricsResponse;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.GeneralExerciseResponse;
import ru.nsu.crossfitbuddy.core.service.GeneralExercisesService;

@RestController
@RequestMapping(Paths.GENERAL_EXERCISES)
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class GeneralExercisesController {
    private final GeneralExercisesService generalExercisesService;

    @PostMapping(Paths.GENERAL_EXERCISES_SET)
    public void setExercises(
            @ModelAttribute
            @Valid GeneralExerciseRequest request) {
        generalExercisesService.createExercise(request);
    }

    @PutMapping(Paths.GENERAL_EXERCISES_UPPDATE)
    public void updateExercise(GeneralExerciseRequest request) {
        generalExercisesService.updateExercises(request);
    }

    @GetMapping(Paths.GENERAL_EXERCISES_GET_EQUIPMENTS)
    public List<ExerciseEquipmentResponse> getAllEquipments() {
        return generalExercisesService.getAllEquipments();
    }

    @GetMapping(Paths.GENERAL_EXERCISES_GET_METRICS)
    public List<ExercisesMetricsResponse> getAllMetrics() {
        return generalExercisesService.getAllMetrics();
    }

    @GetMapping(Paths.GENERAL_EXERCISES_GET_EXERCISES)
    public List<GeneralExerciseResponse> getAllExercises() {
        return generalExercisesService.getAllExercise();
    }

}
