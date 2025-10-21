import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import {MemoryRouter} from "react-router-dom";
import React from 'react';
import Bin from "./Bin";
import * as binService from "../../../../services/binService";

const mockNavigate = jest.fn();

jest.mock("react-router-dom", () => ({
    ...jest.requireActual("react-router-dom"),
    useNavigate: () => mockNavigate,
}));

jest.mock("../../../../services/binService");
jest.mock("../../../../hooks/useFileNavigation");
jest.mock("../../SideBar", () => () => <div data-testid="sidebar">Sidebar</div>);
jest.mock("../../../Layout/Header", () => () => <div data-testid="header">Header</div>);
jest.mock("../../../Navigation/FolderPath", () => () => <div data-testid="folder-path">FolderPath</div>);
jest.mock("./OperateSpecificDeletedFolder/OperateSpecificDeletedFolder", () => () => <div>OperateFolder</div>);
jest.mock("./OperateSpecificDeletedFile/OperateSpecificDeletedFile", () => () => <div>OperateFile</div>);

const mockUseFileNavigation = require("../../../../hooks/useFileNavigation");

describe("Bin Component", () => {
    const mockUserId = "user123";
    const mockRootFolderId = "root456";

    beforeEach(() => {
        localStorage.setItem('userId', mockUserId);
        localStorage.setItem('rootFolderId', mockRootFolderId);

        mockUseFileNavigation.useFileNavigation.mockReturnValue({
            rootFolderId: mockRootFolderId,
            navigationPath: [{id: mockRootFolderId, name: 'Root'}],
            handleFolderClick: jest.fn(),
            handleBackward: jest.fn(),
            handlePathClick: jest.fn(),
        });

        jest.clearAllMocks();
    });

    afterEach(() => {
        localStorage.clear();
    });

    test("testing whether user without login will be redirected to login page", () => {
        localStorage.removeItem('userId');

        render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        expect(mockNavigate).toHaveBeenCalledWith('/login', {replace: true});
    });

    test("testing whether UI elements got rendered when user is logged in", async () => {
        binService.getDeletedFilesAndFolders.mockResolvedValue({
            files: [],
            folders: []
        });

        render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByTestId("header")).toBeInTheDocument();
            expect(screen.getByTestId("sidebar")).toBeInTheDocument();
            expect(screen.getByTestId("folder-path")).toBeInTheDocument();
        });
    });

    test("testing whether table headers are rendered correctly", async () => {
        binService.getDeletedFilesAndFolders.mockResolvedValue({
            files: [],
            folders: []
        });

        render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText("Name")).toBeInTheDocument();
            expect(screen.getByText("Deleted At")).toBeInTheDocument();
            expect(screen.getByText("Size")).toBeInTheDocument();
        });
    });

    test("testing whether empty state message is displayed when no files or folders", async () => {
        binService.getDeletedFilesAndFolders.mockResolvedValue({
            files: [],
            folders: []
        });

        render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText("No deleted files or folders found")).toBeInTheDocument();
        });
    });

    test("testing whether deleted folders are rendered correctly", async () => {
        const mockFolders = [
            {id: 'folder1', name: 'Test Folder 1', deletedAt: '2024-01-01'},
            {id: 'folder2', name: 'Test Folder 2', deletedAt: '2024-01-02'}
        ];

        binService.getDeletedFilesAndFolders.mockResolvedValue({
            files: [],
            folders: mockFolders
        });

        render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText("Test Folder 1")).toBeInTheDocument();
            expect(screen.getByText("Test Folder 2")).toBeInTheDocument();
        });
    });

    test("testing whether deleted files are rendered correctly", async () => {
        const mockFiles = [
            {id: 'file1', name: 'test1.txt', folderId: mockRootFolderId, deletedAt: '2024-01-01', size: 1024},
            {id: 'file2', name: 'test2.txt', folderId: mockRootFolderId, deletedAt: '2024-01-02', size: 2048}
        ];

        binService.getDeletedFilesAndFolders.mockResolvedValue({
            files: mockFiles,
            folders: []
        });

        render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText("test1.txt")).toBeInTheDocument();
            expect(screen.getByText("test2.txt")).toBeInTheDocument();
        });
    });

    test("testing whether files in different folders are filtered correctly", async () => {
        const mockFiles = [
            {id: 'file1', name: 'visible.txt', folderId: mockRootFolderId, deletedAt: '2024-01-01'},
            {id: 'file2', name: 'hidden.txt', folderId: 'otherFolder', deletedAt: '2024-01-02'}
        ];

        binService.getDeletedFilesAndFolders.mockResolvedValue({
            files: mockFiles,
            folders: []
        });

        render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(screen.getByText("visible.txt")).toBeInTheDocument();
            expect(screen.queryByText("hidden.txt")).not.toBeInTheDocument();
        });
    });

    test("testing whether error during fetch redirects to login on 401", async () => {
        const error = new Error('401 Unauthorized');
        binService.getDeletedFilesAndFolders.mockRejectedValue(error);

        render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(mockNavigate).toHaveBeenCalledWith('/login', {replace: true});
        });
    });

    test("testing whether getDeletedFilesAndFolders is called with correct parameters", async () => {
        binService.getDeletedFilesAndFolders.mockResolvedValue({
            files: [],
            folders: []
        });

        render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        await waitFor(() => {
            expect(binService.getDeletedFilesAndFolders).toHaveBeenCalledWith(
                mockRootFolderId,
                mockUserId
            );
        });
    });

    test("testing whether component returns null when user is not logged in", () => {
        localStorage.removeItem('userId');

        const {container} = render(
            <MemoryRouter>
                <Bin />
            </MemoryRouter>
        );

        expect(container.firstChild).toBeNull();
    });
});