package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.exception.BusinessException;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Test")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_NAME = "Test User";
    private static final String ENCODED_PASSWORD = "encoded_password_hash";

    @BeforeEach
    void setUp() {
        userService = new UserService(userMapper, passwordEncoder);
    }

    // ============ register Tests ============

    @Test
    @DisplayName("register successfully creates new user")
    void register_success() {
        User user = createMockUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userMapper.insertUser(any(User.class))).thenReturn(1);

        userService.register(user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(userMapper).insertUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(capturedUser.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        assertThat(capturedUser.getName()).isEqualTo(TEST_NAME);
    }

    @Test
    @DisplayName("register throws exception when email is null")
    void register_emailNull_throwsException() {
        User user = createMockUser(null, TEST_PASSWORD, TEST_NAME);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or empty");

        verify(userMapper, never()).findByEmail(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when email is empty")
    void register_emailEmpty_throwsException() {
        User user = createMockUser("", TEST_PASSWORD, TEST_NAME);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or empty");

        verify(userMapper, never()).findByEmail(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when email is whitespace only")
    void register_emailWhitespace_throwsException() {
        User user = createMockUser("   ", TEST_PASSWORD, TEST_NAME);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or empty");

        verify(userMapper, never()).findByEmail(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when email has no @ symbol")
    void register_emailNoAtSymbol_throwsException() {
        User user = createMockUser("invalidemail.com", TEST_PASSWORD, TEST_NAME);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");

        verify(userMapper, never()).findByEmail(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when email has no dot")
    void register_emailNoDot_throwsException() {
        User user = createMockUser("invalid@emailcom", TEST_PASSWORD, TEST_NAME);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");

        verify(userMapper, never()).findByEmail(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when email already exists")
    void register_emailExists_throwsException() {
        User user = createMockUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);
        User existingUser = createMockUser(TEST_EMAIL, "other_password", "Other User");

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(existingUser);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already exists");

        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when password is null")
    void register_passwordNull_throwsException() {
        User user = createMockUser(TEST_EMAIL, null, TEST_NAME);

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be null or empty");

        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when password is empty")
    void register_passwordEmpty_throwsException() {
        User user = createMockUser(TEST_EMAIL, "", TEST_NAME);

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be null or empty");

        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when password is whitespace only")
    void register_passwordWhitespace_throwsException() {
        User user = createMockUser(TEST_EMAIL, "   ", TEST_NAME);

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password cannot be null or empty");

        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when password is less than 6 characters")
    void register_passwordTooShort_throwsException() {
        User user = createMockUser(TEST_EMAIL, "12345", TEST_NAME);

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password must be at least 6 characters");

        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when name is null")
    void register_nameNull_throwsException() {
        User user = createMockUser(TEST_EMAIL, TEST_PASSWORD, null);

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name cannot be null or empty");

        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when name is empty")
    void register_nameEmpty_throwsException() {
        User user = createMockUser(TEST_EMAIL, TEST_PASSWORD, "");

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name cannot be null or empty");

        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register throws exception when name is whitespace only")
    void register_nameWhitespace_throwsException() {
        User user = createMockUser(TEST_EMAIL, TEST_PASSWORD, "   ");

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name cannot be null or empty");

        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register verifies password is encoded before saving")
    void register_verifyPasswordEncoded() {
        User user = createMockUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);
        String originalPassword = user.getPasswordHash();

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userMapper.insertUser(any(User.class))).thenReturn(1);

        userService.register(user);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(passwordEncoder).encode(originalPassword);
        verify(userMapper).insertUser(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
        assertThat(capturedUser.getPasswordHash()).isNotEqualTo(originalPassword);
    }

    @Test
    @DisplayName("register throws exception when user insertion fails")
    void register_insertFails_throwsException() {
        User user = createMockUser(TEST_EMAIL, TEST_PASSWORD, TEST_NAME);

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userMapper.insertUser(any(User.class))).thenReturn(0);

        assertThatThrownBy(() -> userService.register(user))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save user");

        verify(userMapper).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(userMapper).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register accepts valid email with subdomain")
    void register_emailWithSubdomain_success() {
        User user = createMockUser("user@mail.example.com", TEST_PASSWORD, TEST_NAME);

        when(userMapper.findByEmail("user@mail.example.com")).thenReturn(null);
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userMapper.insertUser(any(User.class))).thenReturn(1);

        userService.register(user);

        verify(userMapper).findByEmail("user@mail.example.com");
        verify(userMapper).insertUser(any(User.class));
    }

    @Test
    @DisplayName("register accepts password with exactly 6 characters")
    void register_passwordExactly6Characters_success() {
        User user = createMockUser(TEST_EMAIL, "123456", TEST_NAME);

        when(userMapper.findByEmail(TEST_EMAIL)).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn(ENCODED_PASSWORD);
        when(userMapper.insertUser(any(User.class))).thenReturn(1);

        userService.register(user);

        verify(passwordEncoder).encode("123456");
        verify(userMapper).insertUser(any(User.class));
    }

    // ============ getUserId Tests ============

    @Test
    @DisplayName("get user id returns user id successfully")
    void getUserId_success() {
        Long expectedUserId = 100L;

        when(userMapper.getUserId(TEST_EMAIL)).thenReturn(expectedUserId);

        Long result = userService.getUserId(TEST_EMAIL);

        assertThat(result).isEqualTo(expectedUserId);
        verify(userMapper).getUserId(TEST_EMAIL);
    }

    @Test
    @DisplayName("get user id throws exception when email is null")
    void getUserId_emailNull_throwsException() {
        assertThatThrownBy(() -> userService.getUserId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or empty");

        verify(userMapper, never()).getUserId(anyString());
    }

    @Test
    @DisplayName("get user id throws exception when email is empty")
    void getUserId_emailEmpty_throwsException() {
        assertThatThrownBy(() -> userService.getUserId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or empty");

        verify(userMapper, never()).getUserId(anyString());
    }

    @Test
    @DisplayName("get user id throws exception when email is whitespace only")
    void getUserId_emailWhitespace_throwsException() {
        assertThatThrownBy(() -> userService.getUserId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email cannot be null or empty");

        verify(userMapper, never()).getUserId(anyString());
    }

    @Test
    @DisplayName("get user id throws exception when email has invalid format")
    void getUserId_emailInvalidFormat_throwsException() {
        assertThatThrownBy(() -> userService.getUserId("invalidemail"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");

        verify(userMapper, never()).getUserId(anyString());
    }

    @Test
    @DisplayName("get user id throws exception when email has no @ symbol")
    void getUserId_emailNoAtSymbol_throwsException() {
        assertThatThrownBy(() -> userService.getUserId("invalid.email.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");

        verify(userMapper, never()).getUserId(anyString());
    }

    @Test
    @DisplayName("get user id throws exception when email has no dot")
    void getUserId_emailNoDot_throwsException() {
        assertThatThrownBy(() -> userService.getUserId("invalid@emailcom"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");

        verify(userMapper, never()).getUserId(anyString());
    }

    @Test
    @DisplayName("get user id throws exception when user not found")
    void getUserId_userNotFound_throwsException() {
        when(userMapper.getUserId(TEST_EMAIL)).thenThrow(new ResourceNotFoundException("User not found with email: " + TEST_EMAIL));

        assertThatThrownBy(() -> userService.getUserId(TEST_EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with email: " + TEST_EMAIL);

        verify(userMapper).getUserId(TEST_EMAIL);
    }

    @Test
    @DisplayName("get user id accepts valid email formats")
    void getUserId_validEmailFormats_success() {
        Long expectedUserId = 100L;
        String[] validEmails = {
                "user@example.com",
                "user.name@example.com",
                "user+tag@example.co.uk",
                "user123@mail.example.com"
        };

        for (String email : validEmails) {
            when(userMapper.getUserId(email)).thenReturn(expectedUserId);

            Long result = userService.getUserId(email);

            assertThat(result).isEqualTo(expectedUserId);
            verify(userMapper).getUserId(email);
        }
    }

    // ============ Helper Methods ============

    private User createMockUser(String email, String password, String name) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setPasswordHash(password);
        user.setName(name);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }
}