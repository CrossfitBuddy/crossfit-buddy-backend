package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.domain.Page;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.entity.Workout;
import ru.nsu.crossfitbuddy.core.workout.WorkoutFilter;
import java.util.UUID;

public interface WorkoutRepositoryCustom {
    Page<Workout> findAccessible(
        UUID userId,
        UserRole activeRole,
        WorkoutFilter filter
    );
}
