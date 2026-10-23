package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.TrainerProfile;

import java.util.UUID;

public interface TrainerProfileRepository extends JpaRepository<TrainerProfile, UUID> {
}
