package ru.nsu.crossfitbuddy.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.crossfitbuddy.core.dto.response.ComplexFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormat;
import ru.nsu.crossfitbuddy.core.entity.ComplexFormatType;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mapper.ComplexFormatMapper;
import ru.nsu.crossfitbuddy.core.repository.ComplexFormatRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplexFormatService {

    private final ComplexFormatRepository complexFormatRepository;

    private final ComplexFormatMapper complexFormatMapper;

    public List<ComplexFormatResponse> getAll() {
        return complexFormatRepository.findAll()
                .stream()
                .map(complexFormatMapper::toResponse)
                .toList();
    }

    @Transactional
    public ComplexFormatResponse updateDescription(ComplexFormatType type, String description) {
        ComplexFormat format = complexFormatRepository.findById(type).orElseThrow(
                () -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND",
                        String.format("Формат комплекса с типом {%s} не найден", type)
                ));

        format.setDescription(description);
        complexFormatRepository.save(format);

        return complexFormatMapper.toResponse(format);
    }
}