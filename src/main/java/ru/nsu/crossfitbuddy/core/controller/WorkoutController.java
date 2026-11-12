package ru.nsu.crossfitbuddy.core.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.crossfitbuddy.auth.security.annotation.AdminAuthority;
import ru.nsu.crossfitbuddy.core.dto.request.UpdateWorkoutFormatRequest;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormatType;
import ru.nsu.crossfitbuddy.core.service.WorkoutFormatService;

import java.util.List;
import java.util.Map;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping(Paths.WORKOUTS)
@RequiredArgsConstructor
public class WorkoutController {

    private final WorkoutFormatService workoutFormatService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, Object>> list() {
        return List.of();
    }

    @GetMapping(Paths.WORKOUTS_FORMATS)
    public List<WorkoutFormatResponse> getAllWorkoutFormats() {
        return workoutFormatService.getAll();
    }

    @AdminAuthority
    @PatchMapping(Paths.WORKOUTS_FORMATS_BY_TYPE)
    public ResponseEntity<WorkoutFormatResponse> updateWorkoutFormatDescription(
            @PathVariable WorkoutFormatType type,
            @Valid @RequestBody UpdateWorkoutFormatRequest request
    ) {
        return ResponseEntity.ok(workoutFormatService.updateDescription(type, request.description()));
    }
}
