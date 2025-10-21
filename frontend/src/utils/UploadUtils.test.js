import {
    sliceFileIntoChunks,
    calculateProgress,
    calculateBatchProgress,
    extractETag,
    formatFileSize,
    validateFileSize,
    buildFormData
} from './uploadUtils';

describe('uploadUtils', () => {
    describe('sliceFileIntoChunks', () => {
        test('should split file into correct number of chunks', () => {
            const mockFile = createMockFile(10 * 1024 * 1024); // 10MB
            const chunkSize = 5 * 1024 * 1024; // 5MB

            const chunks = sliceFileIntoChunks(mockFile, chunkSize);

            expect(chunks).toHaveLength(2);
        });

        test('should create chunks with correct part numbers', () => {
            const mockFile = createMockFile(10 * 1024 * 1024);
            const chunkSize = 5 * 1024 * 1024;

            const chunks = sliceFileIntoChunks(mockFile, chunkSize);

            expect(chunks[0].partNumber).toBe(1);
            expect(chunks[1].partNumber).toBe(2);
        });

        test('should handle file smaller than chunk size', () => {
            const mockFile = createMockFile(2 * 1024 * 1024); // 2MB
            const chunkSize = 5 * 1024 * 1024; // 5MB

            const chunks = sliceFileIntoChunks(mockFile, chunkSize);

            expect(chunks).toHaveLength(1);
            expect(chunks[0].partNumber).toBe(1);
        });

        test('should create chunks with correct start and end positions', () => {
            const mockFile = createMockFile(10 * 1024 * 1024);
            const chunkSize = 5 * 1024 * 1024;

            const chunks = sliceFileIntoChunks(mockFile, chunkSize);

            expect(chunks[0].start).toBe(0);
            expect(chunks[0].end).toBe(5 * 1024 * 1024);
            expect(chunks[1].start).toBe(5 * 1024 * 1024);
            expect(chunks[1].end).toBe(10 * 1024 * 1024);
        });

        test('should handle file size not divisible by chunk size', () => {
            const mockFile = createMockFile(7 * 1024 * 1024); // 7MB
            const chunkSize = 5 * 1024 * 1024; // 5MB

            const chunks = sliceFileIntoChunks(mockFile, chunkSize);

            expect(chunks).toHaveLength(2);
            expect(chunks[1].end).toBe(7 * 1024 * 1024);
        });

        test('should use default chunk size when not provided', () => {
            const mockFile = createMockFile(10 * 1024 * 1024);

            const chunks = sliceFileIntoChunks(mockFile);

            expect(chunks.length).toBeGreaterThan(0);
        });
    });

    describe('calculateProgress', () => {
        test('should calculate 0% progress for first part at start', () => {
            const progress = calculateProgress(1, 10, 0);

            expect(progress).toBe(0);
        });

        test('should calculate 50% progress for part 5 of 10', () => {
            const progress = calculateProgress(5, 10, 0);

            expect(progress).toBe(40);
        });

        test('should calculate 100% progress for last part completed', () => {
            const progress = calculateProgress(10, 10, 1);

            expect(progress).toBe(100);
        });

        test('should include partial progress of current part', () => {
            const progress = calculateProgress(1, 10, 0.5);

            expect(progress).toBe(5);
        });

        test('should not exceed 100%', () => {
            const progress = calculateProgress(11, 10, 1);

            expect(progress).toBe(100);
        });

        test('should handle single part upload', () => {
            const progress = calculateProgress(1, 1, 0.5);

            expect(progress).toBe(50);
        });
    });

    describe('calculateBatchProgress', () => {
        test('should return 0 for empty uploads array', () => {
            const progress = calculateBatchProgress([]);

            expect(progress).toBe(0);
        });

        test('should calculate average progress correctly', () => {
            const uploads = [
                { progress: 1 },     // 100%
                { progress: 0.5 },   // 50%
                { progress: 0 }      // 0%
            ];

            const progress = calculateBatchProgress(uploads);

            expect(progress).toBe(50);
        });

        test('should handle all completed uploads', () => {
            const uploads = [
                { progress: 1 },
                { progress: 1 },
                { progress: 1 }
            ];

            const progress = calculateBatchProgress(uploads);

            expect(progress).toBe(100);
        });

        test('should handle uploads without progress property', () => {
            const uploads = [
                { progress: 0.5 },
                {},
                { progress: 1 }
            ];

            const progress = calculateBatchProgress(uploads);

            expect(progress).toBe(50);
        });

        test('should round result to integer', () => {
            const uploads = [
                { progress: 0.333 },
                { progress: 0.333 },
                { progress: 0.333 }
            ];

            const progress = calculateBatchProgress(uploads);

            expect(Number.isInteger(progress)).toBe(true);
            expect(progress).toBe(33);
        });
    });


    describe('extractETag', () => {
        test('should extract etag property', () => {
            const response = { etag: 'abc123' };

            const etag = extractETag(response);

            expect(etag).toBe('abc123');
        });

        test('should extract eTag property with capital T', () => {
            const response = { eTag: 'def456' };

            const etag = extractETag(response);

            expect(etag).toBe('def456');
        });

        test('should return response directly if it is a string', () => {
            const response = 'ghi789';

            const etag = extractETag(response);

            expect(etag).toBe('ghi789');
        });

        test('should prioritize lowercase etag over eTag', () => {
            const response = { etag: 'lowercase', eTag: 'uppercase' };

            const etag = extractETag(response);

            expect(etag).toBe('lowercase');
        });
    });

    describe('formatFileSize', () => {
        test('should return "0 Bytes" for zero', () => {
            const size = formatFileSize(0);

            expect(size).toBe('0 Bytes');
        });

        test('should format bytes', () => {
            const size = formatFileSize(500);

            expect(size).toBe('500 Bytes');
        });

        test('should format kilobytes', () => {
            const size = formatFileSize(1024);

            expect(size).toBe('1 KB');
        });

        test('should format megabytes', () => {
            const size = formatFileSize(1048576);

            expect(size).toBe('1 MB');
        });

        test('should format gigabytes', () => {
            const size = formatFileSize(1073741824);

            expect(size).toBe('1 GB');
        });

        test('should round to 2 decimal places', () => {
            const size = formatFileSize(1536);

            expect(size).toBe('1.5 KB');
        });
    });

    describe('validateFileSize', () => {
        test('should return true when file size is within limit', () => {
            const isValid = validateFileSize(1000, 2000);

            expect(isValid).toBe(true);
        });

        test('should return true when file size equals limit', () => {
            const isValid = validateFileSize(2000, 2000);

            expect(isValid).toBe(true);
        });

        test('should return false when file size exceeds limit', () => {
            const isValid = validateFileSize(3000, 2000);

            expect(isValid).toBe(false);
        });

        test('should handle zero file size', () => {
            const isValid = validateFileSize(0, 1000);

            expect(isValid).toBe(true);
        });

        test('should handle large file sizes', () => {
            const isValid = validateFileSize(50 * 1024 * 1024, 100 * 1024 * 1024);

            expect(isValid).toBe(true);
        });
    });

    describe('buildFormData', () => {
        test('should create FormData from object', () => {
            const data = { key1: 'value1', key2: 'value2' };

            const formData = buildFormData(data);

            expect(formData).toBeInstanceOf(FormData);
            expect(formData.get('key1')).toBe('value1');
            expect(formData.get('key2')).toBe('value2');
        });

        test('should handle array values', () => {
            const data = { files: ['file1', 'file2', 'file3'] };

            const formData = buildFormData(data);

            expect(formData.getAll('files')).toEqual(['file1', 'file2', 'file3']);
        });

        test('should handle empty object', () => {
            const data = {};

            const formData = buildFormData(data);

            expect(formData).toBeInstanceOf(FormData);
        });

        test('should handle mixed data types', () => {
            const data = {
                string: 'text',
                number: 123,
                boolean: true,
                array: ['a', 'b']
            };

            const formData = buildFormData(data);

            expect(formData.get('string')).toBe('text');
            expect(formData.get('number')).toBe('123');
            expect(formData.get('boolean')).toBe('true');
            expect(formData.getAll('array')).toEqual(['a', 'b']);
        });

        test('should handle File objects', () => {
            const file = new File(['content'], 'test.txt', { type: 'text/plain' });
            const data = { file: file };

            const formData = buildFormData(data);

            expect(formData.get('file')).toBeInstanceOf(File);
        });
    });
});

// Helper function to create mock File object
function createMockFile(size) {
    const mockBlob = new Blob(['a'.repeat(size)]);
    const mockFile = {
        size: size,
        slice: jest.fn((start, end) => mockBlob.slice(start, end))
    };
    return mockFile;
}