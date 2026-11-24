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

@Entity
@Table(name = "complex_round")
@Getter
@Setter
@NoArgsConstructor
public class ComplexRound {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complex_id", nullable = false)
    private Complex complex;

    private int position;
    @Column(name = "repeat_count", nullable = false)
    private int repeatCount;

    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position")
    private List<RoundExercise> exercises = new ArrayList<>();

    public void addExercise(RoundExercise exercise) {
        exercises.add(exercise);
        exercise.setRound(this);
    }
}
