import {
    loadDocument,
    convertDocxToHtml,
    convertHtmlToDocx,
    uploadFile,
    downloadFile,
    uploadBase64Image,
    processImagesInDelta,
    hasBase64Images
} from './editorService';

jest.mock('../utils/apiRequest');
import apiRequest from '../utils/apiRequest';

// Mock DOM methods for downloadFile
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

global.URL = {
    createObjectURL: mockCreateObjectURL,
    revokeObjectURL: mockRevokeObjectURL
};

describe('editorService', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        mockCreateElement.mockReturnValue({
            click: mockClick,
            href: '',
            download: ''
        });
        mockCreateObjectURL.mockReturnValue('blob:mock-url');
    });

    describe('loadDocument', () => {
        test('should load document successfully', async () => {
            const mockResponse = {
                ok: true,
                headers: {
                    get: jest.fn((key) => {
                        if (key === 'content-type') return 'application/json';
                        if (key === 'content-disposition') return 'filename="test.json"';
                        return null;
                    })
                }
            };

            apiRequest.mockResolvedValueOnce(mockResponse);

            const result = await loadDocument('doc123', 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getFileByUserIdAndFileId'),
                expect.objectContaining({
                    method: 'GET',
                    credentials: 'include'
                })
            );
            expect(result.response).toBe(mockResponse);
            expect(result.contentType).toBe('application/json');
            expect(result.fileName).toBe('test.json');
            expect(result.isJson).toBe(true);
        });

        test('should extract filename from content-disposition header', async () => {
            const mockResponse = {
                ok: true,
                headers: {
                    get: jest.fn((key) => {
                        if (key === 'content-type') return 'application/pdf';
                        if (key === 'content-disposition') return 'filename="report.pdf"';
                        return null;
                    })
                }
            };

            apiRequest.mockResolvedValueOnce(mockResponse);

            const result = await loadDocument('doc123', 'user123');

            expect(result.fileName).toBe('report.pdf');
        });

        test('should use default filename when content-disposition is missing', async () => {
            const mockResponse = {
                ok: true,
                headers: {
                    get: jest.fn((key) => {
                        if (key === 'content-type') return 'application/json';
                        return null;
                    })
                }
            };

            apiRequest.mockResolvedValueOnce(mockResponse);

            const result = await loadDocument('doc123', 'user123');

            expect(result.fileName).toBe('document');
        });

        test('should throw error when response is not ok', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false,
                status: 404
            });

            await expect(loadDocument('doc123', 'user123'))
                .rejects.toThrow('Server responded with status 404');
        });

        test('should correctly identify JSON content type', async () => {
            const mockResponse = {
                ok: true,
                headers: {
                    get: jest.fn((key) => {
                        if (key === 'content-type') return 'application/json; charset=utf-8';
                        return null;
                    })
                }
            };

            apiRequest.mockResolvedValueOnce(mockResponse);

            const result = await loadDocument('doc123', 'user123');

            expect(result.isJson).toBe(true);
        });
    });

    describe('convertDocxToHtml', () => {
        test('should convert docx to html successfully', async () => {
            const mockHtmlContent = '<p>Hello World</p>';

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ htmlContent: mockHtmlContent })
            });

            const result = await convertDocxToHtml('doc123', 'user123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/convertDocxToHtmlMammoth'),
                expect.objectContaining({
                    method: 'GET',
                    credentials: 'include'
                })
            );
            expect(result).toBe(mockHtmlContent);
        });

        test('should throw error when conversion fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false,
                status: 500
            });

            await expect(convertDocxToHtml('doc123', 'user123'))
                .rejects.toThrow('Failed to convert document with Mammoth: 500');
        });

        test('should include userId and fileId in query params', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ htmlContent: '<p>test</p>' })
            });

            await convertDocxToHtml('doc456', 'user789');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('ownerId=user789'),
                expect.any(Object)
            );
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('fileId=doc456'),
                expect.any(Object)
            );
        });
    });

    describe('convertHtmlToDocx', () => {
        test('should convert html to docx successfully', async () => {
            const mockResult = {
                fileName: 'document.docx',
                mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
                versionId: 'v123'
            };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResult
            });

            const htmlContent = '<p>Hello World</p>';
            const result = await convertHtmlToDocx(htmlContent, 'document', 'user123', 'doc123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/convertHtmlToDocx'),
                expect.objectContaining({
                    method: 'POST',
                    credentials: 'include',
                    body: expect.stringContaining(htmlContent)
                })
            );
            expect(result).toEqual(mockResult);
        });

        test('should append .docx extension if not present', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({})
            });

            await convertHtmlToDocx('<p>test</p>', 'document', 'user123', 'doc123');

            const callArgs = apiRequest.mock.calls[0][1];
            const body = JSON.parse(callArgs.body);

            expect(body.fileName).toBe('document.docx');
        });

        test('should not append extension if .docx already present', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({})
            });

            await convertHtmlToDocx('<p>test</p>', 'document.docx', 'user123', 'doc123');

            const callArgs = apiRequest.mock.calls[0][1];
            const body = JSON.parse(callArgs.body);

            expect(body.fileName).toBe('document.docx');
        });

        test('should not append extension if .doc already present', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({})
            });

            await convertHtmlToDocx('<p>test</p>', 'document.doc', 'user123', 'doc123');

            const callArgs = apiRequest.mock.calls[0][1];
            const body = JSON.parse(callArgs.body);

            expect(body.fileName).toBe('document.doc');
        });

        test('should throw error when conversion fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false,
                status: 500
            });

            await expect(convertHtmlToDocx('<p>test</p>', 'document', 'user123', 'doc123'))
                .rejects.toThrow('Failed to convert to DOCX: 500');
        });
    });

    describe('uploadFile', () => {
        test('should upload file successfully', async () => {
            const mockResult = {
                fileId: 'file123',
                url: 'https://example.com/file123'
            };

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => mockResult
            });

            const mockFile = new File(['content'], 'test.txt', { type: 'text/plain' });
            const result = await uploadFile(mockFile, 'user123', 'doc123');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/uploadNewFile'),
                expect.objectContaining({
                    method: 'POST',
                    credentials: 'include'
                })
            );
            expect(result).toEqual(mockResult);
        });

        test('should throw error when upload fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false,
                status: 500
            });

            const mockFile = new File(['content'], 'test.txt', { type: 'text/plain' });

            await expect(uploadFile(mockFile, 'user123', 'doc123'))
                .rejects.toThrow('Failed to save: 500');
        });
    });

    describe('downloadFile', () => {
        test('should download file successfully', async () => {
            const mockBlob = new Blob(['file content'], { type: 'text/plain' });

            apiRequest.mockResolvedValueOnce({
                ok: true,
                blob: async () => mockBlob
            });

            await downloadFile('doc123', 'user123', 'test.txt');

            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/getFileByUserIdAndFileId'),
                expect.objectContaining({
                    method: 'GET',
                    credentials: 'include',
                    headers: {
                        'Accept': 'application/octet-stream'
                    }
                })
            );
            expect(mockCreateObjectURL).toHaveBeenCalledWith(mockBlob);
            expect(mockClick).toHaveBeenCalled();
            expect(mockRevokeObjectURL).toHaveBeenCalledWith('blob:mock-url');
        });

        test('should use default filename when not provided', async () => {
            const mockBlob = new Blob(['content']);
            const mockLink = { click: mockClick, href: '', download: '' };
            mockCreateElement.mockReturnValue(mockLink);

            apiRequest.mockResolvedValueOnce({
                ok: true,
                blob: async () => mockBlob
            });

            await downloadFile('doc123', 'user123');

            expect(mockLink.download).toBe('file_doc123');
        });

        test('should throw error when download fails', async () => {
            apiRequest.mockResolvedValueOnce({
                ok: false,
                status: 404
            });

            await expect(downloadFile('doc123', 'user123', 'test.txt'))
                .rejects.toThrow('Failed to download: 404');
        });
    });

    describe('uploadBase64Image', () => {
        test('should upload base64 image successfully', async () => {
            const mockUrl = 'https://example.com/image123.png';

            global.fetch = jest.fn()
                .mockResolvedValueOnce({
                    blob: async () => new Blob(['image data'], { type: 'image/png' })
                });

            apiRequest.mockResolvedValueOnce({
                ok: true,
                json: async () => ({ url: mockUrl })
            });

            const base64Image = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==';
            const result = await uploadBase64Image(base64Image, 'user123', 'doc123');

            expect(result).toBe(mockUrl);
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/uploadNewFile'),
                expect.objectContaining({
                    method: 'POST',
                    credentials: 'include'
                })
            );
        });

        test('should throw error when upload fails', async () => {
            global.fetch = jest.fn()
                .mockResolvedValueOnce({
                    blob: async () => new Blob(['image data'])
                });

            apiRequest.mockResolvedValueOnce({
                ok: false,
                status: 500
            });

            const base64Image = 'data:image/png;base64,abc123';

            await expect(uploadBase64Image(base64Image, 'user123', 'doc123'))
                .rejects.toThrow('Failed to upload image: 500');
        });
    });

    describe('processImagesInDelta', () => {
        test('should process base64 images in delta', async () => {
            global.fetch = jest.fn()
                .mockResolvedValue({
                    blob: async () => new Blob(['image'])
                });

            apiRequest.mockResolvedValue({
                ok: true,
                json: async () => ({ url: 'https://example.com/image.png' })
            });

            const delta = {
                ops: [
                    { insert: 'Hello ' },
                    { insert: { image: 'data:image/png;base64,abc123' } },
                    { insert: ' World' }
                ]
            };

            const result = await processImagesInDelta(delta, 'user123', 'doc123');

            expect(result.ops).toHaveLength(3);
            expect(result.ops[1].insert.image).toBe('https://example.com/image.png');
        });

        test('should not modify non-base64 images', async () => {
            const delta = {
                ops: [
                    { insert: { image: 'https://example.com/existing.png' } }
                ]
            };

            const result = await processImagesInDelta(delta, 'user123', 'doc123');

            expect(result.ops[0].insert.image).toBe('https://example.com/existing.png');
        });

        test('should handle delta with no images', async () => {
            const delta = {
                ops: [
                    { insert: 'Plain text' }
                ]
            };

            const result = await processImagesInDelta(delta, 'user123', 'doc123');

            expect(result.ops).toEqual(delta.ops);
        });
    });

    describe('hasBase64Images', () => {
        test('should return true when delta contains base64 images', () => {
            const delta = {
                ops: [
                    { insert: 'Text' },
                    { insert: { image: 'data:image/png;base64,abc123' } }
                ]
            };

            expect(hasBase64Images(delta)).toBe(true);
        });

        test('should return false when delta has no base64 images', () => {
            const delta = {
                ops: [
                    { insert: 'Text' },
                    { insert: { image: 'https://example.com/image.png' } }
                ]
            };

            expect(hasBase64Images(delta)).toBe(false);
        });

        test('should return false when delta has no images at all', () => {
            const delta = {
                ops: [
                    { insert: 'Plain text only' }
                ]
            };

            expect(hasBase64Images(delta)).toBe(false);
        });

        test('should return false for empty delta', () => {
            const delta = { ops: [] };

            expect(hasBase64Images(delta)).toBe(false);
        });
    });
});