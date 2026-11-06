package ru.nsu.crossfitbuddy.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.crossfitbuddy.core.entity.UserRole;
import ru.nsu.crossfitbuddy.core.entity.Workout;
import ru.nsu.crossfitbuddy.core.repository.WorkoutParticipantRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkoutAccessService {
    private final WorkoutParticipantRepository participantRepo;

    public boolean canView(Workout w, UUID userId, UserRole activeRole) {
        return false;
    }
}
