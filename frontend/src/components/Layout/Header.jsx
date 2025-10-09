import React from 'react';
import driveIcon from '../../assets/images/cloudversify-brands-solid.svg';
import AddNewContextMenu from "../Button/AddNewContextMenu/AddNewContextMenu";

function Header({ showAddNew = false, onFileUploadSuccess, parentId, userId }) {
    return (
        <header>
            <img src={driveIcon} alt="drive-icon" className="drive-icon" />
            <div className="search-bar">
                <label className="search-bar">
                    <input
                        className="search-box"
                        placeholder="🔍 search in the drive"
                        size="50"
                    />
                </label>
            </div>
            {showAddNew && (
                <AddNewContextMenu
                    onFileUploadSuccess={onFileUploadSuccess}
                    parentId={parentId}
                    userId={userId}
                />
            )}
        </header>
    );
}

export default Header;