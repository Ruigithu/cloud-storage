
import { UPLOAD_CONFIG } from './uploadHelper';

/**
 * 将文件切分成指定大小的块
 * @param {File} file - 要切分的文件
 * @param {number} chunkSize - 块的大小（字节）
 * @returns {Array} 包含所有块的数组
 */
export const sliceFileIntoChunks = (file, chunkSize = UPLOAD_CONFIG.CHUNK_SIZE) => {
    const chunks = [];
    const totalChunks = Math.ceil(file.size / chunkSize);

    for (let i = 0; i < totalChunks; i++) {
        const start = i * chunkSize;
        const end = Math.min(start + chunkSize, file.size);
        chunks.push({
            chunk: file.slice(start, end),
            partNumber: i + 1,
            start,
            end,
        });
    }

    return chunks;
};

/**
 * 计算总进度百分比
 * @param {number} currentPart - 当前上传的部分
 * @param {number} totalParts - 总部分数
 * @param {number} partProgress - 当前部分的进度（0-1）
 * @returns {number} 总进度百分比（0-100）
 */
export const calculateProgress = (currentPart, totalParts, partProgress = 0) => {
    const completedProgress = ((currentPart - 1) / totalParts) * 100;
    const currentProgress = (partProgress / totalParts) * 100;
    return Math.min(completedProgress + currentProgress, 100);
};

/**
 * 计算多文件上传的总进度
 * @param {Array} uploads - 上传信息数组
 * @returns {number} 总进度百分比（0-100）
 */
export const calculateBatchProgress = (uploads) => {
    if (uploads.length === 0) return 0;

    const totalProgress = uploads.reduce((sum, upload) => {
        return sum + (upload.progress || 0);
    }, 0);

    return Math.round((totalProgress / uploads.length) * 100);
};

/**
 * 提取 eTag 值（兼容不同的响应格式）
 * @param {Object} response - 响应对象
 * @returns {string} eTag 值
 */
export const extractETag = (response) => {
    return response.etag || response.eTag || response;
};

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化后的文件大小
 */
export const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
};

/**
 * 验证文件大小
 * @param {number} fileSize - 文件大小（字节）
 * @param {number} maxSize - 最大允许大小（字节）
 * @returns {boolean} 是否符合大小限制
 */
export const validateFileSize = (fileSize, maxSize) => {
    return fileSize <= maxSize;
};

/**
 * 构建 FormData 对象
 * @param {Object} data - 要添加到 FormData 的数据对象
 * @returns {FormData} FormData 对象
 */
export const buildFormData = (data) => {
    const formData = new FormData();

    Object.entries(data).forEach(([key, value]) => {
        if (Array.isArray(value)) {
            value.forEach(item => formData.append(key, item));
        } else {
            formData.append(key, value);
        }
    });

    return formData;
};