import Sidebar from "../../SideBar";
import React, {useEffect, useState} from "react";
import driveIcon from "../../../../assets/images/cloudversify-brands-solid.svg";
import "./MyShared.css";
import OperateSpecificSharedFile from "./OperateSpecificSharedFile/OperateSpecificSharedFile";

function MyShared(){
    const [shares, setShares] = useState([]);
    const actualUserId = localStorage.getItem('userId');

    // 获取文件夹列表
    const fetchSharedFiles = async () => {
        try {
            const response = await fetch(
                `http://localhost:8080/getAllSharedFiles?ownerId=${actualUserId}`,
                {
                    method: 'GET',
                    headers: { "Content-Type": "application/json" },
                    credentials: 'include',
                }
            );

            if (response.ok) {
                const fileData = await response.json();
                setShares(fileData);
            }
        } catch (error) {
            console.error('Error fetching data:', error);
        }
    };

    useEffect(() => {
        if (actualUserId) {
            fetchSharedFiles();
        }
    }, [actualUserId]);

    // 移除未使用的 onFileUploadSuccess 函数，除非你在其他地方需要它
    // 移除未使用的 getFileIcon 和 fileIcons，除非你在其他地方需要它们

    return (
        <div className="container">
            <header>
                <img src={driveIcon} alt="drive-icon" className="drive-icon"/>
                <div className="search-bar">
                    <label className="search-bar">
                        <input className="search-box" placeholder="search in the drive" size="50"/>
                    </label>
                </div>
            </header>

            <div className="home-main-content">
                <nav>
                    <Sidebar />
                </nav>

                <div className="main-content">
                    <div className="file-list">
                        <table className="file-table">
                            <thead>
                            <tr className="file-header">
                                <th>Name    <i className="fa-solid fa-sort"></i></th>
                                <th>Expires At   <i className="fa-solid fa-sort"></i></th>
                                <th>Size</th>
                                <th> </th>
                            </tr>
                            </thead>
                            <tbody>
                            {shares && shares.map(share => (
                                <tr key={share.id} className="share-data">
                                    <td>{share.id}</td>
                                    <td>{share.expiresAt}</td>
                                    <td>{share.accessType}</td>
                                    <td>
                                        <OperateSpecificSharedFile userId={actualUserId}/>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {(!shares || shares.length === 0) && (
                            <div className="no-files">No files found</div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default MyShared;