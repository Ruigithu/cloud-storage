import React, { useEffect, useRef, useState, useCallback } from 'react';
import Quill from 'quill';
import 'quill/dist/quill.snow.css';
import './QuillEditor.css';
import SockJS from 'sockjs-client';
import apiRequest from "../../../utils/api";

const QuillEditor = ({ documentId, userId }) => {
    const editorRef = useRef(null);
    const quillRef = useRef(null);
    const [socket, setSocket] = useState(null);
    const [activeUsers, setActiveUsers] = useState(new Set());
    const [isUnsupportedFile, setIsUnsupportedFile] = useState(false);
    const [isImage, setIsImage] = useState(false);
    const [fileUrl, setFileUrl] = useState('');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [lastSaved, setLastSaved] = useState(null);
    const [contentChanged, setContentChanged] = useState(false);
    const contentChangeRef = useRef(false);
    const [saveCount, setSaveCount] = useState(0);
    const [fileInfo, setFileInfo] = useState(null);
    const isSavingRef = useRef(false);

    // Function to process images in the editor content
    const processImagesInDelta = async (delta) => {
        const updatedOps = await Promise.all(delta.ops.map(async op => {
            if (op.insert && op.insert.image && op.insert.image.startsWith('data:image')) {
                const imageBlob = await fetch(op.insert.image).then(res => res.blob());
                const imageFile = new File([imageBlob], `image_${Date.now()}.png`, { type: imageBlob.type });
                const formData = new FormData();
                formData.append('file', imageFile);
                formData.append('ownerId', userId);
                formData.append('fileId', documentId);
                const response = await apiRequest(`${process.env.REACT_APP_API_URL}/uploadNewFile`, {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                });
                if (!response.ok) {
                    throw new Error(`Failed to upload image: ${response.status}`);
                }
                const { url } = await response.json();
                return { insert: { image: url } };
            }
            return op;
        }));
        return { ops: updatedOps };
    };

    // Download file function
    const downloadFile = async () => {
        try {
            const response = await apiRequest(
                `${process.env.REACT_APP_API_URL}/getFileByUserIdAndFileId?ownerId=${userId}&fileId=${documentId}`,
                {
                    method: 'GET',
                    headers: {
                        'Accept': 'application/octet-stream',
                    },
                    credentials: 'include'
                }
            );
            if (!response.ok) {
                throw new Error(`Failed to download: ${response.status}`);
            }
            const blob = await response.blob();
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = fileInfo?.name || `file_${documentId}`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);
        } catch (error) {
            console.error('Error downloading file:', error);
            alert('Failed to download file');
        }
    };

    // Save document function (manual trigger only, no auto-save)
    const saveDocument = useCallback(async () => {
        if (!quillRef.current || isSavingRef.current || isUnsupportedFile) return;

        if (!contentChanged && !contentChangeRef.current && !isImage) {
            console.log('No change detected, skipping save');
            return;
        }

        try {
            setSaving(true);
            isSavingRef.current = true;

            let blob;
            let mimeType = fileInfo?.mimeType || 'application/json';
            let fileName = fileInfo?.name || `document_${documentId}${getFileExtension(mimeType)}`;

            if (isImage) {
                const response = await fetch(fileUrl);
                blob = await response.blob();
                mimeType = fileInfo?.mimeType || 'image/png';
                fileName = fileInfo?.name || `image_${documentId}${getFileExtension(mimeType)}`;
            } else if (fileInfo?.mimeType.includes('application/vnd.openxmlformats-officedocument.wordprocessingml.document') ||
                fileInfo?.mimeType.includes('application/msword')) {
                // For Word documents, use the specialized API endpoint
                const htmlContent = quillRef.current.root.innerHTML;
                const response = await apiRequest(
                    `${process.env.REACT_APP_API_URL}/convertHtmlToDocx`,
                    {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        credentials: 'include',
                        body: JSON.stringify({
                            htmlContent,
                            fileName: fileName.endsWith('.docx') || fileName.endsWith('.doc') ?
                                fileName : `${fileName}.docx`,
                            ownerId: userId,
                            fileId: documentId
                        })
                    }
                );

                if (!response.ok) {
                    throw new Error(`Failed to convert to DOCX: ${response.status}`);
                }

                const result = await response.json();
                setFileInfo({
                    ...fileInfo,
                    name: result.fileName,
                    mimeType: result.mimeType,
                    versionId: result.versionId
                });

                // Save completed via the specialized endpoint
                const now = new Date();
                setLastSaved(now);
                setContentChanged(false);
                contentChangeRef.current = false;
                setSaveCount(prev => prev + 1);
                setSaving(false);
                isSavingRef.current = false;
                return;
            } else {
                // For non-Word documents
                const delta = quillRef.current.getContents();
                const hasBase64Images = delta.ops.some(op => op.insert && op.insert.image && op.insert.image.startsWith('data:image'));
                const updatedDelta = hasBase64Images ? await processImagesInDelta(delta) : delta;
                const deltaJson = JSON.stringify(updatedDelta);
                blob = new Blob([deltaJson], { type: 'application/json' });
            }

            // Upload the file
            const file = new File([blob], fileName, { type: mimeType });
            const formData = new FormData();
            formData.append('file', file);
            formData.append('ownerId', userId);
            formData.append('fileId', documentId);

            const uploadResponse = await apiRequest(
                `${process.env.REACT_APP_API_URL}/uploadNewFile`,
                {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                }
            );

            if (!uploadResponse.ok) {
                throw new Error(`Failed to save: ${uploadResponse.status}`);
            }

            const now = new Date();
            setLastSaved(now);
            setContentChanged(false);
            contentChangeRef.current = false;
            setSaveCount(prev => prev + 1);
        } catch (error) {
            console.error(`Error saving file:`, error);
            alert('Failed to save, please try again');
        } finally {
            setSaving(false);
            isSavingRef.current = false;
        }
    }, [documentId, userId, isImage, fileInfo, contentChanged, fileUrl, isUnsupportedFile, processImagesInDelta]);
    // File extension helper
    function getFileExtension(mimeType) {
        const mimeToExt = {
            'application/json': '.json',
            'text/plain': '.txt',
            'text/html': '.html',
            'image/png': '.png',
            'image/jpeg': '.jpg',
            'application/pdf': '.pdf',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document': '.docx',
            'application/msword': '.doc',
        };
        return mimeToExt[mimeType] || '.txt';
    }

    // Check if file type is editable
    function isEditableFileType(mimeType) {
        const editableTypes = [
            'text/plain',
            'text/html',
            'application/json',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/msword'
        ];
        return editableTypes.some(type => mimeType?.includes(type));
    }

    // Initialize Quill editor and WebSocket connection
    useEffect(() => {
        if (!quillRef.current && !isUnsupportedFile) {
            const toolbarOptions = [
                ['bold', 'italic', 'underline', 'strike'],
                ['blockquote', 'code-block'],
                [{'header': 1}, {'header': 2}],
                [{'list': 'ordered'}, {'list': 'bullet'}],
                [{'script': 'sub'}, {'script': 'super'}],
                [{'indent': '-1'}, {'indent': '+1'}],
                [{'direction': 'rtl'}],
                [{'size': ['small', false, 'large', 'huge']}],
                [{'header': [1, 2, 3, 4, 5, 6, false]}],
                [{'color': []}, {'background': []}],
                [{'font': []}],
                [{'align': []}],
                ['clean'],
                ['link', 'image']
            ];

            quillRef.current = new Quill(editorRef.current, {
                modules: {
                    toolbar: toolbarOptions,
                    history: {
                        delay: 2000,
                        maxStack: 500,
                        userOnly: true
                    }
                },
                theme: 'snow',
                placeholder: 'Start editing the file...',
            });
        }

        // Set up WebSocket for collaborative editing
        const newSocket = new SockJS(`${process.env.REACT_APP_API_URL}/ws/document?userId=${userId}&documentId=${documentId}`, null, {
            transports: ['websocket'],
            withCredentials: true,
            headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
        });

        newSocket.onopen = () => {
            console.log("Connected to WebSocket");
            newSocket.send(JSON.stringify({
                type: 'joinDocument',
                documentId,
                userId
            }));
        };

        newSocket.onmessage = (event) => {
            try {
                const parsedData = JSON.parse(event.data);
                const { type, userId: editingUserId, delta } = parsedData;

                if (type === 'text-change' && editingUserId !== userId) {
                    quillRef.current.updateContents(delta);
                    setContentChanged(true);
                    contentChangeRef.current = true;
                }

                if (type === 'userJoined') {
                    setActiveUsers(prev => new Set([...prev, editingUserId]));
                }

                if (type === 'userLeft') {
                    setActiveUsers(prev => {
                        const newUsers = new Set(prev);
                        newUsers.delete(editingUserId);
                        return newUsers;
                    });
                }
            } catch (error) {
                console.error("Message parsing error:", error);
            }
        };

        // Set up text change event listener for collaborative editing
        if (quillRef.current && !isUnsupportedFile) {
            quillRef.current.on('text-change', (delta, oldDelta, source) => {
                if (source === 'user') {
                    newSocket.send(JSON.stringify({
                        type: 'text-change',
                        delta,
                        documentId,
                        userId
                    }));

                    contentChangeRef.current = true;
                    setContentChanged(true);
                }
            });
        }

        setSocket(newSocket);

        return () => {
            newSocket.close();
        };
    }, [documentId, userId, isUnsupportedFile]);

    // Load document content on component mount
    useEffect(() => {
        const loadDocument = async () => {
            try {
                setLoading(true);
                const response = await apiRequest(
                    `${process.env.REACT_APP_API_URL}/getFileByUserIdAndFileId?ownerId=${userId}&fileId=${documentId}`,
                    {
                        method: 'GET',
                        headers: {
                            'Accept': 'application/json, application/octet-stream',
                            'Content-Type': 'application/json',
                        },
                        credentials: 'include'
                    }
                );

                if (!response.ok) {
                    throw new Error(`Server responded with status ${response.status}`);
                }

                const contentType = response.headers.get('content-type');
                const contentDisposition = response.headers.get('content-disposition');
                let fileName = 'document';
                if (contentDisposition) {
                    const filenameMatch = contentDisposition.match(/filename="(.+)"/);
                    if (filenameMatch) {
                        fileName = filenameMatch[1];
                    }
                }

                setFileInfo({
                    name: fileName,
                    mimeType: contentType,
                    id: documentId
                });

                if (contentType && contentType.includes('application/json')) {
                    const data = await response.json();
                    setFileInfo({
                        name: data.fileName || fileName,
                        mimeType: data.mimeType || contentType,
                        id: documentId,
                        versionId: data.versionId,
                        size: data.size
                    });

                    if (data.mimeType.includes('application/json')) {
                        const delta = JSON.parse(data.content);
                        if (quillRef.current) {
                            quillRef.current.setContents(delta);
                        }
                    } else if (data.mimeType.includes('text/html')) {
                        if (quillRef.current) {
                            quillRef.current.root.innerHTML = data.content;
                        }
                    } else if (isEditableFileType(data.mimeType)) {
                        // Handle other editable text formats
                        if (quillRef.current) {
                            if (data.content) {
                                quillRef.current.setText(data.content);
                            } else {
                                quillRef.current.setText('');
                            }
                        }
                    } else {
                        setIsUnsupportedFile(true);
                        if (quillRef.current) {
                            quillRef.current.disable();
                        }
                    }
                } else {
                    const blob = await response.blob();

                    if (contentType.startsWith('image/')) {
                        const url = URL.createObjectURL(blob);
                        setFileUrl(url);
                        setIsImage(true);
                    } else {
                        setIsUnsupportedFile(true);
                    }
                }
            } catch (error) {
                console.error('Error loading document:', error);
                if (quillRef.current) {
                    quillRef.current.setText('Failed to load, try again');
                }
            } finally {
                setLoading(false);
                setContentChanged(false);
                contentChangeRef.current = false;
            }
        };

        loadDocument();
    }, [documentId, userId]);

    // Warn user about unsaved changes when leaving the page
    useEffect(() => {
        const handleBeforeUnload = (e) => {
            if (contentChanged || contentChangeRef.current) {
                e.preventDefault();
                e.returnValue = 'You have unsaved changes. Are you sure you want to leave?';
                return e.returnValue;
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [contentChanged]);

    const formatLastSaved = () => {
        if (!lastSaved) return 'Not saved yet';
        return `Last saved: ${lastSaved.toLocaleTimeString()}`;
    };

    return (
        <div className="flex flex-col h-full">
            <div className="editor-toolbar">
                {!isUnsupportedFile && !isImage && (
                    <button
                        className={`save-button ${saving ? 'saving' : ''}`}
                        onClick={saveDocument}
                        disabled={saving || (!contentChanged && !contentChangeRef.current)}
                    >
                        {saving ? 'Saving...' : 'Save'}
                    </button>
                )}

                {/*{(isUnsupportedFile || isImage || fileInfo?.mimeType.includes('application/vnd.openxmlformats-officedocument.wordprocessingml.document') ||*/}
                {/*    fileInfo?.mimeType.includes('application/msword')) && (*/}
                {/*    <button*/}
                {/*        className="download-button"*/}
                {/*        onClick={downloadFile}*/}
                {/*    >*/}
                {/*        Download File*/}
                {/*    </button>*/}
                {/*)}*/}

                <div className="save-status">
                    {!isUnsupportedFile && !isImage && (
                        <>
                            <span>{formatLastSaved()}</span>
                            {saveCount > 0 && <span className="save-count">(Saved {saveCount} times)</span>}
                        </>
                    )}
                </div>

                {(contentChanged || contentChangeRef.current) && !isUnsupportedFile && !isImage && (
                    <div className="unsaved-indicator">
                        <span className="unsaved-dot"></span>
                        Unsaved changes
                    </div>
                )}
            </div>

            <div className="flex-grow relative">
                {loading && (
                    <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-50 z-10">
                        <div className="text-gray-600">Loading...</div>
                    </div>
                )}

                {isImage ? (
                    <div className="image-container">
                        <img src={fileUrl} alt="document" className="preview-img"/>
                    </div>
                ) : isUnsupportedFile ? (
                    <div className="unsupported-file-container">
                        <div className="unsupported-file-message">
                            <p>This file type cannot be edited in this editor.</p>
                            <button className="download-button-large" onClick={downloadFile}>
                                Download File
                            </button>
                        </div>
                    </div>
                ) : (
                    <div ref={editorRef} className="h-full editor-container"/>
                )}
            </div>
        </div>
    );
};

export default QuillEditor;