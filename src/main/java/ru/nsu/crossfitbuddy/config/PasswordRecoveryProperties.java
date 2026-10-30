package ru.nsu.crossfitbuddy.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.password-recovery")
public record PasswordRecoveryProperties(
    @NotNull @DurationMin(seconds = 1) Duration codeTtl,
    @Min(1) int maxAttempts) {
}
