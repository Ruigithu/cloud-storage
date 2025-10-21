import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import React from 'react';
import AddNewContextMenu from "./AddNewContextMenu";

jest.mock("../UploadFile/UploadFile", () => {
    return function MockUploadFile({onFileUploadSuccess, folderId, ownerId}) {
        return (
            <div data-testid="upload-file">
                UploadFile
                <span data-testid="folder-id">{folderId}</span>
                <span data-testid="owner-id">{ownerId}</span>
            </div>
        );
    };
});

jest.mock("../CreateFolder/CreateFolder", () => {
    return function MockCreateFolder({onFileUploadSuccess, parentId, userId}) {
        return (
            <div data-testid="create-folder">
                CreateFolder
                <span data-testid="parent-id">{parentId}</span>
                <span data-testid="user-id">{userId}</span>
            </div>
        );
    };
});

jest.mock("../UploadFolder/UploadFolder", () => {
    return function MockUploadFolder({onFileUploadSuccess, parentId, userId}) {
        return (
            <div data-testid="upload-folder">
                UploadFolder
                <span data-testid="parent-id-folder">{parentId}</span>
                <span data-testid="user-id-folder">{userId}</span>
            </div>
        );
    };
});

describe("AddNewContextMenu Component", () => {
    const mockOnFileUploadSuccess = jest.fn();
    const mockParentId = 'parent123';
    const mockUserId = 'user456';

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("testing whether button is rendered with default label", () => {
        render(<AddNewContextMenu />);

        expect(screen.getByText("+ Add New")).toBeInTheDocument();
    });

    test("testing whether button is rendered with custom label", () => {
        render(<AddNewContextMenu buttonLabel="Custom Label" />);

        expect(screen.getByText("Custom Label")).toBeInTheDocument();
    });

    test("testing whether menu is hidden by default", () => {
        render(<AddNewContextMenu />);

        expect(screen.queryByTestId("upload-file")).not.toBeInTheDocument();
        expect(screen.queryByTestId("create-folder")).not.toBeInTheDocument();
        expect(screen.queryByTestId("upload-folder")).not.toBeInTheDocument();
    });

    test("testing whether clicking button shows the menu", () => {
        render(
            <AddNewContextMenu
                onFileUploadSuccess={mockOnFileUploadSuccess}
                parentId={mockParentId}
                userId={mockUserId}
            />
        );

        const button = screen.getByText("+ Add New");
        fireEvent.click(button);

        expect(screen.getByTestId("upload-file")).toBeInTheDocument();
        expect(screen.getByTestId("create-folder")).toBeInTheDocument();
        expect(screen.getByTestId("upload-folder")).toBeInTheDocument();
    });

    test("testing whether clicking button twice toggles menu visibility", () => {
        render(<AddNewContextMenu />);

        const button = screen.getByText("+ Add New");

        fireEvent.click(button);
        expect(screen.getByTestId("upload-file")).toBeInTheDocument();

        fireEvent.click(button);
        expect(screen.queryByTestId("upload-file")).not.toBeInTheDocument();
    });

    test("testing whether clicking outside closes the menu", async () => {
        render(<AddNewContextMenu />);

        const button = screen.getByText("+ Add New");
        fireEvent.click(button);
        expect(screen.getByTestId("upload-file")).toBeInTheDocument();

        fireEvent.click(document.body);

        await waitFor(() => {
            expect(screen.queryByTestId("upload-file")).not.toBeInTheDocument();
        });
    });

    test("testing whether UploadFile receives correct props", () => {
        render(
            <AddNewContextMenu
                onFileUploadSuccess={mockOnFileUploadSuccess}
                parentId={mockParentId}
                userId={mockUserId}
            />
        );

        const button = screen.getByText("+ Add New");
        fireEvent.click(button);

        expect(screen.getByTestId("folder-id")).toHaveTextContent(mockParentId);
        expect(screen.getByTestId("owner-id")).toHaveTextContent(mockUserId);
    });

    test("testing whether CreateFolder receives correct props", () => {
        render(
            <AddNewContextMenu
                onFileUploadSuccess={mockOnFileUploadSuccess}
                parentId={mockParentId}
                userId={mockUserId}
            />
        );

        const button = screen.getByText("+ Add New");
        fireEvent.click(button);

        expect(screen.getByTestId("parent-id")).toHaveTextContent(mockParentId);
        expect(screen.getByTestId("user-id")).toHaveTextContent(mockUserId);
    });

    test("testing whether UploadFolder receives correct props", () => {
        render(
            <AddNewContextMenu
                onFileUploadSuccess={mockOnFileUploadSuccess}
                parentId={mockParentId}
                userId={mockUserId}
            />
        );

        const button = screen.getByText("+ Add New");
        fireEvent.click(button);

        expect(screen.getByTestId("parent-id-folder")).toHaveTextContent(mockParentId);
        expect(screen.getByTestId("user-id-folder")).toHaveTextContent(mockUserId);
    });

    test("testing whether button has correct class name", () => {
        render(<AddNewContextMenu />);

        const button = screen.getByText("+ Add New");
        expect(button).toHaveClass("add-new-button");
    });

    test("testing whether menu container has correct class name", () => {
        const {container} = render(<AddNewContextMenu />);

        const button = screen.getByText("+ Add New");
        fireEvent.click(button);

        const menuContainer = container.querySelector(".menu-container");
        expect(menuContainer).toBeInTheDocument();
    });

    test("testing whether menu list items have correct class name", () => {
        const {container} = render(<AddNewContextMenu />);

        const button = screen.getByText("+ Add New");
        fireEvent.click(button);

        const listItems = container.querySelectorAll(".create-new");
        expect(listItems.length).toBe(3);
    });

    test("testing whether clicking inside menu does not close it", () => {
        const {container} = render(<AddNewContextMenu />);

        const button = screen.getByText("+ Add New");
        fireEvent.click(button);

        const menuContainer = container.querySelector(".menu-container");
        fireEvent.click(menuContainer);

        expect(screen.getByTestId("upload-file")).toBeInTheDocument();
    });

    test("testing whether component works without optional props", () => {
        render(<AddNewContextMenu />);

        const button = screen.getByText("+ Add New");
        fireEvent.click(button);

        expect(screen.getByTestId("upload-file")).toBeInTheDocument();
        expect(screen.getByTestId("create-folder")).toBeInTheDocument();
        expect(screen.getByTestId("upload-folder")).toBeInTheDocument();
    });
});