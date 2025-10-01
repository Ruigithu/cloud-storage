import React, { useRef, useState } from "react";
import "./OperateSpecificDeletedFolder.css"
import apiRequest from "../../../../../utils/apiRequest";

const OperateSpecificDeletedFolder = ({ folder, userId }) => {
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
            const response =await apiRequest(`${process.env.REACT_APP_API_URL}/deleteFolder?folderId=${folderId}&userId=${userId}`, {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: 'include',
            });
            if (!response.ok) {
                throw new Error(`Failed to delete the file: ${response.statusText}`);
            }
            alert("File deleted successfully");

        } catch (error) {
            console.error("Error deleting file:", error);
        }
    };


    async function handleRestoreFolder(id, userId) {
        try {
            const response =await apiRequest(`${process.env.REACT_APP_API_URL}/restoreFolder?folderId=${id}&ownerId=${userId}`, {
                method: "Post",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: 'include',
            });
            if (!response.ok) {
                throw new Error(`Failed to restore the file: ${response.statusText}`);
            }
            console.log("File restored successfully");
        } catch (error) {
            console.error("Error restore file:", error);
        }

    }

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
                            className="delete more"
                            onClick={() => handleDeleteFolder(folder.id, userId)}>
                            Delete
                        </li>
                        <li
                            className="restore more"
                            onClick={() => handleRestoreFolder(folder.id, userId)}>
                            Restore
                        </li>
                    </ul>
                </div>
            )}
        </div>
    );
};

export default OperateSpecificDeletedFolder;