package ru.nsu.crossfitbuddy.common.api;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Comparator;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> uploadTooLarge(HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "AVATAR_TOO_LARGE", "Avatar must not exceed 5 MB.", request);
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    ResponseEntity<ApiError> missingPart(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "The file part is required.", request);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> application(ApiException exception, HttpServletRequest request) {
        return error(exception.status(), exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(AuthenticationException exception, HttpServletRequest request) {
        String code = exception instanceof DisabledException ? "ACCOUNT_DISABLED" : "INVALID_CREDENTIALS";
        String message = exception instanceof DisabledException ? "Your account is blocked. Contact an administrator."
            : "Invalid email or password.";
        return error(HttpStatus.UNAUTHORIZED, code, message, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to perform this action.",
            request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformed(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
            "The request contains malformed JSON or an invalid field value.", request);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrentUpdate(OptimisticLockingFailureException exception, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CONCURRENT_UPDATE",
            "The data was changed by another request. Please try again.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .sorted(Comparator
                .comparingInt(GlobalExceptionHandler::validationPriority)
                .thenComparing(FieldError::getField)
                .thenComparing(FieldError::getDefaultMessage, Comparator.nullsLast(Comparator.naturalOrder())))
            .findFirst()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .orElse("The request contains invalid data.");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    private static int validationPriority(FieldError fieldError) {
        String code = fieldError.getCode();
        return "NotBlank".equals(code) || "NotNull".equals(code) ? 0 : 1;
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
            .body(new ApiError(Instant.now(), status.value(), code, message, request.getRequestURI()));
    }
}
