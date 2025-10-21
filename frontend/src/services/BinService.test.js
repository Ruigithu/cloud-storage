import {
    getDeletedFiles,
    getDeletedFolders,
    getDeletedFilesAndFolders
} from './binService';

jest.mock('../utils/apiRequest');
import apiRequest from '../utils/apiRequest';

describe('binService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('getDeletedFiles', () => {
        test('should fetch deleted files successfully', async () => {
            const mockFiles = [
                { id: 'file1', name: 'deleted1.txt', deletedAt: '2024-01-01' },
                { id: 'file2', name: 'deleted2.pdf', deletedAt: '2024-01-02' }
            ];

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockFiles
            });

            const result = await getDeletedFiles('folder123', 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getAllDeletedFiles'),
                expect.objectContaining({
                    method: 'GET',
                    headers: { "Content-Type": "application/json" }
                })
            );
            expect(result).toEqual(mockFiles);
        });

        test('should return empty array when response is not array', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ message: 'No files' })
            });

            const result = await getDeletedFiles('folder123', 'user123');

            expect(result).toEqual([]);
        });

        test('should throw error on failed request', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(getDeletedFiles('folder123', 'user123'))
                .rejects.toThrow('Failed to fetch deleted files');
        });

        test('should include folderId and ownerId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => []
            });

            await getDeletedFiles('folder456', 'user789');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('folderId=folder456'),
                expect.any(Object)
            );
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('ownerId=user789'),
                expect.any(Object)
            );
        });

        test('should handle network error', async () => {
            apiRequest.mockRejectedValueOnce(new Error('Network error'));

            await expect(getDeletedFiles('folder123', 'user123'))
                .rejects.toThrow('Network error');
        });
    });

    describe('getDeletedFolders', () => {
        test('should fetch deleted folders successfully', async () => {
            const mockFolders = [
                { id: 'folder1', name: 'Deleted Folder 1', deletedAt: '2024-01-01' },
                { id: 'folder2', name: 'Deleted Folder 2', deletedAt: '2024-01-02' }
            ];

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockFolders
            });

            const result = await getDeletedFolders('parent123', 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getAllDeletedFolders'),
                expect.objectContaining({
                    method: 'GET',
                    headers: { "Content-Type": "application/json" }
                })
            );
            expect(result).toEqual(mockFolders);
        });

        test('should return empty array when response is not array', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => null
            });

            const result = await getDeletedFolders('parent123', 'user123');

            expect(result).toEqual([]);
        });

        test('should throw error on failed request', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(getDeletedFolders('parent123', 'user123'))
                .rejects.toThrow('Failed to fetch deleted folders');
        });

        test('should include parentId and userId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => []
            });

            await getDeletedFolders('parent456', 'user789');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('parentId=parent456'),
                expect.any(Object)
            );
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('userId=user789'),
                expect.any(Object)
            );
        });
    });

    describe('getDeletedFilesAndFolders', () => {
        test('should fetch both files and folders successfully', async () => {
            const mockFiles = [
                { id: 'file1', name: 'deleted1.txt' }
            ];
            const mockFolders = [
                { id: 'folder1', name: 'Deleted Folder' }
            ];

            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFiles
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFolders
                });

            const result = await getDeletedFilesAndFolders('folder123', 'user123');

            expect(result).toEqual({
                files: mockFiles,
                folders: mockFolders
            });
        });

        test('should throw error when userId is not provided', async () => {
            await expect(getDeletedFilesAndFolders('folder123', null))
                .rejects.toThrow('User ID is required');

            await expect(getDeletedFilesAndFolders('folder123', undefined))
                .rejects.toThrow('User ID is required');

            await expect(getDeletedFilesAndFolders('folder123', ''))
                .rejects.toThrow('User ID is required');
        });

        test('should remove duplicate folders', async () => {
            const mockFiles = [];
            const mockFolders = [
                { id: 'folder1', name: 'Folder 1' },
                { id: 'folder1', name: 'Folder 1 Duplicate' },
                { id: 'folder2', name: 'Folder 2' }
            ];

            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();

            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFiles
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFolders
                });

            const result = await getDeletedFilesAndFolders('folder123', 'user123');

            expect(result.folders).toHaveLength(2);
            expect(result.folders[0].id).toBe('folder1');
            expect(result.folders[1].id).toBe('folder2');
            expect(consoleWarnSpy).toHaveBeenCalledWith('Duplicate folder ID found: folder1');

            consoleWarnSpy.mockRestore();
        });

        test('should remove duplicate files', async () => {
            const mockFiles = [
                { id: 'file1', name: 'File 1' },
                { id: 'file1', name: 'File 1 Duplicate' },
                { id: 'file2', name: 'File 2' }
            ];
            const mockFolders = [];

            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();

            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFiles
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFolders
                });

            const result = await getDeletedFilesAndFolders('folder123', 'user123');

            expect(result.files).toHaveLength(2);
            expect(result.files[0].id).toBe('file1');
            expect(result.files[1].id).toBe('file2');
            expect(consoleWarnSpy).toHaveBeenCalledWith('Duplicate file ID found: file1');

            consoleWarnSpy.mockRestore();
        });

        test('should handle duplicate IDs across files and folders', async () => {
            const mockFiles = [
                { id: 'item1', name: 'File 1' }
            ];
            const mockFolders = [
                { id: 'item1', name: 'Folder 1' },
                { id: 'item2', name: 'Folder 2' }
            ];

            const consoleWarnSpy = jest.spyOn(console, 'warn').mockImplementation();

            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFiles
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFolders
                });

            const result = await getDeletedFilesAndFolders('folder123', 'user123');

            // Folder with duplicate ID should be filtered out
            expect(result.folders).toHaveLength(1);
            expect(result.folders[0].id).toBe('item2');

            consoleWarnSpy.mockRestore();
        });

        test('should handle empty results', async () => {
            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => []
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => []
                });

            const result = await getDeletedFilesAndFolders('folder123', 'user123');

            expect(result).toEqual({
                files: [],
                folders: []
            });
        });

        test('should handle API errors gracefully', async () => {
            apiRequest.mockRejectedValueOnce(new Error('API Error'));

            await expect(getDeletedFilesAndFolders('folder123', 'user123'))
                .rejects.toThrow('API Error');
        });
    });
});