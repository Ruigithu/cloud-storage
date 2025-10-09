export const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

export const getFileIcon = (fileType) => {
    if (fileType.startsWith("image/")) return "fa-regular fa-image";
    if (fileType.startsWith("video/")) return "fa-regular fa-file-video";
    if (fileType.startsWith("audio/")) return "fa-regular fa-file-audio";
    if (fileType === "application/pdf") return "fa-regular fa-file-pdf";
    if (fileType.includes("word")) return "fa-regular fa-file-word";
    if (fileType.includes("excel")) return "fa-regular fa-file-excel";
    if (fileType.includes("powerpoint")) return "fa-regular fa-file-powerpoint";
    return "fa-regular fa-file";
};

export const FILE_ICON_COLORS = {
    "fa-regular fa-image": "#007cdb",
    "fa-regular fa-file-video": "#ff4500",
    "fa-regular fa-file-audio": "#32cd32",
    "fa-regular fa-file-pdf": "#ff0000",
    "fa-regular fa-file-word": "#2b579a",
    "fa-regular fa-file-excel": "#217346",
    "fa-regular fa-file-powerpoint": "#d24726",
    "fa-regular fa-file": "#808080",
};