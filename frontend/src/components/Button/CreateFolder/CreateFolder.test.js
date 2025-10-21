import {render, screen, waitFor} from "@testing-library/react";
import React from 'react';
import CreateFolder from "./CreateFolder";
import apiRequest from "../../../utils/apiRequest";

jest.mock("../../../utils/apiRequest");
jest.mock("./CreateFolderDialog", () => {
    return function MockCreateFolderDialog({onCreateFolder}) {
        return (
            <div>
                <button onClick={() => onCreateFolder('Test Folder')}>
                    Create Folder Mock
                </button>
            </div>
        );
    };
});

global.alert = jest.fn();

describe("CreateFolder Component", () => {
    const mockOnFileUploadSuccess = jest.fn();
    const mockParentId = 'parent123';
    const mockUserId = 'user456';

    beforeEach(() => {
        jest.clearAllMocks();
        global.alert.mockClear();
    });

    test("testing whether CreateFolderDialog is rendered", () => {
        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        expect(screen.getByText("Create Folder Mock")).toBeInTheDocument();
    });

    test("testing whether handleCreateFolder sends correct FormData", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/createFolder'),
                expect.objectContaining({
                    method: 'POST',
                    credentials: 'include',
                })
            );
        });
    });

    test("testing whether FormData contains correct name", async () => {
        let capturedFormData;
        apiRequest.mockImplementation((url, options) => {
            capturedFormData = options.body;
            return Promise.resolve({ok: true});
        });

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(capturedFormData.get('name')).toBe('Test Folder');
        });
    });

    test("testing whether FormData contains correct parentId", async () => {
        let capturedFormData;
        apiRequest.mockImplementation((url, options) => {
            capturedFormData = options.body;
            return Promise.resolve({ok: true});
        });

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(capturedFormData.get('parentId')).toBe(mockParentId);
        });
    });

    test("testing whether FormData contains correct userId", async () => {
        let capturedFormData;
        apiRequest.mockImplementation((url, options) => {
            capturedFormData = options.body;
            return Promise.resolve({ok: true});
        });

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(capturedFormData.get('userId')).toBe(mockUserId);
        });
    });

    test("testing whether successful creation shows alert", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(global.alert).toHaveBeenCalledWith('Folder created successfully');
        });
    });

    test("testing whether successful creation calls onFileUploadSuccess", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(mockOnFileUploadSuccess).toHaveBeenCalledTimes(1);
        });
    });

    test("testing whether onFileUploadSuccess is not called when callback is not provided", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(global.alert).toHaveBeenCalledWith('Folder created successfully');
        });

        expect(mockOnFileUploadSuccess).not.toHaveBeenCalled();
    });

    test("testing whether failed creation logs error", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: false});

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalledWith('failed');
        });

        consoleErrorSpy.mockRestore();
    });

    test("testing whether failed creation does not call onFileUploadSuccess", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: false});

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalled();
        });

        expect(mockOnFileUploadSuccess).not.toHaveBeenCalled();

        consoleErrorSpy.mockRestore();
    });

    test("testing whether console logs are called during folder creation", async () => {
        const consoleLogSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: true});

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(consoleLogSpy).toHaveBeenCalledWith('Creating folder:', 'Test Folder');
            expect(consoleLogSpy).toHaveBeenCalledWith('parentId:', mockParentId, 'userId:', mockUserId);
        });

        consoleLogSpy.mockRestore();
    });

    test("testing whether API URL is constructed correctly", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        render(
            <CreateFolder
                parentId={mockParentId}
                userId={mockUserId}
                onFileUploadSuccess={mockOnFileUploadSuccess}
            />
        );

        const createButton = screen.getByText("Create Folder Mock");
        createButton.click();

        await waitFor(() => {
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/createFolder'),
                expect.any(Object)
            );
        });
    });
});