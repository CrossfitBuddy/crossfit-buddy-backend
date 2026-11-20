package ru.nsu.crossfitbuddy.core.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.nsu.crossfitbuddy.core.dto.response.exercises.GeneralExerciseResponse;
import ru.nsu.crossfitbuddy.core.entity.Exercise;

@Mapper(config = CentralMapperConfig.class, uses = ExerciseEquipmentMapper.class)
public interface GeneralExerciseMapper {

    @Mapping(source = "image.id", target = "imageFileId")
    @Mapping(source = "video.id", target = "videoFileId")
    GeneralExerciseResponse toResponse(Exercise generalExercises);

    List<GeneralExerciseResponse> toResponseList(List<Exercise> generalExercises);
}
