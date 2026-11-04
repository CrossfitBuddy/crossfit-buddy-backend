package ru.nsu.crossfitbuddy.core.entity;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Setter
@Table(name = "general_exercise")
public class GeneralExercises {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseCategory category;

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "exercises_equipments", joinColumns = @JoinColumn(name = "general_exercise_id"), inverseJoinColumns = @JoinColumn(name = "exercise_equipment_id"))
    private List<ExerciseEquipment> exerciseEquipments;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "exercises_metrics", joinColumns = @JoinColumn(name = "general_exercise_id"), inverseJoinColumns = @JoinColumn(name = "exercise_metric_id"))
    private List<ExercisesMetrics> exercisesMetrics;

}
