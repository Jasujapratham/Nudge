package com.nudge.exception;

/**
 * Thrown when a new reminder is created for a moment that has already passed.
 * Mapped to HTTP 400.
 */
public class InvalidReminderTimeException extends RuntimeException {

    public InvalidReminderTimeException(String message) {
        super(message);
    }
}
