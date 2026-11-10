package com.procurement.monitor.validation;

import com.procurement.monitor.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

public final class Checks {

    private Checks() {}

    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST, message
            );
        }
    }

    public static void conflict(boolean condition, String message) {
        if (condition) {
            throw new ApiException(
                HttpStatus.CONFLICT, message
            );
        }
    }

    public static void forbidden(boolean condition, String message) {
        if (condition) {
            throw new ApiException(
                HttpStatus.FORBIDDEN, message
            );
        }
    }

    public static <T> T found(Optional<T> value, String message) {
        return value.orElseThrow(() ->
            new ApiException(
                HttpStatus.NOT_FOUND, message
            )
        );
    }

    public static <T> T found(Optional<T> value) {
        return found(value, "Record not found");
    }
}
