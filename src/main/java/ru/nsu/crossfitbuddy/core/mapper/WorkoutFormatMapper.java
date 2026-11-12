package ru.nsu.crossfitbuddy.core.mapper;

import org.mapstruct.Mapper;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormat;

@Mapper(config = CentralMapperConfig.class)
public interface WorkoutFormatMapper {
    WorkoutFormatResponse toResponse(WorkoutFormat workoutFormat);
}