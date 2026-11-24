package ru.nsu.crossfitbuddy.core.mapper;

import org.mapstruct.Mapper;
import ru.nsu.crossfitbuddy.core.dto.response.ComplexFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormat;

@Mapper(config = CentralMapperConfig.class)
public interface ComplexFormatMapper {
    ComplexFormatResponse toResponse(ComplexFormat complexFormat);
}