
import React, { useEffect, useRef, useState, useCallback } from 'react';
import Quill from 'quill';
import 'quill/dist/quill.snow.css';
import './QuillEditor.css'
import SockJS from 'sockjs-client';

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

    // 定义防抖函数
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

            // 初始化 Quill
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
                placeholder: '开始编辑文档...',
            });
        }

        const newSocket = new SockJS(`${process.env.REACT_APP_API_URL}/ws/document?userId=${userId}&documentId=${documentId}`, null, {
            transports: ['websocket'],
            withCredentials: true
        });

        newSocket.onopen = () => {
            console.log("Connected to WebSocket");
            // 加入文档
            newSocket.send(JSON.stringify({
                type: 'joinDocument',
                documentId,
                userId
            }));
        };

        newSocket.onmessage = (event) => {
            try {
                console.log("Raw event data:", event.data);
                const parsedData = JSON.parse(event.data);
                console.log("Parsed event data:", parsedData);

                const { type, userId: editingUserId, delta } = parsedData;

                console.log("Event data: ", event.data);
                console.log(`传回的user是：`+editingUserId);

                if (type === 'text-change' && editingUserId !== userId) {
                    quillRef.current.updateContents(delta);
                    // 其他用户的编辑也标记为有变更，需要保存
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

        // 监听内容变化
        if (quillRef.current) {
            quillRef.current.on('text-change', (delta, oldDelta, source) => {
                if (source === 'user') {
                    // 发送到WebSocket
                    newSocket.send(JSON.stringify({
                        type: 'text-change',
                        delta,
                        documentId,
                        userId
                    }));

                    // 标记有内容变更，需要保存
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
                const response = await fetch(
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
                console.log('Response Status:', response.status);
                console.log('Content-Type:', response.headers.get('content-type'))
                if (!response.ok) {
                    throw new Error(`Server responded with status ${response.status}`);
                }

                const contentType = response.headers.get('content-type');

                // 对于Word文档和文本文件，后端会返回JSON格式的文本内容
                if (contentType && contentType.includes('application/json')) {
                    const textContent = await response.text();
                    if (quillRef.current) {
                        quillRef.current.setText(''); // 清空编辑器
                        quillRef.current.insertText(0, textContent);
                    }
                } else if (contentType && contentType.startsWith('image/')) {
                    // 处理图片
                    const blob = await response.blob();
                    const url = URL.createObjectURL(blob);
                    setImageUrl(url);
                    setIsImage(true);
                } else {
                    // 处理其他二进制文件（提供下载）
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
                    quillRef.current.setText('文档加载失败，请重试');
                }
            } finally {
                setLoading(false);
                // 重置内容变更标记
                contentChangeRef.current = false;
            }
        };

        loadDocument();
    }, [documentId, userId]);

    // 保存文档的函数
    const saveDocument = async () => {
        if (isImage || !quillRef.current || saving) return;

        // 如果没有内容变更，不需要保存
        if (!contentChangeRef.current) {
            console.log('文档没有变更，跳过保存');
            return;
        }

        try {
            setSaving(true);

            // 获取内容
            const content = quillRef.current.getText();
            // 如果需要保存富文本内容，可以使用：
            // const content = JSON.stringify(quillRef.current.getContents());

            // 创建FormData对象模拟文件上传
            const blob = new Blob([content], { type: 'text/plain' });
            const file = new File([blob], `document_${documentId}.txt`, { type: 'text/plain' });

            const formData = new FormData();
            formData.append('file', file);
            formData.append('ownerId', userId);
            formData.append('folderId', documentId); // 使用documentId作为folderId，根据需要调整

            const response = await fetch(
                `${process.env.REACT_APP_API_URL}/uploadNewFile`,
                {
                    method: 'POST',
                    credentials: 'include',
                    body: formData
                }
            );

            if (!response.ok) {
                throw new Error(`保存失败: ${response.status}`);
            }

            const now = new Date();
            setLastSaved(now);
            // 重置内容变更标记
            contentChangeRef.current = false;
            // 更新保存次数
            setSaveCount(prev => prev + 1);
            console.log('Document saved successfully', now);
        } catch (error) {
            console.error('Error saving document:', error);
            alert('保存文档失败，请重试');
        } finally {
            setSaving(false);
        }
    };

    // 创建防抖处理的保存函数
    const debouncedSave = useCallback(
        debounce(() => {
            if (contentChangeRef.current) {
                saveDocument();
            }
        }, 1000), // 1秒延迟
        [documentId, userId]
    );

    // 启用/禁用自动保存
    useEffect(() => {
        if (autoSaveEnabled && !isImage) {
            autoSaveIntervalRef.current = setInterval(() => {
                if (contentChangeRef.current) {
                    saveDocument();
                }
            }, 120000); // 每2分钟检查一次是否需要自动保存
        } else if (autoSaveIntervalRef.current) {
            clearInterval(autoSaveIntervalRef.current);
        }

        return () => {
            if (autoSaveIntervalRef.current) {
                clearInterval(autoSaveIntervalRef.current);
            }
        };
    }, [autoSaveEnabled, isImage, saveDocument]);

    // 添加离开页面前的保存
    useEffect(() => {
        const handleBeforeUnload = (e) => {
            if (contentChangeRef.current) {
                // 同步保存文档
                const saveBeforeLeave = async () => {
                    try {
                        await saveDocument();
                    } catch (error) {
                        console.error('Error saving before leave:', error);
                    }
                };

                saveBeforeLeave();

                // 显示确认对话框
                e.preventDefault();
                e.returnValue = '文档有未保存的更改，确定要离开吗？';
                return e.returnValue;
            }
        };

        window.addEventListener('beforeunload', handleBeforeUnload);
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload);
        };
    }, [saveDocument]);

    // 格式化最后保存时间
    const formatLastSaved = () => {
        if (!lastSaved) return '尚未保存';
        return `上次保存: ${lastSaved.toLocaleTimeString()}`;
    };

    // 切换自动保存
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
                    {saving ? '保存中...' : '保存'}
                </button>
                <div className="save-status">
                    <span>{formatLastSaved()}</span>
                    {saveCount > 0 && <span className="save-count">（已保存{saveCount}次）</span>}
                </div>
                <div className="auto-save-toggle">
                    <label className="auto-save-label">
                        <input
                            type="checkbox"
                            checked={autoSaveEnabled}
                            onChange={toggleAutoSave}
                            disabled={isImage}
                        />
                        自动保存
                    </label>
                </div>
                {contentChangeRef.current && (
                    <div className="unsaved-indicator">
                        <span className="unsaved-dot"></span>
                        有未保存的更改
                    </div>
                )}
            </div>
            <div className="flex-grow relative">
                {loading && (
                    <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-50 z-10">
                        <div className="text-gray-600">加载中...</div>
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
// import React, { useEffect, useRef, useState } from 'react';
// import Quill from 'quill';
// import 'quill/dist/quill.snow.css';
// import './QuillEditor.css'
// import SockJS from 'sockjs-client';
//
// const QuillEditor = ({ documentId, userId }) => {
//     const editorRef = useRef(null);
//     const quillRef = useRef(null);
//     const [socket, setSocket] = useState(null);
//     const [activeUsers, setActiveUsers] = useState(new Set());
//     const [isImage, setIsImage] = useState(false);
//     const [imageUrl, setImageUrl] = useState('');
//     const [loading, setLoading] = useState(false);
//
//     useEffect(() => {
//
//         if (!quillRef.current) {
//             const toolbarOptions = [
//                 ['bold', 'italic', 'underline', 'strike'],
//                 ['blockquote', 'code-block'],
//                 [{'header': 1}, {'header': 2}],
//                 [{'list': 'ordered'}, {'list': 'bullet'}],
//                 [{'script': 'sub'}, {'script': 'super'}],
//                 [{'indent': '-1'}, {'indent': '+1'}],
//                 [{'direction': 'rtl'}],
//                 [{'size': ['small', false, 'large', 'huge']}],
//                 [{'header': [1, 2, 3, 4, 5, 6, false]}],
//                 [{'color': []}, {'background': []}],
//                 [{'font': []}],
//                 [{'align': []}],
//                 ['clean'],
//                 ['link', 'image']
//             ];
//
//             // 初始化 Quill
//             quillRef.current = new Quill(editorRef.current, {
//                 modules: {
//                     toolbar: toolbarOptions,
//                     history: {
//                         delay: 2000,
//                         maxStack: 500,
//                         userOnly: true
//                     }
//                 },
//                 theme: 'snow',
//                 placeholder: '开始编辑文档...',
//             });
//         }
//
//
//         const newSocket = new SockJS(`${process.env.REACT_APP_API_URL}/ws/document?userId=${userId}&documentId=${documentId}`, null, {
//             transports: ['websocket'],
//             withCredentials: true
//         });
//
//             newSocket.onopen = () => {
//                 console.log("Connected to WebSocket");
//                 // 加入文档
//                 newSocket.send(JSON.stringify({
//                     type: 'joinDocument',
//                     documentId,
//                     userId
//                 }));
//             };
//
//             newSocket.onmessage = (event) => {
//                 try {
//                     console.log("Raw event data:", event.data);
//                     const parsedData = JSON.parse(event.data);
//                     console.log("Parsed event data:", parsedData);
//
//                     const { type, userId: editingUserId, delta } = parsedData;
//
//                     // 后续处理逻辑
//
//                 console.log("Event data: ", event.data); // 检查 event.data 内容
//                 console.log(`传回的user是：`+editingUserId);
//
//                 if (type === 'text-change' && editingUserId !== userId) {
//                     quillRef.current.updateContents(delta);
//                 }
//
//                 if (type === 'userJoined') {
//                     setActiveUsers(prev => new Set([...prev,editingUserId]));
//                 }
//
//                 if (type === 'userLeft') {
//                     setActiveUsers(prev => {
//                         const newUsers = new Set(prev);
//                         newUsers.delete(editingUserId);
//                         return newUsers;
//                     });
//                 }
//                 } catch (error) {
//                     console.error("Message parsing error:", error);
//                 }
//             };
//
//             quillRef.current.on('text-change', (delta, oldDelta, source) => {
//                 if (source === 'user') {
//                     newSocket.send(JSON.stringify({
//                         type: 'text-change',
//                         delta,
//                         documentId,
//                         userId
//                     }));
//                 }
//             });
//
//             return () => {
//                 newSocket.close();
//             };
//         }, [documentId, userId]);
//
//
//         useEffect(() => {
//             const loadDocument = async () => {
//                 try {
//                     const response = await fetch(
//                         `${process.env.REACT_APP_API_URL}/getFileByUserIdAndFileId?ownerId=${userId}&fileId=${documentId}`,
//                         {
//                             method: 'GET',
//                             headers: {
//                                 'Accept': 'application/json',
//                                 'Content-Type': 'application/json',
//                             },
//                             credentials: 'include'
//                         }
//                     );
//                     console.log('Response Status:', response.status);
//                     console.log('Content-Type:', response.headers.get('content-type'))
//                     if (!response.ok) {
//                         throw new Error(`Server responded with status ${response.status}`);
//                     }
//
//                     const contentType = response.headers.get('content-type');
//
//
//                     // 对于Word文档和文本文件，后端会返回JSON格式的文本内容
//                     if (contentType && contentType.includes('application/json')) {
//                         const textContent = await response.text();
//                         if (quillRef.current) {
//                             quillRef.current.setText(''); // 清空编辑器
//                             quillRef.current.insertText(0, textContent);
//                         }
//                     } else if (contentType.startsWith('image/')) {
//                         // 处理图片
//                         const blob = await response.blob();
//                         const url = URL.createObjectURL(blob);
//                         setImageUrl(url);
//                         setIsImage(true);
//                     } else {
//                         // 处理其他二进制文件（提供下载）
//                         const blob = await response.blob();
//                         const url = URL.createObjectURL(blob);
//                         const contentDisposition = response.headers.get('content-disposition');
//                         let filename = 'document';
//
//                         if (contentDisposition) {
//                             const filenameMatch = contentDisposition.match(/filename="(.+)"/);
//                             if (filenameMatch) {
//                                 filename = filenameMatch[1];
//                             }
//                         }
//
//                         const link = document.createElement('a');
//                         link.href = url;
//                         link.download = filename;
//                         document.body.appendChild(link);
//                         link.click();
//                         document.body.removeChild(link);
//                         URL.revokeObjectURL(url);
//                     }
//                 } catch (error) {
//                     console.error('Error loading document:', error);
//                     if (quillRef.current) {
//                         quillRef.current.setText('文档加载失败，请重试');
//                     }
//                 } finally {
//                     setLoading(false);
//                 }
//             };
//
//             loadDocument();
//         }, [documentId, userId]);
//
//
//
//     return (
//         <div className="flex flex-col h-full">
//             <div className="flex-grow relative">
//                 {loading && (
//                     <div className="absolute inset-0 flex items-center justify-center bg-white bg-opacity-50 z-10">
//                         <div className="text-gray-600">加载中...</div>
//                     </div>
//                 )}
//                 {isImage ? (
//                         <img src={imageUrl} alt="document" className="preview-img"/>
//                 ) : (
//                     <div ref={editorRef} className="h-full"/>
//                 )}
//             </div>
//         </div>
//     );
// };
//
// export default QuillEditor;