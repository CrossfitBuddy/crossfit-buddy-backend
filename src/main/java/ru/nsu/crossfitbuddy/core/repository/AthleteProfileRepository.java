package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.AthleteProfile;

import java.util.UUID;

public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, UUID> {
}
