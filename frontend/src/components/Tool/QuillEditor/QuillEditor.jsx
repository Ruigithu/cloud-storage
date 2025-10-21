import React, { useEffect, useRef, useState, useCallback } from 'react';
import Quill from 'quill';
import 'quill/dist/quill.snow.css';
import './QuillEditor.css';

import {
    loadDocument,
    convertDocxToHtml,
    convertHtmlToDocx,
    uploadFile,
    downloadFile,
    processImagesInDelta,
    hasBase64Images
} from '../../../services/editorService';

import {
    createQuillConfig,
    getFileExtension,
    isWordDocument,
    isImageFile,
    formatLastSaved,
    createFile, isLegacyWordDocument
} from '../../../utils/editorHelper';

const QuillEditor = ({ documentId, userId }) => {
    // Refs
    const editorRef = useRef(null);
    const quillRef = useRef(null);
    const isSavingRef = useRef(false);
    const contentChangeRef = useRef(false);

    // States
    const [isUnsupportedFile, setIsUnsupportedFile] = useState(false);
    // eslint-disable-next-lin
    const [, setIsLegacyDoc] = useState(false);
    const [isImage, setIsImage] = useState(false);
    const [fileUrl, setFileUrl] = useState('');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [lastSaved, setLastSaved] = useState(null);
    const [contentChanged, setContentChanged] = useState(false);
    const [saveCount, setSaveCount] = useState(0);
    const [fileInfo, setFileInfo] = useState(null);

    /**
     * update save status
     */
    const updateSaveStatus = () => {
        const now = new Date();
        setLastSaved(now);
        setContentChanged(false);
        contentChangeRef.current = false;
        setSaveCount(prev => prev + 1);
    };

    /**
     * save doc
     */
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
            }

            else if (isWordDocument(fileInfo?.mimeType)) {
                const htmlContent = quillRef.current.root.innerHTML;
                const result = await convertHtmlToDocx(htmlContent, fileName, userId, documentId);

                setFileInfo({
                    ...fileInfo,
                    name: result.fileName,
                    mimeType: result.mimeType,
                    versionId: result.versionId
                });

                updateSaveStatus();
                return;
            }

            else {
                const delta = quillRef.current.getContents();
                const hasBas64Images = hasBase64Images(delta);
                const updatedDelta = hasBas64Images
                    ? await processImagesInDelta(delta, userId, documentId)
                    : delta;
                const deltaJson = JSON.stringify(updatedDelta);
                blob = new Blob([deltaJson], { type: 'application/json' });
            }

            // 上传文件
            const file = createFile(blob, fileName, mimeType);
            await uploadFile(file, userId, documentId);

            updateSaveStatus();
        } catch (error) {
            console.error(`Error saving file:`, error);
            alert('Failed to save, please try again');
        } finally {
            setSaving(false);
            isSavingRef.current = false;
        }
    }, [documentId, userId, isImage, fileInfo, contentChanged, fileUrl, isUnsupportedFile]);

    /**
     * 下载文件
     */
    const handleDownload = async () => {
        try {
            await downloadFile(documentId, userId, fileInfo?.name);
        } catch (error) {
            console.error('Error downloading file:', error);
            alert('Failed to download file');
        }
    };

    /**
     * load document content
     */
    const loadDocumentContent = useCallback(async () => {
        try {
            setLoading(true);
            const { response, contentType, fileName, isJson } = await loadDocument(documentId, userId);

            const fileInfoBase = {
                name: fileName,
                mimeType: contentType,
                id: documentId
            };

            setFileInfo(fileInfoBase);

            if (isJson) {
                const data = await response.json();
                setFileInfo({
                    ...fileInfoBase,
                    name: data.fileName || fileName,
                    mimeType: data.mimeType || contentType,
                    versionId: data.versionId,
                    size: data.size
                });

                if (isLegacyWordDocument(data.mimeType)) {
                    // 处理 .doc
                    setIsLegacyDoc(true);
                    setIsUnsupportedFile(true);
                    if (quillRef.current) {
                        quillRef.current.disable();
                    }
                } else if (isWordDocument(data.mimeType)) {
                    // 处理 .docx
                    const htmlContent = await convertDocxToHtml(documentId, userId);
                    if (quillRef.current) {
                        quillRef.current.setText('');
                        quillRef.current.clipboard.dangerouslyPasteHTML(htmlContent);
                    }
                } else {
                    // 其他不支持的类型
                    setIsUnsupportedFile(true);
                    if (quillRef.current) {
                        quillRef.current.disable();
                    }
                }
            } else {
                const blob = await response.blob();

                if (isImageFile(contentType)) {
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
    }, [documentId, userId]);

    /**
     * 初始化 Quill 编辑器
     */
    useEffect(() => {
        if (!quillRef.current && !isUnsupportedFile) {
            quillRef.current = new Quill(editorRef.current, createQuillConfig());

            // 监听文本更改
            quillRef.current.on('text-change', (delta, oldDelta, source) => {
                if (source === 'user') {
                    contentChangeRef.current = true;
                    setContentChanged(true);
                }
            });
        }

        return () => {
            // 清理编辑器
            if (quillRef.current) {
                quillRef.current.off('text-change');
            }
        };
    }, [isUnsupportedFile]);

    /**
     * 加载文档
     */
    useEffect(() => {
        loadDocumentContent();
    }, [loadDocumentContent]);

    /**
     * 监听页面离开事件（未保存警告）
     */
    useEffect(() => {
        const handleBeforeUnload = (e) => {
            if (contentChanged || contentChangeRef.current) {
                e.preventDefault();
                e.returnValue = 'You have unsaved changes. Are you sure you want to leave?';
                return e.returnValue;
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => window.removeEventListener('beforeunload', handleBeforeUnload);
    }, [contentChanged]);

    return (
        <div className="flex flex-col h-full">
            {/* 工具栏 */}
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

                <div className="save-status">
                    {!isUnsupportedFile && !isImage && (
                        <>
                            <span>{formatLastSaved(lastSaved)}</span>
                            {saveCount > 0 && (
                                <span className="save-count">(Saved {saveCount} times)</span>
                            )}
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

            {/* 编辑器内容区域 */}
            <div className="flex-grow relative">
                {loading && (
                    <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-50 z-10">
                        <div className="text-gray-600">Loading...</div>
                    </div>
                )}

                {isImage ? (
                    <div className="image-container">
                        <img src={fileUrl} alt="document" className="preview-img" />
                    </div>
                ) : isUnsupportedFile ? (
                    <div className="unsupported-file-container">
                        <div className="unsupported-file-message">
                            <p>This file type cannot be edited in this editor.</p>
                            <button className="download-button-large" onClick={handleDownload}>
                                Download File
                            </button>
                        </div>
                    </div>
                ) : (
                    <div ref={editorRef} className="h-full editor-container" />
                )}
            </div>
        </div>
    );
};

export default QuillEditor;