import {render, screen, fireEvent} from "@testing-library/react";
import React from 'react';
import FolderRow from "./FolderRow";

describe("FolderRow Component", () => {
    const mockOnClick = jest.fn();
    const mockRenderActions = jest.fn();
    const mockUserId = 'user123';

    const mockFolder = {
        id: 'folder456',
        name: 'Test Folder',
        updatedAt: '2024-01-15 10:30:00'
    };

    beforeEach(() => {
        jest.clearAllMocks();
        mockRenderActions.mockReturnValue(<div>Actions</div>);
    });

    test("testing whether folder icon is rendered", () => {
        const {container} = render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const folderIcon = container.querySelector('.fa-folder');
        expect(folderIcon).toBeInTheDocument();
        expect(folderIcon).toHaveClass('fa-solid');
    });

    test("testing whether folder name is rendered", () => {
        render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(screen.getByText("Test Folder")).toBeInTheDocument();
    });

    test("testing whether folder updatedAt is rendered", () => {
        render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(screen.getByText("2024-01-15 10:30:00")).toBeInTheDocument();
    });

    test("testing whether view text is rendered", () => {
        render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(screen.getByText("view")).toBeInTheDocument();
    });

    test("testing whether clicking row calls onClick with folder id and name", () => {
        const {container} = render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const row = container.querySelector('.file-data');
        fireEvent.click(row);

        expect(mockOnClick).toHaveBeenCalledWith('folder456', 'Test Folder');
        expect(mockOnClick).toHaveBeenCalledTimes(1);
    });

    test("testing whether renderActions is called with correct parameters", () => {
        render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    renderActions={mockRenderActions}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(mockRenderActions).toHaveBeenCalledWith(mockFolder, mockUserId);
    });

    test("testing whether renderActions content is displayed", () => {
        render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    renderActions={mockRenderActions}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(screen.getByText("Actions")).toBeInTheDocument();
    });

    test("testing whether row has file-data class", () => {
        const {container} = render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const row = container.querySelector('tr');
        expect(row).toHaveClass('file-data');
    });

    test("testing whether folder icon has correct color", () => {
        const {container} = render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const folderIcon = container.querySelector('.fa-folder');
        expect(folderIcon).toHaveStyle({color: '#ffd129'});
    });

    test("testing whether all four table cells are rendered", () => {
        const {container} = render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const cells = container.querySelectorAll('td');
        expect(cells.length).toBe(4);
    });

    test("testing whether component works without renderActions", () => {
        const {container} = render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const row = container.querySelector('.file-data');
        expect(row).toBeInTheDocument();
        expect(screen.queryByText("Actions")).not.toBeInTheDocument();
    });

    test("testing whether clicking row multiple times calls onClick each time", () => {
        const {container} = render(
            <table>
                <tbody>
                <FolderRow
                    folder={mockFolder}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const row = container.querySelector('.file-data');

        fireEvent.click(row);
        fireEvent.click(row);
        fireEvent.click(row);

        expect(mockOnClick).toHaveBeenCalledTimes(3);
        expect(mockOnClick).toHaveBeenCalledWith('folder456', 'Test Folder');
    });
});