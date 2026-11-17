package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.WorkoutParticipant;
import java.util.UUID;

public interface WorkoutParticipantRepository extends JpaRepository<WorkoutParticipant, UUID> {}
