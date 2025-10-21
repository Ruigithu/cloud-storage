import {
    initResumableUpload,
    getUploadedParts,
    uploadPartWithProgress,
    completeResumableUpload,
    abortResumableUpload,
    initFolderUpload,
    uploadFolderFilePart,
    completeFolderUpload,
    abortFolderUpload
} from './uploadService';

jest.mock('../utils/apiRequest');
import apiRequest from '../utils/apiRequest';

// Mock localStorage
const mockLocalStorage = {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn()
};

Object.defineProperty(global, 'localStorage', {
    value: mockLocalStorage,
    writable: true
});

// Mock XMLHttpRequest
class MockXMLHttpRequest {
    constructor() {
        this.upload = { addEventListener: jest.fn() };
        this.addEventListener = jest.fn();
        this.open = jest.fn();
        this.send = jest.fn();
        this.setRequestHeader = jest.fn();
        this.status = 200;
        this.responseText = '{}';
    }
}

global.XMLHttpRequest = MockXMLHttpRequest;

describe('uploadService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockLocalStorage.getItem.mockReturnValue('test-token-123');
    });

    describe('initResumableUpload', () => {
        test('should initialize resumable upload successfully', async () => {
            const mockResponse = {
                fileId: 'file123',
                uploadId: 'upload123'
            };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const params = {
                ownerId: 'user123',
                folderId: 'folder123',
                fileName: 'test.pdf',
                mimeType: 'application/pdf',
                fileSize: 1024000
            };

            const result = await initResumableUpload(params);

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/resumable/init'),
                expect.objectContaining({
                    method: 'POST',
                    headers: expect.objectContaining({
                        Authorization: 'Bearer test-token-123'
                    })
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should throw error when initialization fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            const params = {
                ownerId: 'user123',
                folderId: 'folder123',
                fileName: 'test.pdf',
                mimeType: 'application/pdf',
                fileSize: 1024000
            };

            await expect(initResumableUpload(params))
                .rejects.toThrow('Failed to initialize upload');
        });

        test('should convert fileSize to string in FormData', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({})
            });

            const params = {
                ownerId: 'user123',
                folderId: 'folder123',
                fileName: 'test.pdf',
                mimeType: 'application/pdf',
                fileSize: 1024000
            };

            await initResumableUpload(params);

            const formData = apiRequest.mock.calls[0][1].body;
            expect(formData).toBeInstanceOf(FormData);
        });
    });

    describe('getUploadedParts', () => {
        test('should get uploaded parts successfully', async () => {
            const mockParts = [
                { partNumber: 1, eTag: 'etag1' },
                { partNumber: 2, eTag: 'etag2' }
            ];

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockParts
            });

            const result = await getUploadedParts('file123', 'upload123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/resumable/parts'),
                expect.objectContaining({
                    method: 'GET',
                    headers: expect.objectContaining({
                        Authorization: 'Bearer test-token-123'
                    })
                })
            );
            expect(result).toEqual(mockParts);
        });

        test('should include fileId and uploadId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => []
            });

            await getUploadedParts('file456', 'upload789');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('fileId=file456'),
                expect.any(Object)
            );
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('uploadId=upload789'),
                expect.any(Object)
            );
        });

        test('should throw error when request fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(getUploadedParts('file123', 'upload123'))
                .rejects.toThrow('Failed to get uploaded parts');
        });
    });

    describe('uploadPartWithProgress', () => {
        test('should upload part with progress tracking', async () => {
            const mockChunk = new Blob(['chunk data']);
            const onProgress = jest.fn();

            const promise = uploadPartWithProgress(
                mockChunk,
                'file123',
                'upload123',
                1,
                onProgress
            );

            // Simulate progress event
            const xhr = new MockXMLHttpRequest();
            const progressCallback = xhr.upload.addEventListener.mock.calls[0][1];
            progressCallback({ lengthComputable: true, loaded: 50, total: 100 });

            expect(onProgress).toHaveBeenCalledWith(0.5);

            // Simulate load event
            xhr.responseText = JSON.stringify({ eTag: 'etag123' });
            const loadCallback = xhr.addEventListener.mock.calls.find(
                call => call[0] === 'load'
            )[1];
            loadCallback();

            const result = await promise;
            expect(result).toEqual({ eTag: 'etag123' });
        });

        test('should handle upload error', async () => {
            const mockChunk = new Blob(['chunk data']);

            const promise = uploadPartWithProgress(
                mockChunk,
                'file123',
                'upload123',
                1,
                jest.fn()
            );

            const xhr = new MockXMLHttpRequest();
            const errorCallback = xhr.addEventListener.mock.calls.find(
                call => call[0] === 'error'
            )[1];
            errorCallback();

            await expect(promise).rejects.toThrow('Network error during upload');
        });

        test('should handle upload abort', async () => {
            const mockChunk = new Blob(['chunk data']);

            const promise = uploadPartWithProgress(
                mockChunk,
                'file123',
                'upload123',
                1,
                jest.fn()
            );

            const xhr = new MockXMLHttpRequest();
            const abortCallback = xhr.addEventListener.mock.calls.find(
                call => call[0] === 'abort'
            )[1];
            abortCallback();

            await expect(promise).rejects.toThrow('Upload aborted');
        });

        test('should not call onProgress when event is not computable', () => {
            const mockChunk = new Blob(['chunk data']);
            const onProgress = jest.fn();

            uploadPartWithProgress(
                mockChunk,
                'file123',
                'upload123',
                1,
                onProgress
            );

            const xhr = new MockXMLHttpRequest();
            const progressCallback = xhr.upload.addEventListener.mock.calls[0][1];
            progressCallback({ lengthComputable: false });

            expect(onProgress).not.toHaveBeenCalled();
        });

        test('should set authorization header', () => {
            const mockChunk = new Blob(['chunk data']);

            uploadPartWithProgress(
                mockChunk,
                'file123',
                'upload123',
                1,
                jest.fn()
            );

            const xhr = new MockXMLHttpRequest();
            expect(xhr.setRequestHeader).toHaveBeenCalledWith(
                'Authorization',
                'Bearer test-token-123'
            );
        });
    });

    describe('completeResumableUpload', () => {
        test('should complete resumable upload successfully', async () => {
            const mockResponse = {
                fileId: 'file123',
                url: 'https://example.com/file123'
            };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const partETags = [
                { partNumber: 1, eTag: 'etag1' },
                { partNumber: 2, eTag: 'etag2' }
            ];

            const result = await completeResumableUpload('file123', 'upload123', partETags);

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/resumable/complete'),
                expect.objectContaining({
                    method: 'POST',
                    headers: expect.objectContaining({
                        'Content-Type': 'application/json',
                        Authorization: 'Bearer test-token-123'
                    })
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should format partETags correctly', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({})
            });

            const partETags = [
                { partNumber: 1, eTag: 'etag1', extraField: 'ignored' },
                { partNumber: 2, eTag: 'etag2' }
            ];

            await completeResumableUpload('file123', 'upload123', partETags);

            const callArgs = apiRequest.mock.calls[0][1];
            const body = JSON.parse(callArgs.body);

            expect(body.partETags).toEqual([
                { partNumber: 1, eTag: 'etag1' },
                { partNumber: 2, eTag: 'etag2' }
            ]);
        });

        test('should throw error when completion fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(completeResumableUpload('file123', 'upload123', []))
                .rejects.toThrow('Failed to complete upload');
        });
    });

    describe('abortResumableUpload', () => {
        test('should abort resumable upload successfully', async () => {
            const mockResponse = { success: true };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const result = await abortResumableUpload('file123', 'upload123', 'folder123', 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/resumable/abort'),
                expect.objectContaining({
                    method: 'POST',
                    headers: expect.objectContaining({
                        Authorization: 'Bearer test-token-123'
                    })
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should throw error when abort fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(abortResumableUpload('file123', 'upload123', 'folder123', 'user123'))
                .rejects.toThrow('Failed to abort upload');
        });
    });

    describe('initFolderUpload', () => {
        test('should initialize folder upload successfully', async () => {
            const mockResponse = {
                uploads: [
                    { fileId: 'file1', uploadId: 'upload1' },
                    { fileId: 'file2', uploadId: 'upload2' }
                ]
            };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const mockFile1 = new File(['content1'], 'file1.txt', { type: 'text/plain' });
            mockFile1.webkitRelativePath = 'folder/file1.txt';

            const mockFile2 = new File(['content2'], 'file2.txt', { type: 'text/plain' });
            mockFile2.webkitRelativePath = 'folder/file2.txt';

            const files = [mockFile1, mockFile2];

            const result = await initFolderUpload(files, 'user123', 'parent123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/folders-init-upload'),
                expect.objectContaining({
                    method: 'POST'
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should throw error when initialization fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            const files = [new File(['content'], 'test.txt')];

            await expect(initFolderUpload(files, 'user123', 'parent123'))
                .rejects.toThrow('Failed to initiate folder upload');
        });
    });

    describe('uploadFolderFilePart', () => {
        test('should upload folder file part successfully', async () => {
            const mockResponse = { eTag: 'etag123' };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const mockChunk = new Blob(['chunk data']);
            const result = await uploadFolderFilePart('file123', 'upload123', 1, mockChunk);

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/folders-upload-part'),
                expect.objectContaining({
                    method: 'POST'
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should throw error with part number when upload fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            const mockChunk = new Blob(['chunk data']);

            await expect(uploadFolderFilePart('file123', 'upload123', 3, mockChunk))
                .rejects.toThrow('Failed to upload part 3');
        });
    });

    describe('completeFolderUpload', () => {
        test('should complete folder upload successfully', async () => {
            const mockResponse = { success: true };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const fileCompletions = [
                {
                    fileId: 'file1',
                    uploadId: 'upload1',
                    partETags: [{ partNumber: 1, eTag: 'etag1' }]
                }
            ];

            const result = await completeFolderUpload(fileCompletions);

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/folders-complete-upload'),
                expect.objectContaining({
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    }
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should throw error when completion fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(completeFolderUpload([]))
                .rejects.toThrow('Failed to complete folder upload');
        });
    });

    describe('abortFolderUpload', () => {
        test('should abort folder upload successfully', async () => {
            const mockResponse = { success: true };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const activeUploads = [
                { fileId: 'file1', uploadId: 'upload1' },
                { fileId: 'file2', uploadId: 'upload2' }
            ];

            const result = await abortFolderUpload(activeUploads, 'user123', 'parent123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/folders-abort-upload'),
                expect.objectContaining({
                    method: 'POST'
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should throw error when abort fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(abortFolderUpload([], 'user123', 'parent123'))
                .rejects.toThrow('Failed to abort folder upload');
        });
    });
});