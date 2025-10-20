
import { useRef, useCallback } from 'react';
import { UPLOAD_CONFIG } from '../utils/uploadHelper';
import { sliceFileIntoChunks, extractETag } from '../utils/uploadUtils';

export const useChunkUpload = () => {
    const xhrRef = useRef(null);
    const isPausedRef = useRef(false);

    /**
     * 上传单个分片
     * @param {Blob} chunk - 文件块
     * @param {Function} uploadFn - 上传函数
     * @param {Function} onProgress - 进度回调
     * @returns {Promise} 包含 partNumber 和 eTag 的对象
     */
    const uploadChunk = useCallback(async (chunk, uploadFn, onProgress) => {
        return new Promise((resolve, reject) => {
            const xhr = uploadFn(chunk, (progress) => {
                if (onProgress) {
                    onProgress(progress);
                }
            });

            xhrRef.current = xhr;

            xhr.then(resolve).catch(reject);
        });
    }, []);

    /**
     * 上传所有分片
     * @param {File} file - 要上传的文件
     * @param {Function} uploadPartFn - 上传单个分片的函数
     * @param {Function} onPartComplete - 单个分片完成的回调
     * @param {Function} onProgress - 总进度回调
     * @param {Array} uploadedParts - 已上传的分片（用于断点续传）
     * @returns {Promise<Array>} 所有分片的 eTag 数组
     */
    const uploadAllChunks = useCallback(
        async (file, uploadPartFn, onPartComplete, onProgress, uploadedParts = []) => {
            const chunks = sliceFileIntoChunks(file, UPLOAD_CONFIG.CHUNK_SIZE);
            const partETags = [...uploadedParts];
            const totalChunks = chunks.length;

            for (let i = 0; i < chunks.length; i++) {
                // 检查是否暂停
                if (isPausedRef.current) {
                    console.log('Upload paused at part', i + 1);
                    return partETags;
                }

                const { chunk, partNumber } = chunks[i];

                // 检查该分片是否已上传
                const alreadyUploaded = partETags.some((part) => part.partNumber === partNumber);
                if (alreadyUploaded) {
                    console.log(`Part ${partNumber} already uploaded, skipping`);
                    if (onProgress) {
                        onProgress(partNumber, totalChunks, 1);
                    }
                    continue;
                }

                try {
                    // 上传分片
                    const result = await uploadPartFn(chunk, partNumber, (progress) => {
                        if (onProgress) {
                            onProgress(partNumber, totalChunks, progress);
                        }
                    });

                    const eTag = extractETag(result);
                    const partInfo = {
                        partNumber,
                        eTag,
                    };

                    partETags.push(partInfo);

                    // 通知分片完成
                    if (onPartComplete) {
                        onPartComplete(partInfo);
                    }
                } catch (error) {
                    if (error.aborted) {
                        console.log(`Part ${partNumber} was aborted`);
                        return partETags;
                    }
                    throw error;
                }
            }

            return partETags;
        },
        []
    );

    /**
     * 暂停上传
     */
    const pause = useCallback(() => {
        isPausedRef.current = true;
        if (xhrRef.current) {
            xhrRef.current.abort();
        }
    }, []);

    /**
     * 恢复上传
     */
    const resume = useCallback(() => {
        isPausedRef.current = false;
    }, []);

    /**
     * 中止上传
     */
    const abort = useCallback(() => {
        isPausedRef.current = true;
        if (xhrRef.current) {
            xhrRef.current.abort();
        }
    }, []);

    /**
     * 检查是否暂停
     */
    const isPaused = useCallback(() => {
        return isPausedRef.current;
    }, []);

    return {
        uploadAllChunks,
        pause,
        resume,
        abort,
        isPaused,
    };
};