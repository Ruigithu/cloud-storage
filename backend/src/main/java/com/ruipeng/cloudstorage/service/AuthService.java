package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.config.security.JWTService;
import com.ruipeng.cloudstorage.dto.request.LoginRequest;
import com.ruipeng.cloudstorage.dto.request.SignupRequest;
import com.ruipeng.cloudstorage.dto.response.AuthResponse;
import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.exception.BusinessException;
import com.ruipeng.cloudstorage.mappers.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication service for handling user login and registration.
 *
 * This service is responsible for:
 * - User authentication and token generation
 * - New user registration and validation
 */
@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructor injection for all dependencies.
     */
    public AuthService(AuthenticationManager authenticationManager,
                       JWTService jwtService,
                       UserMapper userMapper,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param loginRequest the login credentials
     * @return authentication response with token
     * @throws BusinessException if authentication fails
     */
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticateUser(loginRequest);
            setSecurityContext(authentication);
            String token = generateToken(loginRequest.getEmail());

            return createSuccessResponse(token, loginRequest.getEmail());
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {} - Invalid credentials", loginRequest.getEmail());
            throw new BusinessException("AUTH_001", "Invalid email or password");
        } catch (Exception e) {
            log.error("Login failed for user: {}", loginRequest.getEmail(), e);
            throw new BusinessException("AUTH_002", "Login failed: " + e.getMessage());
        }
    }

    /**
     * Registers a new user in the system.
     *
     * @param signupRequest the registration details
     * @return authentication response confirming registration
     * @throws BusinessException if registration fails
     */
    @Transactional
    public AuthResponse signup(SignupRequest signupRequest) {
        validateEmailNotExists(signupRequest.getEmail());

        try {
            User newUser = createUserFromRequest(signupRequest);
            saveUser(newUser);

            log.info("New user created with email: {}", signupRequest.getEmail());
            return new AuthResponse(true, "User registered successfully");
        } catch (Exception e) {
            log.error("Signup failed for email: {}", signupRequest.getEmail(), e);
            throw new BusinessException("AUTH_004", "Signup failed: " + e.getMessage());
        }
    }

    /**
     * Authenticates user credentials.
     */
    private Authentication authenticateUser(LoginRequest loginRequest) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
    }

    /**
     * Sets the authentication in the security context.
     */
    private void setSecurityContext(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * Generates a JWT token for the user.
     */
    private String generateToken(String email) {
        return jwtService.generateToken(email);
    }

    /**
     * Creates a successful authentication response.
     */
    private AuthResponse createSuccessResponse(String token, String email) {
        return new AuthResponse(true, token, email);
    }

    /**
     * Validates that the email is not already registered.
     */
    private void validateEmailNotExists(String email) {
        User existingUser = userMapper.findByEmail(email);
        if (existingUser != null) {
            log.warn("Signup failed: Email already exists - {}", email);
            throw new BusinessException("AUTH_003", "Email already exists");
        }
    }

    /**
     * Creates a User entity from signup request.
     */
    private User createUserFromRequest(SignupRequest signupRequest) {
        User newUser = new User();
        newUser.setEmail(signupRequest.getEmail());
        newUser.setPasswordHash(encodePassword(signupRequest.getPassword()));
        newUser.setName(signupRequest.getName());
        return newUser;
    }

    /**
     * Encodes the plain text password.
     */
    private String encodePassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    /**
     * Saves the user to the database.
     */
    private void saveUser(User user) {
        userMapper.insertUser(user);
    }
}
