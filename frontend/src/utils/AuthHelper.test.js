import { saveAuthData } from './authHelper';

const mockLocalStorage = {
    setItem: jest.fn(),
    getItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
};

Object.defineProperty(window, 'localStorage', {
    value: mockLocalStorage,
    writable: true,
});

describe('authHelper', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('saveAuthData', () => {
        test('should save token to localStorage', () => {
            const token = 'test-token-123';
            const username = 'testuser';

            saveAuthData(token, username);

            expect(mockLocalStorage.setItem).toHaveBeenCalledWith('token', token);
        });

        test('should save username to localStorage', () => {
            const token = 'test-token-123';
            const username = 'testuser';

            saveAuthData(token, username);

            expect(mockLocalStorage.setItem).toHaveBeenCalledWith('user', username);
        });

        test('should save both token and username in correct order', () => {
            const token = 'test-token-456';
            const username = 'johndoe';

            saveAuthData(token, username);

            expect(mockLocalStorage.setItem).toHaveBeenCalledTimes(2);
            expect(mockLocalStorage.setItem).toHaveBeenNthCalledWith(1, 'token', token);
            expect(mockLocalStorage.setItem).toHaveBeenNthCalledWith(2, 'user', username);
        });

        test('should handle empty token', () => {
            const token = '';
            const username = 'testuser';

            saveAuthData(token, username);

            expect(mockLocalStorage.setItem).toHaveBeenCalledWith('token', '');
            expect(mockLocalStorage.setItem).toHaveBeenCalledWith('user', username);
        });

        test('should handle empty username', () => {
            const token = 'test-token-123';
            const username = '';

            saveAuthData(token, username);

            expect(mockLocalStorage.setItem).toHaveBeenCalledWith('token', token);
            expect(mockLocalStorage.setItem).toHaveBeenCalledWith('user', '');
        });
    });
});