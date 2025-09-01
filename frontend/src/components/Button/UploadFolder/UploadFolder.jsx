import React, {useState, useEffect, useCallback} from "react";
import apiRequest from "../../../utils/api";

function UploadFolder({ onFileUploadSuccess, userId, parentId }) {
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [activeUploads, setActiveUploads] = useState([]);
    const [totalFiles, setTotalFiles] = useState(0);
    const [uploadedFiles, setUploadedFiles] = useState(0);
    const [failedFiles, setFailedFiles] = useState([]);
    const [retryQueue, setRetryQueue] = useState([]);
    const cancelRef = React.useRef(false);

    const completeUpload = useCallback(  async (fileId, uploadId, partETags) => {
        console.log("Calling completeUpload with: ", partETags);
        if (cancelRef.current) return;

        const payload = {
            fileId,
            uploadId,
            partETags: partETags.map((part) => ({
                partNumber: part.partNumber,
                eTag: part.eTag,
            })),
        };

        try {
            const response = await apiRequest(
                `${process.env.REACT_APP_API_URL}/resumable/complete`,
                {
                    method: "POST",
                    body: JSON.stringify(payload),
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${localStorage.getItem("token")}`,
                    },
                }
            );

            if (!response.ok) {
                throw new Error(`Failed to complete upload for file with ID ${fileId}`);
            }

            const completeResult = await response.json();
            console.log(`File with ID ${fileId} completed. Result:`, completeResult);

            // 更新上传状态并移除已完成的文件
            const updatedUploads = activeUploads.filter(upload => upload.fileId !== fileId);
            setActiveUploads(updatedUploads);

            // 更新已上传文件计数
            setUploadedFiles(prev => prev + 1);

            return completeResult;
        } catch (error) {
            console.error(`Error completing upload for file with ID ${fileId}:`, error);
            throw error;
        }
    },[activeUploads]);

    const uploadFileParts = useCallback( async (fileInfo, file) => {
        // Skip if already cancelled
        if (cancelRef.current) return;

        const CHUNK_SIZE = 5 * 1024 * 1024;
        const fileId = fileInfo.fileId;
        const uploadId = fileInfo.uploadId;
        const totalChunks = Math.ceil(file.size / CHUNK_SIZE);

        try {
            // Instead of creating all promises at once, process sequentially to allow cancellation
            const formattedPartETags = [];

            for (let partNumber = 1; partNumber <= totalChunks; partNumber++) {
                // Check for cancellation before each part
                if (cancelRef.current) {
                    console.log(`Cancelled upload for ${file.name}`);
                    return;
                }

                const start = (partNumber - 1) * CHUNK_SIZE;
                const end = Math.min(start + CHUNK_SIZE, file.size);
                const chunk = file.slice(start, end);

                const chunkFormData = new FormData();
                chunkFormData.append('fileId', fileId);
                chunkFormData.append('uploadId', uploadId);
                chunkFormData.append('partNumber', partNumber);
                chunkFormData.append('file', chunk);

                try {
                    const response = await apiRequest(
                        `${process.env.REACT_APP_API_URL}/folders-upload-part`,
                        {
                            method: 'POST',
                            body: chunkFormData
                        }
                    );

                    // Check for cancellation after each part
                    if (cancelRef.current) {
                        console.log(`Cancelled upload for ${file.name} after part ${partNumber}`);
                        return;
                    }

                    if (!response.ok) {
                        throw new Error(`Failed to upload part ${partNumber}, status: ${response.status}`);
                    }

                    const partETag = await response.json();
                    const formattedETag = {
                        partNumber: partNumber,
                        eTag: partETag.eTag || partETag.etag || partETag
                    };

                    formattedPartETags.push(formattedETag);

                    const updatedUploads = [...activeUploads];
                    const uploadIndex = updatedUploads.findIndex(u => u.fileId === fileId);
                    if (uploadIndex !== -1) {
                        updatedUploads[uploadIndex].partETags.push(formattedETag);
                        updatedUploads[uploadIndex].progress = partNumber / totalChunks;
                        setActiveUploads(updatedUploads);
                    }
                } catch (error) {
                    console.error(`Error uploading part ${partNumber} for ${file.name}:`, error);
                    throw error;
                }
            }

            // Only complete if not cancelled
            if (!cancelRef.current) {
                await completeUpload(fileId, uploadId, formattedPartETags);
            }
        } catch (error) {
            console.error(`Error uploading file parts for ${file.name}:`, error);

            // 只有在文件未完成的情况下才加入重试队列
            const upload = activeUploads.find(u => u.fileId === fileId);
            if (upload && upload.status !== 'completed') {
                setFailedFiles(prev => [...prev, {
                    fileName: file.name,
                    error: error.message
                }]);
                setRetryQueue(prev => [...prev, {
                    fileInfo,
                    file
                }]);
            }
        }
    },[activeUploads,completeUpload]);

    // retry
    useEffect(() => {
        if (retryQueue.length > 0 && !isUploading) {
            const retryFile = retryQueue[0];
            const newQueue = [...retryQueue];
            newQueue.shift();
            setRetryQueue(newQueue);

            // 重新开始上传这个文件
            uploadFileParts(retryFile.fileInfo, retryFile.file);
        }
    }, [retryQueue, isUploading,uploadFileParts]);

    const completeAllUploads =useCallback( async () => {
        try {
            // 过滤出还没有完成的上传
            const pendingUploads = activeUploads.filter(
                upload => upload.status !== 'completed'
            );

            if (pendingUploads.length === 0) {
                console.log('All uploads already completed');
                setIsUploading(false);
                if (onFileUploadSuccess) {
                    onFileUploadSuccess();
                }
                return;
            }

            const fileCompletions = pendingUploads.map(upload => ({
                fileId: upload.fileId,
                uploadId: upload.uploadId,
                partETags: upload.partETags || []
            }));

            console.log('Completing all uploads with data:', fileCompletions);

            const response = await apiRequest(
                `${process.env.REACT_APP_API_URL}/folders-complete-upload`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(fileCompletions)
                }
            );

            if (!response.ok) {
                throw new Error('Failed to complete uploads');
            }

            const result = await response.json();
            console.log('Complete uploads response:', result);

            alert('Folder uploaded successfully');
            setIsUploading(false);
            setActiveUploads([]);

            if (onFileUploadSuccess) {
                onFileUploadSuccess();
            }
        } catch (error) {
            console.error('Error completing uploads:', error);
            alert('Failed to complete some uploads');
            setIsUploading(false);
        }
    },[activeUploads,onFileUploadSuccess]);

    const checkUploadStatus = useCallback( async () => {
        if (activeUploads.length === 0) return;

        try {
            const fileIds = activeUploads.map(upload => upload.fileId);
            const response = await apiRequest(
                `${process.env.REACT_APP_API_URL}/folders-upload-status?fileIds=${fileIds.join(',')}`
            );

            if (!response.ok) {
                throw new Error('Failed to get upload status');
            }

            const statusData = await response.json();
            const files = statusData.files || [];

            let totalProgress = 0;
            let completedCount = 0;
            const updatedUploads = [...activeUploads];

            files.forEach(fileStatus => {
                const uploadIndex = updatedUploads.findIndex(u => u.fileId === fileStatus.fileId);
                if (uploadIndex !== -1) {
                    updatedUploads[uploadIndex].progress = fileStatus.progress || 0;
                    updatedUploads[uploadIndex].status = fileStatus.status || 'pending'; // 确保状态同步
                    if (fileStatus.status === 'completed') {
                        completedCount++;
                    }
                    totalProgress += fileStatus.progress || 0;
                }
            });

            // 更新总体进度
            const overallProgress = files.length > 0 ? Math.round((totalProgress / files.length) * 100) : 0;
            setUploadProgress(overallProgress);
            setUploadedFiles(completedCount);
            setActiveUploads(updatedUploads);

            // 如果所有文件都上传完成，结束上传过程
            if (completedCount === totalFiles && totalFiles > 0) {
                await completeAllUploads();
            }
        } catch (error) {
            console.error('Error checking upload status:', error);
        }
    },[activeUploads,completeAllUploads,totalFiles]);

    // monitor progress
    useEffect(() => {
        if (activeUploads.length > 0) {
            const interval = setInterval(() => {
                checkUploadStatus();
            }, 2000);

            return () => clearInterval(interval);
        }
    }, [activeUploads,checkUploadStatus]);



    // start uploading
    const handleUploadFolder = async (e) => {
        e.preventDefault();
        const files = e.target.files;
        if (!files || files.length === 0) return;
        // 计算总大小和检查是否都是小文件
        const fileArray = Array.from(files);
        const totalSize = fileArray.reduce((sum, file) => sum + file.size, 0);
        const areAllSmallFiles = fileArray.every(file => file.size < 5 * 1024 * 1024); // 5MB

        if (areAllSmallFiles && totalSize < 50 * 1024 * 1024) { // 如果都是小文件且总大小<50MB
            // 调用简化版的API进行直接上传
            await uploadFolderSimple(e);
        } else {
            // 使用现有的分块上传机制
            await uploadFolderWithChunks(e);
        }
    }
//small folder
    const uploadFolderSimple = async (e) => {
        const files = e.target.files;
        if (!files || files.length === 0) return;

        const formData = new FormData();

        try {
            setIsUploading(true);
            setUploadProgress(0);
            setTotalFiles(files.length);

            Array.from(files).forEach((file, index) => {
                formData.append('files', file);
                const relativePath = file.webkitRelativePath;
                formData.append('paths', relativePath);
            });

            formData.append('folderId', parentId);
            formData.append('userId', userId);

            // 使用XMLHttpRequest来代替fetch以跟踪上传进度
            const xhr = new XMLHttpRequest();

            // 创建一个Promise来包装XMLHttpRequest
            const uploadPromise = new Promise((resolve, reject) => {
                xhr.open('POST', `${process.env.REACT_APP_API_URL}/uploadFolder`);

                // 设置凭证包含
                xhr.setRequestHeader("Authorization", `Bearer ${localStorage.getItem("token")}`);

                // 监听上传进度
                xhr.upload.addEventListener('progress', (event) => {
                    if (event.lengthComputable) {
                        const progress = Math.round((event.loaded / event.total) * 100);
                        setUploadProgress(progress);
                    }
                });

                // 处理完成
                xhr.addEventListener('load', () => {
                    if (xhr.status >= 200 && xhr.status < 300) {
                        resolve(xhr.response);
                    } else {
                        reject(new Error(`HTTP error ${xhr.status}: ${xhr.statusText}`));
                    }
                });

                // 处理错误
                xhr.addEventListener('error', () => {
                    reject(new Error('Network error occurred'));
                });

                // 处理中止
                xhr.addEventListener('abort', () => {
                    reject(new Error('Upload aborted'));
                });

                // 设置响应类型
                xhr.responseType = 'json';

                // 发送请求
                xhr.send(formData);
            });

            const response = await uploadPromise;

            alert('Folder uploaded successfully');
            console.log(response);

            if (onFileUploadSuccess) {
                onFileUploadSuccess();
            }
        } catch (error) {
            console.error('Error uploading folder:', error);
            alert('Folder failed to upload');
        } finally {
            setIsUploading(false);
        }
    };
//large folder
    const uploadFolderWithChunks = async (e) => {
        try {
            setIsUploading(true);
            setUploadProgress(0);
            setTotalFiles(e.target.files.length);
            setUploadedFiles(0);
            setFailedFiles([]);
            setActiveUploads([]);

            // 准备上传文件和路径数据
            const formData = new FormData();
            const fileArray = Array.from(e.target.files);
            fileArray.forEach((file, index) => {
                formData.append('files', file);
                const relativePath = file.webkitRelativePath;
                formData.append('relativePaths', relativePath);
            });


            formData.append('parentFolderId', parentId);
            formData.append('userId', userId);

            // 初始化文件夹上传
            const response = await apiRequest(
                `${process.env.REACT_APP_API_URL}/folders-initiate-upload`,
                {
                    method: 'POST',
                    body: formData
                }
            );

            if (!response.ok) {
                throw new Error('Failed to initiate folder upload');
            }

            const uploadInfo = await response.json();
            const fileUploads = uploadInfo.fileUploads || [];

            console.log('所有相对路径:');
            fileUploads.forEach((fileInfo, index) => {
                console.log(`文件 ${index + 1}: ${fileInfo.relativePath}`);
            });

            console.log('Initiated upload for files:', fileUploads);

            // 创建上传信息数组
            const uploads = [];

            // 设置每个文件的上传信息
            fileUploads.forEach(fileInfo => {
                const relativePath = fileInfo.relativePath;
                const file = fileArray.find(f => f.webkitRelativePath === relativePath);

                if (file) {
                    // 添加到活跃上传列表
                    uploads.push({
                        fileId: fileInfo.fileId,
                        uploadId: fileInfo.uploadId,
                        fileName: file.name,
                        relativePath: relativePath,
                        progress: 0,
                        partETags: [],
                        status: 'pending'
                    });
                }
            });

            // 先更新状态，让UI可以立即反映
            setActiveUploads(uploads);

            // 开始上传每个文件
            for (let i = 0; i < fileUploads.length; i++) {
                // Check if cancelled before starting each file
                if (cancelRef.current) {
                    console.log('Upload cancelled, stopping processing');
                    break;
                }

                const fileInfo = fileUploads[i];
                const relativePath = fileInfo.relativePath;
                const file = fileArray.find(f => f.webkitRelativePath === relativePath);

                if (file) {
                    await uploadFileParts(fileInfo, file);
                }
            }

        } catch (error) {
            console.error('Error starting folder upload:', error);
            alert('Failed to start folder upload');
            setIsUploading(false);
        }
    };





    const cancelUpload = async () => {
        if (!isUploading || activeUploads.length === 0) return;
        // Set the cancellation flag
        cancelRef.current = true;

        try {
            // 创建FormData对象
            const formData = new FormData();

            // 添加每个文件的取消信息
            activeUploads.forEach(upload => {
                formData.append("fileIds", upload.fileId);
                formData.append("uploadIds", upload.uploadId);

            });
            formData.append("userId", userId);
            formData.append("parentFolderId",parentId);

            const response = await apiRequest(
                `${process.env.REACT_APP_API_URL}/folders-abort-upload`,
                {
                    method: 'POST',
                    body: formData
                    // FormData会自动设置正确的Content-Type，不需要手动设置headers
                }
            );
            if (response.ok) {
                setIsUploading(false);
                setActiveUploads([]);

                setRetryQueue([]);
                setFailedFiles([]);
                alert('Upload cancelled');
            }
        } catch (error) {
            console.error('Error cancelling upload:', error);
        }
    };

    return (
        <>
            <label>
                Upload Folder ( &lt;=100MB)
                <input
                    type="file"
                    webkitdirectory="true"
                    className="uploadFolder"
                    onChange={handleUploadFolder}
                    disabled={isUploading}
                    style={{ display: 'none' }}
                />
            </label>

            {/* progress bar */}
            {isUploading && (
                <div style={{
                    position: 'fixed',
                    bottom: '20px',
                    left: '20px',
                    width: '300px',
                    zIndex: 9999,
                    backgroundColor: '#fff',
                    padding: '10px',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                    borderRadius: '4px'
                }}>
                    <div className="progress-container">
                        <div style={{
                            width: '100%',
                            backgroundColor: '#e0e0e0',
                            borderRadius: '4px',
                            height: '8px',
                            overflow: 'hidden'
                        }}>
                            <div style={{
                                width: `${uploadProgress}%`,
                                backgroundColor: '#4caf50',
                                height: '100%',
                                borderRadius: '4px',
                                transition: 'width 0.3s ease'
                            }}></div>
                        </div>
                        <div style={{
                            textAlign: 'center',
                            marginTop: '4px',
                            fontSize: '12px'
                        }}>
                            Uploading: {uploadProgress}% ({uploadedFiles}/{totalFiles} files)
                        </div>
                        <div style={{
                            textAlign: 'center',
                            marginTop: '8px'
                        }}>
                            <button
                                onClick={cancelUpload}
                                style={{
                                    padding: '4px 8px',
                                    fontSize: '12px',
                                    backgroundColor: '#f44336',
                                    color: 'white',
                                    border: 'none',
                                    borderRadius: '4px',
                                    cursor: 'pointer'
                                }}
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* fail files list */}
            {failedFiles.length > 0 && (
                <div style={{
                    position: 'fixed',
                    bottom: '20px',
                    right: '20px',
                    width: '300px',
                    zIndex: 9999,
                    backgroundColor: '#fff',
                    padding: '10px',
                    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                    borderRadius: '4px'
                }}>
                    <div style={{ fontWeight: 'bold', marginBottom: '5px' }}>
                        Failed uploads (retrying):
                    </div>
                    <ul style={{ margin: 0, padding: '0 0 0 20px', maxHeight: '100px', overflow: 'auto' }}>
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