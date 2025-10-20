// 上传配置常量
export const UPLOAD_CONFIG = {
    CHUNK_SIZE: 5 * 1024 * 1024, // 5MB
    MAX_FILE_SIZE: 50 * 1024 * 1024, // 50MB
    MAX_FOLDER_SIZE: 100 * 1024 * 1024, // 100MB
    MAX_RETRIES: 3,
};

// API 端点
export const API_ENDPOINTS = {
    RESUMABLE_INIT: '/resumable/init',
    RESUMABLE_PART: '/resumable/part',
    RESUMABLE_PARTS: '/resumable/parts',
    RESUMABLE_COMPLETE: '/resumable/complete',
    RESUMABLE_ABORT: '/resumable/abort',
    FOLDER_INIT: '/folders-init-upload',
    FOLDER_PART: '/folders-upload-part',
    FOLDER_COMPLETE: '/folders-complete-upload',
    FOLDER_ABORT: '/folders-abort-upload',
};

// 上传状态
export const UPLOAD_STATUS = {
    IDLE: 'idle',
    UPLOADING: 'uploading',
    PAUSED: 'paused',
    COMPLETED: 'completed',
    FAILED: 'failed',
    CANCELLED: 'cancelled',
};