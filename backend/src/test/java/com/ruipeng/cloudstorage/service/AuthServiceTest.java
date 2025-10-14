package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.config.security.JWTService;
import com.ruipeng.cloudstorage.dto.request.LoginRequest;
import com.ruipeng.cloudstorage.dto.request.SignupRequest;
import com.ruipeng.cloudstorage.dto.response.AuthResponse;
import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.exception.BusinessException;
import com.ruipeng.cloudstorage.mappers.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JWTService jwtService;
    @Mock private UserMapper userMapper;
    @Mock private Authentication authentication;
    @Mock private PasswordEncoder passwordEncoder; //keep it or error happens

    @InjectMocks
    private AuthService authService;


    //given-when-then name
    @Test
    @DisplayName("login successfully")
    void loginRequest_withValidCredentials_loginSuccessfully() {
        LoginRequest loginRequest = setValidLoginRequest();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(any())).thenReturn(generateTestToken());

        AuthResponse authResponse = authService.login(loginRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.isSuccess()).isTrue();
        assertThat(authResponse.getEmail()).isEqualTo(loginRequest.getEmail());
        assertThat(authResponse.getToken()).isEqualTo(generateTestToken());

        verify(authenticationManager).authenticate(
                argThat(authentication ->
                        authentication instanceof UsernamePasswordAuthenticationToken&&
                        authentication.getPrincipal().equals(loginRequest.getEmail())&&
                        authentication.getCredentials().equals(loginRequest.getPassword())
                )
        );
        verify(jwtService).generateToken(loginRequest.getEmail());

        verify(authenticationManager,times(1)).authenticate(any());
        verify(jwtService,times(1)).generateToken(anyString());
    }

    @Test
    @DisplayName("failed login because of invalid credentials")
    void loginRequest_withInvalidCredentials_loginFailed() {
        LoginRequest loginRequest = setInvalidLoginRequest();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(
                new BadCredentialsException("Invalid email or password"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "AUTH_001")
                .hasMessage("Invalid email or password");
        verify(authenticationManager,times(1)).authenticate(any());
        verify(jwtService,never()).generateToken(any());
    }

    @Test
    @DisplayName("failed login because of RunTime Exception")
    void loginRequest_throwRunTimeException_loginFailed() {
        LoginRequest loginRequest = setValidLoginRequest();
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenThrow(
                new RuntimeException(getDatabaseErrorMessage()));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "AUTH_002")
                .hasMessage("Login failed: "+getDatabaseErrorMessage());
        verify(authenticationManager,times(1)).authenticate(any());
        verify(jwtService,never()).generateToken(any());
    }

    @Test
    @DisplayName("signup successfully")
    void signupRequest_withUserData_signupSuccessfully() {
        SignupRequest signupRequest = setValidSignupRequest();
        when(userMapper.findByEmail(signupRequest.getEmail())).thenReturn(null);


        AuthResponse authResponse = authService.signup(signupRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.isSuccess()).isTrue();
        assertThat(authResponse.getMessage()).isEqualTo("User registered successfully");

        verify(userMapper,times(1)).findByEmail(anyString());
        verify(userMapper,times(1)).insertUser(any(User.class));
    }

    @Test
    @DisplayName("signup successfully")
    void signupRequest_withExistUser_ThrowBusinessException() {
        SignupRequest signupRequest = setInvalidSignupRequest();
        User user = createUser();
        when(userMapper.findByEmail(signupRequest.getEmail())).thenReturn(user);


        assertThatThrownBy(() -> authService.signup(signupRequest))
        .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "AUTH_003")
                .hasMessage("Email already exists");

        verify(userMapper,times(1)).findByEmail(anyString());
        verify(userMapper,never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("failed login because of RunTime Exception")
    void signupRequest_throwRunTimeException_signupFailed() {
        SignupRequest signupRequest = setValidSignupRequest();
        when(userMapper.insertUser(any(User.class))).thenThrow(new RuntimeException(getDatabaseErrorMessage()));


        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", "AUTH_004")
                .hasMessage("Signup failed: "+getDatabaseErrorMessage());
        verify(userMapper,times(1)).findByEmail(anyString());
        verify(userMapper,times(1)).insertUser(any(User.class));
    }


    private LoginRequest setValidLoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("validEmail@gmail.com");
        loginRequest.setPassword("validPassword");
        return loginRequest;
    }

    private LoginRequest setInvalidLoginRequest() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("invalidEmail@gmail.com");
        loginRequest.setPassword("invalidPassword");
        return loginRequest;
    }


    private String generateTestToken() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    }

    private String getDatabaseErrorMessage() {
        return "Database connection failed";
    }

    private SignupRequest setValidSignupRequest() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("validEmail@gmail.com");
        signupRequest.setPassword("validPassword");
        signupRequest.setName("validName");
        return signupRequest;
    }
    private SignupRequest setInvalidSignupRequest() {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setEmail("existEmail@gmail.com");
        signupRequest.setPassword("existPassword");
        signupRequest.setName("existName");
        return signupRequest;
    }

    private User createUser() {
        User user = new User();
        user.setName("validName");
        user.setName("validEmail@gmail.com");
        user.setPasswordHash("validPassword");
        return user;
    }

}
