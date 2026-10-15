package ru.nsu.crossfitbuddy.core.dto.response;

import org.springframework.http.HttpStatus;

public record ErrorResponse(
    HttpStatus httpStatus,
    String message
) {
}
