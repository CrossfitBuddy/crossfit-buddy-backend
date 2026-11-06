package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "stage_format")
@Getter
@Setter
public class StageFormat {
    @Id
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;
}
