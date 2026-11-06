package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.Workout;

import java.util.Optional;
import java.util.UUID;

public interface WorkoutRepository extends JpaRepository<Workout, UUID> {
    @EntityGraph(attributePaths = {
        "stages",
        "stages.stageFormat",
        "stages.complexFormat",
        "stages.rounds",
        "stages.rounds.exercises",
        "stages.rounds.exercises.exercise"
    })
    Optional<Workout> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {"materials"})
    Optional<Workout> findWithMaterialsById(UUID id);
}
