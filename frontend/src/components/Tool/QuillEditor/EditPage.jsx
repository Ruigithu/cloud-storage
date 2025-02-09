import QuillEditor from "./QuillEditor";
import {useParams} from "react-router-dom";

const EditorPage = () => {
    const { fileId } = useParams();

    return (
        <div className="h-screen">
            <QuillEditor
                documentId={fileId}
                userId={localStorage.getItem('userId')}
            />
        </div>
    );
};

export default EditorPage