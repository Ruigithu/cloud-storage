import {render, screen, fireEvent, waitFor} from "@testing-library/react";
import React from 'react';
import ShareFileDialog from "./ShareFileDialog";
import apiRequest from "../../../utils/apiRequest";

jest.mock("../../../utils/apiRequest");

describe("ShareFileDialog Component", () => {
    const mockFileId = 'file123';
    const mockUserId = 'user456';

    beforeEach(() => {
        jest.clearAllMocks();
        localStorage.setItem('userId', mockUserId);
    });

    afterEach(() => {
        localStorage.clear();
    });

    test("testing whether Share trigger is rendered", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        expect(screen.getByText("Share")).toBeInTheDocument();
    });

    test("testing whether modal is hidden by default", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        expect(screen.queryByText("Share Settings")).not.toBeInTheDocument();
    });

    test("testing whether clicking Share trigger shows the modal", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        expect(screen.getByText("Share Settings")).toBeInTheDocument();
    });

    test("testing whether Access Type select is rendered with default value", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const accessTypeSelect = screen.getByLabelText("Access Type");
        expect(accessTypeSelect).toBeInTheDocument();
        expect(accessTypeSelect).toHaveValue('read');
    });

    test("testing whether Expires After select is rendered with default value", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const expiresSelect = screen.getByLabelText("Expires After");
        expect(expiresSelect).toBeInTheDocument();
        expect(expiresSelect).toHaveValue('day');
    });

    test("testing whether amount input is rendered with default value", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const amountInput = screen.getByDisplayValue('1');
        expect(amountInput).toBeInTheDocument();
        expect(amountInput).toHaveAttribute('type', 'number');
    });

    test("testing whether changing access type updates the select value", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const accessTypeSelect = screen.getByLabelText("Access Type");
        fireEvent.change(accessTypeSelect, {target: {value: 'write'}});

        expect(accessTypeSelect).toHaveValue('write');
    });

    test("testing whether changing duration updates the select value", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const expiresSelect = screen.getByLabelText("Expires After");
        fireEvent.change(expiresSelect, {target: {value: 'week'}});

        expect(expiresSelect).toHaveValue('week');
    });

    test("testing whether changing amount updates the input value", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const amountInput = screen.getByDisplayValue('1');
        fireEvent.change(amountInput, {target: {value: '5'}});

        expect(amountInput).toHaveValue(5);
    });

    test("testing whether amount input is hidden when duration is never", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const expiresSelect = screen.getByLabelText("Expires After");
        fireEvent.change(expiresSelect, {target: {value: 'never'}});

        expect(screen.queryByDisplayValue('1')).not.toBeInTheDocument();
    });

    test("testing whether Create Share Link button is rendered", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        expect(screen.getByText("Create Share Link")).toBeInTheDocument();
    });

    test("testing whether Cancel button is rendered", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        expect(screen.getByText("Cancel")).toBeInTheDocument();
    });

    test("testing whether clicking Cancel closes the modal", async () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const cancelButton = screen.getByText("Cancel");
        fireEvent.click(cancelButton);

        await waitFor(() => {
            expect(screen.queryByText("Share Settings")).not.toBeInTheDocument();
        });
    });

    test("testing whether submitting form calls apiRequest with correct parameters", async () => {
        apiRequest.mockResolvedValueOnce({
            ok: true,
            json: jest.fn().mockResolvedValue({shareLink: 'https://example.com/share/abc123'})
        });

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(apiRequest).toHaveBeenCalledWith(
                expect.stringContaining('/createShareLink'),
                expect.objectContaining({
                    method: 'POST',
                    credentials: 'include',
                })
            );
        });
    });

    test("testing whether FormData contains correct fileId", async () => {
        let capturedFormData;
        apiRequest.mockImplementation((url, options) => {
            capturedFormData = options.body;
            return Promise.resolve({
                ok: true,
                json: jest.fn().mockResolvedValue({shareLink: 'https://example.com/share/abc123'})
            });
        });

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(capturedFormData.get('fileId')).toBe(mockFileId);
        });
    });

    test("testing whether FormData contains correct userId from localStorage", async () => {
        let capturedFormData;
        apiRequest.mockImplementation((url, options) => {
            capturedFormData = options.body;
            return Promise.resolve({
                ok: true,
                json: jest.fn().mockResolvedValue({shareLink: 'https://example.com/share/abc123'})
            });
        });

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(capturedFormData.get('userId')).toBe(mockUserId);
        });
    });

    test("testing whether FormData contains correct accessType", async () => {
        let capturedFormData;
        apiRequest.mockImplementation((url, options) => {
            capturedFormData = options.body;
            return Promise.resolve({
                ok: true,
                json: jest.fn().mockResolvedValue({shareLink: 'https://example.com/share/abc123'})
            });
        });

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const accessTypeSelect = screen.getByLabelText("Access Type");
        fireEvent.change(accessTypeSelect, {target: {value: 'write'}});

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(capturedFormData.get('accessType')).toBe('write');
        });
    });

    test("testing whether successful share displays share link", async () => {
        const mockShareLink = 'https://example.com/share/abc123';
        apiRequest.mockResolvedValueOnce({
            ok: true,
            json: jest.fn().mockResolvedValue({shareLink: mockShareLink})
        });

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(screen.getByDisplayValue(mockShareLink)).toBeInTheDocument();
        });
    });

    test("testing whether share link input is readonly", async () => {
        const mockShareLink = 'https://example.com/share/abc123';
        apiRequest.mockResolvedValueOnce({
            ok: true,
            json: jest.fn().mockResolvedValue({shareLink: mockShareLink})
        });

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            const shareLinkInput = screen.getByDisplayValue(mockShareLink);
            expect(shareLinkInput).toHaveAttribute('readOnly');
        });
    });

    test("testing whether failed share logs error", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockResolvedValueOnce({ok: false});

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalledWith('Share creation failed');
        });

        consoleErrorSpy.mockRestore();
    });

    test("testing whether exception during share logs error", async () => {
        const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
        apiRequest.mockRejectedValueOnce(new Error('Network error'));

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(consoleErrorSpy).toHaveBeenCalledWith(
                'Failed to create share:',
                expect.any(Error)
            );
        });

        consoleErrorSpy.mockRestore();
    });

    test("testing whether closing modal resets all fields", async () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        // Change values
        const accessTypeSelect = screen.getByLabelText("Access Type");
        fireEvent.change(accessTypeSelect, {target: {value: 'write'}});

        const expiresSelect = screen.getByLabelText("Expires After");
        fireEvent.change(expiresSelect, {target: {value: 'week'}});

        const amountInput = screen.getByDisplayValue('1');
        fireEvent.change(amountInput, {target: {value: '5'}});

        // Close modal
        const cancelButton = screen.getByText("Cancel");
        fireEvent.click(cancelButton);

        // Reopen modal
        fireEvent.click(shareTrigger);

        // Check if values are reset
        expect(screen.getByLabelText("Access Type")).toHaveValue('read');
        expect(screen.getByLabelText("Expires After")).toHaveValue('day');
        expect(screen.getByDisplayValue('1')).toBeInTheDocument();
    });

    test("testing whether expiresAt is null when duration is never", async () => {
        let capturedFormData;
        apiRequest.mockImplementation((url, options) => {
            capturedFormData = options.body;
            return Promise.resolve({
                ok: true,
                json: jest.fn().mockResolvedValue({shareLink: 'https://example.com/share/abc123'})
            });
        });

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const expiresSelect = screen.getByLabelText("Expires After");
        fireEvent.change(expiresSelect, {target: {value: 'never'}});

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(capturedFormData.get('expiresAt')).toBe('');
        });
    });

    test("testing whether amount input has correct min and max attributes", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const amountInput = screen.getByDisplayValue('1');
        expect(amountInput).toHaveAttribute('min', '1');
        expect(amountInput).toHaveAttribute('max', '999');
    });

    test("testing whether all access type options are available", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        expect(screen.getByText("Read Only")).toBeInTheDocument();
        expect(screen.getByText("Read & Write")).toBeInTheDocument();
    });

    test("testing whether all duration options are available", () => {
        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        expect(screen.getByText("Hours")).toBeInTheDocument();
        expect(screen.getByText("Days")).toBeInTheDocument();
        expect(screen.getByText("Weeks")).toBeInTheDocument();
        expect(screen.getByText("Months")).toBeInTheDocument();
        expect(screen.getByText("Never")).toBeInTheDocument();
    });

    test("testing whether Share Link label is shown only after successful share", async () => {
        apiRequest.mockResolvedValueOnce({
            ok: true,
            json: jest.fn().mockResolvedValue({shareLink: 'https://example.com/share/abc123'})
        });

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        expect(screen.queryByLabelText("Share Link")).not.toBeInTheDocument();

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(screen.getByLabelText("Share Link")).toBeInTheDocument();
        });
    });

    test("testing whether clicking modal overlay does not close modal", () => {
        const {container} = render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const modalContainer = container.querySelector('.modal-container-share');
        fireEvent.click(modalContainer);

        expect(screen.getByText("Share Settings")).toBeInTheDocument();
    });

    test("testing whether userId defaults to empty string when not in localStorage", async () => {
        localStorage.clear();

        let capturedFormData;
        apiRequest.mockImplementation((url, options) => {
            capturedFormData = options.body;
            return Promise.resolve({
                ok: true,
                json: jest.fn().mockResolvedValue({shareLink: 'https://example.com/share/abc123'})
            });
        });

        render(<ShareFileDialog fileId={mockFileId} />);

        const shareTrigger = screen.getByText("Share");
        fireEvent.click(shareTrigger);

        const submitButton = screen.getByText("Create Share Link");
        fireEvent.click(submitButton);

        await waitFor(() => {
            expect(capturedFormData.get('userId')).toBe('');
        });
    });
});