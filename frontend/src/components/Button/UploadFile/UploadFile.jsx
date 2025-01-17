import React, { useState } from "react";

function UploadFile({ onFileUploadSuccess }) {
    const [isUploading, setIsUploading] = useState(false);

    const handleUploadFile = async (e) => {
        e.preventDefault();
        const file = e.target.files[0];
        if (!file) return;

        try {
            setIsUploading(true);
            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch("http://localhost:8080/upload", {
                method: 'POST',
                body: formData,
                credentials: 'include',
            });

            if (response.ok) {
                alert('File uploaded successfully');
                if (onFileUploadSuccess) {
                    onFileUploadSuccess();  // 调用父组件的回调函数
                }
            } else {
                console.error('Upload failed');
            }
        } catch (error) {
            console.error('Error uploading file:', error);
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <label>
            Upload File
            <input
                type="file"
                className="uploadFile"
                onChange={handleUploadFile}
                disabled={isUploading}
                style={{ display: 'none' }}
            />
        </label>
    );
}

export default UploadFile;