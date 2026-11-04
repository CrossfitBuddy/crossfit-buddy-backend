package ru.nsu.crossfitbuddy.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.nsu.crossfitbuddy.core.entity.EmailToken;

import java.util.Optional;
import java.util.UUID;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {
    Optional<EmailToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
    update EmailToken t
        set t.usedAt = CURRENT_TIMESTAMP
            where t.userAccountId = :userAccountId and t.usedAt is null
    """)
    void invalidateOldTokens(UUID userAccountId);
}
