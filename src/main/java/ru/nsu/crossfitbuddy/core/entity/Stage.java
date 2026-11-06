package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "stage")
@Getter
@Setter
public class Stage {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "format_id", nullable = false)
    private StageFormat stageFormat;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @Column(name = "target_rounds")
    private Integer targetRounds;

    @Column(name = "interval_seconds")
    private Integer intervalSeconds;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "stage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StageRound> rounds = new ArrayList<>();
}
