package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.config.security.JWTService;
import com.ruipeng.cloudstorage.entity.AuthResponse;
import com.ruipeng.cloudstorage.entity.LoginRequest;
import com.ruipeng.cloudstorage.entity.SignupRequest;
import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.mappers.UserMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);


            String token = jwtService.generateToken(loginRequest.getEmail());

            return ResponseEntity.ok(
                    new AuthResponse(true, token, loginRequest.getEmail())
            );

        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(false, "Invalid email or password"));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, "Login failed: " + e.getMessage()));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        try {

            User existingUser = userMapper.findByEmail(signupRequest.getEmail());
            if (existingUser != null) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(new AuthResponse(false, "Email already exists"));
            }


            User newUser = new User();
            newUser.setEmail(signupRequest.getEmail());
            newUser.setPasswordHash(passwordEncoder.encode(signupRequest.getPassword()));
            newUser.setName(signupRequest.getName());

            userMapper.insertUser(newUser);

            return ResponseEntity.ok(
                    new AuthResponse(true, "User registered successfully")
            );

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AuthResponse(false, "Signup failed"));
        }
    }
}