package ru.nsu.crossfitbuddy.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.nsu.crossfitbuddy.core.dto.request.exercises.ExercisesMetricsRequest;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExercisesMetricsResponse;
import ru.nsu.crossfitbuddy.core.entity.ExercisesMetrics;

import java.util.List;

@Mapper(config = CentralMapperConfig.class)
public interface ExercisesMetricsMapper {

    ExercisesMetricsResponse toResponse(ExercisesMetrics entity);

    List<ExercisesMetricsResponse> toResponseList(List<ExercisesMetrics> entities);

    @Mapping(target = "generalExercises", ignore = true)
    ExercisesMetrics toEntity(ExercisesMetricsRequest request);

    @Mapping(target = "generalExercises", ignore = true)
    void updateEntityFromRequest(ExercisesMetricsRequest request, @MappingTarget ExercisesMetrics entity);
}