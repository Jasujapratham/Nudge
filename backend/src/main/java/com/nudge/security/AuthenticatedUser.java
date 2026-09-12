package com.nudge.security;

/**
 * The authenticated principal stored in the SecurityContext.
 *
 * <p>It carries only the id and email - controllers use {@code id} for the
 * ownership check, and nothing heavier (like the JPA entity with its lazy
 * collections) leaks into the web layer.</p>
 */
public record AuthenticatedUser(Long id, String email, String name) {

    public static AuthenticatedUser from(com.nudge.entity.User user) {
        return new AuthenticatedUser(user.getId(), user.getEmail(), user.getName());
    }
}
