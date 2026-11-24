package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workout_stage")
@Getter
@Setter
@NoArgsConstructor
public class WorkoutStage {
    @Id
    private Integer id;
    private String code;
    private String name;
    private String description;
    private int position;
}
