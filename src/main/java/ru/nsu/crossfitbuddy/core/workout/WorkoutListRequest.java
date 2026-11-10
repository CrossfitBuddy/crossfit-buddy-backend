package ru.nsu.crossfitbuddy.core.workout;

import ru.nsu.crossfitbuddy.core.entity.WorkoutStatus;
import ru.nsu.crossfitbuddy.core.entity.WorkoutType;

import java.time.LocalDate;
import java.util.List;

public record WorkoutListRequest(
    String q,
    List<WorkoutStatus> statuses,
    List<WorkoutType> sources,
    LocalDate dateFrom,
    LocalDate dateTo,
    WorkoutSortingKey sort,
    int page,
    int size
) {}
