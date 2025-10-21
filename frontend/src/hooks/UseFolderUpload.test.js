jest.mock('../services/uploadService', () => ({
    initFolderUpload : jest.fn(),
    uploadFolderFilePart : jest.fn(),
    completeResumableUpload : jest.fn(),
    completeFolderUpload : jest.fn(),
    abortFolderUpload : jest.fn()
}));
import { renderHook, act, waitFor } from '@testing-library/react';
import { useFolderUpload } from './useFolderUpload';
import * as uploadService from '../services/uploadService';
import { calculateBatchProgress, extractETag } from '../utils/uploadUtils';
import { UPLOAD_CONFIG, UPLOAD_STATUS } from '../utils/uploadHelper';

// Mock dependencies
jest.mock('../services/uploadService');
jest.mock('../utils/uploadUtils', () => ({
    calculateBatchProgress: jest.fn(),
    extractETag: jest.fn()
}));
jest.mock('../utils/uploadHelper', () => ({
    UPLOAD_CONFIG: {
        CHUNK_SIZE: 5 * 1024 * 1024
    },
    UPLOAD_STATUS: {
        IDLE: 'idle',
        UPLOADING: 'uploading',
        PAUSED: 'paused',
        COMPLETED: 'completed',
        FAILED: 'failed',
        CANCELLED: 'cancelled',
        PENDING: 'pending'
    }
}));

// Mock window.alert
global.alert = jest.fn();

