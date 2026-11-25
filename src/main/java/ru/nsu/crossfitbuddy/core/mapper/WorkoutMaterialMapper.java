package ru.nsu.crossfitbuddy.core.mapper;

import org.springframework.stereotype.Component;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutMaterialResponse;
import ru.nsu.crossfitbuddy.core.entity.WorkoutMaterial;

@Component
public class WorkoutMaterialMapper {

    public WorkoutMaterialResponse toResponse(WorkoutMaterial m) {
        return new WorkoutMaterialResponse(
            m.getWorkoutMaterialType().name(),
            m.getTitle(),
            m.getUrl()
        );
    }
}
