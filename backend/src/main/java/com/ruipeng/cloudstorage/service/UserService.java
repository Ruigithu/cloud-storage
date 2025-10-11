package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.exception.BusinessException;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user operations.
 *
 * Responsibilities:
 * - User registration
 * - User information retrieval
 * - Password management
 */
@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new user in the system.
     *
     * @param user the user to register
     * @throws BusinessException if email already exists or password is invalid
     */
    @Transactional
    public void register(User user) {
        validateRegistration(user);

        String encodedPassword = encodePassword(user.getPasswordHash());
        user.setPasswordHash(encodedPassword);

        saveUser(user);

        log.info("User registered successfully: email={}", user.getEmail());
    }

    /**
     * Gets user ID by email address.
     *
     * @param email the user's email
     * @return the user ID
     * @throws ResourceNotFoundException if user not found
     */
    public Long getUserId(String email) {
        validateEmail(email);

        Long userId = userMapper.getUserId(email);

        if (userId == null) {
            log.warn("User not found with email: {}", email);
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        return userId;
    }



    // ============ Private Helper Methods ============

    private void validateRegistration(User user) {
        validateEmail(user.getEmail());
        validateEmailNotExists(user.getEmail());
        validatePasswordNotEmpty(user.getPasswordHash());
        validateName(user.getName());
    }

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }

        if (!isValidEmailFormat(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    private boolean isValidEmailFormat(String email) {
        // Simple email validation
        return email.contains("@") && email.contains(".");
    }

    private void validateEmailNotExists(String email) {
        User existingUser = userMapper.findByEmail(email);

        if (existingUser != null) {
            log.warn("Registration failed: Email already exists - {}", email);
            throw new BusinessException("USER_001", "Email already exists");
        }
    }

    private void validatePasswordNotEmpty(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
    }

    private String encodePassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }

    private void saveUser(User user) {
        int rows = userMapper.insertUser(user);

        if (rows == 0) {
            throw new RuntimeException("Failed to save user");
        }
    }

}