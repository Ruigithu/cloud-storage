import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import React from 'react';
import OperateSpecificDeletedFolder from "./OperateSpecificDeletedFolder";
import apiRequest from "../../../../../utils/apiRequest";

jest.mock("../../../../../utils/apiRequest");

global.alert = jest.fn();

describe("OperateSpecificDeletedFolder Component", () => {
    const mockFolder = {
        id: 'folder123',
        name: 'Test Folder'
    };
    const mockUserId = 'user456';

    beforeEach(() => {
        jest.clearAllMocks();
        global.alert.mockClear();
    });

    test("testing whether ellipsis button is rendered", () => {
        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        expect(ellipsisButton).toBeInTheDocument();
        expect(ellipsisButton).toHaveClass('fa-solid');
    });

    test("testing whether menu is hidden by default", () => {
        render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        expect(screen.queryByText("Delete")).not.toBeInTheDocument();
        expect(screen.queryByText("Restore")).not.toBeInTheDocument();
    });

    test("testing whether clicking button shows the menu", () => {
        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        expect(screen.getByText("Delete")).toBeInTheDocument();
        expect(screen.getByText("Restore")).toBeInTheDocument();
    });

    test("testing whether clicking button twice toggles menu visibility", () => {
        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');

        fireEvent.click(ellipsisButton);
        expect(screen.getByText("Delete")).toBeInTheDocument();

        fireEvent.click(ellipsisButton);
        expect(screen.queryByText("Delete")).not.toBeInTheDocument();
    });

    test("testing whether clicking outside closes the menu", () => {
        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);
        expect(screen.getByText("Delete")).toBeInTheDocument();

        fireEvent.click(document.body);

        waitFor(() => {
            expect(screen.queryByText("Delete")).not.toBeInTheDocument();
        });
    });

    test("testing whether delete option calls handleDeleteFolder with correct parameters", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const deleteButton = screen.getByText("Delete");
        fireEvent.click(deleteButton);

        await waitFor(() => {
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining(`/deleteFolder?folderId=${mockFolder.id}&userId=${mockUserId}`),
                expect.objectContaining({
                    method: "DELETE",
                    headers: {"Content-Type": "application/json"},
                    credentials: 'include',
                })
            );
        });
    });

    test("testing whether successful delete shows alert message", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const {deleteButton} = getMenuButtons(container);
        fireEvent.click(deleteButton);

        await waitFor(() => {
            expect(global.alert).toHaveBeenCalledWith("File deleted successfully");
        });
    });

    test("testing whether failed delete logs error", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: false, statusText: 'Not Found'});

        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

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

    test("testing whether restore option calls handleRestoreFolder with correct parameters", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const {restoreButton} = getMenuButtons(container);
        fireEvent.click(restoreButton);

        await waitFor(() => {
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining(`/restoreFolder?folderId=${mockFolder.id}&ownerId=${mockUserId}`),
                expect.objectContaining({
                    method: "Post",
                    headers: {"Content-Type": "application/json"},
                    credentials: 'include',
                })
            );
        });
    });

    test("testing whether successful restore logs success message", async () => {
        const consoleLogSpy = jest.spyOn(console, 'log').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: true});

        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const {restoreButton} = getMenuButtons(container);
        fireEvent.click(restoreButton);

        await waitFor(() => {
            expect(consoleLogSpy).toHaveBeenCalledWith("File restored successfully");
        });

        consoleLogSpy.mockRestore();
    });

    test("testing whether failed restore logs error", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: false, statusText: 'Internal Server Error'});

        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const {restoreButton} = getMenuButtons(container);
        fireEvent.click(restoreButton);

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalledWith(
                "Error restore file:",
                expect.any(Error)
            );
        });

        consoleErrorSpy.mockRestore();
    });

    test("testing whether stopPropagation is called on button click", () => {
        const {container} = render(<OperateSpecificDeletedFolder folder={mockFolder} userId={mockUserId} />);

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        const mockEvent = {stopPropagation: jest.fn()};

        ellipsisButton.onclick(mockEvent);

        expect(mockEvent.stopPropagation).toHaveBeenCalled();
    });
});

function getMenuButtons(container) {
    const ellipsisButton = container.querySelector('.fa-ellipsis');
    fireEvent.click(ellipsisButton);

    const deleteButton = screen.getByText("Delete");
    const restoreButton = screen.getByText("Restore");

    return {deleteButton, restoreButton};
}