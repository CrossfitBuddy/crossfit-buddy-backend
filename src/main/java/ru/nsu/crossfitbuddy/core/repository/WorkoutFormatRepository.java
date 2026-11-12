package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormat;
import ru.nsu.crossfitbuddy.core.entity.WorkoutFormatType;

public interface WorkoutFormatRepository extends JpaRepository<WorkoutFormat, WorkoutFormatType> {}