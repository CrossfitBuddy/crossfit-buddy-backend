package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.domain.Page;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.entity.Workout;
import ru.nsu.crossfitbuddy.core.dto.request.WorkoutFilter;

import java.util.Optional;
import java.util.UUID;

public interface WorkoutRepositoryCustom {
    Page<Workout> findAccessible(
        UUID userId,
        UserRole activeRole,
        WorkoutFilter filter
    );
    Optional<Workout> findAccessibleById(UUID id, UUID userId, UserRole activeRole);
}
