
jest.mock('../services/uploadService', () => ({
    initResumableUpload: jest.fn(),
    getUploadedParts: jest.fn(),
    uploadPartWithProgress: jest.fn(),
    completeResumableUpload: jest.fn(),
    abortResumableUpload: jest.fn()
}));
import { renderHook, act } from '@testing-library/react';
import { useFileUpload } from './useFileUpload';
import { useChunkUpload } from './useChunkUpload';
import * as uploadService from '../services/uploadService';
import { calculateProgress } from '../utils/uploadUtils';

// Mock dependencies
jest.mock('./useChunkUpload');
jest.mock('../services/uploadService');
jest.mock('../utils/uploadUtils', () => ({
    calculateProgress: jest.fn()
}));

// Mock window.alert
global.alert = jest.fn();

describe('useFileUpload', () => {
    let mockUploadAllChunks;
    let mockPause;
    let mockResume;
    let mockOnSuccess;

    beforeEach(() => {
        jest.clearAllMocks();

        mockUploadAllChunks = jest.fn();
        mockPause = jest.fn();
        mockResume = jest.fn();

        useChunkUpload.mockReturnValue({
            uploadAllChunks: mockUploadAllChunks,
            pause: mockPause,
            resume: mockResume,
            abort: jest.fn(),
            isPaused: jest.fn()
        });

        mockOnSuccess = jest.fn();

        calculateProgress.mockReturnValue(50);

    });

    describe('Initial state', () => {
        test('should initialize with default values', () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            expect(result.current.isUploading).toBe(false);
            expect(result.current.uploadProgress).toBe(0);
            expect(result.current.isPaused).toBe(false);
        });
    });

    describe('startUpload', () => {
        test('should start upload successfully', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([]);

            mockUploadAllChunks.mockResolvedValueOnce([
                { partNumber: 1, eTag: 'etag1' }
            ]);

            uploadService.completeResumableUpload.mockResolvedValueOnce({});

            const mockFile = new File(['content'], 'test.txt', { type: 'text/plain' });

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            expect(uploadService.initResumableUpload).toHaveBeenCalledWith({
                ownerId: 'user1',
                folderId: 'folder1',
                fileName: 'test.txt',
                mimeType: 'text/plain',
                fileSize: mockFile.size
            });

            expect(mockUploadAllChunks).toHaveBeenCalled();
            expect(uploadService.completeResumableUpload).toHaveBeenCalled();
            expect(mockOnSuccess).toHaveBeenCalled();
            expect(global.alert).toHaveBeenCalledWith('File uploaded successfully');
        });

        test('should not start upload without file', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            await act(async () => {
                await result.current.startUpload(null);
            });

            expect(uploadService.initResumableUpload).not.toHaveBeenCalled();
        });

        test('should set uploading state during upload', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([]);

            mockUploadAllChunks.mockImplementation(() => {
                return new Promise((resolve) => {
                    // Delay to check state
                    setTimeout(() => resolve([]), 100);
                });
            });

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                const uploadPromise = result.current.startUpload(mockFile);

                // Check state during upload
                expect(result.current.isUploading).toBe(true);

                await uploadPromise;
            });
        });

        test('should handle upload error', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            uploadService.initResumableUpload.mockRejectedValueOnce(new Error('Init failed'));

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                'Error uploading file:',
                expect.any(Error)
            );
            expect(result.current.isUploading).toBe(false);

            consoleErrorSpy.mockRestore();
        });

        test('should resume from uploaded parts', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([
                { partNumber: 1, eTag: 'etag1' }
            ]);

            mockUploadAllChunks.mockResolvedValueOnce([
                { partNumber: 1, eTag: 'etag1' },
                { partNumber: 2, eTag: 'etag2' }
            ]);

            uploadService.completeResumableUpload.mockResolvedValueOnce({});

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            expect(uploadService.getUploadedParts).toHaveBeenCalledWith('file123', 'upload123');
        });

        test('should reset state after successful upload', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([]);
            mockUploadAllChunks.mockResolvedValueOnce([{ partNumber: 1, eTag: 'etag1' }]);
            uploadService.completeResumableUpload.mockResolvedValueOnce({});

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            expect(result.current.isUploading).toBe(false);
            expect(result.current.uploadProgress).toBe(0);
            expect(result.current.isPaused).toBe(false);
        });
    });

    describe('pauseUpload', () => {
        test('should pause upload', () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            act(() => {
                result.current.pauseUpload();
            });

            expect(result.current.isPaused).toBe(true);
            expect(mockPause).toHaveBeenCalled();
        });
    });

    describe('resumeUpload', () => {
        test('should resume upload successfully', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            // Start upload first
            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([]);

            mockUploadAllChunks.mockImplementation(() => {
                // Simulate paused upload
                return Promise.resolve([{ partNumber: 1, eTag: 'etag1' }]);
            });

            const mockFile = new File(['content'], 'test.txt');

            // Start upload
            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            // Pause
            act(() => {
                result.current.pauseUpload();
            });

            // Resume
            uploadService.getUploadedParts.mockResolvedValueOnce([
                { partNumber: 1, eTag: 'etag1' }
            ]);

            mockUploadAllChunks.mockResolvedValueOnce([
                { partNumber: 1, eTag: 'etag1' },
                { partNumber: 2, eTag: 'etag2' }
            ]);

            uploadService.completeResumableUpload.mockResolvedValueOnce({});

            await act(async () => {
                await result.current.resumeUpload();
            });

            expect(result.current.isPaused).toBe(false);
            expect(mockResume).toHaveBeenCalled();
        });

        test('should not resume without fileId or uploadId', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            await act(async () => {
                await result.current.resumeUpload();
            });

            expect(mockResume).not.toHaveBeenCalled();
        });

        test('should handle resume error', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            // Start upload
            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([]);
            mockUploadAllChunks.mockResolvedValueOnce([]);

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            // Pause
            act(() => {
                result.current.pauseUpload();
            });

            // Resume with error
            uploadService.getUploadedParts.mockRejectedValueOnce(new Error('Resume failed'));

            await act(async () => {
                await result.current.resumeUpload();
            });

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                'Error resuming upload:',
                expect.any(Error)
            );
            expect(result.current.isUploading).toBe(false);

            consoleErrorSpy.mockRestore();
        });
    });

    describe('cancelUpload', () => {
        test('should cancel upload successfully', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            // Start upload
            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([]);
            mockUploadAllChunks.mockResolvedValueOnce([]);

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            // Cancel
            uploadService.abortResumableUpload.mockResolvedValueOnce({});

            await act(async () => {
                await result.current.cancelUpload();
            });

            expect(uploadService.abortResumableUpload).toHaveBeenCalledWith(
                'file123',
                'upload123',
                'folder1',
                'user1'
            );
            expect(global.alert).toHaveBeenCalledWith('Upload cancelled');
            expect(result.current.isUploading).toBe(false);
        });

        test('should not cancel without fileId or uploadId', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            await act(async () => {
                await result.current.cancelUpload();
            });

            expect(uploadService.abortResumableUpload).not.toHaveBeenCalled();
        });

        test('should handle cancel error', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            // Start upload
            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([]);
            mockUploadAllChunks.mockResolvedValueOnce([]);

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            // Cancel with error
            uploadService.abortResumableUpload.mockRejectedValueOnce(new Error('Cancel failed'));

            await act(async () => {
                await result.current.cancelUpload();
            });

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                'Error cancelling upload:',
                expect.any(Error)
            );
            expect(result.current.isUploading).toBe(false);

            consoleErrorSpy.mockRestore();
        });

        test('should reset all state after cancel', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            // Start upload
            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([]);
            mockUploadAllChunks.mockResolvedValueOnce([]);

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            uploadService.abortResumableUpload.mockResolvedValueOnce({});

            await act(async () => {
                await result.current.cancelUpload();
            });

            expect(result.current.isUploading).toBe(false);
            expect(result.current.uploadProgress).toBe(0);
            expect(result.current.isPaused).toBe(false);
        });
    });

    describe('uploadProgress', () => {
        test('should update progress during upload', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            uploadService.getUploadedParts.mockResolvedValueOnce([]);

            calculateProgress.mockReturnValue(50);

            mockUploadAllChunks.mockImplementation((file, uploadPartFn, onPartComplete, onProgress) => {
                // Simulate progress callback
                if (onProgress) {
                    onProgress(1, 2, 0.5);
                }
                return Promise.resolve([{ partNumber: 1, eTag: 'etag1' }]);
            });

            uploadService.completeResumableUpload.mockResolvedValueOnce({});

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            expect(calculateProgress).toHaveBeenCalledWith(1, 2, 0.5);
        });
    });

    describe('Edge cases', () => {
        test('should handle eTag with different formats', async () => {
            const { result } = renderHook(() =>
                useFileUpload({ ownerId: 'user1', folderId: 'folder1', onSuccess: mockOnSuccess })
            );

            uploadService.initResumableUpload.mockResolvedValueOnce({
                fileId: 'file123',
                uploadId: 'upload123'
            });

            // Mix of eTag and etag
            uploadService.getUploadedParts.mockResolvedValueOnce([
                { partNumber: 1, eTag: 'etag1' },
                { partNumber: 2, etag: 'etag2' }
            ]);

            mockUploadAllChunks.mockResolvedValueOnce([]);
            uploadService.completeResumableUpload.mockResolvedValueOnce({});

            const mockFile = new File(['content'], 'test.txt');

            await act(async () => {
                await result.current.startUpload(mockFile);
            });

            // Should handle both formats
            expect(uploadService.getUploadedParts).toHaveBeenCalled();
        });
    });
});