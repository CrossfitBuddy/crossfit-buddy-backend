package ru.nsu.crossfitbuddy.core.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutDetailsResponse;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutSummaryResponse;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutListResponse;
import ru.nsu.crossfitbuddy.core.entity.Complex;
import ru.nsu.crossfitbuddy.core.entity.Workout;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorkoutResponseMapper {
    private final ComplexResponseMapper complexMapper;
    private final WorkoutMaterialMapper materialMapper;

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

    public WorkoutDetailsResponse toDetails(Workout w) {
        var complexes = w.getComplexes().stream()
                .sorted(Comparator.comparingInt(c -> c.getWorkoutStage().getPosition()))
                .map(complexMapper::toResponse)
                .toList();

        var materials = w.getMaterials().stream()
                .map(materialMapper::toResponse)
                .toList();

        return new WorkoutDetailsResponse(
                w.getId(), w.getTitle(), w.getDescription(),
                w.getType(), w.getStatus(), totalDuration(w.getComplexes()),
                complexes, materials
        );
    }

    private Integer totalDuration(List<Complex> complexes) {
        if (complexes.isEmpty()) {
            return null;
        }
        boolean anyMissing = complexes.stream()
                .anyMatch(s -> s.getTimeLimitSeconds() == null);
        if (anyMissing) {
            return null;
        }
        return complexes.stream()
                .mapToInt(Complex::getTimeLimitSeconds)
                .sum();
    }
}
