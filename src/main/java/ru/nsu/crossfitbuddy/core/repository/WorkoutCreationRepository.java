package ru.nsu.crossfitbuddy.core.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormat;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.entity.Exercise;
import ru.nsu.crossfitbuddy.core.entity.Workout;
import ru.nsu.crossfitbuddy.core.entity.WorkoutStage;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WorkoutCreationRepository {
    private final EntityManager entityManager;

    public List<WorkoutStage> lockStages(Set<Integer> ids) {
        return entityManager.createQuery(
                "select s from WorkoutStage s where s.id in :ids order by s.id", WorkoutStage.class)
            .setParameter("ids", ids)
            .setLockMode(LockModeType.PESSIMISTIC_READ)
            .getResultList();
    }

    public List<ComplexFormat> lockFormats(Set<ComplexFormatType> types) {
        return entityManager.createQuery(
                "select f from ComplexFormat f where f.type in :types order by f.type", ComplexFormat.class)
            .setParameter("types", types)
            .setLockMode(LockModeType.PESSIMISTIC_READ)
            .getResultList();
    }

    public List<Exercise> lockAvailableExercises(Set<UUID> ids, UUID owner) {
        return entityManager.createQuery("""
                select e from Exercise e
                where e.id in :ids and (e.isCommon = true or e.createdBy.id = :owner)
                order by e.id
                """, Exercise.class)
            .setParameter("ids", ids)
            .setParameter("owner", owner)
            .setLockMode(LockModeType.PESSIMISTIC_READ)
            .getResultList();
    }

    public void save(Workout workout) {
        entityManager.persist(workout);
        entityManager.flush();
    }
}
