
import React, { useEffect, useRef, useState, useCallback } from 'react';
import Quill from 'quill';
import 'quill/dist/quill.snow.css';
import './QuillEditor.css'
import SockJS from 'sockjs-client';
import apiRequest from "../../../utils/api";

const QuillEditor = ({ documentId, userId }) => {
    const editorRef = useRef(null);
    const quillRef = useRef(null);
    const [socket, setSocket] = useState(null);
    const [activeUsers, setActiveUsers] = useState(new Set());
    const [isImage, setIsImage] = useState(false);
    const [imageUrl, setImageUrl] = useState('');
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [lastSaved, setLastSaved] = useState(null);
    const [autoSaveEnabled, setAutoSaveEnabled] = useState(true);
    const autoSaveIntervalRef = useRef(null);
    const contentChangeRef = useRef(false);
    const [saveCount, setSaveCount] = useState(0);

    const debounce = (func, wait) => {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    };

    useEffect(() => {
        if (!quillRef.current) {
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

            // initializing Quill
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
                placeholder: 'start editing the file...',
            });
        }

        const newSocket = new SockJS(`${process.env.REACT_APP_API_URL}/ws/document?userId=${userId}&documentId=${documentId}`, null, {
            transports: ['websocket'],
            withCredentials: true
        });

        newSocket.onopen = () => {
            console.log("Connected to WebSocket");
            // add file
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
                    // other user's mark
                    contentChangeRef.current = true;
                }

                if (type === 'userJoined') {
                    setActiveUsers(prev => new Set([...prev,editingUserId]));
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

        // listen the changes of the content
        if (quillRef.current) {
            quillRef.current.on('text-change', (delta, oldDelta, source) => {
                if (source === 'user') {
                    // send to WebSocket
                    newSocket.send(JSON.stringify({
                        type: 'text-change',
                        delta,
                        documentId,
                        userId
                    }));

                    // save the content change
                    contentChangeRef.current = true;
                }
            });
        }

        return () => {
            if (autoSaveIntervalRef.current) {
                clearInterval(autoSaveIntervalRef.current);
            }
            newSocket.close();
        };
    }, [documentId, userId]);

    useEffect(() => {
        const loadDocument = async () => {
            try {
                setLoading(true);
                const response = await apiRequest(
                    `${process.env.REACT_APP_API_URL}/getFileByUserIdAndFileId?ownerId=${userId}&fileId=${documentId}`,
                    {
                        method: 'GET',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json',
                        },
                        credentials: 'include'
                    }
                );
                if (!response.ok) {
                    throw new Error(`Server responded with status ${response.status}`);
                }

                const contentType = response.headers.get('content-type');

                // for Word and text, the backend will send back the content in JSON form
                if (contentType && contentType.includes('application/json')) {
                    const textContent = await response.text();
                    if (quillRef.current) {
                        quillRef.current.setText(''); // clean the editor
                        quillRef.current.insertText(0, textContent);
                    }
                } else if (contentType && contentType.startsWith('image/')) {
                    // handle the image
                    const blob = await response.blob();
                    const url = URL.createObjectURL(blob);
                    setImageUrl(url);
                    setIsImage(true);
                } else {
                    // download file
                    const blob = await response.blob();
                    const url = URL.createObjectURL(blob);
                    const contentDisposition = response.headers.get('content-disposition');
                    let filename = 'document';

                    if (contentDisposition) {
                        const filenameMatch = contentDisposition.match(/filename="(.+)"/);
                        if (filenameMatch) {
                            filename = filenameMatch[1];
                        }
                    }

                    const link = document.createElement('a');
                    link.href = url;
                    link.download = filename;
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    URL.revokeObjectURL(url);
                }
            } catch (error) {
                console.error('Error loading document:', error);
                if (quillRef.current) {
                    quillRef.current.setText('fail loading，try again');
                }
            } finally {
                setLoading(false);
                // reset the content change mark
                contentChangeRef.current = false;
            }
        };

        loadDocument();
    }, [documentId, userId]);

    // save document
    const saveDocument = async () => {
        if (isImage || !quillRef.current || saving) return;

        // no content change, skip saving
        if (!contentChangeRef.current) {
            console.log('no change, skip saving');
            return;
        }

        try {
            setSaving(true);

            // get the content
            const content = quillRef.current.getText();
            // for Rich Text Content
            // const content = JSON.stringify(quillRef.current.getContents());

            // mocking uploading file
            const blob = new Blob([content], { type: 'text/plain' });
            const file = new File([blob], `document_${documentId}.txt`, { type: 'text/plain' });

            const formData = new FormData();
            formData.append('file', file);
            formData.append('ownerId', userId);
            formData.append('folderId', documentId);

            const response = await apiRequest(
                `${process.env.REACT_APP_API_URL}/uploadNewFile`,
                {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                }
            );

            if (!response.ok) {
                throw new Error(`fail saving: ${response.status}`);
            }

            const now = new Date();
            setLastSaved(now);
            contentChangeRef.current = false;
            // the time of saving
            setSaveCount(prev => prev + 1);
            console.log('Document saved successfully', now);
        } catch (error) {
            console.error('Error saving document:', error);
            alert('fail saving, please try again');
        } finally {
            setSaving(false);
        }
    };

    const debouncedSave = useCallback(
        debounce(() => {
            if (contentChangeRef.current) {
                saveDocument();
            }
        }, 1000),
        [documentId, userId]
    );

    // start/forbid automatically save
    useEffect(() => {
        if (autoSaveEnabled && !isImage) {
            autoSaveIntervalRef.current = setInterval(() => {
                if (contentChangeRef.current) {
                    saveDocument();
                }
            }, 120000); // every two minutes check if it needs to save automatically
        } else if (autoSaveIntervalRef.current) {
            clearInterval(autoSaveIntervalRef.current);
        }

        return () => {
            if (autoSaveIntervalRef.current) {
                clearInterval(autoSaveIntervalRef.current);
            }
        };
    }, [autoSaveEnabled, isImage, saveDocument]);

    // save before  leave
    useEffect(() => {
        const handleBeforeUnload = (e) => {
            if (contentChangeRef.current) {
                const saveBeforeLeave = async () => {
                    try {
                        await saveDocument();
                    } catch (error) {
                        console.error('Error saving before leave:', error);
                    }
                };

                saveBeforeLeave();

                // show the confirming dialog
                e.preventDefault();
                e.returnValue = 'Save the changes？';
                return e.returnValue;
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [saveDocument]);


    const formatLastSaved = () => {
        if (!lastSaved) return 'not saved yet';
        return `last saved: ${lastSaved.toLocaleTimeString()}`;
    };

    //
    const toggleAutoSave = () => {
        setAutoSaveEnabled(!autoSaveEnabled);
    };

    return (
        <div className="flex flex-col h-full">
            <div className="editor-toolbar">
                <button
                    className={`save-button ${saving ? 'saving' : ''}`}
                    onClick={saveDocument}
                    disabled={saving || isImage || !contentChangeRef.current}
                >
                    {saving ? 'saving...' : 'save'}
                </button>
                <div className="save-status">
                    <span>{formatLastSaved()}</span>
                    {saveCount > 0 && <span className="save-count">( have been saved for {saveCount} times )</span>}
                </div>
                <div className="auto-save-toggle">
                    <label className="auto-save-label">
                        <input
                            type="checkbox"
                            checked={autoSaveEnabled}
                            onChange={toggleAutoSave}
                            disabled={isImage}
                        />
                        automatically save
                    </label>
                </div>
                {contentChangeRef.current && (
                    <div className="unsaved-indicator">
                        <span className="unsaved-dot"></span>
                        not saved yet
                    </div>
                )}
            </div>
            <div className="flex-grow relative">
                {loading && (
                    <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-50 z-10">
                        <div className="text-gray-600">loading...</div>
                    </div>
                )}
                {isImage ? (
                    <img src={imageUrl} alt="document" className="preview-img"/>
                ) : (
                    <div ref={editorRef} className="h-full editor-container"/>
                )}
            </div>
        </div>
    );
};

export default QuillEditor;