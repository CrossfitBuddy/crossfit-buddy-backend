package ru.nsu.crossfitbuddy.core.user.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import ru.nsu.crossfitbuddy.core.user.domain.UserProfile;

import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
}
