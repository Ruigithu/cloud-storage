import Sidebar from "../../SideBar";
import React, {useEffect, useState} from "react";
import driveIcon from "../../../../assets/images/cloudversify-brands-solid.svg";
import "./Bin.css"
import OperateSpecificDeletedFolder from "./OperateSpecificDeletedFolder/OperateSpecificDeletedFolder";
import OperateSpecificDeletedFile from "./OperateSpecificDeletedFile/OperateSpecificDeletedFile";
import {useNavigate} from "react-router-dom";
import apiRequest from "../../../../utils/api";

function Bin(){
    const [files, setFiles] = useState([]);
    const [folders, setFolders] = useState([]);
    const actualUserId = localStorage.getItem('userId');
    const [rootFolderId, setRootFolderId] = useState(localStorage.getItem('rootFolderId'));
    const [navigationPath, setNavigationPath] = useState([{ id: localStorage.getItem('rootFolderId'), name: 'root' }]);
    const navigate = useNavigate();

    const formatFileSize = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    const fetchFoldersAndFiles =useCallback( async () => {
        try {
            const [fileResponse, folderResponse] = await Promise.all([
                apiRequest(`${process.env.REACT_APP_API_URL}/getAllDeletedFiles?folderId=${rootFolderId}&ownerId=${localStorage.getItem('userId')}`, {
                    method: 'GET',
                    headers: { "Content-Type": "application/json" },
                    credentials: 'include',
                }),
                apiRequest(`${process.env.REACT_APP_API_URL}/getAllDeletedFolders?parentId=${rootFolderId}&userId=${localStorage.getItem('userId')}`, {
                    method: 'GET',
                    headers: { "Content-Type": "application/json" },
                    credentials: 'include',
                })
            ]);

            if (fileResponse.ok && folderResponse.ok) {
                const [fileData, folderData] = await Promise.all([fileResponse.json(), folderResponse.json()]);

                const seenIds = new Map();

                // Process folders first
                const uniqueFolders = folderData.filter(folder => {

                    // Check if we've seen this ID before
                    if (seenIds.has(folder.id)) {
                        console.warn(`Duplicate folder ID found: ${folder.id}`);
                        return false;
                    }

                    // Mark this ID as seen
                    seenIds.set(folder.id, true);
                    return true;
                });

                // Process files
                const uniqueFiles = fileData.filter(file => {

                    // Check if we've seen this ID before
                    if (seenIds.has(file.id)) {
                        console.warn(`Duplicate file ID found: ${file.id}`);
                        return false;
                    }

                    // Mark this ID as seen
                    seenIds.set(file.id, true);
                    return true;
                });

                setFiles(uniqueFiles);
                setFolders(uniqueFolders);
            }
        } catch (error) {
            console.error('Error fetching data:', error);
        }
    },[]);

    useEffect(() => {
        if (actualUserId) {
            fetchFoldersAndFiles();
        }
    }, [rootFolderId, actualUserId,fetchFoldersAndFiles]);

    const getFileIcon = (fileType) => {
        if (fileType.startsWith("image/")) return "fa-regular fa-image"; // img
        if (fileType.startsWith("video/")) return "fa-regular fa-file-video"; // video
        if (fileType.startsWith("audio/")) return "fa-regular fa-file-audio"; // audio
        if (fileType === "application/pdf") return "fa-regular fa-file-pdf"; // PDF
        if (fileType.includes("word")) return "fa-regular fa-file-word"; // Word
        if (fileType.includes("excel")) return "fa-regular fa-file-excel"; // Excel
        if (fileType.includes("powerpoint")) return "fa-regular fa-file-powerpoint"; // PPT
        return "fa-regular fa-file";
    };

    const fileIcons = {
        "fa-regular fa-image": "#007cdb",  // blue（img）
        "fa-regular fa-file-video":"#ff4500", // orange（video）
        "fa-regular fa-file-audio":"#32cd32", // green（audio）
        "fa-regular fa-file-pdf":"#ff0000", // red（PDF）
        "fa-regular fa-file-word":"#2b579a", // dark blue（Word）
        "fa-regular fa-file-excel":"#217346", // green（Excel）
        "fa-regular fa-file-powerpoint": "#d24726", // dark orange（PPT）
        "fa-regular fa-file":"#808080", // gray（default）
    };

    const handleFolderClick = (folderId, folderName) => {
        setRootFolderId(folderId);
        setNavigationPath(prev => [...prev, { id: folderId, name: folderName }]);
    };

    const handleFileClick = (fileId) => {
        navigate(`/editor/${fileId}`);
    };

    const handleBackward = () => {
        if (navigationPath.length > 1) {
            const newPath = navigationPath.slice(0, -1);
            setNavigationPath(newPath);
            setRootFolderId(newPath[newPath.length - 1].id);
        }
    };

    const handlePathClick = (index) => {
        const newPath = navigationPath.slice(0, index + 1);
        setNavigationPath(newPath);
        setRootFolderId(newPath[newPath.length - 1].id);
    };

    // Function to prevent event propagation to parent
    const handleOperationClick = (e) => {
        e.stopPropagation();
    };

    return (
        <div className="container">
            <header>
                <img src={driveIcon} alt="drive-icon" className="drive-icon"/>
                <div className="search-bar">
                    <label className="search-bar">
                        <input className="search-box" placeholder="search in the drive" size="50"/>`
                    </label>
                </div>
            </header>

            <div className="home-main-content">
                <nav>
                    <Sidebar></Sidebar>
                </nav>

                <div className="main-content">
                    <div className="first-row">
                        <div className="folder-path">
                            <i
                                className="fa-solid fa-backward"
                                style={{
                                    color: "#616365",
                                    cursor: navigationPath.length > 1 ? 'pointer' : 'not-allowed',
                                    opacity: navigationPath.length > 1 ? 1 : 0.5,
                                    marginRight:'20px'
                                }}
                                onClick={handleBackward}
                            ></i>
                            <div className="folder-path">
                                {navigationPath.map((folder, index) => (
                                    <span key={folder.id}>
                                        <span
                                            onClick={() => handlePathClick(index)}
                                            style={{ cursor: 'pointer', color: '#3d3e40' }}
                                        >
                                            {folder.name}
                                        </span>
                                        {index < navigationPath.length - 1 && <span style={{ margin: '0 8px' }}>&gt;</span>}
                                    </span>
                                ))}
                            </div>
                        </div>

                    </div>
                    <div className="file-list">
                        <table className="file-table">
                            <thead>
                            <tr className="file-header">
                                <th>Name    <i className="fa-solid fa-sort"></i></th>
                                <th>DeletedAt    <i className="fa-solid fa-sort"></i></th>
                                <th>Size</th>
                                <th> </th>
                            </tr>
                            </thead>
                            <tbody>
                            {folders.map(folder => (
                                <tr key={`folder-${folder.id}`} className="file-data" onClick={() => handleFolderClick(folder.id, folder.name)}>
                                    <td><i className="fa-solid fa-folder" style={{color: "#ffd129"}}></i> {folder.name}
                                    </td>
                                    <td>{folder.updatedAt}</td>
                                    <td></td>
                                    <td onClick={handleOperationClick}>
                                        <OperateSpecificDeletedFolder folder={folder} userId={actualUserId}/>
                                    </td>
                                </tr>
                            ))}
                            {files
                                .filter(file => String(file.folderId) === String(rootFolderId))
                                .map(file => (
                                <tr key={`file-${file.id}`} className="file-data" onClick={() => handleFileClick(file.id)}>
                                    <td><i className={getFileIcon(file.mimeType)} style={{color: fileIcons[getFileIcon(file.mimeType)]}}></i> {file.name}</td>
                                    <td>{file.updatedAt}</td>
                                    <td>{file.size ? formatFileSize(file.size) : '-'}</td>
                                    <td onClick={handleOperationClick}>
                                        <OperateSpecificDeletedFile file={file} userId={actualUserId}/>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {files.length === 0 && folders.length === 0 && (
                            <div className="no-files">No deleted files or folders found</div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}

export default Bin;