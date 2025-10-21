import apiRequest from "../utils/apiRequest";


/**
 * 加载文档内容
 * @param {string} documentId - 文档ID
 * @param {string} userId - 用户ID
 * @returns {Promise<Object>} 文档数据和元信息
 */
export const loadDocument = async (documentId, userId) => {
    const response = await apiRequest(
        `${process.env.REACT_APP_API_URL}/getFileByUserIdAndFileId?ownerId=${userId}&fileId=${documentId}`,
        {
            method: 'GET',
            headers: {
                'Accept': 'application/json, application/octet-stream',
                'Content-Type': 'application/json',
            },
            credentials: 'include'
        }
    );

    if (!response.ok) {
        throw new Error(`Server responded with status ${response.status}`);
    }

    const contentType = response.headers.get('content-type');
    const contentDisposition = response.headers.get('content-disposition');

    let fileName = 'document';
    if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename="(.+)"/);
        if (filenameMatch) {
            fileName = filenameMatch[1];
        }
    }

    return {
        response,
        contentType,
        fileName,
        isJson: contentType && contentType.includes('application/json')
    };
};

/**
 * 转换 Word 文档为 HTML
 * @param {string} documentId - 文档ID
 * @param {string} userId - 用户ID
 * @returns {Promise<string>} HTML 内容
 */
export const convertDocxToHtml = async (documentId, userId) => {
    const response = await apiRequest(
        `${process.env.REACT_APP_API_URL}/convertDocxToHtmlMammoth?ownerId=${userId}&fileId=${documentId}`,
        {
            method: 'GET',
            credentials: 'include'
        }
    );

    if (!response.ok) {
        throw new Error(`Failed to convert document with Mammoth: ${response.status}`);
    }

    const data = await response.json();
    return data.htmlContent;
};

/**
 * 转换 HTML 为 Word 文档
 * @param {string} htmlContent - HTML 内容
 * @param {string} fileName - 文件名
 * @param {string} userId - 用户ID
 * @param {string} documentId - 文档ID
 * @returns {Promise<Object>} 保存结果
 */
export const convertHtmlToDocx = async (htmlContent, fileName, userId, documentId) => {
    const response = await apiRequest(
        `${process.env.REACT_APP_API_URL}/convertHtmlToDocx`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({
                htmlContent,
                fileName: fileName.endsWith('.docx') || fileName.endsWith('.doc')
                    ? fileName
                    : `${fileName}.docx`,
                ownerId: userId,
                fileId: documentId
            })
        }
    );

    if (!response.ok) {
        throw new Error(`Failed to convert to DOCX: ${response.status}`);
    }

    return await response.json();
};

/**
 * 上传文件到服务器
 * @param {File} file - 文件对象
 * @param {string} userId - 用户ID
 * @param {string} documentId - 文档ID
 * @returns {Promise<Object>} 上传结果
 */
export const uploadFile = async (file, userId, documentId) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('ownerId', userId);
    formData.append('fileId', documentId);

    const response = await apiRequest(
        `${process.env.REACT_APP_API_URL}/uploadNewFile`,
        {
            method: 'POST',
            credentials: 'include',
            body: formData
        }
    );

    if (!response.ok) {
        throw new Error(`Failed to save: ${response.status}`);
    }

    return await response.json();
};

/**
 * 下载文件
 * @param {string} documentId - 文档ID
 * @param {string} userId - 用户ID
 * @param {string} fileName - 文件名
 */
export const downloadFile = async (documentId, userId, fileName) => {
    const response = await apiRequest(
        `${process.env.REACT_APP_API_URL}/getFileByUserIdAndFileId?ownerId=${userId}&fileId=${documentId}`,
        {
            method: 'GET',
            headers: {
                'Accept': 'application/octet-stream',
            },
            credentials: 'include'
        }
    );

    if (!response.ok) {
        throw new Error(`Failed to download: ${response.status}`);
    }

    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName || `file_${documentId}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
};

// ==================== 图片处理 ====================

/**
 * 将 base64 图片上传到服务器并返回 URL
 * @param {string} base64Image - base64 格式的图片
 * @param {string} userId - 用户ID
 * @param {string} documentId - 文档ID
 * @returns {Promise<string>} 上传后的图片 URL
 */
export const uploadBase64Image = async (base64Image, userId, documentId) => {
    const imageBlob = await fetch(base64Image).then(res => res.blob());
    const imageFile = new File([imageBlob], `image_${Date.now()}.png`, { type: imageBlob.type });

    const formData = new FormData();
    formData.append('file', imageFile);
    formData.append('ownerId', userId);
    formData.append('fileId', documentId);

    const response = await apiRequest(
        `${process.env.REACT_APP_API_URL}/uploadNewFile`,
        {
            method: 'POST',
            credentials: 'include',
            body: formData
        }
    );

    if (!response.ok) {
        throw new Error(`Failed to upload image: ${response.status}`);
    }

    const { url } = await response.json();
    return url;
};

/**
 * 处理 Quill Delta 中的 base64 图片，将其转换为服务器 URL
 * @param {Object} delta - Quill Delta 对象
 * @param {string} userId - 用户ID
 * @param {string} documentId - 文档ID
 * @returns {Promise<Object>} 处理后的 Delta 对象
 */
export const processImagesInDelta = async (delta, userId, documentId) => {
    const updatedOps = await Promise.all(
        delta.ops.map(async (op) => {
            if (op.insert && op.insert.image && op.insert.image.startsWith('data:image')) {
                const url = await uploadBase64Image(op.insert.image, userId, documentId);
                return { insert: { image: url } };
            }
            return op;
        })
    );

    return { ops: updatedOps };
};

/**
 * 检查 Delta 中是否包含 base64 图片
 * @param {Object} delta - Quill Delta 对象
 * @returns {boolean} 是否包含 base64 图片
 */
export const hasBase64Images = (delta) => {
    return delta.ops.some(
        op => op.insert && op.insert.image && op.insert.image.startsWith('data:image')
    );
};