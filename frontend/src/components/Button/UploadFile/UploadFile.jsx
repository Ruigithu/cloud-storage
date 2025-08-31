import React, { useState, useRef, useCallback } from "react";
import apiRequest from "../../../utils/api";

function UploadFile({ onFileUploadSuccess, ownerId, folderId }) {
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [isPaused, setIsPaused] = useState(false);
    const [uploadId, setUploadId] = useState(null);
    const [fileId, setFileId] = useState(null);
    const [uploadedParts, setUploadedParts] = useState([]);

    const fileRef = useRef(null);
    const xhrRef = useRef(null);
    // 使用 ref 来跟踪暂停状态，确保实时更新
    const isPausedRef = useRef(false);

    const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB 分片大小

    const handleUploadFile = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        fileRef.current = file;
        setIsUploading(true);
        setUploadProgress(0);

        // 同步更新状态和ref
        setIsPaused(false);
        isPausedRef.current = false;

        const formData = new FormData();
        formData.append('ownerId', ownerId);
        formData.append('folderId', folderId);
        formData.append('fileName', file.name);
        formData.append('mimeType', file.type);
        formData.append('fileSize', file.size.toString());

        try {
            // 初始化分片上传
            const initResponse = await apiRequest(`${process.env.REACT_APP_API_URL}/resumable/init`, {
                method: "POST",
                headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
                body: formData,
            });

            if (!initResponse.ok) throw new Error("Failed to initialize upload");
            const initData = await initResponse.json();
            console.log(initData);
            setFileId(initData.fileId);
            setUploadId(initData.uploadId);

            const partsResponse = await apiRequest(
                `${process.env.REACT_APP_API_URL}/resumable/parts?fileId=${initData.fileId}&uploadId=${initData.uploadId}`,
                {
                    method: "GET",
                    headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
                }
            );

            const uploadedParts = await partsResponse.json();
            setUploadedParts(uploadedParts.map((part) => ({
                partNumber: part.partNumber,
                eTag: part.eTag
            })));

            await uploadChunks(file, initData.uploadId, initData.fileId);
        } catch (error) {
            console.error("Error initializing upload:", error);
            setIsUploading(false);
        }
    };

    // 使用 useCallback 来确保函数引用的稳定性
    const uploadChunks = useCallback(async (file, uploadId, fileId, startPartNumber = 1) => {
        const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
        console.log(`Starting upload from part ${startPartNumber} of ${totalChunks}`);

        // 获取当前上传部分的副本
        const partETags = [...uploadedParts];

        try {
            for (let i = startPartNumber; i <= totalChunks; i++) {
                // 使用ref检查是否暂停，确保实时状态
                if (isPausedRef.current) {
                    console.log("Upload paused at part", i);
                    return; // 退出函数但不抛出错误
                }

                // 检查这部分是否已经上传
                const alreadyUploaded = partETags.some(part => part.partNumber === i);
                if (alreadyUploaded) {
                    console.log(`Part ${i} already uploaded, skipping`);
                    setUploadProgress(prev => Math.max(prev, (i / totalChunks) * 100));
                    continue;
                }

                console.log(`Uploading part ${i} of ${totalChunks}`);
                const start = (i - 1) * CHUNK_SIZE;
                const end = Math.min(start + CHUNK_SIZE, file.size);
                const chunk = file.slice(start, end);

                try {
                    const result = await uploadPart(chunk, fileId, uploadId, i);
                    console.log(`Part ${i} upload completed:`, result);

                    // 将此部分添加到我们的跟踪数组
                    partETags.push({
                        partNumber: result.partNumber,
                        eTag: result.eTag
                    });

                    // 更新全局状态
                    setUploadedParts(prev => [...prev.filter(p => p.partNumber !== i), {
                        partNumber: result.partNumber,
                        eTag: result.eTag
                    }]);

                    // 更新进度
                    setUploadProgress((i / totalChunks) * 100);
                } catch (error) {
                    if (error.aborted) {
                        console.log(`Part ${i} was aborted, will resume from here later`);
                        return; // 如果这是中止，则退出但不抛出错误
                    }
                    throw error; // 重新抛出其他错误
                }
            }

            // 所有部分上传完成，完成上传
            await completeUpload(fileId, uploadId, partETags);
        } catch (error) {
            console.error("Error in uploadChunks:", error);
            setIsUploading(false);
        }
    }, [uploadedParts, CHUNK_SIZE, uploadPart, completeUpload]);

    // 上传单个部分的函数
    const uploadPart = (chunk, fileId, uploadId, partNumber) => {
        return new Promise((resolve, reject) => {
            const formData = new FormData();
            formData.append("part", chunk);
            formData.append("fileId", fileId);
            formData.append("uploadId", uploadId);
            formData.append("partNumber", partNumber);

            const xhr = new XMLHttpRequest();
            xhrRef.current = xhr;

            xhr.upload.addEventListener("progress", (event) => {
                if (event.lengthComputable) {
                    const totalChunks = Math.ceil(fileRef.current.size / CHUNK_SIZE);
                    const partProgress = ((partNumber - 1) / totalChunks) * 100;
                    const chunkProgress = (event.loaded / event.total) * (100 / totalChunks);
                    const totalProgress = partProgress + chunkProgress;
                    setUploadProgress(Math.min(totalProgress, 100));
                }
            });

            xhr.addEventListener("load", () => {
                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        const response = JSON.parse(xhr.responseText);
                        console.log(`Part ${partNumber} uploaded successfully:`, response);

                        const extractedETag = response.etag || response.eTag;

                        if (!extractedETag) {
                            console.error("Cannot find eTag in response:", response);
                            reject(new Error("Missing eTag in server response"));
                            return;
                        }

                        resolve({
                            partNumber: partNumber,
                            eTag: extractedETag
                        });
                    } catch (error) {
                        console.error("Error parsing response:", error, xhr.responseText);
                        reject(error);
                    }
                } else {
                    reject(new Error(`Chunk upload failed with status ${xhr.status}`));
                }
            });

            xhr.addEventListener("error", () => {
                reject(new Error("Error uploading chunk"));
            });

            xhr.open("POST", `${process.env.REACT_APP_API_URL}/resumable/part`, true);
            xhr.setRequestHeader("Authorization", `Bearer ${localStorage.getItem("token")}`);
            xhr.send(formData);
        });
    };

    const completeUpload = async (fileId, uploadId, partETags) => {
        console.log("Calling completeUpload with: ", partETags);

        const payload = {
            fileId,
            uploadId,
            partETags: partETags.map((part) => ({
                partNumber: part.partNumber,
                eTag: part.eTag,
            })),
        };
        console.log("Complete upload payload:", JSON.stringify(payload, null, 2));
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

            if (response.ok) {
                alert("File uploaded successfully");
                if (onFileUploadSuccess) onFileUploadSuccess();
            } else {
                console.error("Failed to complete upload");
            }
        } catch (error) {
            console.error("Error completing upload:", error);
        } finally {
            setIsUploading(false);
            setUploadId(null);
            setFileId(null);
            setUploadedParts([]);
            isPausedRef.current = false;
        }
    };

    const pauseUpload = () => {
        // 同步更新状态和ref
        setIsPaused(true);
        isPausedRef.current = true;

        if (xhrRef.current) {
            xhrRef.current.abort();
        }
    };

    const resumeUpload = async () => {
        if (!fileRef.current || !uploadId || !fileId) return;

        // 同步更新状态和ref
        setIsPaused(false);
        isPausedRef.current = false;

        try {
            // 获取服务器上的最新部分信息
            const partsResponse = await apiRequest(
                `${process.env.REACT_APP_API_URL}/resumable/parts?fileId=${fileId}&uploadId=${uploadId}`,
                {
                    method: "GET",
                    headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
                }
            );

            if (!partsResponse.ok) {
                throw new Error("Failed to get uploaded parts");
            }

            const parts = await partsResponse.json();
            console.log("Retrieved parts:", parts);

            // 更新状态中的已上传部分
            const formattedParts = parts.map(part => ({
                partNumber: part.partNumber,
                eTag: part.etag || part.eTag // 兼容两种可能的属性名
            }));

            setUploadedParts(formattedParts);

            // 找到要上传的下一个部分编号
            const uploadedPartNumbers = formattedParts.map(p => p.partNumber);
            let nextPartNumber = 1;

            if (uploadedPartNumbers.length > 0) {
                // 在序列中查找缺口或获取最大值之后的下一个部分
                const maxUploadedPart = Math.max(...uploadedPartNumbers);
                nextPartNumber = maxUploadedPart + 1;
            }

            console.log("Resuming from part:", nextPartNumber);

            // 从下一个部分恢复上传
            await uploadChunks(fileRef.current, uploadId, fileId, nextPartNumber);
        } catch (error) {
            console.error("Error resuming upload:", error);
            setIsUploading(false);
        }
    };

    const cancelUpload = async () => {
        if (!fileId || !uploadId) return;
        const formData = new FormData();
        formData.append("fileId",fileId);
        formData.append("uploadId",uploadId);
        formData.append("folderId",folderId);
        formData.append("userId",ownerId);

        try {
            await apiRequest(`${process.env.REACT_APP_API_URL}/resumable/abort`, {
                method: "POST",
                body:formData,
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${localStorage.getItem("token")}` },
            });
            alert("Upload cancelled");
        } catch (error) {
            console.error("Error cancelling upload:", error);
        } finally {
            setIsUploading(false);
            setUploadId(null);
            setFileId(null);
            setUploadedParts([]);
            setUploadProgress(0);
            isPausedRef.current = false;
        }
    };

    return (
        <>
            <label className="upload-button">
                Upload File (≤50MB)
                <input
                    type="file"
                    className="uploadFile"
                    onChange={handleUploadFile}
                    disabled={isUploading && !isPaused}
                    style={{ display: "none" }}
                />
            </label>
            {isUploading && (
                <div
                    style={{
                        position: "fixed",
                        bottom: "20px",
                        left: "20px",
                        width: "300px",
                        zIndex: 9999,
                        backgroundColor: "#fff",
                        padding: "10px",
                        boxShadow: "0 2px 4px rgba(0,0,0,0.1)",
                        borderRadius: "4px",
                    }}
                >
                    <div className="progress-container">
                        <div
                            style={{
                                width: "100%",
                                backgroundColor: "#e0e0e0",
                                borderRadius: "4px",
                                height: "8px",
                                overflow: "hidden",
                            }}
                        >
                            <div
                                style={{
                                    width: `${uploadProgress}%`,
                                    backgroundColor: "#4caf50",
                                    height: "100%",
                                    borderRadius: "4px",
                                    transition: "width 0.3s ease",
                                }}
                            ></div>
                        </div>
                        <div
                            style={{
                                textAlign: "center",
                                marginTop: "4px",
                                fontSize: "12px",
                            }}
                        >
                            Uploading: {uploadProgress.toFixed(2)}%
                        </div>
                        <div style={{ display: "flex", justifyContent: "space-between", marginTop: "8px" }}>
                            <button onClick={isPaused ? resumeUpload : pauseUpload}>
                                {isPaused ? "Resume" : "Pause"}
                            </button>
                            <button onClick={cancelUpload}>Cancel</button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}

export default UploadFile;