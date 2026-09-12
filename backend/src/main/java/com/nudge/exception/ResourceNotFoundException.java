package com.nudge.exception;

/**
 * Thrown when a requested row does not exist, or when it exists but belongs to
 * somebody else. Both map to HTTP 404 on purpose: revealing "it exists but is
 * not yours" would let an attacker probe other people's reminder ids.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
