package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.dto.response.AuthResponse;
import com.ruipeng.cloudstorage.dto.request.LoginRequest;
import com.ruipeng.cloudstorage.dto.request.SignupRequest;
import com.ruipeng.cloudstorage.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication operations.
 * Handles user login and registration.
 *
 * Endpoints:
 * - POST /api/auth/login - User login
 * - POST /api/auth/signup - User registration
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Constructor with dependency injection.
     *
     * @param authService the authentication service
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest the login credentials
     * @return response with authentication token
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        logLoginAttempt(loginRequest.getEmail());

        AuthResponse response = authService.login(loginRequest);

        logLoginSuccess(loginRequest.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new user in the system.
     *
     * @param signupRequest the registration information
     * @return response confirming registration
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        logSignupAttempt(signupRequest.getEmail());

        AuthResponse response = authService.signup(signupRequest);

        logSignupSuccess(signupRequest.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Logs login attempt.
     */
    private void logLoginAttempt(String email) {
        log.info("Login attempt for user: {}", email);
    }

    /**
     * Logs successful login.
     */
    private void logLoginSuccess(String email) {
        log.info("User logged in successfully: {}", email);
    }

    /**
     * Logs signup attempt.
     */
    private void logSignupAttempt(String email) {
        log.info("Signup attempt for email: {}", email);
    }

    /**
     * Logs successful signup.
     */
    private void logSignupSuccess(String email) {
        log.info("User registered successfully: {}", email);
    }
}