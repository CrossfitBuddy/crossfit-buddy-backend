package ru.nsu.crossfitbuddy.core.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.nsu.crossfitbuddy.core.user.domain.AthleteProfile;

import java.util.UUID;

public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, UUID> {
}
