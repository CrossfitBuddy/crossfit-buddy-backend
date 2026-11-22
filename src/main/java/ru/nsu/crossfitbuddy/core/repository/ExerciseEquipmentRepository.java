package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.ExerciseEquipment;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseEquipmentRepository extends JpaRepository<ExerciseEquipment, UUID> {
    Optional<ExerciseEquipment> findByName(String name);
}
