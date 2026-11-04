package ru.nsu.crossfitbuddy.core.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExerciseEquipmentResponse;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;

@Mapper(config = CentralMapperConfig.class, uses = PropertiesRequestMapper.class)
public interface ExerciseEquipmentMapper {

    @Mapping(target = "image", ignore = true)
    ExerciseEquipmentResponse toResponse(ExerciseEquipment exerciseEquipment);

    List<ExerciseEquipmentResponse> toResponseList(List<ExerciseEquipment> exerciseEquipment);
}
