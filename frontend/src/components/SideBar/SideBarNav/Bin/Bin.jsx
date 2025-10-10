import Sidebar from "../../SideBar";
import React, {useCallback, useEffect, useState} from "react";
import "./Bin.css"
import OperateSpecificDeletedFolder from "./OperateSpecificDeletedFolder/OperateSpecificDeletedFolder";
import OperateSpecificDeletedFile from "./OperateSpecificDeletedFile/OperateSpecificDeletedFile";
import {useNavigate} from "react-router-dom";
import {useFileNavigation} from "../../../../hooks/useFileNavigation";
import Header from "../../../Layout/Header";
import FolderPath from "../../../Navigation/FolderPath";
import FolderRow from "../../../FileList/FolderRow";
import FileRow from "../../../FileList/FileRow";
import {getDeletedFilesAndFolders} from "../../../../services/binService";

function Bin() {
    const navigate = useNavigate();

    const userId = localStorage.getItem('userId');


    useEffect(() => {
        if (!userId) {
            console.warn('User not logged in, redirecting to login page');
            navigate('/login', { replace: true });
        }
    }, [userId, navigate]);

    const [files, setFiles] = useState([]);
    const [folders, setFolders] = useState([]);

    const {
        rootFolderId,
        navigationPath,
        handleFolderClick,
        handleBackward,
        handlePathClick
    } = useFileNavigation(
        localStorage.getItem('rootFolderId'),
        'root'
    );

    const fetchDeletedFoldersAndFiles = useCallback(async () => {
        if (!userId) return;

        try {
            const data = await getDeletedFilesAndFolders(rootFolderId, userId);
            setFiles(data.files);
            setFolders(data.folders);
        } catch (error) {
            console.error('Error fetching deleted data:', error);

            if (error.message?.includes('401') || error.message?.includes('Unauthorized')) {
                navigate('/login', { replace: true });
            }
        }
    }, [userId, rootFolderId, navigate]);

    useEffect(() => {
        if (userId) {
            fetchDeletedFoldersAndFiles();
        }
    }, [rootFolderId, userId, fetchDeletedFoldersAndFiles]);

    const handleOperationClick = (e) => {
        e.stopPropagation();
    };

    if (!userId) {
        return null;
    }

    return (
        <div className="container">
            <Header showAddNew={false} />

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
                                <th>Deleted At <i className="fa-solid fa-sort"></i></th>
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
                                        <div onClick={handleOperationClick}>
                                            <OperateSpecificDeletedFolder
                                                folder={folder}
                                                userId={userId}
                                            />
                                        </div>
                                    )}
                                    userId={userId}
                                />
                            ))}
                            {files
                                .filter(file => String(file.folderId) === String(rootFolderId))
                                .map(file => (
                                    <FileRow
                                        key={file.id}
                                        file={file}
                                        renderActions={(file, userId) => (
                                            <div onClick={handleOperationClick}>
                                                <OperateSpecificDeletedFile
                                                    file={file}
                                                    userId={userId}
                                                />
                                            </div>
                                        )}
                                        userId={userId}
                                    />
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