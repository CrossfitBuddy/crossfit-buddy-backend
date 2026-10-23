package ru.nsu.crossfitbuddy.core.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(Paths.WORKOUTS)
public class WorkoutController {
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, Object>> list() {
        return List.of();
    }
}
