package ru.nsu.crossfitbuddy.core.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.nsu.crossfitbuddy.core.user.domain.TrainerProfile;

import java.util.UUID;

public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, UUID> {
}
