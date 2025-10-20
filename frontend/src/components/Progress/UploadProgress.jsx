import React from 'react';

const UploadProgress = ({
                            progress,
                            isPaused,
                            onPause,
                            onResume,
                            onCancel,
                            additionalInfo,
                            showPauseButton = true,
                        }) => {
    return (
        <div
            style={{
                position: 'fixed',
                bottom: '20px',
                left: '20px',
                width: '300px',
                zIndex: 9999,
                backgroundColor: '#fff',
                padding: '10px',
                boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
                borderRadius: '4px',
            }}
        >
            <div className="progress-container">
                {/* 进度条 */}
                <div
                    style={{
                        width: '100%',
                        backgroundColor: '#e0e0e0',
                        borderRadius: '4px',
                        height: '8px',
                        overflow: 'hidden',
                    }}
                >
                    <div
                        style={{
                            width: `${progress}%`,
                            backgroundColor: '#4caf50',
                            height: '100%',
                            borderRadius: '4px',
                            transition: 'width 0.3s ease',
                        }}
                    />
                </div>

                {/* 进度文本 */}
                <div
                    style={{
                        textAlign: 'center',
                        marginTop: '4px',
                        fontSize: '12px',
                    }}
                >
                    Uploading: {progress.toFixed(2)}%
                    {additionalInfo && ` ${additionalInfo}`}
                </div>

                {/* 控制按钮 */}
                <div
                    style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        marginTop: '8px',
                    }}
                >
                    {showPauseButton && (
                        <button
                            onClick={isPaused ? onResume : onPause}
                            style={{
                                padding: '4px 8px',
                                fontSize: '12px',
                                backgroundColor: isPaused ? '#4caf50' : '#ff9800',
                                color: 'white',
                                border: 'none',
                                borderRadius: '4px',
                                cursor: 'pointer',
                            }}
                        >
                            {isPaused ? 'Resume' : 'Pause'}
                        </button>
                    )}
                    <button
                        onClick={onCancel}
                        style={{
                            padding: '4px 8px',
                            fontSize: '12px',
                            backgroundColor: '#f44336',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer',
                        }}
                    >
                        Cancel
                    </button>
                </div>
            </div>
        </div>
    );
};

export default UploadProgress;