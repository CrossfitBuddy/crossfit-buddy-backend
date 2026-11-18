package ru.nsu.crossfitbuddy.core.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.dto.response.*;
import ru.nsu.crossfitbuddy.core.entity.*;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WorkoutResponseMapper {
    private final StageResponseMapper stageMapper;
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
        var stages = w.getStages().stream()
                .sorted(Comparator.comparingInt(stage -> stage.getStageFormat().getPosition()))
                .map(stageMapper::toResponse)
                .toList();

        var materials = w.getMaterials().stream()
                .map(materialMapper::toResponse)
                .toList();

        return new WorkoutDetailsResponse(
                w.getId(), w.getTitle(), w.getDescription(),
                w.getType(), w.getStatus(), totalDuration(w.getStages()),
                stages, materials
        );
    }

    private Integer totalDuration(List<Stage> stages) {
        if (stages.isEmpty()) {
            return null;
        }
        boolean anyMissing = stages.stream()
                .anyMatch(s -> s.getTimeLimitSeconds() == null);
        if (anyMissing) {
            return null;
        }
        return stages.stream()
                .mapToInt(Stage::getTimeLimitSeconds)
                .sum();
    }
}
