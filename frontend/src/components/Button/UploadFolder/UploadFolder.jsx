import React, { useState } from "react";

function UploadFolder({ onFileUploadSuccess,userId,parentId }) {
    const [isUploading, setIsUploading] = useState(false);

    const handleUploadFolder = async (e) => {
        e.preventDefault();
        const files = e.target.files;
        if (!files) return;

        const formData = new FormData();

        try {
            setIsUploading(true);
            Array.from(files).forEach((file, index) => {
                formData.append('files', file);
                // 获取相对路径
                const relativePath = file.webkitRelativePath;
                formData.append('paths', relativePath);
            });

            formData.append('folderId', parentId);
            formData.append('userId',userId);

            const response = await fetch(`${process.env.REACT_APP_API_URL}/uploadFolder`, {
                method: 'POST',
                body: formData,
                credentials: 'include',
            });

            if (response.ok) {
                alert('Folder uploaded successfully');
                const data = await response.json();
                console.log(data);
                if (onFileUploadSuccess) {
                    onFileUploadSuccess();  // 调用父组件的回调函数
                }
            } else {
                console.error('Upload failed');
                alert('Folder failed to upload')
            }
        } catch (error) {
            console.error('Error uploading file:', error);
        } finally {
            setIsUploading(false);
        }
    };

    return (
        <label>
            Upload Folder
            <input
                type="file"
                webkitdirectory="true"
                className="uploadFolder"
                onChange={handleUploadFolder}
                disabled={isUploading}
                style={{ display: 'none' }}
            />
        </label>
    );
}

export default UploadFolder;