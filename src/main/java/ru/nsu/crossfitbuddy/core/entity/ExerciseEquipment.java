package ru.nsu.crossfitbuddy.core.entity;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Setter
@Table(name = "exercise_equipment")
public class ExerciseEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String imageKey;

    @OneToMany(mappedBy = "exerciseEquipment", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<ExerciseEquipmentProperties> equipmentProperties;

    @ManyToMany(mappedBy = "exerciseEquipments")
    private List<GeneralExercises> generalExercises;

}
