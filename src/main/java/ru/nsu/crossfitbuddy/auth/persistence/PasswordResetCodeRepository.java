package ru.nsu.crossfitbuddy.auth.persistence;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PasswordResetCodeRepository {
    private final JdbcTemplate jdbc;

    public Optional<ResetCode> findLatest(UUID userId) {
        return jdbc.query("""
            select id, code_hash, failed_attempts, expires_at, consumed_at
            from password_reset_code where user_id = ?
            order by created_at desc, id desc limit 1
            """, (rs, row) -> new ResetCode(rs.getObject("id", UUID.class), rs.getString("code_hash"),
                rs.getInt("failed_attempts"), rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("consumed_at") != null), userId).stream().findFirst();
    }

    public void replace(UUID userId, String hash, Instant now, Instant expiresAt) {
        jdbc.update("delete from password_reset_code where user_id = ?", userId);
        jdbc.update("""
            insert into password_reset_code(user_id, code_hash, created_at, expires_at)
            values (?, ?, ?, ?)
            """, userId, hash, Timestamp.from(now), Timestamp.from(expiresAt));
    }

    public void recordFailure(UUID id) {
        jdbc.update("update password_reset_code set failed_attempts = failed_attempts + 1 where id = ?", id);
    }

    public void consume(UUID id, Instant now) {
        jdbc.update("update password_reset_code set consumed_at = ? where id = ?", Timestamp.from(now), id);
    }

    public record ResetCode(UUID id, String hash, int failedAttempts, Instant expiresAt, boolean consumed) {
        @Override
        public String toString() {
            return "ResetCode[credentials=REDACTED]";
        }
    }
}
