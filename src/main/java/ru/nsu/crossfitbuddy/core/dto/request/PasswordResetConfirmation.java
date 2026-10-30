package ru.nsu.crossfitbuddy.core.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmation(
    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 320, message = "Email must not exceed 320 characters.")
    String email,

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Recovery code is required.")
    @Pattern(regexp = "[0-9]{6}", message = "Recovery code must contain exactly 6 digits.")
    String code,

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password is required.")
    @Size(min = 8, max = 30, message = "Password must contain between 8 and 30 characters.")
    @Pattern(regexp = "(?s)(?=.*\\p{L})(?=.*\\p{Nd})(?=.*[^\\p{L}\\p{Nd}\\s]).*",
        message = "Password must contain letters, digits, and special characters.")
    String password,

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password confirmation is required.")
    @Size(min = 8, max = 30, message = "Password confirmation must contain between 8 and 30 characters.")
    String passwordConfirmation) {

    public PasswordResetConfirmation {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "PasswordResetConfirmation[credentials=REDACTED]";
    }
}
