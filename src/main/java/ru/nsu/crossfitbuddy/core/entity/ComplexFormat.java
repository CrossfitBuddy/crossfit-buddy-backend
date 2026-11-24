package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.Column;
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
@Table(name = "complex_format")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ComplexFormat {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ComplexFormatType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;
}