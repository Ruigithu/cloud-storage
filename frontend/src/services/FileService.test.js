import {
    getRootFiles,
    getFilesByFolder,
    getRootFolders,
    getFoldersByParent,
    getFilesAndFolders
} from './fileService';

jest.mock('../utils/apiRequest');
import apiRequest from '../utils/apiRequest';

describe('fileService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    describe('getRootFiles', () => {
        test('should fetch root files successfully', async () => {
            const mockFiles = [
                { id: 'file1', name: 'document1.txt' },
                { id: 'file2', name: 'document2.pdf' }
            ];

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockFiles
            });

            const result = await getRootFiles('user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getRootFiles'),
                expect.objectContaining({
                    method: 'GET',
                    headers: { "Content-Type": "application/json" }
                })
            );
            expect(result).toEqual(mockFiles);
        });

        test('should extract files array from nested response', async () => {
            const mockFiles = [{ id: 'file1', name: 'test.txt' }];

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ files: mockFiles })
            });

            const result = await getRootFiles('user123');

            expect(result).toEqual(mockFiles);
        });

        test('should return empty array when files property is missing', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ message: 'No files' })
            });

            const result = await getRootFiles('user123');

            expect(result).toEqual([]);
        });

        test('should throw error when request fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(getRootFiles('user123'))
                .rejects.toThrow('Failed to fetch root files');
        });

        test('should include ownerId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => []
            });

            await getRootFiles('user456');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('ownerId=user456'),
                expect.any(Object)
            );
        });
    });

    describe('getFilesByFolder', () => {
        test('should fetch files by folder successfully', async () => {
            const mockFiles = [
                { id: 'file1', name: 'doc1.txt', folderId: 'folder123' }
            ];

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockFiles
            });

            const result = await getFilesByFolder('folder123', 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getAllFiles'),
                expect.objectContaining({
                    method: 'GET',
                    headers: { "Content-Type": "application/json" }
                })
            );
            expect(result).toEqual(mockFiles);
        });

        test('should extract files from nested response', async () => {
            const mockFiles = [{ id: 'file1', name: 'test.txt' }];

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ files: mockFiles, count: 1 })
            });

            const result = await getFilesByFolder('folder123', 'user123');

            expect(result).toEqual(mockFiles);
        });

        test('should include folderId and ownerId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => []
            });

            await getFilesByFolder('folder456', 'user789');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('folderId=folder456'),
                expect.any(Object)
            );
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('ownerId=user789'),
                expect.any(Object)
            );
        });

        test('should throw error when request fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(getFilesByFolder('folder123', 'user123'))
                .rejects.toThrow('Failed to fetch files');
        });
    });

    describe('getRootFolders', () => {
        test('should fetch root folders successfully', async () => {
            const mockData = {
                folders: [
                    { id: 'folder1', name: 'Documents' },
                    { id: 'folder2', name: 'Images' }
                ],
                rootFolderId: 'root123'
            };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockData
            });

            const result = await getRootFolders('user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getRootFolders'),
                expect.objectContaining({
                    method: 'GET',
                    headers: { "Content-Type": "application/json" }
                })
            );
            expect(result).toEqual(mockData);
        });

        test('should include userId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ folders: [] })
            });

            await getRootFolders('user456');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('userId=user456'),
                expect.any(Object)
            );
        });

        test('should throw error when request fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(getRootFolders('user123'))
                .rejects.toThrow('Failed to fetch root folders');
        });
    });

    describe('getFoldersByParent', () => {
        test('should fetch folders by parent successfully', async () => {
            const mockFolders = [
                { id: 'folder1', name: 'Subfolder 1', parentId: 'parent123' }
            ];

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ folders: mockFolders })
            });

            const result = await getFoldersByParent('parent123', 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getAllFolders'),
                expect.objectContaining({
                    method: 'GET',
                    headers: { "Content-Type": "application/json" }
                })
            );
            expect(result).toEqual(mockFolders);
        });

        test('should return data directly if folders property is missing', async () => {
            const mockFolders = [{ id: 'folder1', name: 'Folder 1' }];

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockFolders
            });

            const result = await getFoldersByParent('parent123', 'user123');

            expect(result).toEqual(mockFolders);
        });

        test('should include parentId and userId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ folders: [] })
            });

            await getFoldersByParent('parent456', 'user789');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('parentId=parent456'),
                expect.any(Object)
            );
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('userId=user789'),
                expect.any(Object)
            );
        });

        test('should throw error when request fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(getFoldersByParent('parent123', 'user123'))
                .rejects.toThrow('Failed to fetch folders');
        });
    });

    describe('getFilesAndFolders', () => {
        test('should fetch root files and folders successfully', async () => {
            const mockFiles = [{ id: 'file1', name: 'test.txt' }];
            const mockFolderData = {
                folders: [{ id: 'folder1', name: 'Folder 1' }],
                rootFolderId: 'root123'
            };

            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFiles
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFolderData
                });

            const result = await getFilesAndFolders(null, 'user123');

            expect(result).toEqual({
                files: mockFiles,
                folders: mockFolderData.folders,
                rootFolderId: 'root123'
            });
        });

        test('should fetch files and folders by folderId', async () => {
            const mockFiles = [{ id: 'file1', name: 'test.txt' }];
            const mockFolders = [{ id: 'folder1', name: 'Subfolder' }];

            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFiles
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => ({ folders: mockFolders })
                });

            const result = await getFilesAndFolders('folder123', 'user123');

            expect(result).toEqual({
                files: mockFiles,
                folders: mockFolders
            });
            expect(result).not.toHaveProperty('rootFolderId');
        });

        test('should throw error when userId is not provided', async () => {
            await expect(getFilesAndFolders('folder123', null))
                .rejects.toThrow('User ID is required');

            await expect(getFilesAndFolders('folder123', undefined))
                .rejects.toThrow('User ID is required');

            await expect(getFilesAndFolders('folder123', ''))
                .rejects.toThrow('User ID is required');
        });

        test('should handle folders as direct array response', async () => {
            const mockFiles = [];
            const mockFolders = [{ id: 'folder1', name: 'Folder' }];

            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFiles
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => mockFolders
                });

            const result = await getFilesAndFolders('folder123', 'user123');

            expect(result.folders).toEqual(mockFolders);
        });

        test('should return empty arrays when no data', async () => {
            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => []
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => ({ folders: [] })
                });

            const result = await getFilesAndFolders('folder123', 'user123');

            expect(result).toEqual({
                files: [],
                folders: []
            });
        });

        test('should handle API errors', async () => {
            apiRequest.mockRejectedValueOnce(new Error('Network error'));

            await expect(getFilesAndFolders('folder123', 'user123'))
                .rejects.toThrow('Network error');
        });

        test('should call appropriate APIs for root folder', async () => {
            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => []
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => ({ folders: [] })
                });

            await getFilesAndFolders(null, 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getRootFiles'),
                expect.any(Object)
            );
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getRootFolders'),
                expect.any(Object)
            );
        });

        test('should call appropriate APIs for specific folder', async () => {
            apiRequest
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => []
                })
                .mockResolvedValueOnce({
                    ok: true,
                    json: async () => ({ folders: [] })
                });

            await getFilesAndFolders('folder456', 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getAllFiles'),
                expect.any(Object)
            );
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getAllFolders'),
                expect.any(Object)
            );
        });
    });
});