describe('useFolderUpload', () => {
    let mockOnSuccess;

    beforeEach(() => {
        jest.clearAllMocks();
        mockOnSuccess = jest.fn();

        calculateBatchProgress.mockReturnValue(50);
        extractETag.mockImplementation((result) => result.eTag || result);

    });

    describe('Initial state', () => {
        test('should initialize with default values', () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            expect(result.current.isUploading).toBe(false);
            expect(result.current.uploadProgress).toBe(0);
            expect(result.current.activeUploads).toEqual([]);
            expect(result.current.totalFiles).toBe(0);
            expect(result.current.uploadedFiles).toBe(0);
            expect(result.current.failedFiles).toEqual([]);
        });
    });

    describe('startUpload', () => {
        test('should start folder upload successfully', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFiles = [
                new File(['content1'], 'file1.txt', { type: 'text/plain' }),
                new File(['content2'], 'file2.txt', { type: 'text/plain' })
            ];

            mockFiles[0].webkitRelativePath = 'folder/file1.txt';
            mockFiles[1].webkitRelativePath = 'folder/file2.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' },
                    { fileId: 'file2', uploadId: 'upload2', relativePath: 'folder/file2.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'etag1' });
            uploadService.completeResumableUpload.mockResolvedValue({});

            await act(async () => {
                await result.current.startUpload(mockFiles);
            });

            expect(uploadService.initFolderUpload).toHaveBeenCalledWith(mockFiles, 'user1', 'parent1');
            expect(result.current.totalFiles).toBe(2);
        });

        test('should not start upload without files', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            await act(async () => {
                await result.current.startUpload(null);
            });

            expect(uploadService.initFolderUpload).not.toHaveBeenCalled();
        });

        test('should not start upload with empty file array', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            await act(async () => {
                await result.current.startUpload([]);
            });

            expect(uploadService.initFolderUpload).not.toHaveBeenCalled();
        });

        test('should set uploading state during upload', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFiles = [new File(['content'], 'file1.txt')];
            mockFiles[0].webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'etag1' });
            uploadService.completeResumableUpload.mockResolvedValue({});

            await act(async () => {
                const uploadPromise = result.current.startUpload(mockFiles);

                // Check state during upload
                expect(result.current.isUploading).toBe(true);

                await uploadPromise;
            });
        });

        test('should handle init upload error', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFiles = [new File(['content'], 'file1.txt')];

            uploadService.initFolderUpload.mockRejectedValueOnce(new Error('Init failed'));

            await act(async () => {
                await result.current.startUpload(mockFiles);
            });

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                'Error starting folder upload:',
                expect.any(Error)
            );
            expect(global.alert).toHaveBeenCalledWith('Failed to start folder upload');
            expect(result.current.isUploading).toBe(false);

            consoleErrorSpy.mockRestore();
        });

        test('should update uploadedFiles count', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFile = new File(['content'], 'file1.txt');
            mockFile.webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'etag1' });
            uploadService.completeResumableUpload.mockResolvedValue({});

            await act(async () => {
                await result.current.startUpload([mockFile]);
            });

            await waitFor(() => {
                expect(result.current.uploadedFiles).toBe(1);
            });
        });
    });

    describe('uploadFileParts', () => {
        test('should upload file in chunks', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFile = new File(['a'.repeat(10 * 1024 * 1024)], 'large.txt');
            mockFile.webkitRelativePath = 'folder/large.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/large.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'etag' });
            uploadService.completeResumableUpload.mockResolvedValue({});

            await act(async () => {
                await result.current.startUpload([mockFile]);
            });

            // Should upload multiple chunks
            const expectedChunks = Math.ceil(mockFile.size / UPLOAD_CONFIG.CHUNK_SIZE);
            expect(uploadService.uploadFolderFilePart).toHaveBeenCalledTimes(expectedChunks);
        });

        test('should handle file upload error and add to failed files', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFile = new File(['content'], 'file1.txt');
            mockFile.webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockRejectedValueOnce(new Error('Upload failed'));

            await act(async () => {
                await result.current.startUpload([mockFile]);
            });

            await waitFor(() => {
                expect(result.current.failedFiles.length).toBeGreaterThan(0);
            });

            consoleErrorSpy.mockRestore();
        });

        test('should update progress for each part uploaded', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFile = new File(['content'], 'file1.txt');
            mockFile.webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'etag' });
            uploadService.completeResumableUpload.mockResolvedValue({});

            await act(async () => {
                await result.current.startUpload([mockFile]);
            });

            expect(calculateBatchProgress).toHaveBeenCalled();
        });
    });

    describe('cancelUpload', () => {
        test('should cancel upload successfully', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            // Start upload
            const mockFile = new File(['content'], 'file1.txt');
            mockFile.webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockImplementation(() => {
                return new Promise((resolve) => {
                    setTimeout(() => resolve({ eTag: 'etag' }), 1000);
                });
            });

            act(() => {
                result.current.startUpload([mockFile]);
            });

            // Cancel
            uploadService.abortFolderUpload.mockResolvedValueOnce({});

            await act(async () => {
                await result.current.cancelUpload();
            });

            expect(uploadService.abortFolderUpload).toHaveBeenCalled();
            expect(global.alert).toHaveBeenCalledWith('Upload cancelled');
            expect(result.current.isUploading).toBe(false);
            expect(result.current.activeUploads).toEqual([]);
        });

        test('should not cancel if not uploading', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            await act(async () => {
                await result.current.cancelUpload();
            });

            expect(uploadService.abortFolderUpload).not.toHaveBeenCalled();
        });

        test('should handle cancel error', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            // Start upload
            const mockFile = new File(['content'], 'file1.txt');
            mockFile.webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            act(() => {
                result.current.startUpload([mockFile]);
            });

            uploadService.abortFolderUpload.mockRejectedValueOnce(new Error('Cancel failed'));

            await act(async () => {
                await result.current.cancelUpload();
            });

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                'Error cancelling upload:',
                expect.any(Error)
            );

            consoleErrorSpy.mockRestore();
        });
    });

    describe('completeAllUploads', () => {
        test('should complete all uploads successfully', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFile = new File(['content'], 'file1.txt');
            mockFile.webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'etag' });
            uploadService.completeResumableUpload.mockResolvedValue({});
            uploadService.completeFolderUpload.mockResolvedValue({});

            await act(async () => {
                await result.current.startUpload([mockFile]);
            });

            await act(async () => {
                await result.current.completeAllUploads();
            });

            expect(global.alert).toHaveBeenCalledWith('Folder uploaded successfully');
            expect(mockOnSuccess).toHaveBeenCalled();
            expect(result.current.isUploading).toBe(false);
        });

        test('should handle no pending uploads', async () => {
            const consoleLogSpy = jest.spyOn(console, 'log').mockImplementation();

            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            await act(async () => {
                await result.current.completeAllUploads();
            });

            expect(consoleLogSpy).toHaveBeenCalledWith('All uploads already completed');
            expect(result.current.isUploading).toBe(false);
            expect(mockOnSuccess).toHaveBeenCalled();

            consoleLogSpy.mockRestore();
        });

        test('should handle completion error', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFile = new File(['content'], 'file1.txt');
            mockFile.webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'etag' });
            uploadService.completeResumableUpload.mockResolvedValue({});

            await act(async () => {
                await result.current.startUpload([mockFile]);
            });

            uploadService.completeFolderUpload.mockRejectedValueOnce(new Error('Complete failed'));

            await act(async () => {
                await result.current.completeAllUploads();
            });

            expect(consoleErrorSpy).toHaveBeenCalledWith(
                'Error completing uploads:',
                expect.any(Error)
            );
            expect(global.alert).toHaveBeenCalledWith('Failed to complete some uploads');

            consoleErrorSpy.mockRestore();
        });
    });

    describe('Progress calculation', () => {
        test('should update progress as uploads complete', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFile = new File(['content'], 'file1.txt');
            mockFile.webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'etag' });
            uploadService.completeResumableUpload.mockResolvedValue({});

            calculateBatchProgress.mockReturnValue(75);

            await act(async () => {
                await result.current.startUpload([mockFile]);
            });

            await waitFor(() => {
                expect(result.current.uploadProgress).toBe(75);
            });
        });
    });

    describe('Multiple file upload', () => {
        test('should handle multiple files', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFiles = [
                new File(['content1'], 'file1.txt'),
                new File(['content2'], 'file2.txt'),
                new File(['content3'], 'file3.txt')
            ];

            mockFiles[0].webkitRelativePath = 'folder/file1.txt';
            mockFiles[1].webkitRelativePath = 'folder/file2.txt';
            mockFiles[2].webkitRelativePath = 'folder/file3.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' },
                    { fileId: 'file2', uploadId: 'upload2', relativePath: 'folder/file2.txt' },
                    { fileId: 'file3', uploadId: 'upload3', relativePath: 'folder/file3.txt' }
                ]
            });

            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'etag' });
            uploadService.completeResumableUpload.mockResolvedValue({});

            await act(async () => {
                await result.current.startUpload(mockFiles);
            });

            expect(result.current.totalFiles).toBe(3);
        });
    });

    describe('Edge cases', () => {
        test('should handle file without webkitRelativePath', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFile = new File(['content'], 'file1.txt');
            // No webkitRelativePath set

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'file1.txt' }
                ]
            });

            await act(async () => {
                await result.current.startUpload([mockFile]);
            });

            expect(uploadService.initFolderUpload).toHaveBeenCalled();
        });

        test('should handle extractETag with different formats', async () => {
            const { result } = renderHook(() =>
                useFolderUpload({ userId: 'user1', parentId: 'parent1', onSuccess: mockOnSuccess })
            );

            const mockFile = new File(['content'], 'file1.txt');
            mockFile.webkitRelativePath = 'folder/file1.txt';

            uploadService.initFolderUpload.mockResolvedValueOnce({
                fileUploads: [
                    { fileId: 'file1', uploadId: 'upload1', relativePath: 'folder/file1.txt' }
                ]
            });

            extractETag.mockReturnValue('extracted-etag');
            uploadService.uploadFolderFilePart.mockResolvedValue({ eTag: 'test-etag' });
            uploadService.completeResumableUpload.mockResolvedValue({});

            await act(async () => {
                await result.current.startUpload([mockFile]);
            });

            expect(extractETag).toHaveBeenCalled();
        });
    });
});