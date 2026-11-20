package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "complex")
@Getter
@Setter
public class Complex {
    @Id
    @GeneratedValue
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

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "target_rounds")
    private Integer targetRounds;

    @Column(name = "interval_seconds")
    private Integer intervalSeconds;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "complex", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplexRound> rounds = new ArrayList<>();
}
