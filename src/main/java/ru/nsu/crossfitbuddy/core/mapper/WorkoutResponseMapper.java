package ru.nsu.crossfitbuddy.core.mapper;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutListResponse;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutSummaryResponse;
import ru.nsu.crossfitbuddy.core.entity.Workout;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;

import java.util.UUID;

@Component
public class WorkoutResponseMapper {
    public WorkoutSummaryResponse toResponse(Workout workout, UUID userId) {
        return new WorkoutSummaryResponse(
            workout.getId(),
            workout.getTitle(),
            workout.getCreatedAt(),
            workout.getStatus(),
            workout.getCreatedBy().equals(userId) ? WorkoutType.OWN : workout.getType()
        );
    }

    public WorkoutListResponse toResponse(Page<Workout> page, UUID userId) {
        return new WorkoutListResponse(
            page.getContent().stream()
                .map(workout -> toResponse(workout, userId)).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
