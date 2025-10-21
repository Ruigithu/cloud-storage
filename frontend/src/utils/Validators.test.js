import {
    MIN_PASSWORD_LENGTH,
    ERROR_MESSAGES,
    emailValidator,
    passwordValidator,
    usernameValidator,
    validateField,
    validateLoginForm,
    validateSignUpForm
} from './validators';

// Mock the utils functions
jest.mock('./utils.js', () => ({
    isEmpty: (value) => !value || value.length === 0,
    isEmailFormat: (value) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value),
    isMinLength: (value, minLength) => value.length >= minLength
}));

describe('validators', () => {
    describe('ERROR_MESSAGES', () => {
        test('should have all required error message constants', () => {
            expect(ERROR_MESSAGES).toHaveProperty('EMPTY_EMAIL');
            expect(ERROR_MESSAGES).toHaveProperty('INVALID_EMAIL');
            expect(ERROR_MESSAGES).toHaveProperty('EMPTY_PASSWORD');
            expect(ERROR_MESSAGES).toHaveProperty('PASSWORD_TOO_SHORT');
            expect(ERROR_MESSAGES).toHaveProperty('LOGIN_FAILED');
            expect(ERROR_MESSAGES).toHaveProperty('INVALID_CREDENTIALS');
            expect(ERROR_MESSAGES).toHaveProperty('EMPTY_USERNAME');
            expect(ERROR_MESSAGES).toHaveProperty('SIGNUP_FAILED');
            expect(ERROR_MESSAGES).toHaveProperty('EMAIL_EXISTS');
        });

        test('should have MIN_PASSWORD_LENGTH of 6', () => {
            expect(MIN_PASSWORD_LENGTH).toBe(6);
        });
    });

    describe('emailValidator', () => {
        test('should return error for empty email', () => {
            const error = emailValidator('');

            expect(error).toBe(ERROR_MESSAGES.EMPTY_EMAIL);
        });

        test('should return error for whitespace-only email', () => {
            const error = emailValidator('   ');

            expect(error).toBe(ERROR_MESSAGES.EMPTY_EMAIL);
        });

        test('should return error for invalid email format', () => {
            const error = emailValidator('notanemail');

            expect(error).toBe(ERROR_MESSAGES.INVALID_EMAIL);
        });

        test('should return error for email without @', () => {
            const error = emailValidator('user.com');

            expect(error).toBe(ERROR_MESSAGES.INVALID_EMAIL);
        });

        test('should return error for email without domain', () => {
            const error = emailValidator('user@');

            expect(error).toBe(ERROR_MESSAGES.INVALID_EMAIL);
        });

        test('should return empty string for valid email', () => {
            const error = emailValidator('user@example.com');

            expect(error).toBe('');
        });

        test('should trim whitespace before validation', () => {
            const error = emailValidator('  user@example.com  ');

            expect(error).toBe('');
        });

        test('should accept email with subdomain', () => {
            const error = emailValidator('user@mail.example.com');

            expect(error).toBe('');
        });
    });

    describe('passwordValidator', () => {
        test('should return error for empty password', () => {
            const error = passwordValidator('');

            expect(error).toBe(ERROR_MESSAGES.EMPTY_PASSWORD);
        });

        test('should return error for whitespace-only password', () => {
            const error = passwordValidator('   ');

            expect(error).toBe(ERROR_MESSAGES.EMPTY_PASSWORD);
        });

        test('should return error for password shorter than minimum length', () => {
            const error = passwordValidator('12345');

            expect(error).toBe(ERROR_MESSAGES.PASSWORD_TOO_SHORT);
        });

        test('should return empty string for password at minimum length', () => {
            const error = passwordValidator('123456');

            expect(error).toBe('');
        });

        test('should return empty string for password longer than minimum', () => {
            const error = passwordValidator('1234567890');

            expect(error).toBe('');
        });

        test('should trim whitespace before validation', () => {
            const error = passwordValidator('  123456  ');

            expect(error).toBe('');
        });
    });

    describe('usernameValidator', () => {
        test('should return error for empty username', () => {
            const error = usernameValidator('');

            expect(error).toBe(ERROR_MESSAGES.EMPTY_USERNAME);
        });

        test('should return error for whitespace-only username', () => {
            const error = usernameValidator('   ');

            expect(error).toBe(ERROR_MESSAGES.EMPTY_USERNAME);
        });

        test('should return empty string for valid username', () => {
            const error = usernameValidator('john_doe');

            expect(error).toBe('');
        });

        test('should trim whitespace before validation', () => {
            const error = usernameValidator('  john_doe  ');

            expect(error).toBe('');
        });

        test('should accept username with spaces', () => {
            const error = usernameValidator('John Doe');

            expect(error).toBe('');
        });

        test('should accept single character username', () => {
            const error = usernameValidator('J');

            expect(error).toBe('');
        });
    });

    describe('validateField', () => {
        test('should validate email field', () => {
            const error = validateField('email', 'invalid-email');

            expect(error).toBe(ERROR_MESSAGES.INVALID_EMAIL);
        });

        test('should validate password field', () => {
            const error = validateField('password', '123');

            expect(error).toBe(ERROR_MESSAGES.PASSWORD_TOO_SHORT);
        });

        test('should validate username field', () => {
            const error = validateField('username', '');

            expect(error).toBe(ERROR_MESSAGES.EMPTY_USERNAME);
        });

        test('should return empty string for valid email', () => {
            const error = validateField('email', 'user@example.com');

            expect(error).toBe('');
        });

        test('should return empty string for unknown field', () => {
            const error = validateField('unknownField', 'value');

            expect(error).toBe('');
        });

        test('should handle null field name', () => {
            const error = validateField(null, 'value');

            expect(error).toBe('');
        });
    });

    describe('validateLoginForm', () => {
        test('should return errors for empty email and password', () => {
            const errors = validateLoginForm('', '');

            expect(errors.email).toBe(ERROR_MESSAGES.EMPTY_EMAIL);
            expect(errors.password).toBe(ERROR_MESSAGES.EMPTY_PASSWORD);
        });

        test('should return error only for invalid email', () => {
            const errors = validateLoginForm('invalid-email', 'password123');

            expect(errors.email).toBe(ERROR_MESSAGES.INVALID_EMAIL);
            expect(errors.password).toBe('');
        });

        test('should return error only for short password', () => {
            const errors = validateLoginForm('user@example.com', '123');

            expect(errors.email).toBe('');
            expect(errors.password).toBe(ERROR_MESSAGES.PASSWORD_TOO_SHORT);
        });

        test('should return no errors for valid credentials', () => {
            const errors = validateLoginForm('user@example.com', 'password123');

            expect(errors.email).toBe('');
            expect(errors.password).toBe('');
        });

        test('should return object with email and password properties', () => {
            const errors = validateLoginForm('test', 'test');

            expect(errors).toHaveProperty('email');
            expect(errors).toHaveProperty('password');
        });
    });

    describe('validateSignUpForm', () => {
        test('should return errors for all empty fields', () => {
            const errors = validateSignUpForm('', '', '');

            expect(errors.username).toBe(ERROR_MESSAGES.EMPTY_USERNAME);
            expect(errors.email).toBe(ERROR_MESSAGES.EMPTY_EMAIL);
            expect(errors.password).toBe(ERROR_MESSAGES.EMPTY_PASSWORD);
        });

        test('should return error only for empty username', () => {
            const errors = validateSignUpForm('', 'user@example.com', 'password123');

            expect(errors.username).toBe(ERROR_MESSAGES.EMPTY_USERNAME);
            expect(errors.email).toBe('');
            expect(errors.password).toBe('');
        });

        test('should return error only for invalid email', () => {
            const errors = validateSignUpForm('john', 'invalid-email', 'password123');

            expect(errors.username).toBe('');
            expect(errors.email).toBe(ERROR_MESSAGES.INVALID_EMAIL);
            expect(errors.password).toBe('');
        });

        test('should return error only for short password', () => {
            const errors = validateSignUpForm('john', 'user@example.com', '123');

            expect(errors.username).toBe('');
            expect(errors.email).toBe('');
            expect(errors.password).toBe(ERROR_MESSAGES.PASSWORD_TOO_SHORT);
        });

        test('should return no errors for valid signup data', () => {
            const errors = validateSignUpForm('john_doe', 'user@example.com', 'password123');

            expect(errors.username).toBe('');
            expect(errors.email).toBe('');
            expect(errors.password).toBe('');
        });

        test('should return object with username, email and password properties', () => {
            const errors = validateSignUpForm('test', 'test', 'test');

            expect(errors).toHaveProperty('username');
            expect(errors).toHaveProperty('email');
            expect(errors).toHaveProperty('password');
        });

        test('should handle multiple validation errors', () => {
            const errors = validateSignUpForm('', 'invalid', '12');

            expect(errors.username).toBe(ERROR_MESSAGES.EMPTY_USERNAME);
            expect(errors.email).toBe(ERROR_MESSAGES.INVALID_EMAIL);
            expect(errors.password).toBe(ERROR_MESSAGES.PASSWORD_TOO_SHORT);
        });
    });
});