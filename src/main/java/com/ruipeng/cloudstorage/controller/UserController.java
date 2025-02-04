package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.entity.UserInfo;
import com.ruipeng.cloudstorage.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/getUserInfo")
    public ResponseEntity<?> getUserInfo(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok().body(
                new UserInfo(userService.getUserId(userDetails.getUsername()),userDetails.getUsername()));

    }
}
