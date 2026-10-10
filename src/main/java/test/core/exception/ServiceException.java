package test.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public class ServiceException extends RuntimeException {

    private final HttpStatus httpStatus;

    private final String message;

    private final Throwable throwable;

    public ServiceException(HttpStatus httpStatus, String message) {
        this(httpStatus, message, null);
    }
}
