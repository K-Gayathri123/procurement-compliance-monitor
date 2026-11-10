package com.procurement.monitor.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.format.DateTimeParseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ProblemDetail problem(HttpStatus status, String message) {
        ProblemDetail detail =
            ProblemDetail.forStatusAndDetail(status, message);

        String requestId = MDC.get("requestId");

        if (requestId != null) {
            detail.setProperty("requestId", requestId);
        }

        return detail;
    }

    @ExceptionHandler(ApiException.class)
    public ProblemDetail application(ApiException exception) {
        return problem(
            exception.getStatus(), exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(
        MethodArgumentNotValidException exception
    ) {
        ProblemDetail detail = problem(
            HttpStatus.BAD_REQUEST,
            "Input validation failed"
        );

        detail.setProperty(
            "errors",
            exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                    error.getField() + ": "
                        + error.getDefaultMessage())
                .toList()
        );

        return detail;
    }

    @ExceptionHandler({
        ConstraintViolationException.class,
        HandlerMethodValidationException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class,
        DateTimeParseException.class,
        NumberFormatException.class,
        ArithmeticException.class
    })
    public ProblemDetail invalidInput(Exception exception) {
        return problem(
            HttpStatus.BAD_REQUEST,
            "Invalid input, date, or numeric precision"
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail uploadTooLarge(Exception exception) {
        return problem(
            HttpStatus.PAYLOAD_TOO_LARGE,
            "Upload exceeds the configured limit"
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accessDenied(Exception exception) {
        return problem(
            HttpStatus.FORBIDDEN, "Access denied"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail integrity(Exception exception) {
        return problem(
            HttpStatus.CONFLICT,
            "Request conflicts with existing database records"
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail unexpected(Exception exception) {
        // Do not log request bodies, passwords, JWTs, or SQL parameters.
        log.error(
            "Unhandled error; requestId={}, type={}",
            MDC.get("requestId"),
            exception.getClass().getSimpleName()
        );

        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unexpected server error"
        );
    }
}
