import { loginAPI, signupAPI, getUserInfo, API_ENDPOINTS } from './authService';
import { ERROR_MESSAGES } from '../utils/validators';

global.fetch = jest.fn();

jest.mock('../utils/apiRequest');
import apiRequest from '../utils/apiRequest';

describe('authService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('API_ENDPOINTS', () => {
        test('should have LOGIN endpoint', () => {
            expect(API_ENDPOINTS.LOGIN).toContain('/api/auth/login');
        });

        test('should have SIGNUP endpoint', () => {
            expect(API_ENDPOINTS.SIGNUP).toContain('/api/auth/signup');
        });

        test('should have USERINFO endpoint', () => {
            expect(API_ENDPOINTS.USERINFO).toContain('/getUserInfo');
        });
    });

    describe('loginAPI', () => {
        test('should successfully login with valid credentials', async () => {
            const mockResponse = {
                token: 'test-token-123',
                userId: 'user-123',
                userName: 'testuser'
            };

            fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const credentials = {
                email: 'test@example.com',
                password: 'password123'
            };

            const result = await loginAPI(credentials);

            expect(fetch).toHaveBeenCalledWith(
                API_ENDPOINTS.LOGIN,
                expect.objectContaining({
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({
                        email: credentials.email,
                        password: credentials.password
                    })
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should throw error on failed login with server error message', async () => {
            const errorMessage = 'Invalid email or password';

            fetch.mockResolvedValueOnce({
                ok: false,
                json: async () => ({ message: errorMessage })
            });

            const credentials = {
                email: 'test@example.com',
                password: 'wrongpassword'
            };

            await expect(loginAPI(credentials)).rejects.toThrow(errorMessage);
        });

        test('should throw default error message when server response has no message', async () => {
            fetch.mockResolvedValueOnce({
                ok: false,
                json: async () => ({})
            });

            const credentials = {
                email: 'test@example.com',
                password: 'password123'
            };

            await expect(loginAPI(credentials)).rejects.toThrow(ERROR_MESSAGES.INVALID_CREDENTIALS);
        });

        test('should handle network error', async () => {
            fetch.mockRejectedValueOnce(new Error('Network error'));

            const credentials = {
                email: 'test@example.com',
                password: 'password123'
            };

            await expect(loginAPI(credentials)).rejects.toThrow('Network error');
        });

        test('should handle malformed JSON response', async () => {
            fetch.mockResolvedValueOnce({
                ok: false,
                json: async () => {
                    throw new Error('Invalid JSON');
                }
            });

            const credentials = {
                email: 'test@example.com',
                password: 'password123'
            };

            await expect(loginAPI(credentials)).rejects.toThrow(ERROR_MESSAGES.INVALID_CREDENTIALS);
        });
    });

    describe('signupAPI', () => {
        test('should successfully signup with valid data', async () => {
            const mockResponse = {
                token: 'test-token-456',
                userId: 'user-456',
                userName: 'newuser'
            };

            fetch.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const userData = {
                email: 'newuser@example.com',
                name: 'New User',
                password: 'password123'
            };

            const result = await signupAPI(userData);

            expect(fetch).toHaveBeenCalledWith(
                API_ENDPOINTS.SIGNUP,
                expect.objectContaining({
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Accept': 'application/json'
                    },
                    body: JSON.stringify({
                        email: userData.email,
                        name: userData.name,
                        password: userData.password
                    })
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should throw error when email already exists', async () => {
            const errorMessage = 'Email already exists';

            fetch.mockResolvedValueOnce({
                ok: false,
                json: async () => ({ message: errorMessage })
            });

            const userData = {
                email: 'existing@example.com',
                name: 'Test User',
                password: 'password123'
            };

            await expect(signupAPI(userData)).rejects.toThrow(errorMessage);
        });

        test('should throw default error message on failed signup', async () => {
            fetch.mockResolvedValueOnce({
                ok: false,
                json: async () => ({})
            });

            const userData = {
                email: 'test@example.com',
                name: 'Test User',
                password: 'password123'
            };

            await expect(signupAPI(userData)).rejects.toThrow(ERROR_MESSAGES.SIGNUP_FAILED);
        });

        test('should handle network error during signup', async () => {
            fetch.mockRejectedValueOnce(new Error('Network error'));

            const userData = {
                email: 'test@example.com',
                name: 'Test User',
                password: 'password123'
            };

            await expect(signupAPI(userData)).rejects.toThrow('Network error');
        });
    });

    describe('getUserInfo', () => {
        test('should successfully fetch user info', async () => {
            const mockUserInfo = {
                userId: 'user-789',
                userName: 'testuser',
                email: 'test@example.com'
            };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockUserInfo
            });

            const result = await getUserInfo();

            expect(apiRequest).toHaveBeenCalledWith(
                API_ENDPOINTS.USERINFO,
                expect.objectContaining({
                    method: 'GET',
                    credentials: 'include'
                })
            );
            expect(result).toEqual(mockUserInfo);
        });

        test('should throw error when fetch fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(getUserInfo()).rejects.toThrow('Failed to fetch user info');
        });

        test('should handle network error', async () => {
            apiRequest.mockRejectedValueOnce(new Error('Network error'));

            await expect(getUserInfo()).rejects.toThrow('Network error');
        });

        test('should include credentials in request', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({})
            });

            await getUserInfo();

            expect(apiRequest).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    credentials: 'include'
                })
            );
        });
    });
});