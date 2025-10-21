/**
 * Quill 编辑器工具栏配置
 */
export const TOOLBAR_OPTIONS = [
    ['bold', 'italic', 'underline', 'strike'],
    ['blockquote', 'code-block'],
    [{ 'header': 1 }, { 'header': 2 }],
    [{ 'list': 'ordered' }, { 'list': 'bullet' }],
    [{ 'script': 'sub' }, { 'script': 'super' }],
    [{ 'indent': '-1' }, { 'indent': '+1' }],
    [{ 'direction': 'rtl' }],
    [{ 'size': ['small', false, 'large', 'huge'] }],
    [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
    [{ 'color': [] }, { 'background': [] }],
    [{ 'font': [] }],
    [{ 'align': [] }],
    ['clean'],
    ['link', 'image']
];

/**
 * 创建 Quill 编辑器配置对象
 * @returns {Object} Quill 配置
 */
export const createQuillConfig = () => ({
    modules: {
        toolbar: TOOLBAR_OPTIONS,
        history: {
            delay: 2000,
            maxStack: 500,
            userOnly: true
        }
    },
    theme: 'snow',
    placeholder: 'Start editing the file...',
});

// ==================== 文件工具函数 ====================

/**
 * MIME 类型到文件扩展名的映射
 */
const MIME_TO_EXT = {
    'application/json': '.json',
    'text/plain': '.txt',
    'text/html': '.html',
    'image/png': '.png',
    'image/jpeg': '.jpg',
    'application/pdf': '.pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': '.docx',
    'application/msword': '.doc',
};

/**
 * 根据 MIME 类型获取文件扩展名
 * @param {string} mimeType - MIME 类型
 * @returns {string} 文件扩展名
 */
export const getFileExtension = (mimeType) => {
    return MIME_TO_EXT[mimeType] || '.txt';
};

/**
 * 检查是否为 Word 文档（仅 .docx）
 * @param {string} mimeType - MIME 类型
 * @returns {boolean} 是否为 .docx 文档
 */
export const isWordDocument = (mimeType) => {
    if (!mimeType) return false;
    return mimeType.includes('application/vnd.openxmlformats-officedocument.wordprocessingml.document');
};

/**
 * 检查是否为旧版 Word 文档（.doc）
 * @param {string} mimeType - MIME 类型
 * @returns {boolean} 是否为 .doc 文档
 */
export const isLegacyWordDocument = (mimeType) => {
    if (!mimeType) return false;
    return mimeType.includes('application/msword');
};

/**
 * 检查是否为图片文件
 * @param {string} mimeType - MIME 类型
 * @returns {boolean} 是否为图片
 */
export const isImageFile = (mimeType) => {
    if (!mimeType) return false;
    return mimeType.startsWith('image/');
};

/**
 * 检查是否为 JSON 文件
 * @param {string} mimeType - MIME 类型
 * @returns {boolean} 是否为 JSON
 */
export const isJsonFile = (mimeType) => {
    if (!mimeType) return false;
    return mimeType.includes('application/json');
};

/**
 * 格式化最后保存时间
 * @param {Date|null} lastSaved - 最后保存时间
 * @returns {string} 格式化后的时间字符串
 */
export const formatLastSaved = (lastSaved) => {
    if (!lastSaved) return 'Not saved yet';
    return `Last saved: ${lastSaved.toLocaleTimeString()}`;
};

/**
 * 创建文件对象
 * @param {Blob} blob - 文件数据
 * @param {string} fileName - 文件名
 * @param {string} mimeType - MIME 类型
 * @returns {File} 文件对象
 */
export const createFile = (blob, fileName, mimeType) => {
    return new File([blob], fileName, { type: mimeType });
};
