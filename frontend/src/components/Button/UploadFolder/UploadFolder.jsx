import React from 'react';
import { useFolderUpload } from '../../../hooks/useFolderUpload';
import UploadProgress from '../../Progress/UploadProgress';

function UploadFolder({ onFileUploadSuccess, userId, parentId }) {
    const {
        isUploading,
        uploadProgress,
        totalFiles,
        uploadedFiles,
        failedFiles,
        startUpload,
        cancelUpload,
    } = useFolderUpload({
        userId,
        parentId,
        onSuccess: onFileUploadSuccess,
    });

    /**
     * 文件夹选择处理
     */
    const handleFolderChange = (e) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            startUpload(files);
        }
    };

    return (
        <>
            {/* 上传按钮 */}
            <label>
                Upload Folder (≤100MB)
                <input
                    type="file"
                    webkitdirectory="true"
                    className="uploadFolder"
                    onChange={handleFolderChange}
                    disabled={isUploading}
                    style={{ display: 'none' }}
                />
            </label>

            {/* 上传进度 */}
            {isUploading && (
                <UploadProgress
                    progress={uploadProgress}
                    isPaused={false}
                    onCancel={cancelUpload}
                    additionalInfo={`(${uploadedFiles}/${totalFiles} files)`}
                    showPauseButton={false}
                />
            )}

            {/* 失败文件列表 */}
            {failedFiles.length > 0 && (
                <div
                    style={{
                        position: 'fixed',
                        bottom: '20px',
                        right: '20px',
                        width: '300px',
                        zIndex: 9999,
                        backgroundColor: '#fff',
                        padding: '10px',
                        boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                        borderRadius: '4px',
                    }}
                >
                    <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>
                        Failed uploads (retrying):
                    </div>
                    <ul
                        style={{
                            margin: 0,
                            padding: '0 0 0 20px',
                            maxHeight: '100px',
                            overflow: 'auto',
                        }}
                    >
                        {failedFiles.map((file, index) => (
                            <li key={index} style={{ fontSize: '12px' }}>
                                {file.fileName}
                            </li>
                        ))}
                    </ul>
                </div>
            )}
        </>
    );
}

export default UploadFolder;