package ru.nsu.crossfitbuddy.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutDetailsResponse;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.mapper.WorkoutMapper;
import ru.nsu.crossfitbuddy.core.repository.WorkoutRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkoutDetailsService {
    private final WorkoutRepository workoutRepository;
    private final WorkoutAccessService accessService;
    private final WorkoutMapper mapper;

    public Optional<WorkoutDetailsResponse> findDetails(UUID id, UUID userId, UserRole activeRole) {
        return workoutRepository.findWithDetailsById(id)
            .filter(w -> accessService.canView(w, userId, activeRole))
            .map(mapper::toDetails);
    }
}
