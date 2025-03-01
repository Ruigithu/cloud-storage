import React, {useEffect, useRef, useState} from 'react';
import { useParams, useNavigate } from 'react-router-dom';


const ShareHandler = () => {
    const { shareId } = useParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const hasCheckedShare = useRef(false);


    useEffect(() => {
        if (hasCheckedShare.current) return; // 避免重复调用
        hasCheckedShare.current = true;
        const checkShare = async () => {
            try {
                console.log(`hhhhh`);
                console.log(localStorage.getItem('userId'));
                const userId = localStorage.getItem('userId');
                // 只有当userId有值且不是字符串"null"时才添加userId参数
                let url = `${process.env.REACT_APP_API_URL}/share/${shareId}`;
                if (userId && userId !== "null") {
                    url += `?userId=${userId}`;
                }
                const response = await fetch(
                    `${url}`,
                    {
                        credentials: 'include',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json',
                        }
                    }
                );
                console.log(String(response.status));
                if (response.status === 401) {
                    // 保存当前URL并重定向到登录页面
                    console.log(window.location.pathname);
                    localStorage.setItem('redirectAfterLogin', `/share/${shareId}`);
                    navigate('/login');
                    return;
                }

                if (!response.ok) {

                    if (response.status === 404) {
                        navigate('/shareCanceled');
                        return;
                    }
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                const { type, fileId, fileName, filePath } = data;

                // 根据后端返回的type类型处理
                if (type === 'READ') {
                    try {
                        // 使用fileId进行下载
                        const downloadResponse = await fetch(
                            `${process.env.REACT_APP_API_URL}/download?fileId=${fileId}`,
                            {
                                credentials: 'include',
                                headers: {
                                    'Accept': 'application/json'
                                }
                            }
                        );

                        if (!downloadResponse.ok) {
                            throw new Error('Download failed');
                        }

                        const blob = await downloadResponse.blob();
                        const url = window.URL.createObjectURL(blob);
                        const link = document.createElement('a');
                        link.href = url;
                        link.download = fileName || 'download';
                        document.body.appendChild(link);
                        link.click();
                        document.body.removeChild(link);
                        window.URL.revokeObjectURL(url);
                        navigate('/');
                    } catch (error) {
                        setError('Failed to download file');
                    }
                } else if (type === 'WRITE') {
                    // 由于后端已经根据userId是否存在处理了权限，
                    // 如果返回WRITE类型，则直接处理保存逻辑

                    try {

                        const saveResponse = await fetch(

                            `${process.env.REACT_APP_API_URL}/saveShare/${shareId}?userId=${localStorage.getItem('userId')}&rootFolderId=${localStorage.getItem('rootFolderId')}`,
                            {
                                method: 'POST',
                                credentials: 'include',
                                headers: {
                                    'Accept': 'application/json',
                                    'Content-Type': 'application/json'
                                }
                            }
                        );

                        if (!saveResponse.ok) {
                            throw new Error('Failed to save shared file');
                        }

                        const { newFileId } = await saveResponse.json();
                        navigate(`/editor/${newFileId}`);
                    } catch (error) {
                        setError('Failed to save shared file');
                    }
                }
            } catch (error) {
                console.error('Error accessing share:', error);
                setError('Failed to access shared content');
            } finally {
                setLoading(false);
            }
        };


        if (shareId) {
            checkShare();
        }
    }, [shareId, navigate]);

    if (loading) {
        return <div>Loading...</div>;
    }

    if (error) {
        return <div>{error}</div>;
    }

    return null;
};

export default ShareHandler;