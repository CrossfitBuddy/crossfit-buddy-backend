package ru.nsu.crossfitbuddy.core.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;

public interface ExerciseEquipmentRepository extends JpaRepository<ExerciseEquipment, UUID> {
    public Optional<ExerciseEquipment> findByName(String name);
}
