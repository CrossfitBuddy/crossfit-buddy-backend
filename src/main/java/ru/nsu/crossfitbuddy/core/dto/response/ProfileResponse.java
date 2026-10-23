package ru.nsu.crossfitbuddy.core.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import ru.nsu.crossfitbuddy.core.entity.FitnessLevel;
import ru.nsu.crossfitbuddy.core.entity.Gender;
import ru.nsu.crossfitbuddy.core.entity.RxPreference;
import ru.nsu.crossfitbuddy.core.entity.UserRole;

public record ProfileResponse(
    UUID id, String email, Set<UserRole> roles,
    String firstName, String lastName, String displayName, LocalDate birthDate,
    Gender gender, String city, String about, String avatarUrl, String initials,
    AthleteFields athlete, TrainerFields trainer) {

    public record AthleteFields(Integer heightCm, BigDecimal weightKg,
        FitnessLevel fitnessLevel, RxPreference rxPreference) {}

    public record TrainerFields(String specializations, Integer experienceYears, String methodology) {}
}
