package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity(name = "WorkoutComplex")
@Table(name = "complex")
@Getter
@Setter
@NoArgsConstructor
public class Complex {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_stage_id", nullable = false)
    private WorkoutStage workoutStage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complex_format_type", nullable = false)
    private ComplexFormat complexFormat;

    private int position;
    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;
    @Column(name = "interval_seconds")
    private Integer intervalSeconds;
    @Column(name = "target_rounds")
    private Integer targetRounds;

    @OneToMany(mappedBy = "complex", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<ComplexRound> rounds = new ArrayList<>();

    public void addRound(ComplexRound round) {
        rounds.add(round);
        round.setComplex(this);
    }
}
