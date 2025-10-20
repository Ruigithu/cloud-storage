
import React from 'react';
import { useFileUpload } from '../../../hooks/useFileUpload';
import UploadProgress from '../../Progress/UploadProgress';

function UploadFile({ onFileUploadSuccess, ownerId, folderId }) {
    const {
        isUploading,
        uploadProgress,
        isPaused,
        startUpload,
        pauseUpload,
        resumeUpload,
        cancelUpload,
    } = useFileUpload({
        ownerId,
        folderId,
        onSuccess: onFileUploadSuccess,
    });

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            startUpload(file);
        }
    };

    return (
        <>
            <label className="upload-button">
                Upload File (≤50MB)
                <input
                    type="file"
                    className="uploadFile"
                    onChange={handleFileChange}
                    disabled={isUploading && !isPaused}
                    style={{ display: 'none' }}
                />
            </label>

            {isUploading && (
                <UploadProgress
                    progress={uploadProgress}
                    isPaused={isPaused}
                    onPause={pauseUpload}
                    onResume={resumeUpload}
                    onCancel={cancelUpload}
                    showPauseButton={true}
                />
            )}
        </>
    );
}

export default UploadFile;