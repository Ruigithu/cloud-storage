import React, { useState, useRef } from "react";
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

    const CHUNK_SIZE = 5 * 1024 * 1024; // 5MB 分片大小

    const handleUploadFile = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        fileRef.current = file;
        setIsUploading(true);
        setUploadProgress(0);
        setIsPaused(false);

        const formData =new FormData();
        formData.append('ownerId',ownerId);
        formData.append('folderId',folderId);
        formData.append('fileName', file.name);
        formData.append('mimeType', file.type);
        formData.append('fileSize', file.size.toString());
        try {
            // 初始化分片上传
            const initResponse = await apiRequest(`${process.env.REACT_APP_API_URL}/resumable/init `, {
                method: "POST",
                headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
                body:formData,

            });

            if (!initResponse.ok) throw new Error("Failed to initialize upload");
            const initData = await initResponse.json();  // 解析JSON响应
            console.log(initData);
            setFileId(initData.fileId);  // 设置文件ID
            setUploadId(initData.uploadId);  // 设置上传ID

            // 查询已上传的分片
            const partsResponse = await apiRequest(
                `${process.env.REACT_APP_API_URL}/resumable/parts?fileId=${initData.fileId}&uploadId=${initData.uploadId}`,
                {
                    method: "GET",
                    headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
                }
            );
            console.log(2);

            const uploadedParts = await partsResponse.json();
            setUploadedParts(uploadedParts.map((part) => ({
                partNumber: part.partNumber,
                eTag: part.eTag
            })));

            // 开始分片上传
            await uploadChunks(file, initData.uploadId,initData.fileId);
        } catch (error) {
            console.error("Error initializing upload:", error);
            setIsUploading(false);
        }
    };

    const uploadChunks = async (file, uploadId, fileId) => {
        const totalChunks = Math.ceil(file.size / CHUNK_SIZE);
        const partETags = [...uploadedParts];

        try {
            for (let i = 1; i <= totalChunks; i++) {
                if (isPaused) {
                    console.log("Upload paused");
                    return;
                }

                // 检查是否已上传该分片
                if (partETags.some(part => part.partNumber === i)) {
                    setUploadProgress(prevProgress =>
                        Math.max(prevProgress, (i / totalChunks) * 100));
                    continue;
                }

                const start = (i - 1) * CHUNK_SIZE;
                const end = Math.min(start + CHUNK_SIZE, file.size);
                const chunk = file.slice(start, end);

                // 等待每个分片完成上传后再继续
                const result = await uploadPart(chunk, fileId, uploadId, i);
                partETags.push({
                    partNumber: result.partNumber,
                    eTag: result.eTag
                });
            }

            // 所有分片都上传完毕后才完成上传
            await completeUpload(fileId, uploadId, partETags);

        } catch (error) {
            console.error("Error uploading chunks:", error);
            setIsUploading(false);
        }
    };

// New function to upload a single part with a Promise
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
                    const chunkProgress = (event.loaded / event.total) * (100 / totalChunks);
                    setUploadProgress(prev => Math.min(prev + chunkProgress, 100));
                }
            });

            xhr.addEventListener("load", () => {
                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        console.log("Raw response:", xhr.responseText);
                        const response = JSON.parse(xhr.responseText);
                        console.log(`Part ${partNumber} upload response:`, response);

                        // 打印对象的所有属性
                        console.log("Response properties:", Object.keys(response));

                        // 无论响应结构如何，确保我们提取正确的值
                        const extractedPartNumber = response.partNumber || partNumber;
                        const extractedETag = response.eTag;

                        if (!extractedETag) {
                            console.error("Cannot find eTag in response:", response);
                            reject(new Error("Missing eTag in server response"));
                            return;
                        }

                        resolve({
                            partNumber: extractedPartNumber,
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
        console.log("Calling completeUpload with3: ", partETags);

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
        }
    };

    const pauseUpload = () => {
        setIsPaused(true);
        if (xhrRef.current) {
            xhrRef.current.abort();
        }
    };

    const resumeUpload = async () => {
        if (!fileRef.current || !uploadId) return;

        setIsPaused(false);
        await uploadChunks(fileRef.current, uploadId);
    };

    const cancelUpload = async () => {
        if (!fileId || !uploadId) return;

        try {
            await apiRequest(`${process.env.REACT_APP_API_URL}/resumable/abort`, {
                method: "POST",
                body: JSON.stringify({ fileId, uploadId }),
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