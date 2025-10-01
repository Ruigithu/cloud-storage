import {ERROR_MESSAGES} from "../utils/validators.js";

const getApiUrl = () => {
    return process.env.REACT_APP_API_URL || 'http://localhost:8080';
};

export const API_ENDPOINTS = {
    LOGIN: `${getApiUrl()}/login`,
};

export const loginAPI = async (credentials) => {
    const formData = new URLSearchParams();
    formData.append('username', credentials.username);
    formData.append('password', credentials.password);

    const response = await fetch(API_ENDPOINTS.LOGIN, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Accept': 'application/json'
        },
        body: formData.toString(),
    });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || ERROR_MESSAGES.INVALID_CREDENTIALS);
    }

    return await response.json();
};