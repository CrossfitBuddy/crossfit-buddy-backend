package ru.nsu.crossfitbuddy.core.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import ru.nsu.crossfitbuddy.core.entity.FitnessLevel;
import ru.nsu.crossfitbuddy.core.entity.RxPreference;

import java.math.BigDecimal;

public record AthleteProfileRequest(
    @Min(50)
    @Max(250)
    Integer heightCm,

    @DecimalMin("20")
    @DecimalMax("500")
    @Digits(integer = 4, fraction = 2)
    BigDecimal weightKg,

    FitnessLevel fitnessLevel,

    RxPreference rxPreference
) {
}
