import apiRequest from "../utils/apiRequest";

/**
 * 检查分享链接的有效性并获取分享信息
 * @param {string} shareId - 分享ID
 * @param {string|null} userId - 用户ID（可选）
 * @returns {Promise<{status: number, data?: Object, needsAuth?: boolean}>}
 */
export const checkShareLink = async (shareId, userId) => {
    let url = `${process.env.REACT_APP_API_URL}/share/${shareId}`;

    if (userId && userId !== "null") {
        url += `?userId=${userId}`;
    }

    const response = await apiRequest(url, {
        credentials: 'include',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json',
        }
    });

    // 处理401未授权
    if (response.status === 401) {
        return { status: 401, needsAuth: true };
    }

    // 处理404不存在
    if (response.status === 404) {
        return { status: 404 };
    }

    if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    const data = await response.json();
    return { status: 200, data };
};

/**
 * 下载文件
 * @param {string} fileId - 文件ID
 * @returns {Promise<Blob>}
 */
export const downloadFile = async (fileId) => {
    const response = await apiRequest(
        `${process.env.REACT_APP_API_URL}/download?fileId=${fileId}`,
        {
            credentials: 'include',
            headers: {
                'Accept': 'application/json'
            }
        }
    );

    if (!response.ok) {
        throw new Error('Download failed');
    }

    return await response.blob();
};

/**
 * 触发浏览器下载文件
 * @param {Blob} blob - 文件数据
 * @param {string} fileName - 文件名
 */
export const triggerFileDownload = (blob, fileName) => {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName || 'download';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
};

/**
 * 保存分享的文件到用户账户
 * @param {string} shareId - 分享ID
 * @param {string} userId - 用户ID
 * @param {string} rootFolderId - 根文件夹ID
 * @returns {Promise<{newFileId: string}>}
 */
export const saveSharedFile = async (shareId, userId, rootFolderId) => {
    const response = await apiRequest(
        `${process.env.REACT_APP_API_URL}/saveShare/${shareId}?userId=${userId}&rootFolderId=${rootFolderId}`,
        {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Accept': 'application/json',
                'Content-Type': 'application/json'
            }
        }
    );

    if (!response.ok) {
        throw new Error('Failed to save shared file');
    }

    return await response.json();
};

/**
 * 处理READ类型的分享（下载文件）
 * @param {string} fileId - 文件ID
 * @param {string} fileName - 文件名
 */
export const handleReadShare = async (fileId, fileName) => {
    const blob = await downloadFile(fileId);
    triggerFileDownload(blob, fileName);
};

/**
 * 处理WRITE类型的分享（保存到账户）
 * @param {string} shareId - 分享ID
 * @param {string} userId - 用户ID
 * @param {string} rootFolderId - 根文件夹ID
 * @returns {Promise<string>} 新文件ID
 */
export const handleWriteShare = async (shareId, userId, rootFolderId) => {
    const { newFileId } = await saveSharedFile(shareId, userId, rootFolderId);
    return newFileId;
};