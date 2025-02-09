import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';


const ShareHandler = () => {
    const { shareId } = useParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const checkShare = async () => {
            try {
                const response = await fetch(
                    `http://localhost:8080/share/${shareId}?userId=${localStorage.getItem('userId')}`,
                    {
                        credentials: 'include',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json',
                        }
                    }
                );

                if (response.status === 401) {
                    // 保存当前URL并重定向到登录页面
                    localStorage.setItem('redirectAfterLogin', window.location.pathname);
                    navigate('/login');
                    return;
                }

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                const { type, requiresAuth, fileId, fileName } = data;

                if (type === 'READ') {
                    try {
                        const downloadResponse = await fetch(
                            `http://localhost:8080/download?fileId=${fileId}`,
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
                    if (requiresAuth) {
                        localStorage.setItem('redirectAfterLogin', window.location.pathname);
                        navigate('/login');
                    } else {
                        try {
                            const saveResponse = await fetch(
                                `http://localhost:8080/saveShare/${shareId}?userId=${localStorage.getItem('userId')}`,
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
                }
            } catch (error) {
                console.error('Error accessing share:', error);
                setError('Failed to access shared content');
            } finally {
                setLoading(false);
            }
        };

        checkShare();
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