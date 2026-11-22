package ru.nsu.crossfitbuddy.core.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExercisesMetricsResponse;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;

@Mapper(config = CentralMapperConfig.class)
public interface ExercisesMetricsMapper {

    ExercisesMetricsResponse toResponse(ExercisesMetrics exercisesMetrics);

    List<ExercisesMetricsResponse> toResponseList(List<ExercisesMetrics> exercisesMetrics);

}
