import { useState, useRef, useCallback } from 'react';
import { useChunkUpload } from './useChunkUpload';
import * as uploadService from '../services/uploadService';
import { calculateProgress } from '../utils/uploadUtils';
import { UPLOAD_STATUS } from '../utils/uploadHelper';

export const useFileUpload = ({ ownerId, folderId, onSuccess }) => {
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [isPaused, setIsPaused] = useState(false);
    const [uploadId, setUploadId] = useState(null);
    const [fileId, setFileId] = useState(null);
    const [uploadedParts, setUploadedParts] = useState([]);

    const fileRef = useRef(null);
    const { uploadAllChunks, pause: pauseChunks, resume: resumeChunks } = useChunkUpload();

    /**
     * 初始化上传
     */
    const initUpload = useCallback(
        async (file) => {
            const initData = await uploadService.initResumableUpload({
                ownerId,
                folderId,
                fileName: file.name,
                mimeType: file.type,
                fileSize: file.size,
            });

            setFileId(initData.fileId);
            setUploadId(initData.uploadId);

            // 获取已上传的分片
            const parts = await uploadService.getUploadedParts(initData.fileId, initData.uploadId);
            const formattedParts = parts.map((part) => ({
                partNumber: part.partNumber,
                eTag: part.eTag || part.etag,
            }));

            setUploadedParts(formattedParts);

            return { ...initData, uploadedParts: formattedParts };
        },
        [ownerId, folderId]
    );

    /**
     * 上传文件分片
     */
    const uploadFileParts = useCallback(
        async (file, fileId, uploadId, uploadedParts) => {
            const uploadPartFn = (chunk, partNumber, onProgress) => {
                return uploadService.uploadPartWithProgress(
                    chunk,
                    fileId,
                    uploadId,
                    partNumber,
                    onProgress
                );
            };

            const handleProgress = (partNumber, totalParts, partProgress) => {
                const progress = calculateProgress(partNumber, totalParts, partProgress);
                setUploadProgress(progress);
            };

            const handlePartComplete = (partInfo) => {
                setUploadedParts((prev) => [
                    ...prev.filter((p) => p.partNumber !== partInfo.partNumber),
                    partInfo,
                ]);
            };

            const partETags = await uploadAllChunks(
                file,
                uploadPartFn,
                handlePartComplete,
                handleProgress,
                uploadedParts
            );

            return partETags;
        },
        [uploadAllChunks]
    );

    /**
     * 完成上传
     */
    const completeUpload = useCallback(
        async (fileId, uploadId, partETags) => {
            await uploadService.completeResumableUpload(fileId, uploadId, partETags);

            alert('File uploaded successfully');

            if (onSuccess) {
                onSuccess();
            }

            // 重置状态
            setIsUploading(false);
            setUploadId(null);
            setFileId(null);
            setUploadedParts([]);
            setUploadProgress(0);
            setIsPaused(false);
        },
        [onSuccess]
    );

    /**
     * 开始上传
     */
    const startUpload = useCallback(
        async (file) => {
            if (!file) return;

            fileRef.current = file;
            setIsUploading(true);
            setUploadProgress(0);
            setIsPaused(false);

            try {
                // 初始化上传
                const { fileId, uploadId, uploadedParts } = await initUpload(file);

                // 上传分片
                const partETags = await uploadFileParts(file, fileId, uploadId, uploadedParts);

                // 完成上传
                await completeUpload(fileId, uploadId, partETags);
            } catch (error) {
                console.error('Error uploading file:', error);
                setIsUploading(false);
            }
        },
        [initUpload, uploadFileParts, completeUpload]
    );

    /**
     * 暂停上传
     */
    const pauseUpload = useCallback(() => {
        setIsPaused(true);
        pauseChunks();
    }, [pauseChunks]);

    /**
     * 恢复上传
     */
    const resumeUpload = useCallback(async () => {
        if (!fileRef.current || !uploadId || !fileId) return;

        setIsPaused(false);
        resumeChunks();

        try {
            // 获取最新的已上传分片
            const parts = await uploadService.getUploadedParts(fileId, uploadId);
            const formattedParts = parts.map((part) => ({
                partNumber: part.partNumber,
                eTag: part.eTag || part.etag,
            }));

            setUploadedParts(formattedParts);

            // 继续上传
            const partETags = await uploadFileParts(
                fileRef.current,
                fileId,
                uploadId,
                formattedParts
            );

            // 完成上传
            await completeUpload(fileId, uploadId, partETags);
        } catch (error) {
            console.error('Error resuming upload:', error);
            setIsUploading(false);
        }
    }, [fileId, uploadId, resumeChunks, uploadFileParts, completeUpload]);

    /**
     * 取消上传
     */
    const cancelUpload = useCallback(async () => {
        if (!fileId || !uploadId) return;

        try {
            await uploadService.abortResumableUpload(fileId, uploadId, folderId, ownerId);
            alert('Upload cancelled');
        } catch (error) {
            console.error('Error cancelling upload:', error);
        } finally {
            setIsUploading(false);
            setUploadId(null);
            setFileId(null);
            setUploadedParts([]);
            setUploadProgress(0);
            setIsPaused(false);
        }
    }, [fileId, uploadId, folderId, ownerId]);

    return {
        // 状态
        isUploading,
        uploadProgress,
        isPaused,

        // 方法
        startUpload,
        pauseUpload,
        resumeUpload,
        cancelUpload,
    };
};