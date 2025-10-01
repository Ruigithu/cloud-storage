import React, {useRef, useState} from "react";
import "./OperateSpecificFile.css"
import ShareFileDialog from "../ShareFileDialog/ShareFileDialog";
import apiRequest from "../../../utils/apiRequest";


const OperateSpecificFile = ({file,userId}) => {
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
    const handleDownload = (file) => {
        const downloadUrl = `/download?fileId=${file.id}`;
        const fetchFiles = async () => {
            try {
                const response =await apiRequest(`${process.env.REACT_APP_API_URL}${downloadUrl}`, {
                    method: 'GET',
                    credentials: 'include',
                });

                if (response.ok) {
                    const blob = await response.blob();
                    const url = window.URL.createObjectURL(blob);
                    const link = document.createElement('a');

                    link.href = url;
                    link.setAttribute('download', file.name);
                    document.body.appendChild(link);
                    link.click();

                    window.URL.revokeObjectURL(url);
                } else {
                    console.error('fail downloading');
                }
            } catch (error) {
                console.error('Error fetching file:', error);
            }
        };

        fetchFiles();
    };

    React.useEffect(() => {
        document.addEventListener("click", handleClickOutside);
        return () => {
            document.removeEventListener("click", handleClickOutside);
        };
    }, []);

    async function handleDeleteFile(fileId, userId) {
        try {
            const response =await apiRequest(`${process.env.REACT_APP_API_URL}/softDeleteFile?fileId=${fileId}&userId=${userId}`, {
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

    return (
        <div className="operate-file-menu-container">
            <i className="fa-solid fa-ellipsis more" style={{color: '#bcbdbd'}}
               ref={buttonRef} onClick={handleButtonClick}></i>

            {isMenuVisible && (
                <div ref={menuRef} className="menu-container">
                    <ul className="menu-list">
                        <li className="download more" onClick={() => handleDownload(file)}>
                            Download
                        </li>
                        <li className="delete more"
                            onClick={() =>{handleDeleteFile(file.id,userId)} }>
                            Delete
                        </li>
                        <li className="share more">
                            <ShareFileDialog fileId={file.id}/>
                        </li>
                    </ul>
                </div>
            )}
        </div>
    );
};
export default OperateSpecificFile