import React, {useEffect, useRef, useState} from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiRequest from "../../utils/api";


const ShareHandler = () => {
    const { shareId } = useParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const hasCheckedShare = useRef(false);


    useEffect(() => {
        if (hasCheckedShare.current) return; // avoid repeating calling
        hasCheckedShare.current = true;
        const checkShare = async () => {
            try {
                console.log(localStorage.getItem('userId'));
                const userId = localStorage.getItem('userId');
                let url = `${process.env.REACT_APP_API_URL}/share/${shareId}`;
                if (userId && userId !== "null") {
                    url += `?userId=${userId}`;
                }
                const response = await apiRequest(
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
                const { type, fileId, fileName} = data;


                if (type === 'READ') {
                    try {
                        const downloadResponse =  await apiRequest(
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

                    try {

                        const saveResponse = await apiRequest(

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