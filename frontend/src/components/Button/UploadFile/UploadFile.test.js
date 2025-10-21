import {render, screen, fireEvent} from "@testing-library/react";
import React from 'react';
import UploadFile from "./UploadFile";
import {useFileUpload} from '../../../hooks/useFileUpload';

jest.mock('../../../hooks/useFileUpload');
jest.mock('../../Progress/UploadProgress', () => {
    return function MockUploadProgress({progress, isPaused, onPause, onResume, onCancel, showPauseButton}) {
        return (
            <div data-testid="upload-progress">
                Progress: {progress}%
                <span data-testid="is-paused">{isPaused ? 'true' : 'false'}</span>
                <span data-testid="show-pause">{showPauseButton ? 'true' : 'false'}</span>
                <button onClick={onPause}>Pause</button>
                <button onClick={onResume}>Resume</button>
                <button onClick={onCancel}>Cancel</button>
            </div>
        );
    };
});

describe("UploadFile Component", () => {
    const mockOnFileUploadSuccess = jest.fn();
    const mockStartUpload = jest.fn();
    const mockPauseUpload = jest.fn();
    const mockResumeUpload = jest.fn();
    const mockCancelUpload = jest.fn();
    const mockOwnerId = 'owner123';
    const mockFolderId = 'folder456';

    const defaultHookReturn = {
        isUploading: false,
        uploadProgress: 0,
        isPaused: false,
        startUpload: mockStartUpload,
        pauseUpload: mockPauseUpload,
        resumeUpload: mockResumeUpload,
        cancelUpload: mockCancelUpload,
    };

    beforeEach(() => {
        jest.clearAllMocks();
        useFileUpload.mockReturnValue(defaultHookReturn);
    });

    test("testing whether upload label is rendered", () => {
        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        expect(screen.getByText(/Upload File/i)).toBeInTheDocument();
    });

    test("testing whether file input has uploadFile class", () => {
        const {container} = render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const input = container.querySelector('.uploadFile');
        expect(input).toBeInTheDocument();
    });

    test("testing whether file input is hidden", () => {
        const {container} = render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        expect(input).toHaveStyle({display: 'none'});
    });

    test("testing whether label has upload-button class", () => {
        const {container} = render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const label = container.querySelector('.upload-button');
        expect(label).toBeInTheDocument();
    });

    test("testing whether useFileUpload hook is called with correct parameters", () => {
        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        expect(useFileUpload).toHaveBeenCalledWith({
            ownerId: mockOwnerId,
            folderId: mockFolderId,
            onSuccess: mockOnFileUploadSuccess,
        });
    });

    test("testing whether selecting file calls startUpload", () => {
        const {container} = render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        const mockFile = new File(['content'], 'test.txt', {type: 'text/plain'});

        Object.defineProperty(input, 'files', {
            value: [mockFile],
            writable: false,
        });

        fireEvent.change(input);

        expect(mockStartUpload).toHaveBeenCalledWith(mockFile);
    });

    test("testing whether upload progress is not shown when not uploading", () => {
        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        expect(screen.queryByTestId("upload-progress")).not.toBeInTheDocument();
    });

    test("testing whether upload progress is shown when uploading", () => {
        useFileUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
            uploadProgress: 50,
        });

        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        expect(screen.getByTestId("upload-progress")).toBeInTheDocument();
    });

    test("testing whether upload progress displays correct percentage", () => {
        useFileUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
            uploadProgress: 75,
        });

        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        expect(screen.getByText(/Progress: 75%/i)).toBeInTheDocument();
    });

    test("testing whether pause button is shown", () => {
        useFileUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
        });

        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        expect(screen.getByTestId("show-pause")).toHaveTextContent("true");
    });

    test("testing whether isPaused status is passed to progress component", () => {
        useFileUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
            isPaused: true,
        });

        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        expect(screen.getByTestId("is-paused")).toHaveTextContent("true");
    });

    test("testing whether input is disabled when uploading and not paused", () => {
        useFileUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
            isPaused: false,
        });

        const {container} = render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        expect(input).toBeDisabled();
    });

    test("testing whether input is enabled when uploading but paused", () => {
        useFileUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
            isPaused: true,
        });

        const {container} = render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        expect(input).not.toBeDisabled();
    });

    test("testing whether input is enabled when not uploading", () => {
        const {container} = render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const input = container.querySelector('input[type="file"]');
        expect(input).not.toBeDisabled();
    });

    test("testing whether startUpload is not called when no file selected", () => {
        const {container} = render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
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

    test("testing whether pauseUpload is passed to progress component", () => {
        useFileUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
        });

        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const pauseButton = screen.getByText("Pause");
        fireEvent.click(pauseButton);

        expect(mockPauseUpload).toHaveBeenCalledTimes(1);
    });

    test("testing whether resumeUpload is passed to progress component", () => {
        useFileUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
        });

        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const resumeButton = screen.getByText("Resume");
        fireEvent.click(resumeButton);

        expect(mockResumeUpload).toHaveBeenCalledTimes(1);
    });

    test("testing whether cancelUpload is passed to progress component", () => {
        useFileUpload.mockReturnValue({
            ...defaultHookReturn,
            isUploading: true,
        });

        render(
            <UploadFile
                onFileUploadSuccess={mockOnFileUploadSuccess}
                ownerId={mockOwnerId}
                folderId={mockFolderId}
            />
        );

        const cancelButton = screen.getByText("Cancel");
        fireEvent.click(cancelButton);

        expect(mockCancelUpload).toHaveBeenCalledTimes(1);
    });
});