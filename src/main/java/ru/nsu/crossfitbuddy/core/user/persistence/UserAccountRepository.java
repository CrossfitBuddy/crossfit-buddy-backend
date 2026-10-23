package ru.nsu.crossfitbuddy.core.user.persistence;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import ru.nsu.crossfitbuddy.core.user.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findById(UUID id);

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByIdAndDeletedFalseAndEnabledTrue(UUID id);

    boolean existsByEmail(String email);
}
