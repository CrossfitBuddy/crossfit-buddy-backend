package ru.nsu.crossfitbuddy.core.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.nsu.crossfitbuddy.core.dto.response.exercises.GeneralExerciseResponse;
import ru.nsu.crossfitbuddy.core.entity.GeneralExercises;

@Mapper(config = CentralMapperConfig.class, uses = ExerciseEquipmentMapper.class)
public interface GeneralExerciseMapper {

    @Mapping(target = "image", ignore = true)
    @Mapping(target = "videoExample", ignore = true)
    GeneralExerciseResponse toResponse(GeneralExercises generalExercises);

    List<GeneralExerciseResponse> toResponseList(List<GeneralExercises> generalExercises);
}
