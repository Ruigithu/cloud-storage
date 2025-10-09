import apiRequest from "../utils/apiRequest";
const API_URL = process.env.REACT_APP_API_URL;


/**
 * 获取根目录的文件列表
 * @param {string} ownerId - 用户 ID
 * @returns {Promise<Array>} 文件列表
 */
export const getRootFiles = async (ownerId) => {
    const response = await apiRequest(
        `${API_URL}/getRootFiles?ownerId=${ownerId}`,
        {
            method: 'GET',
            headers: { "Content-Type": "application/json" }
        }
    );

    if (!response.ok) {
        throw new Error('Failed to fetch root files');
    }

    const data = await response.json();
    return Array.isArray(data) ? data : (data.files || []);
};

/**
 * 获取指定文件夹下的文件列表
 * @param {string} folderId - 文件夹 ID
 * @param {string} ownerId - 用户 ID
 * @returns {Promise<Array>} 文件列表
 */
export const getFilesByFolder = async (folderId, ownerId) => {
    const response = await apiRequest(
        `${API_URL}/getAllFiles?folderId=${folderId}&ownerId=${ownerId}`,
        {
            method: 'GET',
            headers: { "Content-Type": "application/json" }
        }
    );

    if (!response.ok) {
        throw new Error('Failed to fetch files');
    }

    const data = await response.json();
    return Array.isArray(data) ? data : (data.files || []);
};
/**
 * 获取根目录的文件夹列表
 * @param {string} userId - 用户 ID
 * @returns {Promise<Object>} { folders: Array, rootFolderId: string }
 */
export const getRootFolders = async (userId) => {
    const response = await apiRequest(
        `${API_URL}/getRootFolders?userId=${userId}`,
        {
            method: 'GET',
            headers: { "Content-Type": "application/json" }
        }
    );

    if (!response.ok) {
        throw new Error('Failed to fetch root folders');
    }

    return await response.json();
};

/**
 * 获取指定文件夹下的子文件夹列表
 * @param {string} parentId - 父文件夹 ID
 * @param {string} userId - 用户 ID
 * @returns {Promise<Array>} 文件夹列表
 */
export const getFoldersByParent = async (parentId, userId) => {
    const response = await apiRequest(
        `${API_URL}/getAllFolders?parentId=${parentId}&userId=${userId}`,
        {
            method: 'GET',
            headers: { "Content-Type": "application/json" }
        }
    );

    if (!response.ok) {
        throw new Error('Failed to fetch folders');
    }

    const data = await response.json();
    return data.folders || data;
};


/**
 * 同时获取文件和文件夹（推荐使用此方法）
 * @param {string|null} folderId - 文件夹 ID，null 表示根目录
 * @param {string} userId - 用户 ID
 * @returns {Promise<Object>} { files: Array, folders: Array, rootFolderId?: string }
 */
export const getFilesAndFolders = async (folderId, userId) => {
    if (!userId) {
        throw new Error('User ID is required');
    }

    const filePromise = folderId
        ? await getFilesByFolder(folderId, userId)
        : await getRootFiles(userId);

    const folderPromise = folderId
        ? await getFoldersByParent(folderId, userId)
        : await getRootFolders(userId);

    const [files, folderData] = await Promise.all([
        filePromise,
        folderPromise
    ]);

    const folders = Array.isArray(folderData)
        ? folderData
        : (folderData.folders || []);

    const result = { files, folders };

    if (!folderId && folderData.rootFolderId) {
        result.rootFolderId = folderData.rootFolderId;
    }

    return result;
};






