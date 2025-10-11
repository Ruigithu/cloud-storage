package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.dto.UserInfo;
import com.ruipeng.cloudstorage.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user information operations.
 * Handles user profile retrieval.
 */
@RestController
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    /**
     * Constructor with dependency injection.
     *
     * @param userService the user service
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Gets current user information.
     *
     * @param userDetails the authenticated user details
     * @return user information
     */
    @GetMapping("/getUserInfo")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        logGetUserInfo(userDetails.getUsername());

        String email = userDetails.getUsername();
        Long userId = userService.getUserId(email);

        UserInfo userInfo = buildUserInfo(userId, email);

        return ResponseEntity.ok().body(userInfo);
    }

    // ============ Private Helper Methods ============

    /**
     * Builds UserInfo object.
     */
    private UserInfo buildUserInfo(Long userId, String email) {
        return new UserInfo(userId, email);
    }

    // ============ Logging Methods ============

    private void logGetUserInfo(String username) {
        log.info("Getting user info for: {}", username);
    }
}