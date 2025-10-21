import {
    checkShareLink,
    downloadFile,
    triggerFileDownload,
    saveSharedFile,
    handleReadShare,
    handleWriteShare
} from './shareService';

jest.mock('../utils/apiRequest');
import apiRequest from '../utils/apiRequest';

// Mock DOM methods
const mockCreateElement = jest.fn();
const mockAppendChild = jest.fn();
const mockRemoveChild = jest.fn();
const mockClick = jest.fn();
const mockCreateObjectURL = jest.fn();
const mockRevokeObjectURL = jest.fn();

global.document = {
    createElement: mockCreateElement,
    body: {
        appendChild: mockAppendChild,
        removeChild: mockRemoveChild
    }
};

global.window = {
    URL: {
        createObjectURL: mockCreateObjectURL,
        revokeObjectURL: mockRevokeObjectURL
    }
};

describe('shareService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockCreateElement.mockReturnValue({
            click: mockClick,
            href: '',
            download: ''
        });
        mockCreateObjectURL.mockReturnValue('blob:mock-url');
    });

    describe('checkShareLink', () => {
        test('should check share link successfully', async () => {
            const mockData = {
                fileId: 'file123',
                fileName: 'document.pdf',
                shareType: 'READ'
            };

            apiRequest.mockResolvedValueOnce({
                status: 200,
                ok: true,
                json: async () => mockData
            });

            const result = await checkShareLink('share123', 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/share/share123'),
                expect.objectContaining({
                    credentials: 'include',
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json'
                    }
                })
            );
            expect(result).toEqual({
                status: 200,
                data: mockData
            });
        });

        test('should include userId in query params when provided', async () => {
            apiRequest.mockResolvedValueOnce({
                status: 200,
                ok: true,
                json: async () => ({})
            });

            await checkShareLink('share123', 'user456');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('userId=user456'),
                expect.any(Object)
            );
        });

        test('should not include userId when it is null', async () => {
            apiRequest.mockResolvedValueOnce({
                status: 200,
                ok: true,
                json: async () => ({})
            });

            await checkShareLink('share123', null);

            const url = apiRequest.mock.calls[0][0];
            expect(url).not.toContain('userId');
        });

        test('should not include userId when it is string "null"', async () => {
            apiRequest.mockResolvedValueOnce({
                status: 200,
                ok: true,
                json: async () => ({})
            });

            await checkShareLink('share123', 'null');

            const url = apiRequest.mock.calls[0][0];
            expect(url).not.toContain('userId');
        });

        test('should return 401 status for unauthorized access', async () => {
            apiRequest.mockResolvedValueOnce({
                status: 401,
                ok: false
            });

            const result = await checkShareLink('share123', 'user123');

            expect(result).toEqual({
                status: 401,
                needsAuth: true
            });
        });

        test('should return 404 status for non-existent share', async () => {
            apiRequest.mockResolvedValueOnce({
                status: 404,
                ok: false
            });

            const result = await checkShareLink('invalid-share', 'user123');

            expect(result).toEqual({
                status: 404
            });
        });

        test('should throw error for other HTTP errors', async () => {
            apiRequest.mockResolvedValueOnce({
                status: 500,
                ok: false
            });

            await expect(checkShareLink('share123', 'user123'))
                .rejects.toThrow('HTTP error! status: 500');
        });
    });

    describe('downloadFile', () => {
        test('should download file successfully', async () => {
            const mockBlob = new Blob(['file content'], { type: 'text/plain' });

            apiRequest.mockResolvedValueOnce({
                ok: true,
                blob: async () => mockBlob
            });

            const result = await downloadFile('file123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/download?fileId=file123'),
                expect.objectContaining({
                    credentials: 'include',
                    headers: {
                        'Accept': 'application/json'
                    }
                })
            );
            expect(result).toBe(mockBlob);
        });

        test('should throw error when download fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(downloadFile('file123'))
                .rejects.toThrow('Download failed');
        });

        test('should include fileId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                blob: async () => new Blob()
            });

            await downloadFile('file456');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('fileId=file456'),
                expect.any(Object)
            );
        });
    });

    describe('triggerFileDownload', () => {
        test('should trigger file download with provided filename', () => {
            const mockBlob = new Blob(['content'], { type: 'text/plain' });
            const mockLink = { click: mockClick, href: '', download: '' };
            mockCreateElement.mockReturnValue(mockLink);

            triggerFileDownload(mockBlob, 'test.txt');

            expect(mockCreateObjectURL).toHaveBeenCalledWith(mockBlob);
            expect(mockLink.href).toBe('blob:mock-url');
            expect(mockLink.download).toBe('test.txt');
            expect(mockAppendChild).toHaveBeenCalledWith(mockLink);
            expect(mockClick).toHaveBeenCalled();
            expect(mockRemoveChild).toHaveBeenCalledWith(mockLink);
            expect(mockRevokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
        });

        test('should use default filename when not provided', () => {
            const mockBlob = new Blob(['content']);
            const mockLink = { click: mockClick, href: '', download: '' };
            mockCreateElement.mockReturnValue(mockLink);

            triggerFileDownload(mockBlob);

            expect(mockLink.download).toBe('download');
        });

        test('should use default filename for null', () => {
            const mockBlob = new Blob(['content']);
            const mockLink = { click: mockClick, href: '', download: '' };
            mockCreateElement.mockReturnValue(mockLink);

            triggerFileDownload(mockBlob, null);

            expect(mockLink.download).toBe('download');
        });
    });

    describe('saveSharedFile', () => {
        test('should save shared file successfully', async () => {
            const mockResponse = {
                newFileId: 'newfile123'
            };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const result = await saveSharedFile('share123', 'user123', 'root123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/saveShare/share123'),
                expect.objectContaining({
                    method: 'POST',
                    credentials: 'include'
                })
            );
            expect(result).toEqual(mockResponse);
        });

        test('should include userId and rootFolderId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ newFileId: 'file123' })
            });

            await saveSharedFile('share456', 'user789', 'root456');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('userId=user789'),
                expect.any(Object)
            );
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('rootFolderId=root456'),
                expect.any(Object)
            );
        });

        test('should throw error when save fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(saveSharedFile('share123', 'user123', 'root123'))
                .rejects.toThrow('Failed to save shared file');
        });

        test('should use POST method', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ newFileId: 'file123' })
            });

            await saveSharedFile('share123', 'user123', 'root123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.any(String),
                expect.objectContaining({
                    method: 'POST'
                })
            );
        });
    });

    describe('handleReadShare', () => {
        test('should download file for READ share', async () => {
            const mockBlob = new Blob(['content'], { type: 'text/plain' });
            const mockLink = { click: mockClick, href: '', download: '' };
            mockCreateElement.mockReturnValue(mockLink);

            apiRequest.mockResolvedValueOnce({
                ok: true,
                blob: async () => mockBlob
            });

            await handleReadShare('file123', 'document.pdf');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/download?fileId=file123'),
                expect.any(Object)
            );
            expect(mockClick).toHaveBeenCalled();
        });

        test('should use provided filename', async () => {
            const mockBlob = new Blob(['content']);
            const mockLink = { click: mockClick, href: '', download: '' };
            mockCreateElement.mockReturnValue(mockLink);

            apiRequest.mockResolvedValueOnce({
                ok: true,
                blob: async () => mockBlob
            });

            await handleReadShare('file123', 'my-document.pdf');

            expect(mockLink.download).toBe('my-document.pdf');
        });

        test('should propagate download errors', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(handleReadShare('file123', 'document.pdf'))
                .rejects.toThrow('Download failed');
        });
    });

    describe('handleWriteShare', () => {
        test('should save shared file for WRITE share', async () => {
            const mockResponse = {
                newFileId: 'newfile456'
            };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResponse
            });

            const result = await handleWriteShare('share123', 'user123', 'root123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/saveShare/share123'),
                expect.any(Object)
            );
            expect(result).toBe('newfile456');
        });

        test('should return newFileId', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ newFileId: 'file789' })
            });

            const newFileId = await handleWriteShare('share123', 'user123', 'root123');

            expect(newFileId).toBe('file789');
        });

        test('should propagate save errors', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false
            });

            await expect(handleWriteShare('share123', 'user123', 'root123'))
                .rejects.toThrow('Failed to save shared file');
        });
    });
});