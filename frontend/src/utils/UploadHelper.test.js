import { UPLOAD_CONFIG, API_ENDPOINTS, UPLOAD_STATUS } from './uploadHelper';

describe('uploadHelper', () => {
    describe('UPLOAD_CONFIG', () => {
        test('should have CHUNK_SIZE of 5MB', () => {
            expect(UPLOAD_CONFIG.CHUNK_SIZE).toBe(5 * 1024 * 1024);
        });

        test('should have MAX_FILE_SIZE of 50MB', () => {
            expect(UPLOAD_CONFIG.MAX_FILE_SIZE).toBe(50 * 1024 * 1024);
        });

        test('should have MAX_FOLDER_SIZE of 100MB', () => {
            expect(UPLOAD_CONFIG.MAX_FOLDER_SIZE).toBe(100 * 1024 * 1024);
        });

        test('should have MAX_RETRIES of 3', () => {
            expect(UPLOAD_CONFIG.MAX_RETRIES).toBe(3);
        });

        test('should have all required properties', () => {
            expect(UPLOAD_CONFIG).toHaveProperty('CHUNK_SIZE');
            expect(UPLOAD_CONFIG).toHaveProperty('MAX_FILE_SIZE');
            expect(UPLOAD_CONFIG).toHaveProperty('MAX_FOLDER_SIZE');
            expect(UPLOAD_CONFIG).toHaveProperty('MAX_RETRIES');
        });

        test('should have exactly 4 properties', () => {
            const propertyCount = Object.keys(UPLOAD_CONFIG).length;

            expect(propertyCount).toBe(4);
        });

        test('should have MAX_FILE_SIZE greater than CHUNK_SIZE', () => {
            expect(UPLOAD_CONFIG.MAX_FILE_SIZE).toBeGreaterThan(UPLOAD_CONFIG.CHUNK_SIZE);
        });

        test('should have MAX_FOLDER_SIZE greater than MAX_FILE_SIZE', () => {
            expect(UPLOAD_CONFIG.MAX_FOLDER_SIZE).toBeGreaterThan(UPLOAD_CONFIG.MAX_FILE_SIZE);
        });
    });

    describe('API_ENDPOINTS', () => {
        test('should have RESUMABLE_INIT endpoint', () => {
            expect(API_ENDPOINTS.RESUMABLE_INIT).toBe('/resumable/init');
        });

        test('should have RESUMABLE_PART endpoint', () => {
            expect(API_ENDPOINTS.RESUMABLE_PART).toBe('/resumable/part');
        });

        test('should have RESUMABLE_PARTS endpoint', () => {
            expect(API_ENDPOINTS.RESUMABLE_PARTS).toBe('/resumable/parts');
        });

        test('should have RESUMABLE_COMPLETE endpoint', () => {
            expect(API_ENDPOINTS.RESUMABLE_COMPLETE).toBe('/resumable/complete');
        });

        test('should have RESUMABLE_ABORT endpoint', () => {
            expect(API_ENDPOINTS.RESUMABLE_ABORT).toBe('/resumable/abort');
        });

        test('should have FOLDER_INIT endpoint', () => {
            expect(API_ENDPOINTS.FOLDER_INIT).toBe('/folders-init-upload');
        });

        test('should have FOLDER_PART endpoint', () => {
            expect(API_ENDPOINTS.FOLDER_PART).toBe('/folders-upload-part');
        });

        test('should have FOLDER_COMPLETE endpoint', () => {
            expect(API_ENDPOINTS.FOLDER_COMPLETE).toBe('/folders-complete-upload');
        });

        test('should have FOLDER_ABORT endpoint', () => {
            expect(API_ENDPOINTS.FOLDER_ABORT).toBe('/folders-abort-upload');
        });

        test('should have all required properties', () => {
            expect(API_ENDPOINTS).toHaveProperty('RESUMABLE_INIT');
            expect(API_ENDPOINTS).toHaveProperty('RESUMABLE_PART');
            expect(API_ENDPOINTS).toHaveProperty('RESUMABLE_PARTS');
            expect(API_ENDPOINTS).toHaveProperty('RESUMABLE_COMPLETE');
            expect(API_ENDPOINTS).toHaveProperty('RESUMABLE_ABORT');
            expect(API_ENDPOINTS).toHaveProperty('FOLDER_INIT');
            expect(API_ENDPOINTS).toHaveProperty('FOLDER_PART');
            expect(API_ENDPOINTS).toHaveProperty('FOLDER_COMPLETE');
            expect(API_ENDPOINTS).toHaveProperty('FOLDER_ABORT');
        });

        test('should have exactly 9 endpoints', () => {
            const endpointCount = Object.keys(API_ENDPOINTS).length;

            expect(endpointCount).toBe(9);
        });

        test('should have all endpoints start with /', () => {
            const allStartWithSlash = Object.values(API_ENDPOINTS).every(
                endpoint => endpoint.startsWith('/')
            );

            expect(allStartWithSlash).toBe(true);
        });
    });

    describe('UPLOAD_STATUS', () => {
        test('should have IDLE status', () => {
            expect(UPLOAD_STATUS.IDLE).toBe('idle');
        });

        test('should have UPLOADING status', () => {
            expect(UPLOAD_STATUS.UPLOADING).toBe('uploading');
        });

        test('should have PAUSED status', () => {
            expect(UPLOAD_STATUS.PAUSED).toBe('paused');
        });

        test('should have COMPLETED status', () => {
            expect(UPLOAD_STATUS.COMPLETED).toBe('completed');
        });

        test('should have FAILED status', () => {
            expect(UPLOAD_STATUS.FAILED).toBe('failed');
        });

        test('should have CANCELLED status', () => {
            expect(UPLOAD_STATUS.CANCELLED).toBe('cancelled');
        });

        test('should have all required properties', () => {
            expect(UPLOAD_STATUS).toHaveProperty('IDLE');
            expect(UPLOAD_STATUS).toHaveProperty('UPLOADING');
            expect(UPLOAD_STATUS).toHaveProperty('PAUSED');
            expect(UPLOAD_STATUS).toHaveProperty('COMPLETED');
            expect(UPLOAD_STATUS).toHaveProperty('FAILED');
            expect(UPLOAD_STATUS).toHaveProperty('CANCELLED');
        });

        test('should have exactly 6 statuses', () => {
            const statusCount = Object.keys(UPLOAD_STATUS).length;

            expect(statusCount).toBe(6);
        });

        test('should have all unique status values', () => {
            const values = Object.values(UPLOAD_STATUS);
            const uniqueValues = new Set(values);

            expect(uniqueValues.size).toBe(values.length);
        });
    });
});