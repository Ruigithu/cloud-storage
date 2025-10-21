import React, { useEffect, useRef, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
    checkShareLink,
    handleReadShare,
    handleWriteShare
} from '../../services/shareService';

const ShareHandler = () => {
    const { shareId } = useParams();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const hasCheckedShare = useRef(false);

    useEffect(() => {
        if (hasCheckedShare.current) return;
        hasCheckedShare.current = true;

        const processShare = async () => {
            try {
                const userId = localStorage.getItem('userId');

                // 检查分享链接
                const result = await checkShareLink(shareId, userId);

                // 处理需要认证的情况
                if (result.needsAuth) {
                    localStorage.setItem('redirectAfterLogin', `/share/${shareId}`);
                    navigate('/login');
                    return;
                }

                // 处理分享不存在的情况
                if (result.status === 404) {
                    navigate('/shareCanceled');
                    return;
                }

                const { type, fileId, fileName } = result.data;

                // 根据分享类型处理
                if (type === 'READ') {
                    await handleReadShare(fileId, fileName);
                    navigate('/');
                } else if (type === 'WRITE') {
                    const rootFolderId = localStorage.getItem('rootFolderId');
                    const newFileId = await handleWriteShare(shareId, userId, rootFolderId);
                    navigate(`/editor/${newFileId}`);
                }
            } catch (err) {
                console.error('Error processing share:', err);
                setError(err.message || 'Failed to access shared content');
            } finally {
                setLoading(false);
            }
        };

        if (shareId) {
            processShare();
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