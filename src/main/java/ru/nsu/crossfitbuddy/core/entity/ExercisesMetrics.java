package ru.nsu.crossfitbuddy.core.entity;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Entity
@Setter
@Table(name = "exercise_metric")
public class ExercisesMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MetricsValueType metricsValueType;

    @Pattern(regexp = "^[a-zA-Zа-яА-ЯёЁ ]+$", message = "Допускаются только буквы и пробелы")
    private String unit;

    @ManyToMany(mappedBy = "exercisesMetrics")
    private List<GeneralExercises> generalExercises;

}
