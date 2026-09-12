package com.nudge.dto;

import com.nudge.entity.User;

/**
 * Public view of a user. Deliberately has no password field, so a hash can
 * never leak through an API response.
 */
public class UserResponse {

    private final Long id;
    private final String name;
    private final String email;

    public UserResponse(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
