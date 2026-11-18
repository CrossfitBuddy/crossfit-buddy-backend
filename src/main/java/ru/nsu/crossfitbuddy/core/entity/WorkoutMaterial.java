package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "workout_material")
@Getter
@Setter
public class WorkoutMaterial {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "material_type", columnDefinition = "material_type", nullable = false)
    private WorkoutMaterialType workoutMaterialType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;
}
