package ru.nsu.crossfitbuddy.core.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
    @NotBlank(message = "Email is required.")
    @Email(message = "Enter a valid email address.")
    @Size(max = 320, message = "Email must not exceed 320 characters.")
    String email) {

    public PasswordResetRequest {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "PasswordResetRequest[credentials=REDACTED]";
    }
}
