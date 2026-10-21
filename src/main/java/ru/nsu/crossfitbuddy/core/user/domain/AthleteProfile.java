package ru.nsu.crossfitbuddy.core.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "athlete_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AthleteProfile {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserAccount user;

    @Min(50)
    @Max(250)
    @Column(name = "height_cm")
    private Integer heightCm;

    @DecimalMin("20")
    @DecimalMax("500")
    @Digits(integer = 4, fraction = 2)
    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "fitness_level", length = 20)
    private FitnessLevel fitnessLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "rx_preference", length = 20)
    private RxPreference rxPreference;

    @Version
    @Column(nullable = false)
    private Long version;

    public static AthleteProfile create(UserAccount user) {
        var profile = new AthleteProfile();
        profile.user = Objects.requireNonNull(user, "User account is required");
        profile.id = user.getId();
        return profile;
    }

    public void updateMeasurements(Integer heightCm, BigDecimal weightKg) {
        this.heightCm = heightCm;
        this.weightKg = weightKg;
    }

    public void updatePreferences(FitnessLevel fitnessLevel, RxPreference rxPreference) {
        this.fitnessLevel = fitnessLevel;
        this.rxPreference = rxPreference;
    }
}
