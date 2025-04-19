import CreateFolderDialog from "./CreateFolderDialog";
import apiRequest from "../../../utils/api";


function CreateFolder({parentId,userId,onFileUploadSuccess}){
    const handleCreateFolder = async (name) => {
        console.log('Creating folder:', name);
        console.log('parentId:', parentId, 'userId:', userId);

        const formData = new FormData();
        formData.append('name', name);
        formData.append('parentId',parentId);
        formData.append('userId',userId);

        const response =await apiRequest(`${process.env.REACT_APP_API_URL}/createFolder`, {
            method: 'POST',
            body: formData,
            credentials: 'include',
        });

        if (response.ok) {
            alert('Folder created successfully');
            if (onFileUploadSuccess) {
                onFileUploadSuccess();
            }
        } else {
            console.error('failed');
    }};

    return (
        <div>
            <CreateFolderDialog onCreateFolder={handleCreateFolder} />
        </div>
    );

}

export default CreateFolder