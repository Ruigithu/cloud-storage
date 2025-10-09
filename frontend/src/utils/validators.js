import {isEmpty, isEmailFormat, isMinLength} from "./utils.js";

export const MIN_PASSWORD_LENGTH = 6;

export const ERROR_MESSAGES = {
    EMPTY_EMAIL: "email can't be empty",
    INVALID_EMAIL: "invalid email address",
    EMPTY_PASSWORD: "password can't be empty",
    PASSWORD_TOO_SHORT: `password at least has ${MIN_PASSWORD_LENGTH} characters`,
    LOGIN_FAILED: 'login failed',
    INVALID_CREDENTIALS: 'invalid password or email, please try again',
    EMPTY_USERNAME:"Name can't be empty",
    SIGNUP_FAILED: "Registration failed. Please try again.",
    EMAIL_EXISTS: "Email already exists",
};

export const emailValidator = (value) => {
    const trimmedValue = value.trim();
    if (isEmpty(trimmedValue)) return ERROR_MESSAGES.EMPTY_EMAIL;
    if (!isEmailFormat(trimmedValue)) return ERROR_MESSAGES.INVALID_EMAIL;
    return "";
};

export const passwordValidator = (value) => {
    const trimmedValue = value.trim();
    if (isEmpty(trimmedValue)) return ERROR_MESSAGES.EMPTY_PASSWORD;
    if (!isMinLength(trimmedValue, MIN_PASSWORD_LENGTH)) return ERROR_MESSAGES.PASSWORD_TOO_SHORT;
    return "";
};

export const usernameValidator=(value)=>{
    const trimmedValue = value.trim();
    if (isEmpty(trimmedValue)) return ERROR_MESSAGES.EMPTY_USERNAME;
    return "";
}

const FIELD_VALIDATORS = {
    email: emailValidator,
    password: passwordValidator,
    username:usernameValidator
};

export const validateField = (name, value) => {
    const validator = FIELD_VALIDATORS[name];
    return validator ? validator(value) : "";
};

export const validateLoginForm = (email, password) => {
    return {
        email: validateField('email', email),
        password: validateField('password', password)
    };
};

export const validateSignUpForm = (username,email,password) => {
    return {
        email: validateField('email', email),
        username: validateField('username', username),
        password: validateField('password', password)
    };
};