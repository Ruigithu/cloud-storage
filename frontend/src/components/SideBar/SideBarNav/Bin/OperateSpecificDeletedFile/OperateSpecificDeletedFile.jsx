import React, {useRef, useState} from "react";
import "./OperateSpecificDeletedFile.css"
import apiRequest from "../../../../../utils/apiRequest";


const OperateSpecificDeletedFile = ({file,userId}) => {
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

    async function handleDeleteFile(fileId, userId) {
        try {
            const response =await apiRequest(`${process.env.REACT_APP_API_URL}/deleteFile?fileId=${fileId}&userId=${userId}`, {
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
    }

    async function handleRestoreFile(id, userId) {
        try {
            const response =await apiRequest(`${process.env.REACT_APP_API_URL}/restoreFile?fileId=${id}&ownerId=${userId}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: 'include',
            });

            if (!response.ok) {
                throw new Error(`Failed to delete the file: ${response.statusText}`);
            }

            alert("File restored successfully");
        } catch (error) {
           alert("Error restoring file:"+ error);
        }
    }

    return (
        <div className="operate-file-menu-container">

            <i className="fa-solid fa-ellipsis more" style={{color: '#bcbdbd'}}
               ref={buttonRef} onClick={handleButtonClick}></i>


            {isMenuVisible && (
                <div ref={menuRef} className="menu-container">
                    <ul className="menu-list">


                        <li className="delete more"
                            onClick={() => {
                                handleDeleteFile(file.id, userId)
                            }}>
                            Delete
                        </li>
                        <li
                            className="restore more"
                            onClick={() => handleRestoreFile(file.id, userId)}>
                            Restore
                        </li>

                    </ul>
                </div>
            )}
        </div>
    );
};
export default OperateSpecificDeletedFile