package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.Workout;
import java.util.UUID;

public interface WorkoutRepository extends JpaRepository<Workout, UUID>, WorkoutRepositoryCustom {}
