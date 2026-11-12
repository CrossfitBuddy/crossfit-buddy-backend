package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workout_format")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkoutFormat {
    @Id
    @Enumerated(EnumType.STRING)
    private WorkoutFormatType type;

    private String name;

    private String description;

    private boolean durationRequired;

    private boolean intervalRequired;
}