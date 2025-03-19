import React, { useRef, useState } from "react";
import "./OperateSpecificFolder.css"

const OperateSpecificFolder = ({ folder, userId }) => {
    const [isMenuVisible, setIsMenuVisible] = useState(false);
    const menuRef = useRef(null);
    const buttonRef = useRef(null);


    const handleButtonClick = (e) => {
        e.stopPropagation();
        console.log("Button clicked");
        setIsMenuVisible(!isMenuVisible);
    };

    const handleClickOutside = (e) => {
        if (menuRef.current &&
            !menuRef.current.contains(e.target) &&
            !buttonRef.current.contains(e.target)) {
            setIsMenuVisible(false);
        }
    };

    React.useEffect(() => {
        document.addEventListener("click", handleClickOutside);
        return () => {
            document.removeEventListener("click", handleClickOutside);
        };
    }, []);

    const handleDeleteFolder = async (folderId, userId) => {
        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/softDeleteFolder?folderId=${folderId}&userId=${userId}`, {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: 'include',
            });
            if (!response.ok) {
                throw new Error(`Failed to delete the file: ${response.statusText}`);
            }
            console.log("File deleted successfully");
        } catch (error) {
            console.error("Error deleting file:", error);
        }
    };

    const handleDownloadFolder = async (folderId, userId) => {
        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/downloadFolder?folderId=${folderId}&userId=${userId}`, {
                method: 'GET',
                credentials: 'include',
            });

            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', 'folder.zip');
                document.body.appendChild(link);
                link.click();
                link.remove();
                window.URL.revokeObjectURL(url);
            } else {
                console.error('fail downloading file');
            }
        } catch (error) {
            console.error('Error downloading folder:', error);
        }
    };

    return (
        <div className="operate-file-menu-container">
            <i
                className="fa-solid fa-ellipsis more"
                style={{color: '#bcbdbd'}}
                ref={buttonRef}
                onClick={handleButtonClick}
            />

            {isMenuVisible && (
                <div ref={menuRef} className="menu-container">
                    <ul className="menu-list">
                        <li
                            className="download more"
                            onClick={() => handleDownloadFolder(folder.id, userId)}>
                            Download
                        </li>
                        <li
                            className="delete more"
                            onClick={() => handleDeleteFolder(folder.id, userId)}>
                            Delete
                        </li>
                        <li
                            className="share more"
                            onClick={() => alert("Share Selected")}>
                            Share
                        </li>
                    </ul>
                </div>
            )}
        </div>
    );
};

export default OperateSpecificFolder;