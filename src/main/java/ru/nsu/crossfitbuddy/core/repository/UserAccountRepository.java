package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.nsu.crossfitbuddy.core.entity.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findById(UUID id);

    boolean existsByEmail(String email);
}
