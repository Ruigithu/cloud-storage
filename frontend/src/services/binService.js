import apiRequest from "../utils/apiRequest";
const API_URL = process.env.REACT_APP_API_URL;

/**
 * 获取已删除的文件列表
 * @param {string} folderId - 文件夹 ID
 * @param {string} ownerId - 用户 ID
 * @returns {Promise<Array>} 已删除的文件列表
 */
export const getDeletedFiles = async (folderId, ownerId) => {
    const response = await apiRequest(
        `${API_URL}/getAllDeletedFiles?folderId=${folderId}&ownerId=${ownerId}`,
        {
            method: 'GET',
            headers: { "Content-Type": "application/json" }
        }
    );

    if (!response.ok) {
        throw new Error('Failed to fetch deleted files');
    }

    const data = await response.json();
    return Array.isArray(data) ? data : [];
};

/**
 * 获取已删除的文件夹列表
 * @param {string} parentId - 父文件夹 ID
 * @param {string} userId - 用户 ID
 * @returns {Promise<Array>} 已删除的文件夹列表
 */
export const getDeletedFolders = async (parentId, userId) => {
    const response = await apiRequest(
        `${API_URL}/getAllDeletedFolders?parentId=${parentId}&userId=${userId}`,
        {
            method: 'GET',
            headers: { "Content-Type": "application/json" }
        }
    );

    if (!response.ok) {
        throw new Error('Failed to fetch deleted folders');
    }

    const data = await response.json();
    return Array.isArray(data) ? data : [];
};

/**
 * 同时获取已删除的文件和文件夹
 * @param {string} folderId - 文件夹 ID
 * @param {string} userId - 用户 ID
 * @returns {Promise<Object>} { files: Array, folders: Array }
 */
export const getDeletedFilesAndFolders = async (folderId, userId) => {
    if (!userId) {
        throw new Error('User ID is required');
    }

    const [files, folders] = await Promise.all([
        getDeletedFiles(folderId, userId),
        getDeletedFolders(folderId, userId)
    ]);

    // remove duplicates--should use backend to deal with this situation
    const seenIds = new Map();

    const uniqueFolders = folders.filter(folder => {
        if (seenIds.has(folder.id)) {
            console.warn(`Duplicate folder ID found: ${folder.id}`);
            return false;
        }
        seenIds.set(folder.id, true);
        return true;
    });

    const uniqueFiles = files.filter(file => {
        if (seenIds.has(file.id)) {
            console.warn(`Duplicate file ID found: ${file.id}`);
            return false;
        }
        seenIds.set(file.id, true);
        return true;
    });

    return {
        files: uniqueFiles,
        folders: uniqueFolders
    };
};