
import React,{useState,useEffect} from "react";
import {useDispatch } from 'react-redux';
import {setUserInfo} from "../../components/Tool/UserInfo/userSlice";
import {useNavigate} from "react-router-dom";
import driveIcon from "../../assets/images/cloudversify-brands-solid.svg";
import './home.css';
import AddNewContextMenu     from "../../components/Button/AddNewContextMenu/AddNewContextMenu";
import OperateSpecificFile from "../../components/Button/OperateSpecificFile/OperateSpecificFile";
import Sidebar from "../../components/SideBar/SideBar";
import OperateSpecificFolder from "../../components/Button/OperateSpecificFolder/OperateSpecificFolder";
import apiRequest from "../../utils/api";


function Home() {
    const [files, setFiles] = useState([]);
    const [folders,setFolders]=useState([])
    const [actualUserId,setActualUserId] = useState(null);
    const [rootFolderId,setRootFolderId]=useState(null)
    const dispatch=useDispatch();
    const navigate =useNavigate();
    const [navigationPath, setNavigationPath] = useState([{ id: 1, name: 'root' }]);
    //get userid, restore it in localstorage
    const fetchUserId = async()=>{
        try {
            const response = await apiRequest(`${process.env.REACT_APP_API_URL}/getUserInfo`,
                {
                    method: 'GET',
                    credentials: 'include',
                });
            if (response.ok) {
                const userInfo = await response.json();
                console.log(userInfo+`haha`);
                dispatch(setUserInfo(userInfo));
                if (userInfo?.userId !== undefined && userInfo?.userName!==undefined) {
                    localStorage.setItem('userId', userInfo.userId);
                    localStorage.setItem(`${userInfo.userId}`,userInfo.userName);
                    setActualUserId(userInfo.userId);
                    console.log(`userId:`+localStorage.getItem('userId'));
                    console.log(`userName:`+localStorage.getItem(`${userInfo.userId}`));
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

    // get  folders and files list
    const fetchFoldersAndFiles = async (userId = null) => {
        try {
            const currentUserId = userId || actualUserId || localStorage.getItem('userId');

            if (!currentUserId) {
                console.error('No userId available for fetching folders and files');
                return;
            }


            // If rootFolderId is null, the API should return the user's root folder
            const endpoint = rootFolderId
                ? `${process.env.REACT_APP_API_URL}/getAllFiles?folderId=${rootFolderId}&ownerId=${currentUserId}`
                : `${process.env.REACT_APP_API_URL}/getRootFiles?ownerId=${currentUserId}`;

            const [fileResponse, folderResponse] = await Promise.all([
                apiRequest(endpoint, {
                    method: 'GET',
                    headers: { "Content-Type": "application/json" }
                }),
                apiRequest(rootFolderId
                    ? `${process.env.REACT_APP_API_URL}/getAllFolders?parentId=${rootFolderId}&userId=${currentUserId}`
                    : `${process.env.REACT_APP_API_URL}/getRootFolders?userId=${currentUserId}`, {
                    method: 'GET',
                    headers: { "Content-Type": "application/json" }
                })
            ]);

            if (fileResponse && folderResponse && fileResponse.ok && folderResponse.ok) {
                const [fileData, folderData] = await Promise.all([fileResponse.json(), folderResponse.json()]);

                // If this is the first load (rootFolderId is null), set the root folder ID
                if (rootFolderId === null && folderData.rootFolderId) {
                    setRootFolderId(folderData.rootFolderId);
                    localStorage.setItem('rootFolderId', folderData.rootFolderId);
                    setNavigationPath([{ id: folderData.rootFolderId, name: 'root' }]);
                }

                setFiles(Array.isArray(fileData) ? fileData : (fileData.files || []));
                setFolders(folderData.folders || folderData);
            }
        } catch (error) {
            console.error('Error fetching data:', error);
        }
    };

    useEffect(() => {
        fetchUserId();
    }, []);

    useEffect(() => {
        if (actualUserId) {
            fetchFoldersAndFiles();
        }
        if (actualUserId){
            const redirectUrl = localStorage.getItem('redirectAfterLogin');
            if (redirectUrl) {
                console.log(redirectUrl);
                localStorage.removeItem('redirectAfterLogin');
                navigate(redirectUrl);

            }
        }

    }, [rootFolderId, actualUserId]);


    const onFileUploadSuccess = () => {
        fetchFoldersAndFiles();  // refresh the file list
    };

    const getFileIcon = (fileType) => {
        if (fileType.startsWith("image/")) return "fa-regular fa-image";
        if (fileType.startsWith("video/")) return "fa-regular fa-file-video";
        if (fileType.startsWith("audio/")) return "fa-regular fa-file-audio";
        if (fileType === "application/pdf") return "fa-regular fa-file-pdf";
        if (fileType.includes("word")) return "fa-regular fa-file-word";
        if (fileType.includes("excel")) return "fa-regular fa-file-excel";
        if (fileType.includes("powerpoint")) return "fa-regular fa-file-powerpoint";
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
                                        <td>view</td>
                                        <td><OperateSpecificFolder folder={folder} userId={localStorage.getItem('userId')}/></td>
                                    </tr>
                                ))}
                                {files.map(file => (

                                    <tr key={file.id} className="file-data"   >
                                        <td  onClick={()=>handleFileClick(file.id)}><i className={getFileIcon(file.mimeType)} style={{color: fileIcons[getFileIcon(file.mimeType)]}}></i> {file.name}</td>
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