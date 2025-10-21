import {render, screen, fireEvent} from "@testing-library/react";
import React from 'react';
import FileRow from "./FileRow";
import * as fileHelper from '../../utils/fileHelper';

jest.mock('../../utils/fileHelper', () => ({
    getFileIcon: jest.fn(),
    FILE_ICON_COLORS: {
        'fa-file-pdf': '#ff0000',
        'fa-file-word': '#0000ff',
        'fa-file-image': '#00ff00',
        'fa-file': '#808080',
    },
    formatFileSize: jest.fn(),
}));

describe("FileRow Component", () => {
    const mockOnClick = jest.fn();
    const mockRenderActions = jest.fn();
    const mockUserId = 'user123';

    const mockFile = {
        id: 'file789',
        name: 'test-document.pdf',
        updatedAt: '2024-01-20 14:45:00',
        size: 1024000,
        mimeType: 'application/pdf'
    };

    beforeEach(() => {
        jest.clearAllMocks();
        fileHelper.getFileIcon.mockReturnValue('fa-file-pdf');
        fileHelper.formatFileSize.mockReturnValue('1.00 MB');
        mockRenderActions.mockReturnValue(<div>Actions</div>);
    });

    test("testing whether file icon is rendered", () => {
        const {container} = render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const fileIcon = container.querySelector('.fa-file-pdf');
        expect(fileIcon).toBeInTheDocument();
    });

    test("testing whether file name is rendered", () => {
        render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(screen.getByText("test-document.pdf")).toBeInTheDocument();
    });

    test("testing whether file updatedAt is rendered", () => {
        render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(screen.getByText("2024-01-20 14:45:00")).toBeInTheDocument();
    });

    test("testing whether formatted file size is displayed", () => {
        render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(screen.getByText("1.00 MB")).toBeInTheDocument();
    });

    test("testing whether getFileIcon is called with correct mimeType", () => {
        render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(fileHelper.getFileIcon).toHaveBeenCalledWith('application/pdf');
    });

    test("testing whether formatFileSize is called with correct size", () => {
        render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(fileHelper.formatFileSize).toHaveBeenCalledWith(1024000);
    });

    test("testing whether clicking file name calls onClick with file id", () => {
        const {container} = render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const firstCell = container.querySelector('td');
        fireEvent.click(firstCell);

        expect(mockOnClick).toHaveBeenCalledWith('file789');
        expect(mockOnClick).toHaveBeenCalledTimes(1);
    });

    test("testing whether renderActions is called with correct parameters", () => {
        render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    renderActions={mockRenderActions}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(mockRenderActions).toHaveBeenCalledWith(mockFile, mockUserId);
    });

    test("testing whether renderActions content is displayed", () => {
        render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
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
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const row = container.querySelector('tr');
        expect(row).toHaveClass('file-data');
    });

    test("testing whether file icon has correct color from FILE_ICON_COLORS", () => {
        const {container} = render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const fileIcon = container.querySelector('.fa-file-pdf');
        expect(fileIcon).toHaveStyle({color: '#ff0000'});
    });

    test("testing whether all four table cells are rendered", () => {
        const {container} = render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
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
                <FileRow
                    file={mockFile}
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

    test("testing whether different file types get different icons", () => {
        fileHelper.getFileIcon.mockReturnValue('fa-file-word');

        const {container} = render(
            <table>
                <tbody>
                <FileRow
                    file={{...mockFile, mimeType: 'application/msword'}}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const wordIcon = container.querySelector('.fa-file-word');
        expect(wordIcon).toBeInTheDocument();
        expect(wordIcon).toHaveStyle({color: '#0000ff'});
    });

    test("testing whether clicking file name multiple times calls onClick each time", () => {
        const {container} = render(
            <table>
                <tbody>
                <FileRow
                    file={mockFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        const firstCell = container.querySelector('td');

        fireEvent.click(firstCell);
        fireEvent.click(firstCell);

        expect(mockOnClick).toHaveBeenCalledTimes(2);
        expect(mockOnClick).toHaveBeenCalledWith('file789');
    });

    test("testing whether component handles image files correctly", () => {
        fileHelper.getFileIcon.mockReturnValue('fa-file-image');

        const imageFile = {
            ...mockFile,
            name: 'photo.jpg',
            mimeType: 'image/jpeg'
        };

        const {container} = render(
            <table>
                <tbody>
                <FileRow
                    file={imageFile}
                    onClick={mockOnClick}
                    userId={mockUserId}
                />
                </tbody>
            </table>
        );

        expect(screen.getByText("photo.jpg")).toBeInTheDocument();
        const imageIcon = container.querySelector('.fa-file-image');
        expect(imageIcon).toBeInTheDocument();
        expect(imageIcon).toHaveStyle({color: '#00ff00'});
    });
});