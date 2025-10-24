/**
 * 上传相关 API 服务
 */
import apiRequest from '../../src/utils/apiRequest';
import { API_ENDPOINTS } from '../utils/uploadHelper';
import { buildFormData } from '../utils/uploadUtils';

const API_URL = process.env.REACT_APP_API_URL;

/**
 * 获取认证 token
 */
const getAuthToken = () => localStorage.getItem('token');

/**
 * 获取认证头
 */
const getAuthHeaders = () => ({
    Authorization: `Bearer ${getAuthToken()}`,
});


/**
 * 初始化断点续传
 */
export const initResumableUpload = async ({ ownerId, folderId, fileName, mimeType, fileSize }) => {
    const formData = buildFormData({
        ownerId,
        folderId,
        fileName,
        mimeType,
        fileSize: fileSize.toString(),
    });

    const response = await apiRequest(`${API_URL}${API_ENDPOINTS.RESUMABLE_INIT}`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: formData,
    });

    if (!response.ok) {
        throw new Error('Failed to initialize upload');
    }

    return response.json();
};

/**
 * 获取已上传的部分列表
 */
export const getUploadedParts = async (fileId, uploadId) => {
    const response = await apiRequest(
        `${API_URL}${API_ENDPOINTS.RESUMABLE_PARTS}?fileId=${fileId}&uploadId=${uploadId}`,
        {

            method: 'GET',
            headers: getAuthHeaders(),
        }
    );

    if (!response.ok) {
        throw new Error('Failed to get uploaded parts');
    }

    return response.json();
};

/**
 * 上传单个分片（使用 XMLHttpRequest 支持进度回调）
 */
export const uploadPartWithProgress = (chunk, fileId, uploadId, partNumber, onProgress,onXHRCreated) => {
    return new Promise((resolve, reject) => {
        const formData = buildFormData({
            part: chunk,
            fileId,
            uploadId,
            partNumber,
        });

        const xhr = new XMLHttpRequest();

        if (onXHRCreated) {
            onXHRCreated(xhr);
        }

        // 进度监听
        xhr.upload.addEventListener('progress', (event) => {
            if (event.lengthComputable && onProgress) {
                const progress = event.loaded / event.total;
                onProgress(progress);
            }
        });

        // 完成监听
        xhr.addEventListener('load', () => {
            if (xhr.status >= 200 && xhr.status < 300) {
                try {
                    const response = JSON.parse(xhr.responseText);
                    resolve(response);
                } catch (error) {
                    reject(new Error('Failed to parse response'));
                }
            } else {
                reject(new Error(`Upload failed with status ${xhr.status}`));
            }
        });

        // 错误监听
        xhr.addEventListener('error', () => {
            reject(new Error('Network error during upload'));
        });

        // 中止监听
        xhr.addEventListener('abort', () => {
            const error = new Error('Upload aborted');
            error.aborted = true;
            reject(error);
        });

        xhr.open('POST', `${API_URL}${API_ENDPOINTS.RESUMABLE_PART}`, true);
        xhr.setRequestHeader('Authorization', `Bearer ${getAuthToken()}`);
        xhr.send(formData);

        // 返回 xhr 实例以便外部可以调用 abort
        return xhr;
    });
};

/**
 * 完成断点续传
 */
export const completeResumableUpload = async (fileId, uploadId, partETags) => {
    const payload = {
        fileId,
        uploadId,
        partETags: partETags.map((part) => ({
            partNumber: part.partNumber,
            eTag: part.eTag,
        })),
    };

    const response = await apiRequest(`${API_URL}${API_ENDPOINTS.RESUMABLE_COMPLETE}`, {
        method: 'POST',
        body: JSON.stringify(payload),
        headers: {
            'Content-Type': 'application/json',
            ...getAuthHeaders(),
        },
    });

    if (!response.ok) {
        throw new Error('Failed to complete upload');
    }

    return response.json();
};

/**
 * 取消断点续传
 */
export const abortResumableUpload = async (fileId, uploadId, folderId, userId) => {
    const formData = buildFormData({
        fileId,
        uploadId,
        folderId,
        userId,
    });

    const response = await apiRequest(`${API_URL}${API_ENDPOINTS.RESUMABLE_ABORT}`, {
        method: 'POST',
        body: formData,
        headers: getAuthHeaders(),
    });

    if (!response.ok) {
        throw new Error('Failed to abort upload');
    }

    return response.json();
};

/**
 * ========== 文件夹上传相关 API ==========
 */

/**
 * 初始化文件夹上传
 */
export const initFolderUpload = async (files, userId, parentId) => {
    const formData = new FormData();
    formData.append('userId', userId);
    formData.append('parentFolderId', parentId);

    files.forEach((file) => {
        formData.append('files', file);
        formData.append('relativePaths', file.webkitRelativePath);
    });

    const response = await apiRequest(`${API_URL}${API_ENDPOINTS.FOLDER_INIT}`, {
        method: 'POST',
        body: formData,
    });

    if (!response.ok) {
        throw new Error('Failed to initiate folder upload');
    }

    return response.json();
};

/**
 * 上传文件夹中的单个文件分片
 */
export const uploadFolderFilePart = async (fileId, uploadId, partNumber, chunk) => {
    const formData = buildFormData({
        fileId,
        uploadId,
        partNumber,
        file: chunk,
    });

    const response = await apiRequest(`${API_URL}${API_ENDPOINTS.FOLDER_UPLOAD_PART}`, {
        method: 'POST',
        body: formData,
    });

    if (!response.ok) {
        throw new Error(`Failed to upload part ${partNumber}`);
    }

    return response.json();
};

/**
 * 完成文件夹上传
 */
export const completeFolderUpload = async (fileCompletions) => {
    const response = await apiRequest(`${API_URL}${API_ENDPOINTS.FOLDER_COMPLETE}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(fileCompletions),
    });

    if (!response.ok) {
        throw new Error('Failed to complete folder upload');
    }

    return response.json();
};

/**
 * 取消文件夹上传
 */
export const abortFolderUpload = async (activeUploads, userId, parentId) => {
    const formData = new FormData();

    activeUploads.forEach((upload) => {
        formData.append('fileIds', upload.fileId);
        formData.append('uploadIds', upload.uploadId);
    });

    formData.append('userId', userId);
    formData.append('parentFolderId', parentId);

    const response = await apiRequest(`${API_URL}${API_ENDPOINTS.FOLDER_ABORT}`, {
        method: 'POST',
        body: formData,
    });

    if (!response.ok) {
        throw new Error('Failed to abort folder upload');
    }

    return response.json();
};