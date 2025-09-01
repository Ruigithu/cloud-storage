import Sidebar from "../../SideBar";
import React, {useCallback, useEffect, useState} from "react";
import driveIcon from "../../../../assets/images/cloudversify-brands-solid.svg";
import "./MyShared.css";
import OperateSpecificSharedFile from "./OperateSpecificSharedFile/OperateSpecificSharedFile";
import apiRequest from "../../../../utils/api";

function MyShared(){
    const [shares, setShares] = useState([]);
    const actualUserId = localStorage.getItem('userId');

    // 获取文件夹列表
    const fetchSharedFiles =useCallback( async () => {
        try {
            const response = await apiRequest(
                `${process.env.REACT_APP_API_URL}/getAllSharedFiles?ownerId=${actualUserId}`,
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
    },[]);

    useEffect(() => {
        if (actualUserId) {
            fetchSharedFiles();
        }
    }, [actualUserId,fetchSharedFiles]);


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
                                <th>FileName <i className="fa-solid fa-sort"></i></th>
                                <th>ShareLink <i className="fa-solid fa-sort"></i></th>
                                <th>Expires At <i className="fa-solid fa-sort"></i></th>
                                <th>Authorization</th>
                                <th></th>
                            </tr>
                            </thead>
                            <tbody>
                            {shares && shares.map(share => (
                                <tr key={share.id} className="share-data">
                                    <td>{share.fileName}</td>
                                    <td>{share.shareLink}</td>
                                    <td>
                                        {share.active ?
                                            share.expiresAt :
                                            <span style={{ color: 'red' }}>canceled</span>
                                        }
                                    </td>
                                    <td>{share.type}</td>

                                    <td>
                                        <OperateSpecificSharedFile shareId ={share.id} userId={actualUserId} active={share.active} refresh={fetchSharedFiles}/>
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