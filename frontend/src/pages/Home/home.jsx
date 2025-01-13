import driveIcon from "../../assets/images/cloudversify-brands-solid.svg";
import React from "react";
import './home.css';

function Home() {
    // 你现在 App.jsx 中的内容移到这里

    const files = [
        { name: 'Document 1.docx', type: 'document', lastModified: '2024-01-12', size: '1.2 MB' },
        { name: 'Image.jpg', type: 'image', lastModified: '2024-01-11', size: '2.5 MB' },
        // 添加更多示例文件
    ];

    return (
        <div className="container">


            <nav className="aside-nav">
                <img src={driveIcon} alt="drive-icon" className="drive-icon"/>
                <div className="aside-button">My Drive</div>
                <div className="aside-button">Shared with me</div>
                <div className="aside-button">Recent</div>
                <div className="aside-button">Starred</div>
            </nav>



            <div className="main-content">
                <div className="search-bar">
                    <label className="search-bar" >
                        <input placeholder="🔍search in the drive" size="50"/>
                    </label>
                </div>

                <div className="main-button">
                    <button type="button">+Add New</button>
                    <button type="button">Change View</button>
                </div>


                <div className="file-list">
                    <table className="file-table">
                        <thead>
                        <tr className="file-header">
                            <th>Name</th>
                            <th>Last Modified</th>
                            <th>Size</th>
                        </tr>
                        </thead>
                        {files.map((file) => (
                            <tbody key={file.name}>
                            <tr className="file-data">
                                <td>{file.name}</td>
                                <td>{file.lastModified}</td>
                                <td>{file.size}</td>
                            </tr>
                            </tbody>))}
                    </table>
                </div>
            </div>
        </div>

    );
}

export default Home;