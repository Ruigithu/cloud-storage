import React from 'react';
import { getFileIcon, FILE_ICON_COLORS, formatFileSize } from '../../utils/fileHelper';

function FileRow({ file, onClick, renderActions, userId }) {
    const icon = getFileIcon(file.mimeType);

    return (
        <tr className="file-data">
            <td onClick={() => onClick(file.id)}>
                <i
                    className={icon}
                    style={{ color: FILE_ICON_COLORS[icon] }}
                ></i>
                {' '}{file.name}
            </td>
            <td>{file.updatedAt}</td>
            <td>{formatFileSize(file.size)}</td>
            <td>{renderActions && renderActions(file, userId)}</td>
        </tr>
    );
}

export default FileRow;