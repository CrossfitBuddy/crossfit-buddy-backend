package ru.nsu.crossfitbuddy.core.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Setter
@Table(name = "exercise")
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private boolean isCommon = false;

    @Column(nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseCategory category;

    @ManyToOne
    @JoinColumn
    private S3File image;

    @ManyToOne
    @JoinColumn
    private S3File video;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "exercises_equipments", joinColumns = @JoinColumn(name = "general_exercise_id"), inverseJoinColumns = @JoinColumn(name = "exercise_equipment_id"))
    private List<ExerciseEquipment> exerciseEquipments;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(name = "exercises_metrics", joinColumns = @JoinColumn(name = "general_exercise_id"), inverseJoinColumns = @JoinColumn(name = "exercise_metric_id"))
    private List<ExercisesMetrics> exercisesMetrics;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

}
