import driveIcon from "../../assets/images/cloudversify-brands-solid.svg";
import React,{useState,useEffect} from "react";
import './home.css';
import AddNewContextMenu     from "../../components/Button/AddNewContextMenu/AddNewContextMenu";
import OperateSpecificFile from "../../components/Button/OperateSpecificFile/OperateSpecificFile";
import Sidebar from "../../components/SideBar/SideBar";
import { useDispatch } from 'react-redux';
import {setUserInfo} from "../../components/Tool/UserInfo/userSlice";
import OperateSpecificFolder from "../../components/Button/OperateSpecificFolder/OperateSpecificFolder";

function Home() {
    const [files, setFiles] = useState([]);
    const [folders,setFolders]=useState([])
    const [actualUserId,setActualUserId] = useState(null);
    const [rootFolderId,setRootFolderId]=useState(1)
    const dispatch=useDispatch();
    const [navigationPath, setNavigationPath] = useState([{ id: 1, name: 'root' }]);
    //获取用户id 存到localstorage
    const fetchUserId = async()=>{
        try {
            const response = await fetch(`http://localhost:8080/getUserInfo`,
                {
                    method: 'GET',
                    credentials: 'include',
                });
            if (response.ok) {
                const userInfo = await response.json();
                console.log(userInfo);
                // 存储到 Redux
                dispatch(setUserInfo(userInfo));
                // 只存储 ID 到 localStorage（避免 undefined）
                if (userInfo?.userId !== undefined) {
                    localStorage.setItem('userId', userInfo.userId);
                    setActualUserId(userInfo.userId);
                    console.log(`userId:`+localStorage.getItem('userId'));
                }
            }
        }catch (error){
            console.error('Failed to fetch user info:', error);
        }
    }

    const formatFileSize = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    // 获取文件夹列表
    const fetchFoldersAndFiles = async () => {

        try {
            const [fileResponse, folderResponse] = await Promise.all([
                fetch(`http://localhost:8080/getAllFiles?folderId=${rootFolderId}&ownerId=${localStorage.getItem('userId')}`, {
                    method: 'GET',
                    headers: { "Content-Type": "application/json" },
                    credentials: 'include',
                }),
                fetch(`http://localhost:8080/getAllFolders?parentId=${rootFolderId}&userId=${localStorage.getItem('userId')}`, {
                    method: 'GET',
                    headers: { "Content-Type": "application/json" },
                    credentials: 'include',
                })
            ]);

            if (fileResponse.ok && folderResponse.ok) {
                const [fileData, folderData] = await Promise.all([fileResponse.json(), folderResponse.json()]);
                setFiles(fileData);
                setFolders(folderData);
            }
        } catch (error) {
            console.error('Error fetching data:', error);
        }
    };


    // 组件挂载时获取文件列表
    useEffect(() => {
        fetchUserId();
    }, []);

    useEffect(() => {
        if (actualUserId) {
            fetchFoldersAndFiles();
        }
    }, [rootFolderId, actualUserId]);
    // 文件上传成功后的回调函数
    const onFileUploadSuccess = () => {
        fetchFoldersAndFiles();  // 上传成功后刷新文件列表
    };

    const getFileIcon = (fileType) => {
        if (fileType.startsWith("image/")) return "fa-regular fa-image"; // 图片
        if (fileType.startsWith("video/")) return "fa-regular fa-file-video"; // 视频
        if (fileType.startsWith("audio/")) return "fa-regular fa-file-audio"; // 音频
        if (fileType === "application/pdf") return "fa-regular fa-file-pdf"; // PDF
        if (fileType.includes("word")) return "fa-regular fa-file-word"; // Word 文档
        if (fileType.includes("excel")) return "fa-regular fa-file-excel"; // Excel 文件
        if (fileType.includes("powerpoint")) return "fa-regular fa-file-powerpoint"; // PPT
        return "fa-regular fa-file";
    };
    const fileIcons = {
        "fa-regular fa-image": "#007cdb",  // 蓝色（图片）
        "fa-regular fa-file-video":"#ff4500", // 橙色（视频）
        "fa-regular fa-file-audio":"#32cd32", // 绿色（音频）
        "fa-regular fa-file-pdf":"#ff0000", // 红色（PDF）
        "fa-regular fa-file-word":"#2b579a", // 深蓝色（Word）
        "fa-regular fa-file-excel":"#217346", // 绿色（Excel）
        "fa-regular fa-file-powerpoint": "#d24726", // 深橙色（PPT）
        "fa-regular fa-file":"#808080", // 灰色（默认）
};

    const handleFolderClick = (folderId, folderName) => {
        setRootFolderId(folderId);
        setNavigationPath(prev => [...prev, { id: folderId, name: folderName }]);
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




    return (
        <div className="container">
            <header>
                <img src={driveIcon} alt="drive-icon" className="drive-icon"/>
                <div className="search-bar">
                    <label className="search-bar">
                        <input className="search-box" placeholder=" 🔍search in the drive" size="50"/>
                    </label>
                </div>
                <AddNewContextMenu onFileUploadSuccess={onFileUploadSuccess} parentId={rootFolderId} userId={localStorage.getItem('userId')}/>
                {/*<form onSubmit={handleUpload}>*/}
                {/*    <label className="file-upload-btn">*/}
                {/*        + Add New*/}
                {/*        <input type="file" style={{display: 'none'}}/>*/}
                {/*    </label>*/}
                {/*</form>*/}
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
                        <div className="main-button">
                            <button type="button">Change View</button>
                        </div>

                    </div>
                    <div className="file-list">
                        <table className="file-table">
                                <thead>
                                <tr className="file-header">
                                    <th>Name    <i className="fa-solid fa-sort"></i></th>
                                    <th>Last Modified    <i className="fa-solid fa-sort"></i></th>
                                    <th>Size</th>
                                    <th> </th>
                                </tr>
                                </thead>
                                <tbody>
                                {folders.map(folder => (
                                    <tr key={folder.id} className="file-data" onClick={() => handleFolderClick(folder.id,folder.name)}>
                                        <td><i className="fa-solid fa-folder" style={{color: "#ffd129"}}></i> {folder.name}
                                        </td>
                                        <td>{folder.updatedAt}</td>
                                        <td>{folder.id}</td>
                                        <td><OperateSpecificFolder folder={folder} userId={localStorage.getItem('userId')}/></td>
                                    </tr>
                                ))}
                                {files.map(file => (
                                    <tr key={file.id} className="file-data">
                                        <td><i className={getFileIcon(file.mimeType)} style={{color: fileIcons[getFileIcon(file.mimeType)]}}></i> {file.name}</td>
                                        <td>{file.updatedAt}</td>
                                        <td>{formatFileSize(file.size)}</td>
                                        <td><OperateSpecificFile file={file} userId={localStorage.getItem('userId')}/></td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                            {files.length === 0 && (
                                <div className="no-files">No files found</div>
                            )}
                    </div>

                </div>
            </div>
        </div>

    );
}

export default Home;