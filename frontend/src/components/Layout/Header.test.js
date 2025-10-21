import {render, screen} from "@testing-library/react";
import React from 'react';
import Header from "./Header";

jest.mock("../Button/AddNewContextMenu/AddNewContextMenu", () => {
    return function MockAddNewContextMenu({onFileUploadSuccess, parentId, userId}) {
        return (
            <div data-testid="add-new-menu">
                AddNewContextMenu
                <span data-testid="parent-id">{parentId}</span>
                <span data-testid="user-id">{userId}</span>
            </div>
        );
    };
});

jest.mock('../../assets/images/cloudversify-brands-solid.svg', () => 'test-drive-icon.svg');

describe("Header Component", () => {
    const mockOnFileUploadSuccess = jest.fn();
    const mockParentId = 'parent123';
    const mockUserId = 'user456';

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("testing whether drive icon is rendered", () => {
        render(<Header />);

        const driveIcon = screen.getByAltText("drive-icon");
        expect(driveIcon).toBeInTheDocument();
        expect(driveIcon).toHaveClass("drive-icon");
    });

    test("testing whether search input is rendered", () => {
        render(<Header />);

        const searchInput = screen.getByPlaceholderText(/search in the drive/i);
        expect(searchInput).toBeInTheDocument();
        expect(searchInput).toHaveClass("search-box");
    });

    test("testing whether search input has correct size attribute", () => {
        render(<Header />);

        const searchInput = screen.getByPlaceholderText(/search in the drive/i);
        expect(searchInput).toHaveAttribute("size", "50");
    });

    test("testing whether AddNewContextMenu is not rendered when showAddNew is false", () => {
        render(<Header showAddNew={false} />);

        expect(screen.queryByTestId("add-new-menu")).not.toBeInTheDocument();
    });

    test("testing whether AddNewContextMenu is not rendered by default", () => {
        render(<Header />);

        expect(screen.queryByTestId("add-new-menu")).not.toBeInTheDocument();
    });

    test("testing whether AddNewContextMenu is rendered when showAddNew is true", () => {
        render(
            <Header
                showAddNew={true}
                onFileUploadSuccess={mockOnFileUploadSuccess}
                parentId={mockParentId}
                userId={mockUserId}
            />
        );

        expect(screen.getByTestId("add-new-menu")).toBeInTheDocument();
    });

    test("testing whether AddNewContextMenu receives correct props", () => {
        render(
            <Header
                showAddNew={true}
                onFileUploadSuccess={mockOnFileUploadSuccess}
                parentId={mockParentId}
                userId={mockUserId}
            />
        );

        expect(screen.getByTestId("parent-id")).toHaveTextContent(mockParentId);
        expect(screen.getByTestId("user-id")).toHaveTextContent(mockUserId);
    });

    test("testing whether all header elements are rendered together", () => {
        render(
            <Header
                showAddNew={true}
                onFileUploadSuccess={mockOnFileUploadSuccess}
                parentId={mockParentId}
                userId={mockUserId}
            />
        );

        expect(screen.getByAltText("drive-icon")).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/search in the drive/i)).toBeInTheDocument();
        expect(screen.getByTestId("add-new-menu")).toBeInTheDocument();
    });

    test("testing whether drive icon has correct src attribute", () => {
        render(<Header />);

        const driveIcon = screen.getByAltText("drive-icon");
        expect(driveIcon).toHaveAttribute("src", "test-drive-icon.svg");
    });

    test("testing whether header element exists", () => {
        const {container} = render(<Header />);

        const headerElement = container.querySelector("header");
        expect(headerElement).toBeInTheDocument();
    });

    test("testing whether search input is contained in label with search-bar class", () => {
        const {container} = render(<Header />);

        const searchLabel = container.querySelector("label.search-bar");
        expect(searchLabel).toBeInTheDocument();

        const searchInput = searchLabel.querySelector(".search-box");
        expect(searchInput).toBeInTheDocument();
    });
});