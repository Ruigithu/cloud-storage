import { useState, useRef, useCallback, useEffect } from 'react';
import * as uploadService from '../services/uploadService';
import { calculateBatchProgress, extractETag } from '../utils/uploadUtils';
import { UPLOAD_CONFIG, UPLOAD_STATUS } from '../utils/uploadHelper';

export const useFolderUpload = ({ userId, parentId, onSuccess }) => {
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [activeUploads, setActiveUploads] = useState([]);
    const [totalFiles, setTotalFiles] = useState(0);
    const [uploadedFiles, setUploadedFiles] = useState(0);
    const [failedFiles, setFailedFiles] = useState([]);
    const [retryQueue, setRetryQueue] = useState([]);

    const cancelRef = useRef(false);

    /**
     * 完成单个文件上传
     */
    const completeFileUpload = useCallback(
        async (fileId, uploadId, partETags) => {
            if (cancelRef.current) return;

            // const payload = {
            //     fileId,
            //     uploadId,
            //     partETags: partETags.map((part) => ({
            //         partNumber: part.partNumber,
            //         eTag: part.eTag,
            //     })),
            // };

            try {
                await uploadService.completeResumableUpload(fileId, uploadId, partETags);

                // 更新状态
                setActiveUploads((prev) => prev.filter((upload) => upload.fileId !== fileId));
                setUploadedFiles((prev) => prev + 1);
            } catch (error) {
                console.error(`Error completing upload for file ${fileId}:`, error);
                throw error;
            }
        },
        []
    );
    /**
     * 上传单个文件的所有分片
     */
    const uploadFileParts = useCallback(
        async (fileInfo, file) => {
            // 检查是否取消
            if (cancelRef.current) return;

            const { fileId, uploadId } = fileInfo;
            const totalChunks = Math.ceil(file.size /UPLOAD_CONFIG.CHUNK_SIZE);
            const formattedPartETags = [];

            try {
                for (let partNumber = 1; partNumber <= totalChunks; partNumber++) {
                    // 检查取消
                    if (cancelRef.current) {
                        console.log(`Cancelled upload for ${file.name}`);
                        return;
                    }

                    const start = (partNumber - 1) * UPLOAD_CONFIG.CHUNK_SIZE;
                    const end = Math.min(start + UPLOAD_CONFIG.CHUNK_SIZE, file.size);
                    const chunk = file.slice(start, end);

                    // 上传分片
                    const partETag = await uploadService.uploadFolderFilePart(
                        fileId,
                        uploadId,
                        partNumber,
                        chunk
                    );

                    // 检查取消
                    if (cancelRef.current) {
                        console.log(`Cancelled upload for ${file.name} after part ${partNumber}`);
                        return;
                    }

                    const formattedETag = {
                        partNumber,
                        eTag: extractETag(partETag),
                    };

                    formattedPartETags.push(formattedETag);

                    // 更新进度
                    setActiveUploads((prev) => {
                        const updated = [...prev];
                        const uploadIndex = updated.findIndex((u) => u.fileId === fileId);
                        if (uploadIndex !== -1) {
                            updated[uploadIndex].partETags.push(formattedETag);
                            updated[uploadIndex].progress = partNumber / totalChunks;
                        }
                        return updated;
                    });
                }

                // 完成单个文件
                if (!cancelRef.current) {
                    await completeFileUpload(fileId, uploadId, formattedPartETags);
                }
            } catch (error) {
                console.error(`Error uploading file parts for ${file.name}:`, error);

                // 加入重试队列
                setFailedFiles((prev) => [
                    ...prev,
                    {
                        fileName: file.name,
                        error: error.message,
                    },
                ]);
                setRetryQueue((prev) => [...prev, { fileInfo, file }]);
            }
        },
        [completeFileUpload]
    );


    /**
     * 完成所有上传
     */
    const completeAllUploads = useCallback(async () => {
        try {
            // 过滤未完成的上传
            const pendingUploads = activeUploads.filter(
                (upload) => upload.status !== UPLOAD_STATUS.COMPLETED
            );

            if (pendingUploads.length === 0) {
                console.log('All uploads already completed');
                setIsUploading(false);
                if (onSuccess) {
                    onSuccess();
                }
                return;
            }

            const fileCompletions = pendingUploads.map((upload) => ({
                fileId: upload.fileId,
                uploadId: upload.uploadId,
                partETags: upload.partETags || [],
            }));

            await uploadService.completeFolderUpload(fileCompletions);

            alert('Folder uploaded successfully');
            setIsUploading(false);
            setActiveUploads([]);

            if (onSuccess) {
                onSuccess();
            }
        } catch (error) {
            console.error('Error completing uploads:', error);
            alert('Failed to complete some uploads');
            setIsUploading(false);
        }
    }, [activeUploads, onSuccess]);

    /**
     * 开始上传文件夹
     */
    const startUpload = useCallback(
        async (files) => {
            if (!files || files.length === 0) return;

            const fileArray = Array.from(files);
            setIsUploading(true);
            setTotalFiles(fileArray.length);
            setUploadedFiles(0);
            setFailedFiles([]);
            setRetryQueue([]);
            cancelRef.current = false;

            try {
                // 初始化文件夹上传
                const uploadInfo = await uploadService.initFolderUpload(fileArray, userId, parentId);
                const fileUploads = uploadInfo.fileUploads || [];

                // 创建上传信息数组
                const uploads = fileUploads.map((fileInfo) => {
                    const file = fileArray.find((f) => f.webkitRelativePath === fileInfo.relativePath);
                    return {
                        fileId: fileInfo.fileId,
                        uploadId: fileInfo.uploadId,
                        fileName: file?.name,
                        relativePath: fileInfo.relativePath,
                        progress: 0,
                        partETags: [],
                        status: UPLOAD_STATUS.PENDING,
                    };
                });

                setActiveUploads(uploads);

                // 上传每个文件
                for (let i = 0; i < fileUploads.length; i++) {
                    if (cancelRef.current) {
                        console.log('Upload cancelled, stopping processing');
                        break;
                    }

                    const fileInfo = fileUploads[i];
                    const file = fileArray.find((f) => f.webkitRelativePath === fileInfo.relativePath);

                    if (file) {
                        await uploadFileParts(fileInfo, file);
                    }
                }
            } catch (error) {
                console.error('Error starting folder upload:', error);
                alert('Failed to start folder upload');
                setIsUploading(false);
            }
        },
        [userId, parentId, uploadFileParts]
    );

    /**
     * 取消上传
     */
    const cancelUpload = useCallback(async () => {
        if (!isUploading || activeUploads.length === 0) return;

        cancelRef.current = true;

        try {
            await uploadService.abortFolderUpload(activeUploads, userId, parentId);

            setIsUploading(false);
            setActiveUploads([]);
            setRetryQueue([]);
            setFailedFiles([]);
            alert('Upload cancelled');
        } catch (error) {
            console.error('Error cancelling upload:', error);
        }
    }, [isUploading, activeUploads, userId, parentId]);

    /**
     * 自动重试失败的文件
     */
    useEffect(() => {
        if (retryQueue.length > 0 && !isUploading) {
            const retryFile = retryQueue[0];
            setRetryQueue((prev) => prev.slice(1));
            uploadFileParts(retryFile.fileInfo, retryFile.file);
        }
    }, [retryQueue, isUploading, uploadFileParts]);

    /**
     * 计算总进度
     */
    useEffect(() => {
        const progress = calculateBatchProgress(activeUploads);
        setUploadProgress(progress);
    }, [activeUploads]);

    return {
        // 状态
        isUploading,
        uploadProgress,
        totalFiles,
        uploadedFiles,
        failedFiles,
        activeUploads,

        // 方法
        startUpload,
        cancelUpload,
        completeAllUploads,
    };
};