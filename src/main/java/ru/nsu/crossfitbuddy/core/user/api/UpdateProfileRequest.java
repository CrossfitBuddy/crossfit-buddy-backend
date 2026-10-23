package ru.nsu.crossfitbuddy.core.user.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import ru.nsu.crossfitbuddy.core.user.domain.Gender;

import java.time.LocalDate;

public record UpdateProfileRequest(
    @NotBlank
    @Size(max = 100)
    String firstName,

    @NotBlank
    @Size(max = 100)
    String lastName,

    @PastOrPresent
    LocalDate birthDate,

    Gender gender,

    @Size(max = 120)
    String city,

    @Size(max = 5000)
    String about,

    @Valid
    AthleteProfileRequest athlete,

    @Valid
    TrainerProfileRequest trainer
) {
}
