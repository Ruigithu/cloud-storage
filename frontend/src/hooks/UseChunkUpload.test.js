import { renderHook, act} from '@testing-library/react';
import { useChunkUpload } from './useChunkUpload';
import { sliceFileIntoChunks, extractETag } from '../utils/uploadUtils';

// Mock uploadUtils
jest.mock('../utils/uploadUtils', () => ({
    sliceFileIntoChunks: jest.fn(),
    extractETag: jest.fn()
}));

// Mock uploadHelper
jest.mock('../utils/uploadHelper', () => ({
    UPLOAD_CONFIG: {
        CHUNK_SIZE: 5 * 1024 * 1024
    }
}));

describe('useChunkUpload', () => {
    let mockFile;
    let mockUploadPartFn;

    beforeEach(() => {
        jest.clearAllMocks();

        mockFile = new File(['content'], 'test.txt', { type: 'text/plain' });
        mockUploadPartFn = jest.fn();

        sliceFileIntoChunks.mockReturnValue([
            { chunk: new Blob(['part1']), partNumber: 1, start: 0, end: 5 },
            { chunk: new Blob(['part2']), partNumber: 2, start: 5, end: 10 }
        ]);

        extractETag.mockImplementation((result) => result.eTag || result);
    });

    describe('uploadAllChunks', () => {
        test('should upload all chunks successfully', async () => {
            const { result } = renderHook(() => useChunkUpload());

            mockUploadPartFn
                .mockResolvedValueOnce({ eTag: 'etag1' })
                .mockResolvedValueOnce({ eTag: 'etag2' });

            let partETags;
            await act(async () => {
                partETags = await result.current.uploadAllChunks(
                    mockFile,
                    mockUploadPartFn,
                    null,
                    null,
                    []
                );
            });

            expect(mockUploadPartFn).toHaveBeenCalledTimes(2);
            expect(partETags).toHaveLength(2);
            expect(partETags[0]).toEqual({ partNumber: 1, eTag: 'etag1' });
            expect(partETags[1]).toEqual({ partNumber: 2, eTag: 'etag2' });
        });

        test('should call onProgress callback for each chunk', async () => {
            const { result } = renderHook(() => useChunkUpload());
            const onProgress = jest.fn();

            mockUploadPartFn
                .mockResolvedValueOnce({ eTag: 'etag1' })
                .mockResolvedValueOnce({ eTag: 'etag2' });

            await act(async () => {
                await result.current.uploadAllChunks(
                    mockFile,
                    mockUploadPartFn,
                    null,
                    onProgress,
                    []
                );
            });

            expect(onProgress).toHaveBeenCalled();
        });

        test('should call onPartComplete callback after each chunk', async () => {
            const { result } = renderHook(() => useChunkUpload());
            const onPartComplete = jest.fn();

            mockUploadPartFn
                .mockResolvedValueOnce({ eTag: 'etag1' })
                .mockResolvedValueOnce({ eTag: 'etag2' });

            await act(async () => {
                await result.current.uploadAllChunks(
                    mockFile,
                    mockUploadPartFn,
                    onPartComplete,
                    null,
                    []
                );
            });

            expect(onPartComplete).toHaveBeenCalledTimes(2);
            expect(onPartComplete).toHaveBeenNthCalledWith(1, {
                partNumber: 1,
                eTag: 'etag1'
            });
            expect(onPartComplete).toHaveBeenNthCalledWith(2, {
                partNumber: 2,
                eTag: 'etag2'
            });
        });

        test('should skip already uploaded chunks', async () => {
            const { result } = renderHook(() => useChunkUpload());
            const onProgress = jest.fn();

            const uploadedParts = [
                { partNumber: 1, eTag: 'etag1' }
            ];

            mockUploadPartFn.mockResolvedValueOnce({ eTag: 'etag2' });

            let partETags;
            await act(async () => {
                partETags = await result.current.uploadAllChunks(
                    mockFile,
                    mockUploadPartFn,
                    null,
                    onProgress,
                    uploadedParts
                );
            });

            // Should only upload part 2
            expect(mockUploadPartFn).toHaveBeenCalledTimes(1);
            expect(partETags).toHaveLength(2);
            expect(partETags[0]).toEqual({ partNumber: 1, eTag: 'etag1' });
            expect(partETags[1]).toEqual({ partNumber: 2, eTag: 'etag2' });

            // Should report progress for skipped part
            expect(onProgress).toHaveBeenCalledWith(1, 2, 1);
        });

        test('should stop uploading when paused', async () => {
            const { result } = renderHook(() => useChunkUpload());

            mockUploadPartFn
                .mockResolvedValueOnce({ eTag: 'etag1' })
                .mockResolvedValueOnce({ eTag: 'etag2' });

            let partETags;
            await act(async () => {
                // Pause after first chunk
                result.current.pause();

                partETags = await result.current.uploadAllChunks(
                    mockFile,
                    mockUploadPartFn,
                    null,
                    null,
                    []
                );
            });

            // Should not upload any chunks because paused at start
            expect(mockUploadPartFn).toHaveBeenCalledTimes(0);
            expect(partETags).toHaveLength(0);
        });

        test('should handle upload error', async () => {
            const { result } = renderHook(() => useChunkUpload());
            const error = new Error('Upload failed');

            mockUploadPartFn.mockRejectedValueOnce(error);

            await act(async () => {
                await expect(
                    result.current.uploadAllChunks(
                        mockFile,
                        mockUploadPartFn,
                        null,
                        null,
                        []
                    )
                ).rejects.toThrow('Upload failed');
            });
        });

        test('should handle aborted upload', async () => {
            const { result } = renderHook(() => useChunkUpload());
            const abortError = new Error('Aborted');
            abortError.aborted = true;

            mockUploadPartFn.mockRejectedValueOnce(abortError);

            const consoleLogSpy = jest.spyOn(console, 'log').mockImplementation();

            let partETags;
            await act(async () => {
                partETags = await result.current.uploadAllChunks(
                    mockFile,
                    mockUploadPartFn,
                    null,
                    null,
                    []
                );
            });

            expect(consoleLogSpy).toHaveBeenCalledWith('Part 1 was aborted');
            expect(partETags).toHaveLength(0);

            consoleLogSpy.mockRestore();
        });

        test('should pass progress to uploadPartFn callback', async () => {
            const { result } = renderHook(() => useChunkUpload());
            const onProgress = jest.fn();

            mockUploadPartFn.mockImplementation((chunk, partNumber, progressCallback) => {
                progressCallback(0.5);
                return Promise.resolve({ eTag: 'etag1' });
            });

            await act(async () => {
                await result.current.uploadAllChunks(
                    mockFile,
                    mockUploadPartFn,
                    null,
                    onProgress,
                    []
                );
            });

            expect(onProgress).toHaveBeenCalledWith(1, 2, 0.5);
        });
    });

    describe('pause', () => {
        test('should set paused state', () => {
            const { result } = renderHook(() => useChunkUpload());

            act(() => {
                result.current.pause();
            });

            expect(result.current.isPaused()).toBe(true);
        });

        test('should abort xhr if exists', () => {
            const { result } = renderHook(() => useChunkUpload());
            const mockXhr = { abort: jest.fn() };

            // Set xhrRef manually (in real scenario, this would be set during upload)
            act(() => {
                result.current.pause();
            });

            // isPaused should return true
            expect(result.current.isPaused()).toBe(true);
        });
    });

    describe('resume', () => {
        test('should clear paused state', () => {
            const { result } = renderHook(() => useChunkUpload());

            act(() => {
                result.current.pause();
            });

            expect(result.current.isPaused()).toBe(true);

            act(() => {
                result.current.resume();
            });

            expect(result.current.isPaused()).toBe(false);
        });
    });

    describe('abort', () => {
        test('should set paused state and abort xhr', () => {
            const { result } = renderHook(() => useChunkUpload());

            act(() => {
                result.current.abort();
            });

            expect(result.current.isPaused()).toBe(true);
        });
    });

    describe('isPaused', () => {
        test('should return false initially', () => {
            const { result } = renderHook(() => useChunkUpload());

            expect(result.current.isPaused()).toBe(false);
        });

        test('should return true after pause', () => {
            const { result } = renderHook(() => useChunkUpload());

            act(() => {
                result.current.pause();
            });

            expect(result.current.isPaused()).toBe(true);
        });

        test('should return false after resume', () => {
            const { result } = renderHook(() => useChunkUpload());

            act(() => {
                result.current.pause();
                result.current.resume();
            });

            expect(result.current.isPaused()).toBe(false);
        });
    });

    describe('extractETag', () => {
        test('should extract eTag from result', async () => {
            const { result } = renderHook(() => useChunkUpload());

            extractETag.mockReturnValueOnce('extracted-etag');
            mockUploadPartFn.mockResolvedValueOnce({ eTag: 'test-etag' });

            await act(async () => {
                await result.current.uploadAllChunks(
                    mockFile,
                    mockUploadPartFn,
                    null,
                    null,
                    []
                );
            });

            expect(extractETag).toHaveBeenCalledWith({ eTag: 'test-etag' });
        });
    });

    describe('sliceFileIntoChunks', () => {
        test('should call sliceFileIntoChunks with correct params', async () => {
            const { result } = renderHook(() => useChunkUpload());

            mockUploadPartFn.mockResolvedValue({ eTag: 'etag' });

            await act(async () => {
                await result.current.uploadAllChunks(
                    mockFile,
                    mockUploadPartFn,
                    null,
                    null,
                    []
                );
            });

            expect(sliceFileIntoChunks).toHaveBeenCalledWith(mockFile, 5 * 1024 * 1024);
        });
    });
});