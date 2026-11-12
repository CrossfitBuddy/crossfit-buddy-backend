package ru.nsu.crossfitbuddy.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutFormatResponse;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormat;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormatType;
import ru.nsu.crossfitbuddy.core.exception.ApiException;
import ru.nsu.crossfitbuddy.core.mapper.WorkoutFormatMapper;
import ru.nsu.crossfitbuddy.core.repository.WorkoutFormatRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkoutFormatService {

    private final WorkoutFormatRepository workoutFormatRepository;

    private final WorkoutFormatMapper workoutFormatMapper;

    public List<WorkoutFormatResponse> getAll() {
        return workoutFormatRepository.findAll()
                .stream()
                .map(workoutFormatMapper::toResponse)
                .toList();
    }

    @Transactional
    public WorkoutFormatResponse updateDescription(WorkoutFormatType type, String description) {
        WorkoutFormat format = workoutFormatRepository.findById(type).orElseThrow(
                () -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "NOT_FOUND",
                        String.format("Формат комплекса с типом {%s} не найден", type)
                ));

        format.setDescription(description);
        workoutFormatRepository.save(format);

        return workoutFormatMapper.toResponse(format);
    }
}