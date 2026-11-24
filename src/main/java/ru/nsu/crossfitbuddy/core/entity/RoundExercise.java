package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "round_exercise")
@Getter
@Setter
@NoArgsConstructor
public class RoundExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "round_id", nullable = false)
    private ComplexRound round;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;

    private int position;
    private Integer repetitions;
    @Column(name = "weight_kg", precision = 7, scale = 2)
    private BigDecimal weightKg;
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    @Column(name = "distance_meters")
    private Integer distanceMeters;
    @Column(name = "height_cm")
    private Integer heightCm;
    private String notes;
}
