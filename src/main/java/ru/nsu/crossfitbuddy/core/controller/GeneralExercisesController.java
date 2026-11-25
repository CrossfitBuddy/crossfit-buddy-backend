package ru.nsu.crossfitbuddy.core.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.nsu.crossfitbuddy.auth.security.annotation.AdminAuthority;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.GeneralExerciseRequest;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExerciseEquipmentResponse;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExercisesMetricsResponse;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.GeneralExerciseResponse;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.ExerciseCategory;
import ru.nsu.crossfitbuddy.core.service.GeneralExercisesService;

@RestController
@RequestMapping(Paths.GENERAL_EXERCISES)
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class GeneralExercisesController {
  private final GeneralExercisesService generalExercisesService;

  @AdminAuthority
  @PostMapping(Paths.GENERAL_EXERCISES_SET)
  public void setExercises(
      @ModelAttribute
      @Valid GeneralExerciseRequest request,
      @AuthenticationPrincipal CurrentUser userDetails) {
    generalExercisesService.createExercise(request, userDetails);
  }

  @AdminAuthority
  @PutMapping(Paths.GENERAL_EXERCISES_UPDATE)
  public void updateExercise(@Valid GeneralExerciseRequest request,
      @AuthenticationPrincipal CurrentUser userDetails) {
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

  @GetMapping(Paths.GENERAL_EXERCISES_CATALOG)
  public List<GeneralExerciseResponse> getCatalog(
          @RequestParam(required = false) String name,
          @RequestParam(required = false) ExerciseCategory category,
          @RequestParam(required = false) UUID equipmentId) {
    return generalExercisesService.getExercisesCatalog(name, category, equipmentId);
  }

  @GetMapping(Paths.GENERAL_EXERCISES_DETAILS)
  public GeneralExerciseResponse getDetails(@PathVariable UUID id) {
    return generalExercisesService.getExerciseDetails(id);
  }
}
