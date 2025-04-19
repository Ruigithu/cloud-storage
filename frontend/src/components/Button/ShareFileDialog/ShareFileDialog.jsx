import React, { useState } from 'react';
import './ShareFileDialog.css';
import apiRequest from "../../../utils/api";

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

            const response =await apiRequest(`${process.env.REACT_APP_API_URL}/createShareLink`, {
                method: 'POST',
                body: formData,
                credentials:'include'
            });

            if (response.ok) {
                const data = await response.json();
                setShareLink(data.shareLink || '');
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
                <div className="modal-overlay-share">
                    <div
                        className="modal-container-share"
                        onClick={e => e.stopPropagation()}
                    >
                    <div className="modal-content-share">
                        <h2 className="modal-header-share">Share Settings</h2>
                        <form onSubmit={handleShare} className="form-container-share">
                            <div className="form-group-share">
                                <label>Access Type</label>
                                <select
                                    value={accessType}
                                    onChange={(e) => setAccessType(e.target.value)}
                                    className="select-input-share"
                                >
                                    <option value="read">Read Only</option>
                                    <option value="write">Read & Write</option>
                                </select>
                            </div>

                            <div className="form-group-share">
                                <label>Expires After</label>
                                <div className="duration-container-share">
                                    <select
                                        value={duration}
                                        onChange={(e) => setDuration(e.target.value)}
                                        className="select-input-share"
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
                                            className="text-input-share"
                                        />
                                    )}
                                </div>
                            </div>

                            {shareLink && (
                                <div className="form-group-share">
                                    <label>Share Link</label>
                                    <input
                                        type="text"
                                        value={shareLink}
                                        readOnly
                                        className="text-input-share"
                                    />
                                </div>
                            )}

                            <div className="button-container-share">
                                <button type="submit" className="submit-button-share">
                                    Create Share Link
                                </button>
                                <button
                                    type="button"
                                    onClick={handleCloseModal}
                                    className="cancel-button-share"
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
