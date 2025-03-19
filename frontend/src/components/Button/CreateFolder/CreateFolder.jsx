import CreateFolderDialog from "./CreateFolderDialog";


function CreateFolder({parentId,userId,onFileUploadSuccess}){
    const handleCreateFolder = async (name) => {
        console.log('Creating folder:', name);
        console.log('parentId:', parentId, 'userId:', userId);
        // 创建文件夹逻辑
        const formData = new FormData();
        formData.append('name', name);
        formData.append('parentId',parentId);
        formData.append('userId',userId);

        const response = await fetch(`${process.env.REACT_APP_API_URL}/createFolder`, {
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