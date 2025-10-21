import {render, screen, fireEvent} from "@testing-library/react";
import React from 'react';
import FolderPath from "./FolderPath";

describe("FolderPath Component", () => {
    const mockOnBackward = jest.fn();
    const mockOnPathClick = jest.fn();

    const singleFolderPath = [
        {id: 'root', name: 'Root'}
    ];

    const multipleFolderPath = [
        {id: 'root', name: 'Root'},
        {id: 'folder1', name: 'Documents'},
        {id: 'folder2', name: 'Work'}
    ];

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("testing whether backward icon is rendered", () => {
        const {container} = render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        const backwardIcon = container.querySelector('.fa-backward');
        expect(backwardIcon).toBeInTheDocument();
    });

    test("testing whether all folder names are rendered", () => {
        render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        expect(screen.getByText("Root")).toBeInTheDocument();
        expect(screen.getByText("Documents")).toBeInTheDocument();
        expect(screen.getByText("Work")).toBeInTheDocument();
    });

    test("testing whether folder separators are rendered correctly", () => {
        const {container} = render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        const separators = container.querySelectorAll('span');
        const separatorTexts = Array.from(separators)
            .map(span => span.textContent)
            .filter(text => text === '>');

        expect(separatorTexts.length).toBe(2);
    });

    test("testing whether clicking backward icon calls onBackward", () => {
        const {container} = render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        const backwardIcon = container.querySelector('.fa-backward');
        fireEvent.click(backwardIcon);

        expect(mockOnBackward).toHaveBeenCalledTimes(1);
    });

    test("testing whether clicking folder name calls onPathClick with correct index", () => {
        render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        const documentsFolder = screen.getByText("Documents");
        fireEvent.click(documentsFolder);

        expect(mockOnPathClick).toHaveBeenCalledWith(1);
    });

    test("testing whether clicking root folder calls onPathClick with index 0", () => {
        render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        const rootFolder = screen.getByText("Root");
        fireEvent.click(rootFolder);

        expect(mockOnPathClick).toHaveBeenCalledWith(0);
    });

    test("testing whether backward icon is disabled when only one folder", () => {
        const {container} = render(
            <FolderPath
                navigationPath={singleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        const backwardIcon = container.querySelector('.fa-backward');
        expect(backwardIcon).toHaveStyle({opacity: '0.5'});
        expect(backwardIcon).toHaveStyle({cursor: 'not-allowed'});
    });

    test("testing whether backward icon is enabled when multiple folders", () => {
        const {container} = render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        const backwardIcon = container.querySelector('.fa-backward');
        expect(backwardIcon).toHaveStyle({opacity: '1'});
        expect(backwardIcon).toHaveStyle({cursor: 'pointer'});
    });

    test("testing whether no separator is shown after last folder", () => {
        render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        const workFolder = screen.getByText("Work");
        const parentSpan = workFolder.closest('span');

        expect(parentSpan.textContent).not.toContain('>');
    });

    test("testing whether clicking different folders calls onPathClick with different indexes", () => {
        render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        fireEvent.click(screen.getByText("Root"));
        expect(mockOnPathClick).toHaveBeenCalledWith(0);

        fireEvent.click(screen.getByText("Documents"));
        expect(mockOnPathClick).toHaveBeenCalledWith(1);

        fireEvent.click(screen.getByText("Work"));
        expect(mockOnPathClick).toHaveBeenCalledWith(2);

        expect(mockOnPathClick).toHaveBeenCalledTimes(3);
    });

    test("testing whether folder names have pointer cursor", () => {
        render(
            <FolderPath
                navigationPath={multipleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        const rootFolder = screen.getByText("Root");
        expect(rootFolder).toHaveStyle({cursor: 'pointer'});
    });

    test("testing whether single folder path renders correctly without separator", () => {
        const {container} = render(
            <FolderPath
                navigationPath={singleFolderPath}
                onBackward={mockOnBackward}
                onPathClick={mockOnPathClick}
            />
        );

        expect(screen.getByText("Root")).toBeInTheDocument();

        const separators = container.querySelectorAll('span');
        const hasSeparator = Array.from(separators).some(span => span.textContent === '>');

        expect(hasSeparator).toBe(false);
    });
});