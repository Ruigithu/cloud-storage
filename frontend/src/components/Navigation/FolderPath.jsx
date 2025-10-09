import React from 'react';

function FolderPath({ navigationPath, onBackward, onPathClick }) {
    return (
        <div className="first-row">
            <div className="folder-path">
                <i
                    className="fa-solid fa-backward"
                    style={{
                        color: "#616365",
                        cursor: navigationPath.length > 1 ? 'pointer' : 'not-allowed',
                        opacity: navigationPath.length > 1 ? 1 : 0.5,
                        marginRight: '20px'
                    }}
                    onClick={onBackward}
                />
                <div className="folder-path">
                    {navigationPath.map((folder, index) => (
                        <span key={folder.id}>
                            <span
                                onClick={() => onPathClick(index)}
                                style={{ cursor: 'pointer', color: '#3d3e40' }}
                            >
                                {folder.name}
                            </span>
                            {index < navigationPath.length - 1 &&
                                <span style={{ margin: '0 8px' }}>&gt;</span>
                            }
                        </span>
                    ))}
                </div>
            </div>
        </div>
    );
}

export default FolderPath;