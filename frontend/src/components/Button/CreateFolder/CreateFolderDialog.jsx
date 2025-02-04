import {useState} from "react";
import "./CreateFolderDialog.css"

const CreateFolderDialog = ({ onCreateFolder }) => {
    const [showModal, setShowModal] = useState(false);
    const [folderName, setFolderName] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState('');

    // 新增：重置所有状态的函数
    const resetModal = () => {
        setFolderName('');
        setError('');
        setIsLoading(false);
    };

    // 修改：关闭模态框时调用重置函数
    const handleCloseModal = () => {
        setShowModal(false);
        resetModal();
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {

            await onCreateFolder(folderName.trim());
            setFolderName('');
            handleCloseModal(); // 使用新的关闭函数
        } catch (error) {
            setError('Failed to create folder. Please try again.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <>
            <div
                onClick={() => setShowModal(true)}
                className="modal-trigger"
            >
                Create New Folder
            </div>

            {showModal && (
                <div className="modal-overlay">
                    <div
                        className="modal-container"
                        onClick={e => e.stopPropagation()}
                    >
                        <div className="modal-content">
                            <h2 className="modal-title">Create New Folder</h2>

                            <form onSubmit={handleSubmit} className="form-container">
                                <input
                                    type="text"
                                    value={folderName}
                                    onChange={(e) => setFolderName(e.target.value)}
                                    placeholder="Enter folder name"
                                    className="form-input"
                                    autoFocus
                                />

                                {error && (
                                    <div className="error-message">{error}</div>
                                )}

                                <div className="button-container">
                                    <button
                                        type="button"
                                        onClick={handleCloseModal} // 使用新的关闭函数
                                        className="cancel-button"
                                        disabled={isLoading}
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        type="submit"
                                        className="create-button"
                                        disabled={isLoading}
                                    >
                                        {isLoading ? 'Creating...' : 'Create'}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};
export default CreateFolderDialog