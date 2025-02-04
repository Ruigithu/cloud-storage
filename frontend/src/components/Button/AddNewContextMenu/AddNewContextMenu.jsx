import React, { useState, useRef } from "react";
import "./AddNewContextMenu.css";
import UploadFile from "../UploadFile/UploadFile";
import CreateFolder from "../CreateFolder/CreateFolder";
import UploadFolder from "../UploadFolder/UploadFolder";

    const AddNewContextMenu = ({ buttonLabel = "+ Add New" , onFileUploadSuccess ,parentId,userId}) => {
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
                            <UploadFile onFileUploadSuccess={onFileUploadSuccess} folderId={parentId} ownerId={userId}/>
                        </li>
                        <li className="create-new">
                            <CreateFolder onFileUploadSuccess={onFileUploadSuccess} parentId={parentId} userId={userId}/>
                        </li>
                        <li className="create-new">
                            <UploadFolder  onFileUploadSuccess={onFileUploadSuccess} parentId={parentId} userId={userId}/>
                        </li>
                    </ul>
                    </div>
                )}
            </div>
        );
    };


    export default AddNewContextMenu;
