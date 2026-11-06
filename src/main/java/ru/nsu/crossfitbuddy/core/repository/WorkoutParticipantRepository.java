package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.nsu.crossfitbuddy.core.entity.WorkoutParticipant;

import java.util.UUID;

public interface WorkoutParticipantRepository extends JpaRepository<WorkoutParticipant, UUID> {
    @Query("""
        select (count(p) > 0) from WorkoutParticipant p
        where p.workout.id = :workoutId and p.athlete.id = :userId
          and p.trainer is not null
    """)
    boolean existsAssignedAthlete(UUID workoutId, UUID userId);

    @Query("""
        select (count(p) > 0) from WorkoutParticipant p
        where p.workout.id = :workoutId and p.athlete.id = :userId
          and p.trainer is null
    """)
    boolean existsSharedParticipant(UUID workoutId, UUID userId);

    @Query("""
        select (count(p) > 0) from WorkoutParticipant p
        where p.workout.id = :workoutId and p.trainer.id = :trainerId
    """)
    boolean existsByWorkoutIdAndTrainerId(UUID workoutId, UUID trainerId);
}
