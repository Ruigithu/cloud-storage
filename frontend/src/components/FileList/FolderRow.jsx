import React from 'react';

function FolderRow({ folder, onClick, renderActions, userId }) {
    return (
        <tr className="file-data" onClick={() => onClick(folder.id, folder.name)}>
            <td>
                <i className="fa-solid fa-folder" style={{ color: "#ffd129" }}></i>
                {' '}{folder.name}
            </td>
            <td>{folder.updatedAt}</td>
            <td>view</td>
            <td>{renderActions && renderActions(folder, userId)}</td>
        </tr>
    );
}

export default FolderRow;