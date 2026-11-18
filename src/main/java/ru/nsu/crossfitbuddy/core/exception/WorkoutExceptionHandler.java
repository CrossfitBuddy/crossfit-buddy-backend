package ru.nsu.crossfitbuddy.core.exception;

import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ru.nsu.crossfitbuddy.core.controller.WorkoutController;
import ru.nsu.crossfitbuddy.core.dto.response.ErrorResponse;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutErrorResponse;
import ru.nsu.crossfitbuddy.core.dto.workout.WorkoutFieldError;
import ru.nsu.crossfitbuddy.core.dto.workout.json.UnsupportedWorkoutFieldException;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice(assignableTypes = WorkoutController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WorkoutExceptionHandler {
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> unsupportedMediaType(HttpServletRequest request) {
        var body = new ErrorResponse(Instant.now(), 415, "UNSUPPORTED_MEDIA_TYPE",
            "Content-Type must be application/json.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(WorkoutValidationException.class)
    ResponseEntity<WorkoutErrorResponse> references(
        WorkoutValidationException exception, HttpServletRequest request) {
        return error("VALIDATION_ERROR", exception.fieldErrors(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<WorkoutErrorResponse> validation(
        MethodArgumentNotValidException exception, HttpServletRequest request) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
            .map(field -> {
                String path = field.getField().replaceAll("\\.params\\[([^\\]]+)\\]", ".params.$1");
                return new WorkoutFieldError(path, "INVALID_VALUE", field.getDefaultMessage());
            })
            .sorted(Comparator.comparing(WorkoutFieldError::field).thenComparing(WorkoutFieldError::message))
            .distinct()
            .toList();
        return error("VALIDATION_ERROR", errors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<WorkoutErrorResponse> malformed(
        HttpMessageNotReadableException exception, HttpServletRequest request) {
        String field = "$";
        String unknownField = null;
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof UnsupportedWorkoutFieldException unsupported) {
                unknownField = unsupported.field();
            }
            if (cause instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
                var path = new StringBuilder();
                for (var reference : mapping.getPath()) {
                    if (reference.getFieldName() != null) {
                        if (!path.isEmpty()) {
                            path.append('.');
                        }
                        path.append(reference.getFieldName());
                    } else if (reference.getIndex() >= 0) {
                        path.append('[').append(reference.getIndex()).append(']');
                    }
                }
                field = path.toString();
            }
        }
        if (unknownField != null
            && !field.equals(unknownField)
            && !field.endsWith("." + unknownField)) {
            field = field.equals("$") ? unknownField : field + "." + unknownField;
        }
        var fieldError = new WorkoutFieldError(
            field, "INVALID_VALUE", "Malformed JSON, unsupported field or invalid field value.");
        return error("INVALID_REQUEST", List.of(fieldError), request);
    }

    private static ResponseEntity<WorkoutErrorResponse> error(
        String code, List<WorkoutFieldError> errors, HttpServletRequest request) {
        var body = new WorkoutErrorResponse(
            Instant.now(), 400, code, "The workout contains invalid data.", request.getRequestURI(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
