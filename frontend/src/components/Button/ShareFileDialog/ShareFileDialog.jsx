import React, { useState } from 'react';
import './ShareFileDialog.css';

function ShareFileDialog({ fileId }) {
    const [showModal, setShowModal] = useState(false);
    const [duration, setDuration] = useState('day');
    const [amount, setAmount] = useState(1);
    const [accessType, setAccessType] = useState('read');
    const [shareLink, setShareLink] = useState('');

    const resetModal = () => {
        setDuration('day');
        setAccessType('read');
        setAmount(1);
        setShareLink('');
    };

    const handleCloseModal = () => {
        setShowModal(false);
        resetModal();
    };

    const handleShare = async (e) => {
        e.preventDefault();

        let expiresAt = new Date();
        switch (duration) {
            case 'hour':
                expiresAt.setHours(expiresAt.getHours() + amount);
                break;
            case 'day':
                expiresAt.setDate(expiresAt.getDate() + amount);
                break;
            case 'week':
                expiresAt.setDate(expiresAt.getDate() + amount * 7);
                break;
            case 'month':
                expiresAt.setMonth(expiresAt.getMonth() + amount);
                break;
            case 'never':
                expiresAt = null;
                break;
        }

        try {
            const formData = new FormData();
            formData.append('fileId', fileId);
            formData.append('userId', localStorage.getItem('userId') || '');
            formData.append('accessType', accessType);
            formData.append('expiresAt', expiresAt ? expiresAt.toISOString() : '');

            const response = await fetch('http://localhost:8080/createShareLink', {
                method: 'POST',
                body: formData,
                credentials:'include'
            });

            if (response.ok) {
                const data = await response.json();
                setShareLink(data.shareLink || ''); // 假设后端返回 shareLink
            } else {
                console.error('Share creation failed');
            }
        } catch (error) {
            console.error('Failed to create share:', error);
        }
    };

    return (
        <>
            <div
                onClick={() => setShowModal(true)}
                className="cursor-pointer text-blue-500 hover:text-blue-700"
            >
                Share
            </div>
            {showModal && (
                <div className="modal-overlay">
                    <div
                        className="modal-container"
                        onClick={e => e.stopPropagation()}
                    >
                    <div className="modal-content">
                        <h2 className="modal-header">Share Settings</h2>
                        <form onSubmit={handleShare} className="form-container">
                            <div className="form-group">
                                <label>Access Type</label>
                                <select
                                    value={accessType}
                                    onChange={(e) => setAccessType(e.target.value)}
                                    className="select-input"
                                >
                                    <option value="read">Read Only</option>
                                    <option value="write">Read & Write</option>
                                </select>
                            </div>

                            <div className="form-group">
                                <label>Expires After</label>
                                <div className="duration-container">
                                    <select
                                        value={duration}
                                        onChange={(e) => setDuration(e.target.value)}
                                        className="select-input"
                                    >
                                        <option value="hour">Hours</option>
                                        <option value="day">Days</option>
                                        <option value="week">Weeks</option>
                                        <option value="month">Months</option>
                                        <option value="never">Never</option>
                                    </select>

                                    {duration !== 'never' && (
                                        <input
                                            type="number"
                                            min="1"
                                            max="999"
                                            value={amount}
                                            onChange={(e) => setAmount(parseInt(e.target.value) || 1)}
                                            className="text-input"
                                        />
                                    )}
                                </div>
                            </div>

                            {shareLink && (
                                <div className="form-group">
                                    <label>Share Link</label>
                                    <input
                                        type="text"
                                        value={shareLink}
                                        readOnly
                                        className="text-input"
                                    />
                                </div>
                            )}

                            <div className="button-container">
                                <button type="submit" className="submit-button">
                                    Create Share Link
                                </button>
                                <button
                                    type="button"
                                    onClick={handleCloseModal}
                                    className="cancel-button"
                                >
                                    Cancel
                                </button>
                            </div>
                        </form>
                    </div>
                    </div>
                </div>
            )}
        </>
    );
}

export default ShareFileDialog;
