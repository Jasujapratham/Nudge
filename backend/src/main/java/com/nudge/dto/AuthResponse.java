package com.nudge.dto;

/**
 * Returned by signup and login: the JWT plus the user profile.
 */
public class AuthResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final UserResponse user;

    public AuthResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public UserResponse getUser() {
        return user;
    }
}
