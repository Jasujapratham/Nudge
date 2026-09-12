package com.nudge.exception;

/**
 * Thrown when a signup email is already registered. Mapped to HTTP 409 Conflict.
 */
public class EmailAlreadyUsedException extends RuntimeException {

    public EmailAlreadyUsedException(String email) {
        super("An account with email " + email + " already exists. Try logging in instead.");
    }
}
