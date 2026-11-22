package ru.nsu.crossfitbuddy.core.dto.request.exercises;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record ExerciseEquipmentRequest(
    UUID id,
    @NotBlank(message = "Необходимо указать название оборудования") String name,
    String image,
    @Valid List<EquipmentPropertiesRequest> propertiesRequests
) {}
