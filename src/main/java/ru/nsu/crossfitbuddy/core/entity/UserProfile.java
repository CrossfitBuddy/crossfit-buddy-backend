package ru.nsu.crossfitbuddy.core.entity;

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
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Entity
@Table(name = "user_profile")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile {
    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserAccount user;

    @Size(max = 100)
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Size(max = 100)
    @Column(name = "last_name", length = 100)
    private String lastName;

    @PastOrPresent
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Size(max = 120)
    @Column(length = 120)
    private String city;

    @Size(max = 5000)
    @Column(columnDefinition = "text")
    private String about;

    @Size(max = 512)
    @Column(name = "avatar_key", length = 512)
    private String avatarKey;

    @Column(name = "display_name", length = 201)
    private String legacyDisplayName;

    @Version
    @Column(nullable = false)
    private Long version;

    public static UserProfile create(UserAccount user, String firstName, String lastName) {
        var profile = new UserProfile();
        profile.user = Objects.requireNonNull(user, "User account is required");
        profile.id = user.getId();
        profile.firstName = normalize(firstName);
        profile.lastName = normalize(lastName);
        return profile;
    }

    public void updateDetails(String firstName, String lastName, LocalDate birthDate, Gender gender, String city,
        String about, String avatarKey) {
        this.firstName = normalize(firstName);
        this.lastName = normalize(lastName);
        this.birthDate = birthDate;
        this.gender = gender;
        this.city = normalize(city);
        this.about = normalize(about);
        this.avatarKey = normalize(avatarKey);
    }

    public String getDisplayName() {
        String name = Stream.of(firstName, lastName)
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .collect(Collectors.joining(" "));
        return name.isEmpty() ? legacyDisplayName : name;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
