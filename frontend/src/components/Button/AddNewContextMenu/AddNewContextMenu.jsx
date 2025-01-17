import React, { useState, useRef } from "react";
import "./AddNewContextMenu.css";
import UploadFile from "../UploadFile/UploadFile";

    const AddNewContextMenu = ({ buttonLabel = "+ Add New" , onFileUploadSuccess }) => {
        const [isMenuVisible, setIsMenuVisible] = useState(false);
        const menuRef = useRef(null);
        const buttonRef = useRef(null);



        // 点击按钮显示/隐藏菜单
        const handleButtonClick = () => {
            setIsMenuVisible(!isMenuVisible);
        };

        // 点击外部关闭菜单
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

        return (
            <div className="add-new-menu-container">
                {/* 触发按钮 */}
                <button
                    ref={buttonRef}
                    className="add-new-button"
                    onClick={handleButtonClick}
                >
                    {buttonLabel}
                </button>

                {/* 下拉菜单 */}
                {isMenuVisible && (
                    <div ref={menuRef} className="menu-container">
                    <ul className="menu-list">
                        <li className="create-new">
                            <UploadFile onFileUploadSuccess={onFileUploadSuccess}/>
                        </li>
                        <li className="create-new"
                            onClick={() => alert("Create Folder Selected")}>
                            Create Folder
                        </li>
                        <li className="create-new"
                            onClick={() => alert("Upload Folder Selected")}>
                            Upload Folder
                        </li>
                    </ul>
                    </div>
                )}
            </div>
        );
    };


    export default AddNewContextMenu;
