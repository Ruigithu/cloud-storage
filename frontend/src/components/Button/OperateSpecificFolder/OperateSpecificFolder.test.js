import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import React from 'react';
import OperateSpecificFolder from "./OperateSpecificFolder";
import apiRequest from "../../../utils/apiRequest";

jest.mock("../../../utils/apiRequest");

global.alert = jest.fn();

describe("OperateSpecificFolder Component", () => {
    const mockFolder = {
        id: 'folder123',
        name: 'Test Folder'
    };
    const mockUserId = 'user456';

    beforeEach(() => {
        jest.clearAllMocks();
        global.alert.mockClear();
        global.URL.createObjectURL = jest.fn(() => 'blob:mock-url');
        global.URL.revokeObjectURL = jest.fn();
    });

    test("testing whether ellipsis button is rendered", () => {
        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        expect(ellipsisButton).toBeInTheDocument();
        expect(ellipsisButton).toHaveClass('fa-solid');
    });

    test("testing whether menu is hidden by default", () => {
        render(<OperateSpecificFolder folder={mockFolder} userId={mockUserId} />);

        expect(screen.queryByText("Download")).not.toBeInTheDocument();
        expect(screen.queryByText("Delete")).not.toBeInTheDocument();
        expect(screen.queryByText("Share")).not.toBeInTheDocument();
    });

    test("testing whether clicking button shows the menu", () => {
        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        expect(screen.getByText("Download")).toBeInTheDocument();
        expect(screen.getByText("Delete")).toBeInTheDocument();
        expect(screen.getByText("Share")).toBeInTheDocument();
    });

    test("testing whether clicking button twice toggles menu visibility", () => {
        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');

        fireEvent.click(ellipsisButton);
        expect(screen.getByText("Download")).toBeInTheDocument();

        fireEvent.click(ellipsisButton);
        expect(screen.queryByText("Download")).not.toBeInTheDocument();
    });

    test("testing whether clicking outside closes the menu", async () => {
        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);
        expect(screen.getByText("Download")).toBeInTheDocument();

        fireEvent.click(document.body);

        await waitFor(() => {
            expect(screen.queryByText("Download")).not.toBeInTheDocument();
        });
    });

    test("testing whether delete option calls handleDeleteFolder with correct parameters", async () => {
        apiRequest.mockResolvedValueOnce({ok: true});

        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const deleteButton = screen.getByText("Delete");
        fireEvent.click(deleteButton);

        await waitFor(() => {
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining(`/softDeleteFolder?folderId=${mockFolder.id}&userId=${mockUserId}`),
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

        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const deleteButton = screen.getByText("Delete");
        fireEvent.click(deleteButton);

        await waitFor(() => {
            expect(consoleLogSpy).toHaveBeenCalledWith("File deleted successfully");
        });

        consoleLogSpy.mockRestore();
    });

    test("testing whether failed delete logs error", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: false, statusText: 'Not Found'});

        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const deleteButton = screen.getByText("Delete");
        fireEvent.click(deleteButton);

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalledWith(
                "Error deleting file:",
                expect.any(Error)
            );
        });

        consoleErrorSpy.mockRestore();
    });

    test("testing whether download option calls handleDownloadFolder with correct parameters", async () => {
        const mockBlob = new Blob(['test'], {type: 'application/zip'});
        apiRequest.mockResolvedValueOnce({
            ok: true,
            blob: jest.fn().mockResolvedValue(mockBlob)
        });

        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const downloadButton = screen.getByText("Download");
        fireEvent.click(downloadButton);

        await waitFor(() => {
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining(`/downloadFolder?folderId=${mockFolder.id}&userId=${mockUserId}`),
                expect.objectContaining({
                    method: 'GET',
                    credentials: 'include',
                })
            );
        });
    });

    test("testing whether successful download creates and clicks download link", async () => {
        const mockBlob = new Blob(['test'], {type: 'application/zip'});
        const mockLink = {
            href: '',
            setAttribute: jest.fn(),
            click: jest.fn(),
            remove: jest.fn(),
        };

        document.createElement = jest.fn().mockReturnValue(mockLink);
        document.body.appendChild = jest.fn();

        apiRequest.mockResolvedValueOnce({
            ok: true,
            blob: jest.fn().mockResolvedValue(mockBlob)
        });

        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const downloadButton = screen.getByText("Download");
        fireEvent.click(downloadButton);

        await waitFor(() => {
            expect(mockLink.click).toHaveBeenCalled();
            expect(mockLink.remove).toHaveBeenCalled();
            expect(global.URL.revokeObjectURL).toHaveBeenCalled();
        });
    });

    test("testing whether failed download logs error", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: false});

        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const downloadButton = screen.getByText("Download");
        fireEvent.click(downloadButton);

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalledWith('fail downloading file');
        });

        consoleErrorSpy.mockRestore();
    });

    test("testing whether share option shows alert", () => {
        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const shareButton = screen.getByText("Share");
        fireEvent.click(shareButton);

        expect(global.alert).toHaveBeenCalledWith("Share Selected");
    });

    test("testing whether stopPropagation is called on button click", () => {
        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        const mockEvent = {stopPropagation: jest.fn()};

        ellipsisButton.onclick(mockEvent);

        expect(mockEvent.stopPropagation).toHaveBeenCalled();
    });

    test("testing whether menu has correct class names", () => {
        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        const menuContainer = container.querySelector('.menu-container');
        expect(menuContainer).toBeInTheDocument();

        const menuList = container.querySelector('.menu-list');
        expect(menuList).toBeInTheDocument();
    });

    test("testing whether all menu items have correct class names", () => {
        const {container} = render(
            <OperateSpecificFolder folder={mockFolder} userId={mockUserId} />
        );

        const ellipsisButton = container.querySelector('.fa-ellipsis');
        fireEvent.click(ellipsisButton);

        expect(container.querySelector('.download.more')).toBeInTheDocument();
        expect(container.querySelector('.delete.more')).toBeInTheDocument();
        expect(container.querySelector('.share.more')).toBeInTheDocument();
    });
});