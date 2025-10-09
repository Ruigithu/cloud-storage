import apiRequest from './apiRequest';

global.fetch = jest.fn();
const mockLocalStorage = {
    getItem: jest.fn(),
    removeItem: jest.fn(),
};
Object.defineProperty(window, 'localStorage', {
    value: mockLocalStorage,
});

delete window.location;
window.location = { href: '' };

describe('apiRequest', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should include Content-Type header by default', async () => {
        prepareSuccessResponse();

        await getDataFromBackend( 'https://api.example.com/test');

        expect(fetch).toHaveBeenCalledWith(
            'https://api.example.com/test',
            expect.objectContaining({
                headers: expect.objectContaining({
                    'Content-Type': 'application/json'
                })
            })
        );
    });

    test('should add Authorization header when token exists', async () => {
        prepareTheLocalStorageWithToken();
        prepareSuccessResponse();

        await getDataFromBackend('https://api.example.com/test');

        expect(fetch).toHaveBeenCalledWith(
            'https://api.example.com/test',
            expect.objectContaining({
                headers: expect.objectContaining({
                    'Authorization': 'Bearer test-token-123'
                })
            })
        );
    });

    test('should NOT add Authorization header when token does not exist', async () => {
        prepareTheEmptyLocalStorage();
        prepareSuccessResponse();

        await getDataFromBackend('https://api.example.com/test');

        const callArgs = fetch.mock.calls[0][1];
        expect(callArgs.headers['Authorization']).toBeUndefined();
    });

    test('should remove Content-Type header when body is FormData', async () => {
        prepareSuccessResponse();

        await postDataToBackendWithFormData('https://api.example.com/test');

        const callArgs = fetch.mock.calls[0][1];
        expect(callArgs.headers['Content-Type']).toBeUndefined();
    });

    test('should handle 401 status by clearing storage and redirecting', async () => {
        prepareTheLocalStorageWithToken();
        prepareUnsuccessResponseWith401Error();

        const result =  await getDataFromBackend('https://api.example.com/test');

        expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('token');
        expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('isLoggedIn');
        expect(window.location.href).toBe('/login');
        expect(result).toBeNull();
    });

    test('should return response for successful requests', async () => {
        prepareSuccessResponse();

        const result = await getDataFromBackend('https://api.example.com/test');

        expect(result.status).toBe(200);
    });

    test('should merge custom headers with default headers', async () => {
        prepareSuccessResponse();

        await sendRequestWithCustomHeader('https://api.example.com/test');

        expect(fetch).toHaveBeenCalledWith(
            'https://api.example.com/test',
            expect.objectContaining({
                headers: expect.objectContaining({
                    'Content-Type': 'application/json',
                    'X-Custom-Header': 'custom-value'
                })
            })
        );
    });
});

function prepareSuccessResponse(){
    fetch.mockResolvedValueOnce({
        status: 200,
        json: async () => ({data:'test'})
    });
}
function prepareUnsuccessResponseWith401Error(){
    fetch.mockResolvedValueOnce({
        status: 401
    });
}

function prepareTheEmptyLocalStorage(){
    mockLocalStorage.getItem.mockReturnValue(null);
}
function prepareTheLocalStorageWithToken(){
    mockLocalStorage.getItem.mockReturnValue("test-token-123");
}

function prepareFormData(){
    const formData = new FormData();
    formData.append("testKey","testValue");
    return formData;
}

async function getDataFromBackend(backendApi) {
    return await apiRequest(backendApi);
}
async function postDataToBackendWithFormData(backendApi) {
    const formData = prepareFormData();

    return await apiRequest(backendApi, {
        method: 'POST',
        body: formData
    });
}

async function sendRequestWithCustomHeader(backendApi){
    const option = {
        headers: {
            'X-Custom-Header': 'custom-value'
        }
    }
    return await apiRequest(backendApi,option);
}