package com.example.taskmanager.common;

import java.time.Instant;

/**
 * Standard JSON error shape returned by the global exception handler.
 *
 * @param timestamp time the error response was created
 * @param status HTTP status code
 * @param error HTTP reason phrase
 * @param message client-safe explanation of the failure
 * @param path request path that failed
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
