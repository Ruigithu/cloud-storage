import {render, screen, fireEvent} from "@testing-library/react";
import React from 'react';
import UploadFolder from "./UploadFolder";
import {useFolderUpload} from '../../../hooks/useFolderUpload';

jest.mock('../../../hooks/useFolderUpload');
jest.mock('../../Progress/UploadProgress', () => {
    return function MockUploadProgress({progress, isPaused, onCancel, additionalInfo, showPauseButton}) {
        return (
            <div data-testid="upload-progress">
                Progress: {progress}%
                <span data-testid="additional-info">{additionalInfo}</span>
                <span data-testid="show-pause">{showPauseButton ? 'true' : 'false'}</span>
                <button onClick={onCancel}>Cancel</button>
            </div>
        );
    };
});

describe("UploadFolder Component", () => {
    const mockOnFileUploadSuccess = jest.fn();
    const mockStartUpload = jest.fn();
    const mockCancelUpload = jest.fn();
    const mockUserId = 'user123';
    const mockParentId = 'parent456';

    const defaultHookReturn = {
        isUploading: false,
        uploadProgress: 0,
        totalFiles: 0,
        uploadedFiles: 0,
        failedFiles: [],
        startUpload: mockStartUpload,
        cancelUpload: mockCancelUpload,
    };

    beforeEach(() => {
        jest.clearAllMocks();
        useFolderUpload.mockReturnValue(defaultHookReturn);
    });

    test("testing whether upload label is rendered", () => {
        render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        expect(screen.getByText(/Upload Folder/i)).toBeInTheDocument();
    });

    test("testing whether file input has webkitdirectory attribute", () => {
        const {container} = render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        expect(input).toHaveAttribute('webkitdirectory', 'true');
    });

    test("testing whether file input has uploadFolder class", () => {
        const {container} = render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        const input = container.querySelector('.uploadFolder');
        expect(input).toBeInTheDocument();
    });

    test("testing whether file input is hidden", () => {
        const {container} = render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        expect(input).toHaveStyle({display: 'none'});
    });

    test("testing whether useFolderUpload hook is called with correct parameters", () => {
        render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        expect(useFolderUpload).toHaveBeenCalledWith({
            userId: mockUserId,
            parentId: mockParentId,
            onSuccess: mockOnFileUploadSuccess,
        });
    });

    test("testing whether selecting folder calls startUpload", () => {
        const {container} = render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        const mockFiles = [
            new File(['content1'], 'file1.txt'),
            new File(['content2'], 'file2.txt'),
        ];

        Object.defineProperty(input, 'files', {
            value: mockFiles,
            writable: false,
        });

        fireEvent.change(input);

        expect(mockStartUpload).toHaveBeenCalledWith(mockFiles);
    });

    test("testing whether upload progress is not shown when not uploading", () => {
        render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        expect(screen.queryByTestId("upload-progress")).not.toBeInTheDocument();
    });

    test("testing whether upload progress is shown when uploading", () => {
        useFolderUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
            uploadProgress: 50,
            totalFiles: 10,
            uploadedFiles: 5,
        });

        render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        expect(screen.getByTestId("upload-progress")).toBeInTheDocument();
    });

    test("testing whether upload progress displays correct percentage", () => {
        useFolderUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
            uploadProgress: 75,
            totalFiles: 10,
            uploadedFiles: 7,
        });

        render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        expect(screen.getByText(/Progress: 75%/i)).toBeInTheDocument();
    });

    test("testing whether additional info shows uploaded files count", () => {
        useFolderUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
            uploadProgress: 50,
            totalFiles: 10,
            uploadedFiles: 5,
        });

        render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        expect(screen.getByTestId("additional-info")).toHaveTextContent("(5/10 files)");
    });

    test("testing whether pause button is not shown", () => {
        useFolderUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
            uploadProgress: 50,
        });

        render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        expect(screen.getByTestId("show-pause")).toHaveTextContent("false");
    });

    test("testing whether input is disabled when uploading", () => {
        useFolderUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
        });

        const {container} = render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        expect(input).toBeDisabled();
    });

    test("testing whether input is enabled when not uploading", () => {
        const {container} = render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        expect(input).not.toBeDisabled();
    });

    test("testing whether failed files list is not shown when no failures", () => {
        render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        expect(screen.queryByText(/Failed uploads/i)).not.toBeInTheDocument();
    });

    test("testing whether failed files list is shown when there are failures", () => {
        useFolderUpload.mockReturnValue({
            ...defaultHookReturn,
            failedFiles: [
                {fileName: 'failed1.txt'},
                {fileName: 'failed2.txt'},
            ],
        });

        render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        expect(screen.getByText(/Failed uploads/i)).toBeInTheDocument();
        expect(screen.getByText("failed1.txt")).toBeInTheDocument();
        expect(screen.getByText("failed2.txt")).toBeInTheDocument();
    });

    test("testing whether startUpload is not called when no files selected", () => {
        const {container} = render(
            <UploadFolder
                onFileUploadSuccess={mockOnFileUploadSuccess}
                userId={mockUserId}
                parentId={mockParentId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        Object.defineProperty(input, 'files', {
            value: [],
            writable: false,
        });

        fireEvent.change(input);

        expect(mockStartUpload).not.toHaveBeenCalled();
    });
});