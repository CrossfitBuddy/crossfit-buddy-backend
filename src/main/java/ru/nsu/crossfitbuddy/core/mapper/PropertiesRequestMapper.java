package ru.nsu.crossfitbuddy.core.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import ru.nsu.crossfitbuddy.core.dto.response.exercises.EquipmentPropertiesResponse;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipmentProperties;

@Mapper(config = CentralMapperConfig.class)
public interface PropertiesRequestMapper {
    EquipmentPropertiesResponse toResponse(
            ExerciseEquipmentProperties equipmentProperties);

    List<EquipmentPropertiesResponse> toResponseList(
            List<ExerciseEquipmentProperties> equipmentProperties);
}
