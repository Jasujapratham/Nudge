package com.nudge.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Single JSON shape used by every error response, so the frontend can always
 * read {@code response.data.message} (and optionally {@code response.data.errors}).
 */
public record ApiErrorResponse(
        int status,
        String message,
        Map<String, String> errors,
        LocalDateTime timestamp
) {

    public static ApiErrorResponse of(int status, String message) {
        return new ApiErrorResponse(status, message, null, LocalDateTime.now());
    }

    public static ApiErrorResponse of(int status, String message, Map<String, String> errors) {
        return new ApiErrorResponse(status, message, errors, LocalDateTime.now());
    }
}
