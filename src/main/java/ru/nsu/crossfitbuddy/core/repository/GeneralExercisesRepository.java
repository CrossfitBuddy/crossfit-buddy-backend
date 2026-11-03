package ru.nsu.crossfitbuddy.core.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.nsu.crossfitbuddy.core.entity.GeneralExercises;

public interface GeneralExercisesRepository extends JpaRepository<GeneralExercises, UUID> {
}
