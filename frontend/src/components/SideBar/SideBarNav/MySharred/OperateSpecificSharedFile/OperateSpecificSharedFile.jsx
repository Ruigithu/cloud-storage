import React, {useRef, useState} from "react";
import "./OperateSpecificSharedFile.css"


const OperateSpecificSharedFile = ({shareId,userId,active,refresh}) => {
    const [isMenuVisible, setIsMenuVisible] = useState(false);
    const menuRef = useRef(null);
    const buttonRef = useRef(null);



    // 点击按钮显示/隐藏菜单
    const handleButtonClick = (e) => {
        e.stopPropagation(); // 阻止事件冒泡
        console.log("Button clicked");
        setIsMenuVisible(!isMenuVisible);
    };

    // 点击外部关闭菜单
    const handleClickOutside = (e) => {
        console.log("Clicked outside:", e.target);
        console.log("Button ref:", buttonRef.current);
        console.log("Menu ref:", menuRef.current);

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


    async function handleCancelShare(id, userId) {
        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/cancelShare?shareId=${id}&ownerId=${userId}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: 'include',
            });

            if (!response.ok) {
                throw new Error(`Failed to delete the file: ${response.statusText}`);
            }

            alert("Share canceled successfully");
        } catch (error) {
           alert("Error cancelling share:"+ error);
        }
    }
    async function handleRestore(id, userId) {
        try {
            const response = await fetch(`${process.env.REACT_APP_API_URL}/restore?shareId=${id}&ownerId=${userId}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                credentials: 'include',
            });

            if (!response.ok) {
                throw new Error(`Failed to restore the file: ${response.statusText}`);

            }

            alert("Share restored successfully");
        } catch (error) {
            alert("Error restoring file:"+ error);
        }
    }

    return (
        <div className="operate-file-menu-container">
            {/* 触发按钮 */}
            <i className="fa-solid fa-ellipsis more" style={{color: '#bcbdbd'}}
               ref={buttonRef} onClick={handleButtonClick}></i>

            {/* 下拉菜单 */}
            {isMenuVisible && (
                <div ref={menuRef} className="menu-container">
                    <ul className="menu-list">
                        {active ?
                            <li
                                className="restore more"
                                onClick={() => handleCancelShare(shareId, userId)}>
                                Cancel Share
                            </li> :
                            <li
                                className="restore more"
                                onClick={() => handleRestore(shareId, userId)}>
                                Restore
                            </li>
                        }


                    </ul>
                </div>
            )}
        </div>
    );
};
export default OperateSpecificSharedFile