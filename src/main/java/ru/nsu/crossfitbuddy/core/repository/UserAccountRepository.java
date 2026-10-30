package ru.nsu.crossfitbuddy.core.repository;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.nsu.crossfitbuddy.core.entity.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByEmail(String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from UserAccount account where account.email = :email")
    Optional<UserAccount> findByEmailForUpdate(@Param("email") String email);

    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByIdAndDeletedFalseAndEnabledTrue(UUID id);

    boolean existsByEmail(String email);
}
