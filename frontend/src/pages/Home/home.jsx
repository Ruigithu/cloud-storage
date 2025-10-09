import React, { useState, useEffect, useCallback } from "react";

import { useNavigate } from "react-router-dom";
import './home.css';

import Header from '../../components/Layout/Header';
import Sidebar from "../../components/SideBar/SideBar";
import FolderPath from '../../components/Navigation/FolderPath';
import FolderRow from '../../components/FileList/FolderRow';
import FileRow from '../../components/FileList/FileRow';
import OperateSpecificFile from "../../components/Button/OperateSpecificFile/OperateSpecificFile";
import OperateSpecificFolder from "../../components/Button/OperateSpecificFolder/OperateSpecificFolder";

import { useFileNavigation } from '../../hooks/useFileNavigation';
import {getFilesAndFolders} from "../../services/fileService";
import {getUserInfo} from "../../services/authService";

function Home() {
    const [files, setFiles] = useState([]);
    const [folders, setFolders] = useState([]);
    const [actualUserId, setActualUserId] = useState(null);
    const navigate = useNavigate();

    const {
        rootFolderId,
        setRootFolderId,
        navigationPath,
        setNavigationPath,
        handleFolderClick,
        handleBackward,
        handlePathClick
    } = useFileNavigation(null, 'root');

    useEffect(() => {
        const fetchUserId = async () => {
            try {
                const userInfo = await getUserInfo();

                if (userInfo?.userId && userInfo?.userName) {
                    localStorage.setItem('userId', userInfo.userId);
                    localStorage.setItem(`${userInfo.userId}`, userInfo.userName);
                    setActualUserId(userInfo.userId);
                }
            } catch (error) {
                console.error('Failed to fetch user info:', error);
            }
        };

        fetchUserId();
    }, []);

    const fetchFoldersAndFiles = useCallback(async (userId = null) => {
        try {
            const currentUserId = userId || actualUserId || localStorage.getItem('userId');

            if (!currentUserId) {
                console.error('No userId available');
                return;
            }

            const data = await getFilesAndFolders(rootFolderId, currentUserId);

            if (rootFolderId === null && data.rootFolderId) {
                setRootFolderId(data.rootFolderId);
                localStorage.setItem('rootFolderId', data.rootFolderId);
                setNavigationPath([{ id: data.rootFolderId, name: 'root' }]);
            }

            setFiles(data.files);
            setFolders(data.folders);
        } catch (error) {
            console.error('Error fetching data:', error);
        }
    }, [actualUserId, rootFolderId, setRootFolderId, setNavigationPath]);

    useEffect(() => {
        if (actualUserId) {
            fetchFoldersAndFiles();
        }
    }, [rootFolderId, actualUserId, fetchFoldersAndFiles]);

    const onFileUploadSuccess = () => {
        fetchFoldersAndFiles();
    };

    const handleFileClick = (fileId) => {
        navigate(`/editor/${fileId}`);
    };

    return (
        <div className="container">
            <Header
                showAddNew={true}
                onFileUploadSuccess={onFileUploadSuccess}
                parentId={rootFolderId}
                userId={localStorage.getItem('userId')}
            />

            <div className="home-main-content">
                <nav>
                    <Sidebar />
                </nav>

                <div className="main-content">
                    <FolderPath
                        navigationPath={navigationPath}
                        onBackward={handleBackward}
                        onPathClick={handlePathClick}
                    />

                    <div className="file-list">
                        <table className="file-table">
                            <thead>
                            <tr className="file-header">
                                <th>Name <i className="fa-solid fa-sort"></i></th>
                                <th>Last Modified <i className="fa-solid fa-sort"></i></th>
                                <th>Size</th>
                                <th> </th>
                            </tr>
                            </thead>
                            <tbody>
                            {folders.map(folder => (
                                <FolderRow
                                    key={folder.id}
                                    folder={folder}
                                    onClick={handleFolderClick}
                                    renderActions={(folder, userId) => (
                                        <OperateSpecificFolder folder={folder} userId={userId} />
                                    )}
                                    userId={localStorage.getItem('userId')}
                                />
                            ))}
                            {files.map(file => (
                                <FileRow
                                    key={file.id}
                                    file={file}
                                    onClick={handleFileClick}
                                    renderActions={(file, userId) => (
                                        <OperateSpecificFile file={file} userId={userId} />
                                    )}
                                    userId={localStorage.getItem('userId')}
                                />
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