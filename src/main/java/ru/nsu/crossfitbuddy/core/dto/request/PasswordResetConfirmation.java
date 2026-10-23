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
    @Size(min = 14, max = 72, message = "Password must contain between 14 and 72 characters.")
    @Pattern(regexp = "(?s)(?=.*\\p{L})(?=.*\\p{Nd}).*", message = "Password must contain letters and digits.")
    String password,

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password confirmation is required.")
    @Size(max = 72, message = "Password confirmation must not exceed 72 characters.")
    String passwordConfirmation) {

    public PasswordResetConfirmation {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "PasswordResetConfirmation[credentials=REDACTED]";
    }
}
