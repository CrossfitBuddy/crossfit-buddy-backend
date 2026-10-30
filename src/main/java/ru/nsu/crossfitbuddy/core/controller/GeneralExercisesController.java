package ru.nsu.crossfitbuddy.core.controller;

import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest;

@RestController
@RequestMapping(Paths.GENERAL_EXERCISES)
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class GeneralExercisesController {
    @PostMapping(Paths.GENERAL_EXERCISES_SET)
    public void setExercises(@ModelAttribute GeneralExerciseRequest request) {
        return;
    }

}
