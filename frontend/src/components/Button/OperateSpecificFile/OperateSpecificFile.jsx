import React, {useRef, useState} from "react";
import "./OperateSpecificFile.css"


const OperateSpecificFile = ({file,userId}) => {
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
    const handleDownload = (file) => {
        console.log(`fileId 是：${file.id}`);
        const downloadUrl = `/download?fileId=${file.id}`;
        const fetchFiles = async () => {
            try {
                const response = await fetch(`http://localhost:8080${downloadUrl}`, {
                    method: 'GET',
                    credentials: 'include', // 如果需要带上 cookie
                });

                if (response.ok) {
                    // 获取文件内容（作为 Blob 对象）
                    const blob = await response.blob();

                    // 创建一个 URL 来表示 Blob 对象
                    const url = window.URL.createObjectURL(blob);

                    // 创建一个临时链接并模拟点击下载
                    const link = document.createElement('a');
                    link.href = url;
                    link.setAttribute('download', file.name); // 设置下载的文件名
                    document.body.appendChild(link);
                    link.click(); // 模拟点击链接

                    // 释放 URL 对象
                    window.URL.revokeObjectURL(url);
                } else {
                    console.error('文件下载失败');
                }
            } catch (error) {
                console.error('Error fetching file:', error);
            }
        };

        fetchFiles(); // 调用下载文件的函数
    };

    React.useEffect(() => {
        document.addEventListener("click", handleClickOutside);
        return () => {
            document.removeEventListener("click", handleClickOutside);
        };
    }, []);

    async function handleDeleteFile(fileId, userId) {
        try {
            const response = await fetch(`http://localhost:8080/softDeleteFile?fileId=${fileId}&userId=${userId}`, {
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
            {/* 触发按钮 */}
            <i className="fa-solid fa-ellipsis more" style={{color: '#bcbdbd'}}
               ref={buttonRef} onClick={handleButtonClick}></i>

            {/* 下拉菜单 */}
            {isMenuVisible && (
                <div ref={menuRef} className="menu-container">
                    <ul className="menu-list">
                        <li className="rename more"
                            onClick={() => alert("Rename Selected")}>
                            Rename
                        </li>
                        <li className="download more" onClick={() => handleDownload(file)}>
                            Download
                        </li>
                        <li className="delete more"
                            onClick={() =>{handleDeleteFile(file.id,userId)} }>
                            Delete
                        </li>
                        <li className="share more"
                            onClick={() => alert("Share Selected")}>
                            Share
                        </li>
                    </ul>
                </div>
            )}
        </div>
    );
};
export default OperateSpecificFile