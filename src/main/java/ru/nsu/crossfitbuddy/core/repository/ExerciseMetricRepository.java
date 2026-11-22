package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseMetricRepository extends JpaRepository<ExercisesMetrics, UUID> {
    Optional<ExercisesMetrics> findByName(String name);
}
