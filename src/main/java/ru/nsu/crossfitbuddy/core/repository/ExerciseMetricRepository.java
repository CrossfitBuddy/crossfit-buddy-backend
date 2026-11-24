package ru.nsu.crossfitbuddy.core.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;

public interface ExerciseMetricRepository extends JpaRepository<ExercisesMetrics, UUID> {
    public Optional<ExercisesMetrics> findByName(String name);
}
