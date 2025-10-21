import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import React from 'react';
import CreateFolderDialog from "./CreateFolderDialog";

describe("CreateFolderDialog Component", () => {
    const mockOnCreateFolder = jest.fn();

    beforeEach(() => {
        jest.clearAllMocks();
    });

    test("testing whether trigger text is rendered", () => {
        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        expect(screen.getByText("Create New Folder")).toBeInTheDocument();
    });

    test("testing whether modal is hidden by default", () => {
        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        expect(screen.queryByPlaceholderText("Enter folder name")).not.toBeInTheDocument();
    });

    test("testing whether clicking trigger shows the modal", () => {
        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        expect(screen.getByPlaceholderText("Enter folder name")).toBeInTheDocument();
    });

    test("testing whether modal displays correct title", () => {
        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const titles = screen.getAllByText("Create New Folder");
        expect(titles.length).toBeGreaterThan(1);
    });

    test("testing whether input field is rendered in modal", () => {
        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        expect(input).toBeInTheDocument();
    });

    test("testing whether Cancel and Create buttons are rendered", () => {
        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        expect(screen.getByText("Cancel")).toBeInTheDocument();
        expect(screen.getByText("Create")).toBeInTheDocument();
    });

    test("testing whether typing in input updates its value", () => {
        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        fireEvent.change(input, {target: {value: 'My New Folder'}});

        expect(input).toHaveValue('My New Folder');
    });

    test("testing whether clicking Cancel closes the modal", async () => {
        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const cancelButton = screen.getByText("Cancel");
        fireEvent.click(cancelButton);

        await waitFor(() => {
            expect(screen.queryByPlaceholderText("Enter folder name")).not.toBeInTheDocument();
        });
    });

    test("testing whether submitting form calls onCreateFolder with folder name", async () => {
        mockOnCreateFolder.mockResolvedValueOnce();

        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        fireEvent.change(input, {target: {value: 'Test Folder'}});

        const createButton = screen.getByText("Create");
        fireEvent.click(createButton);

        await waitFor(() => {
            expect(mockOnCreateFolder).toHaveBeenCalledWith('Test Folder');
        });
    });

    test("testing whether folder name is trimmed before submission", async () => {
        mockOnCreateFolder.mockResolvedValueOnce();

        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        fireEvent.change(input, {target: {value: '  Test Folder  '}});

        const createButton = screen.getByText("Create");
        fireEvent.click(createButton);

        await waitFor(() => {
            expect(mockOnCreateFolder).toHaveBeenCalledWith('Test Folder');
        });
    });

    test("testing whether successful submission closes the modal", async () => {
        mockOnCreateFolder.mockResolvedValueOnce();

        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        fireEvent.change(input, {target: {value: 'Test Folder'}});

        const createButton = screen.getByText("Create");
        fireEvent.click(createButton);

        await waitFor(() => {
            expect(screen.queryByPlaceholderText("Enter folder name")).not.toBeInTheDocument();
        });
    });

    test("testing whether successful submission clears the input", async () => {
        mockOnCreateFolder.mockResolvedValueOnce();

        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        fireEvent.change(input, {target: {value: 'Test Folder'}});

        const createButton = screen.getByText("Create");
        fireEvent.click(createButton);

        await waitFor(() => {
            expect(mockOnCreateFolder).toHaveBeenCalled();
        });

        // Open modal again to check if input is cleared
        fireEvent.click(trigger);
        const newInput = screen.getByPlaceholderText("Enter folder name");
        expect(newInput).toHaveValue('');
    });

    test("testing whether failed submission shows error message", async () => {
        mockOnCreateFolder.mockRejectedValueOnce(new Error('Failed'));

        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        fireEvent.change(input, {target: {value: 'Test Folder'}});

        const createButton = screen.getByText("Create");
        fireEvent.click(createButton);

        await waitFor(() => {
            expect(screen.getByText("Failed to create folder. Please try again.")).toBeInTheDocument();
        });
    });

    test("testing whether buttons are disabled during submission", async () => {
        mockOnCreateFolder.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        fireEvent.change(input, {target: {value: 'Test Folder'}});

        const createButton = screen.getByText("Create");
        fireEvent.click(createButton);

        await waitFor(() => {
            expect(screen.getByText("Creating...")).toBeInTheDocument();
        });

        const cancelButton = screen.getByText("Cancel");
        expect(cancelButton).toBeDisabled();
    });

    test("testing whether Create button shows loading text during submission", async () => {
        mockOnCreateFolder.mockImplementation(() => new Promise(resolve => setTimeout(resolve, 100)));

        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        fireEvent.change(input, {target: {value: 'Test Folder'}});

        const createButton = screen.getByText("Create");
        fireEvent.click(createButton);

        await waitFor(() => {
            expect(screen.getByText("Creating...")).toBeInTheDocument();
        });
    });

    test("testing whether clicking modal overlay does not close the modal", () => {
        const {container} = render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const modalContainer = container.querySelector('.modal-container');
        fireEvent.click(modalContainer);

        expect(screen.getByPlaceholderText("Enter folder name")).toBeInTheDocument();
    });

    test("testing whether input has autoFocus", () => {
        render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = screen.getByPlaceholderText("Enter folder name");
        expect(input).toHaveAttribute('autoFocus');
    });

    test("testing whether form has correct class name", () => {
        const {container} = render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const form = container.querySelector('.form-container-createDialog');
        expect(form).toBeInTheDocument();
    });

    test("testing whether input has correct class name", () => {
        const {container} = render(<CreateFolderDialog onCreateFolder={mockOnCreateFolder} />);

        const trigger = screen.getByText("Create New Folder");
        fireEvent.click(trigger);

        const input = container.querySelector('.form-input-createDialog');
        expect(input).toBeInTheDocument();
    });
});