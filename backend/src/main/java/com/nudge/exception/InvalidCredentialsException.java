package com.nudge.exception;

/**
 * Thrown when the email is unknown or the password does not match. Mapped to
 * HTTP 401.
 *
 * <p>The message is intentionally vague on purpose: telling a caller whether the
 * email exists would help an attacker enumerate registered accounts.</p>
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Email or password is incorrect.");
    }
}
