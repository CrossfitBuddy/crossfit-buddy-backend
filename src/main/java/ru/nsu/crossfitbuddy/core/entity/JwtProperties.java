package ru.nsu.crossfitbuddy.core.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(@NotBlank String secret, @NotBlank String issuer, @NotNull Duration accessTokenTtl) {
    @Override
    public String toString() {
        return "JwtProperties[secret=REDACTED, issuer=" + issuer + ", accessTokenTtl=" + accessTokenTtl + "]";
    }
}
