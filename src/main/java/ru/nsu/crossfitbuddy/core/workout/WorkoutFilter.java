package ru.nsu.crossfitbuddy.core.workout;

import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record WorkoutFilter(
    List<String> titlePatterns,
    Set<WorkoutStatus> statuses,
    Set<WorkoutType> sources,
    Instant dateFrom,
    Instant dateToExclusive,
    WorkoutSortingKey sort,
    int page,
    int size
) {
    public WorkoutFilter {
        titlePatterns = List.copyOf(titlePatterns);
        statuses = Set.copyOf(statuses);
        sources = Set.copyOf(sources);
    }
}
