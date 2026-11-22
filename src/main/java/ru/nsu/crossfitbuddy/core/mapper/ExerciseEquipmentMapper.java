package ru.nsu.crossfitbuddy.core.mapper;

import org.mapstruct.Mapper;
import ru.nsu.crossfitbuddy.core.dto.response.exercises.ExerciseEquipmentResponse;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;
import java.util.List;

@Mapper(config = CentralMapperConfig.class, uses = PropertiesRequestMapper.class)
public interface ExerciseEquipmentMapper {

    ExerciseEquipmentResponse toResponse(ExerciseEquipment equipment);

    List<ExerciseEquipmentResponse> toResponseList(List<ExerciseEquipment> equipment);
}
