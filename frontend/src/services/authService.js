import {ERROR_MESSAGES} from "../utils/validators.js";
import apiRequest from "../utils/apiRequest";

const getApiUrl = () => {
    return process.env.REACT_APP_API_URL || 'http://localhost:8080';
};

export const API_ENDPOINTS = {
    LOGIN: `${getApiUrl()}/api/auth/login`,
    SIGNUP:`${getApiUrl()}/api/auth/signup`,
    USERINFO:`${getApiUrl()}/getUserInfo`
};

export const loginAPI = async (credentials) => {
    const response = await fetch(API_ENDPOINTS.LOGIN, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        body: JSON.stringify({
            email: credentials.email,
            password: credentials.password
        })
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || ERROR_MESSAGES.INVALID_CREDENTIALS);
    }

    return await response.json();
};

export const signupAPI = async (userData) => {
    const response = await fetch(API_ENDPOINTS.SIGNUP, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        body: JSON.stringify({
            email: userData.email,
            name:userData.name,
            password: userData.password
        })
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || ERROR_MESSAGES.SIGNUP_FAILED);
    }

    return await response.json();
};

/**
 * get user's information
 * @returns {Promise<Object>} 用户信息 { userId, userName, email }
 */
export const getUserInfo = async () => {
    const response = await apiRequest(API_ENDPOINTS.USERINFO, {
        method: 'GET',
        credentials: 'include',
    });

    if (!response.ok) {
        throw new Error('Failed to fetch user info');
    }

    return await response.json();
};