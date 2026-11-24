package ru.nsu.crossfitbuddy.core.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.nsu.crossfitbuddy.core.entity.Exercise;

public interface GeneralExercisesRepository extends JpaRepository<Exercise, UUID> {
}
