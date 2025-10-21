import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import React from 'react';
import OperateSpecificDeletedFile from "./OperateSpecificDeletedFile";
import apiRequest from "../../../../../utils/apiRequest";

jest.mock("../../../../../utils/apiRequest");

global.alert = jest.fn();

describe("OperateSpecificDeletedFile Component", () => {
    const mockFile = {
        id: 'file123',
        name: 'test.txt'
    };
    const mockUserId = 'user456';

    beforeEach(() => {
        jest.clearAllMocks();
        global.alert.mockClear();
    });

    test("testing whether ellipsis button is rendered", () => {
        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        expect(ellipsisButton).toBeInTheDocument();
        expect(ellipsisButton).toHaveClass('fa-solid');
    });

    test("testing whether menu is hidden by default", () => {
        render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        expect(screen.queryByText("Delete")).not.toBeInTheDocument();
        expect(screen.queryByText("Restore")).not.toBeInTheDocument();
    });

    test("testing whether clicking button shows the menu", () => {
        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        expect(screen.getByText("Delete")).toBeInTheDocument();
        expect(screen.getByText("Restore")).toBeInTheDocument();
    });

    test("testing whether clicking button twice toggles menu visibility", () => {
        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');

        fireEvent.click(ellipsisButton);
        expect(screen.getByText("Delete")).toBeInTheDocument();

        fireEvent.click(ellipsisButton);
        expect(screen.queryByText("Delete")).not.toBeInTheDocument();
    });

    test("testing whether clicking outside closes the menu", () => {
        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);
        expect(screen.getByText("Delete")).toBeInTheDocument();

        fireEvent.click(document.body);

        waitFor(() => {
            expect(screen.queryByText("Delete")).not.toBeInTheDocument();
        });
    });

    test("testing whether delete option calls handleDeleteFile with correct parameters", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const deleteButton = screen.getByText("Delete");
        fireEvent.click(deleteButton);

        await waitFor(() => {
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining(`/deleteFile?fileId=${mockFile.id}&userId=${mockUserId}`),
                expect.objectContaining({
                    method: "DELETE",
                    headers: {"Content-Type": "application/json"},
                    credentials: 'include',
                })
            );
        });
    });

    test("testing whether successful delete logs success message", async () => {
        const consoleLogSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: true});

        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const {deleteButton} = getMenuButtons(container);
        fireEvent.click(deleteButton);

        await waitFor(() => {
            expect(consoleLogSpy).toHaveBeenCalledWith("File deleted successfully");
        });

        consoleLogSpy.mockRestore();
    });

    test("testing whether failed delete logs error", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: false, statusText: 'Not Found'});

        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const {deleteButton} = getMenuButtons(container);
        fireEvent.click(deleteButton);

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalledWith(
                "Error deleting file:",
                expect.any(Error)
            );
        });

        consoleErrorSpy.mockRestore();
    });

    test("testing whether restore option calls handleRestoreFile with correct parameters", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const {restoreButton} = getMenuButtons(container);
        fireEvent.click(restoreButton);

        await waitFor(() => {
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining(`/restoreFile?fileId=${mockFile.id}&ownerId=${mockUserId}`),
                expect.objectContaining({
                    method: "POST",
                    headers: {"Content-Type": "application/json"},
                    credentials: 'include',
                })
            );
        });
    });

    test("testing whether successful restore shows alert message", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const {restoreButton} = getMenuButtons(container);
        fireEvent.click(restoreButton);

        await waitFor(() => {
            expect(global.alert).toHaveBeenCalledWith("File restored successfully");
        });
    });

    test("testing whether failed restore shows alert with error", async () => {
        const errorMessage = "Failed to delete the file: Internal Server Error";
        apiRequest.mockResolvedValueOnce({ok: false, statusText: 'Internal Server Error'});

        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const {restoreButton} = getMenuButtons(container);
        fireEvent.click(restoreButton);

        await waitFor(() => {
            expect(global.alert).toHaveBeenCalledWith(
                expect.stringContaining("Error restoring file:")
            );
        });
    });

    test("testing whether stopPropagation is called on button click", () => {
        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        const mockEvent = {stopPropagation: jest.fn()};

        ellipsisButton.onclick(mockEvent);

        expect(mockEvent.stopPropagation).toHaveBeenCalled();
    });

    test("testing whether API request fails and throws error", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockRejectedValueOnce(new Error('Network error'));

        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const {deleteButton} = getMenuButtons(container);
        fireEvent.click(deleteButton);

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalledWith(
                "Error deleting file:",
                expect.any(Error)
            );
        });

        consoleErrorSpy.mockRestore();
    });

    test("testing whether menu stays visible when clicking inside menu", () => {
        const {container} = render(<OperateSpecificDeletedFile file={mockFile} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const menuContainer = screen.getByText("Delete").closest('.menu-container');
        fireEvent.click(menuContainer);

        expect(screen.getByText("Delete")).toBeInTheDocument();
    });
});

function getMenuButtons(container) {
    const ellipsisButton = container.querySelector('.fa-ellipsis');
    fireEvent.click(ellipsisButton);

    const deleteButton = screen.getByText("Delete");
    const restoreButton = screen.getByText("Restore");

    return {deleteButton, restoreButton};
}