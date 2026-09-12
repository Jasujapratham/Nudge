package com.nudge.service;

import com.nudge.dto.AuthResponse;
import com.nudge.dto.LoginRequest;
import com.nudge.dto.SignupRequest;
import com.nudge.dto.UserResponse;
import com.nudge.entity.User;
import com.nudge.exception.EmailAlreadyUsedException;
import com.nudge.exception.InvalidCredentialsException;
import com.nudge.repository.UserRepository;
import com.nudge.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for signing up and logging in.
 *
 * <p>Passwords are hashed with BCrypt on the way in and compared with
 * {@code passwordEncoder.matches} on the way in to login. The stored hash is
 * never sent back to the client.</p>
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyUsedException(email);
        }

        User user = new User(
                request.getName().trim(),
                email,
                passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim())
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }
}
