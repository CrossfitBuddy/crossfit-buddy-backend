package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.nsu.crossfitbuddy.core.entity.GeneralExercises;

import java.util.Optional;
import java.util.UUID;

public interface GeneralExercisesRepository extends JpaRepository<GeneralExercises, UUID>, JpaSpecificationExecutor<GeneralExercises> {

    @Query("SELECT e FROM GeneralExercises e " +
        "LEFT JOIN FETCH e.exerciseEquipments eq " +
        "LEFT JOIN FETCH e.exercisesMetrics em " +
        "WHERE e.id = :id")
    Optional<GeneralExercises> findByIdWithDetails(@Param("id") UUID id);
}
