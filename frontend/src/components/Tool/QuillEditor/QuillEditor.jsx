import React, { useEffect, useRef, useState } from 'react';
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

    useEffect(() => {
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


        const newSocket = new SockJS(`http://localhost:8080/ws/document?userId=${userId}&documentId=${documentId}`, null, {
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

                    // 后续处理逻辑

                console.log("Event data: ", event.data); // 检查 event.data 内容
                console.log(`传回的user是：`+editingUserId);

                if (type === 'text-change' && editingUserId !== userId) {
                    quillRef.current.updateContents(delta);
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

            quillRef.current.on('text-change', (delta, oldDelta, source) => {
                if (source === 'user') {
                    newSocket.send(JSON.stringify({
                        type: 'text-change',
                        delta,
                        documentId,
                        userId
                    }));
                }
            });

            return () => {
                newSocket.close();
            };
        }, [documentId, userId]);


        useEffect(() => {
            const loadDocument = async () => {
                try {
                    const response = await fetch(
                        `http://localhost:8080/getFileByUserIdAndFileId?ownerId=${userId}&fileId=${documentId}`,
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
                    } else if (contentType.startsWith('image/')) {
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
                }
            };

            loadDocument();
        }, [documentId, userId]);

        // useEffect(() => {
        //     if (!socket) return;
        //
        //     socket.emit('getActiveUsers', {documentId});
        //
        //     socket.on('activeUsers', (users) => {
        //         setActiveUsers(new Set(users));
        //     });
        //
        //     return () => {
        //         socket.off('activeUsers');
        //     };
        // }, [socket, documentId]);



    return (
        <div className="flex flex-col h-full">
            <div className="bg-gray-100 p-2 flex items-center info">
                <div className="text-sm text-gray-600">
                    在线用户: {Array.from(activeUsers).join(', ')}
                </div>
                <div className="ml-auto">
                    <span className="text-sm text-gray-600">
                        文档ID: {documentId}
                    </span>
                </div>
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
                    <div ref={editorRef} className="h-full"/>
                )}
            </div>
        </div>
    );
};

export default QuillEditor;