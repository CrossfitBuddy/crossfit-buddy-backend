package ru.nsu.crossfitbuddy.core.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import ru.nsu.crossfitbuddy.core.entity.UserProfile;

import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from UserProfile profile where profile.id = :id")
    Optional<UserProfile> findForUpdate(@Param("id") UUID id);
}
