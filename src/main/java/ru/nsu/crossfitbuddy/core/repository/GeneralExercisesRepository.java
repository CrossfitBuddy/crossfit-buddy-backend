package ru.nsu.crossfitbuddy.core.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.nsu.crossfitbuddy.core.entity.Exercise;

public interface GeneralExercisesRepository extends JpaRepository<Exercise, UUID>, JpaSpecificationExecutor<Exercise> {

    @Query("SELECT e FROM Exercise e " +
            "LEFT JOIN FETCH e.exerciseEquipments " +
            "LEFT JOIN FETCH e.exercisesMetrics " +
            "LEFT JOIN FETCH e.image " +
            "LEFT JOIN FETCH e.video " +
            "WHERE e.id = :id")
    Optional<Exercise> findByIdWithDetails(@Param("id") UUID id);
}