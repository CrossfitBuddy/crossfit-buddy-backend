package ru.nsu.crossfitbuddy.core.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "trainer_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainerProfile {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserAccount user;

    @Size(max = 5000)
    @Column(columnDefinition = "text")
    private String specializations;

    @PositiveOrZero
    @Column(name = "experience_years")
    private Integer experienceYears;

    @Size(max = 5000)
    @Column(columnDefinition = "text")
    private String methodology;

    @Version
    @Column(nullable = false)
    private Long version;

    public static TrainerProfile create(UserAccount user) {
        var profile = new TrainerProfile();
        profile.user = Objects.requireNonNull(user, "User account is required");
        profile.id = user.getId();
        return profile;
    }

    public void updateDetails(String specializations, Integer experienceYears, String methodology) {
        this.specializations = normalize(specializations);
        this.experienceYears = experienceYears;
        this.methodology = normalize(methodology);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
