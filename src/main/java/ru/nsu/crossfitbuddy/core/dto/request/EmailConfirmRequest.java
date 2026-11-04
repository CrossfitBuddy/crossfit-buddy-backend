package ru.nsu.crossfitbuddy.core.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailConfirmRequest(
    @NotBlank
    @Size(max = 2048)
    String token
) {
}
