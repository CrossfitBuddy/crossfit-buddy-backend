package ru.nsu.crossfitbuddy.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "password_reset_code", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetCode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount userAccount;

    @Column(nullable = false, length = 255)
    private String codeHash;

    @Column(nullable = false)
    private int failedAttempts;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant consumedAt;

    public PasswordResetCode(UserAccount userAccount) {
        this.userAccount = userAccount;
    }

    public void replace(String codeHash, Instant now, Instant expiresAt) {
        this.codeHash = codeHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
        this.failedAttempts = 0;
        this.consumedAt = null;
    }

    public void recordFailure() {
        failedAttempts = Math.incrementExact(failedAttempts);
    }

    public void consume(Instant now) {
        consumedAt = now;
    }
}
