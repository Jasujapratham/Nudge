package com.nudge.controller;

import com.nudge.dto.AuthResponse;
import com.nudge.dto.LoginRequest;
import com.nudge.dto.SignupRequest;
import com.nudge.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoints (no token required): sign up and log in.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/auth/signup - creates the account and returns a token straight away. */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        // 201 Created, because this endpoint creates a new resource.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** POST /api/auth/login - verifies credentials and returns a token. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
