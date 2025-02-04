import OperateSpecificFile from "../../../Button/OperateSpecificFile/OperateSpecificFile";
import Sidebar from "../../SideBar";
import AddNewContextMenu from "../../../Button/AddNewContextMenu/AddNewContextMenu";
import {useEffect,useState} from "react";
import driveIcon from "../../../../assets/images/cloudversify-brands-solid.svg";

function MyShared(){
    const [files, setFiles] = useState([]);
    const actualUserId = 3;

    // 获取文件列表
    const fetchFiles = async () => {
        try {
            const response = await fetch((`http://localhost:8080/getAllFiles?userId=${actualUserId}`),{
                method:'GET',
                credentials: 'include',
            });
            if (response.ok) {
                const data = await response.json();
                setFiles(data);
            }
        } catch (error) {
            console.error('Error fetching files:', error);
        }
    };
    const formatFileSize = (bytes) => {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    };

    // 组件挂载时获取文件列表
    useEffect(() => {
        fetchFiles();
    }, []);

    // 文件上传成功后的回调函数
    const onFileUploadSuccess = () => {
        fetchFiles();  // 上传成功后刷新文件列表
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
                <AddNewContextMenu onFileUploadSuccess={onFileUploadSuccess}/>
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
                    <div className="main-button">
                        <button type="button">Change View</button>
                    </div>
                    <div className="file-list">
                        <table className="file-table">
                            <thead>
                            <tr className="file-header">
                                <th>Name</th>
                                <th>Last Modified</th>
                                <th>Size</th>
                                <th> </th>
                            </tr>
                            </thead>
                            <tbody>
                            {files.map(file => (  // files 是你的文件数组
                                <tr key={file.fileid} className="file-data" >
                                    <td>{file.filename}</td>
                                    <td>{file.contenttype}</td>
                                    <td>{formatFileSize(file.filesize)}</td>
                                    <td><OperateSpecificFile file={file} /></td>
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
export default MyShared