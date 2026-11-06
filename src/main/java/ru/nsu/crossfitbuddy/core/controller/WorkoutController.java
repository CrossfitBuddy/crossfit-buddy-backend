package ru.nsu.crossfitbuddy.core.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutDetailsResponse;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.service.WorkoutDetailsService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(Paths.WORKOUTS)
public class WorkoutController {
    private final WorkoutDetailsService detailsService;

    public WorkoutController(WorkoutDetailsService detailsService) {
        this.detailsService = detailsService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, Object>> list() {
        return List.of();
    }

    @GetMapping("/{id}")
    public WorkoutDetailsResponse get(@PathVariable UUID id, @AuthenticationPrincipal CurrentUser currentUser) {
        return detailsService.findDetails(id, currentUser.id(), UserRole.valueOf(currentUser.activeRole()))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKOUT_NOT_FOUND", "Workout not found"));
    }
}
