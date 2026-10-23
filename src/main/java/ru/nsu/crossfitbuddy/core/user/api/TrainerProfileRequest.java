package ru.nsu.crossfitbuddy.core.user.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TrainerProfileRequest(
    @Size(max = 5000)
    String specializations,

    @Min(0)
    @Max(80)
    Integer experienceYears,

    @Size(max = 5000)
    String methodology
) {
}
