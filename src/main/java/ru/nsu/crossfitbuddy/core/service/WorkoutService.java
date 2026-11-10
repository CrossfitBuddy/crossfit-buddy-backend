package ru.nsu.crossfitbuddy.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.crossfitbuddy.core.entity.CurrentUser;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.dto.response.WorkoutListResponse;
import ru.nsu.crossfitbuddy.core.mapper.WorkoutFilterMapper;
import ru.nsu.crossfitbuddy.core.mapper.WorkoutResponseMapper;
import ru.nsu.crossfitbuddy.core.repository.WorkoutRepository;
import ru.nsu.crossfitbuddy.core.workout.WorkoutListRequest;

@Service
@RequiredArgsConstructor
public class WorkoutService {
    private final WorkoutRepository workouts;
    private final WorkoutFilterMapper filterMapper;
    private final WorkoutResponseMapper responseMapper;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public WorkoutListResponse list(CurrentUser user, WorkoutListRequest request) {
        var filter = filterMapper.toFilter(request);
        var page = workouts.findAccessible(
            user.id(),
            UserRole.valueOf(user.activeRole()),
            filter
        );
        return responseMapper.toResponse(page, user.id());
    }
}
