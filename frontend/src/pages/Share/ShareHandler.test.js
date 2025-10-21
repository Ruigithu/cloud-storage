import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { useParams, useNavigate } from 'react-router-dom';
import ShareHandler from './ShareHandler';
import {
    checkShareLink,
    handleReadShare,
    handleWriteShare
} from '../../services/shareService';

// Mock react-router-dom
jest.mock('react-router-dom', () => ({
    useParams: jest.fn(),
    useNavigate: jest.fn()
}));

// Mock shareService
jest.mock('../../services/shareService', () => ({
    checkShareLink: jest.fn(),
    handleReadShare: jest.fn(),
    handleWriteShare: jest.fn()
}));

// Mock localStorage
const mockLocalStorage = {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn()
};

Object.defineProperty(window, 'localStorage', {
    value: mockLocalStorage,
    writable: true
});

describe('ShareHandler', () => {
    let mockNavigate;

    beforeEach(() => {
        jest.clearAllMocks();
        mockNavigate = jest.fn();
        useNavigate.mockReturnValue(mockNavigate);
        mockLocalStorage.getItem.mockImplementation((key) => {
            if (key === 'userId') return 'user123';
            if (key === 'rootFolderId') return 'root123';
            return null;
        });
    });

    describe('Loading state', () => {
        test('should display loading message initially', () => {
            useParams.mockReturnValue({ shareId: 'share123' });
            checkShareLink.mockImplementation(() => new Promise(() => {})); // Never resolves

            render(<ShareHandler />);

            expect(screen.getByText('Loading...')).toBeInTheDocument();
        });
    });

    describe('READ share type', () => {
        test('should handle READ share successfully', async () => {
            useParams.mockReturnValue({ shareId: 'share123' });

            checkShareLink.mockResolvedValueOnce({
                status: 200,
                data: {
                    type: 'READ',
                    fileId: 'file123',
                    fileName: 'document.pdf'
                }
            });

            handleReadShare.mockResolvedValueOnce();

            render(<ShareHandler />);

            await waitFor(() => {
                expect(checkShareLink).toHaveBeenCalledWith('share123', 'user123');
            });

            await waitFor(() => {
                expect(handleReadShare).toHaveBeenCalledWith('file123', 'document.pdf');
            });

            await waitFor(() => {
                expect(mockNavigate).toHaveBeenCalledWith('/');
            });
        });

        test('should download file for READ share', async () => {
            useParams.mockReturnValue({ shareId: 'share456' });

            checkShareLink.mockResolvedValueOnce({
                status: 200,
                data: {
                    type: 'READ',
                    fileId: 'file456',
                    fileName: 'report.xlsx'
                }
            });

            handleReadShare.mockResolvedValueOnce();

            render(<ShareHandler />);

            await waitFor(() => {
                expect(handleReadShare).toHaveBeenCalledWith('file456', 'report.xlsx');
            });
        });
    });

    describe('WRITE share type', () => {
        test('should handle WRITE share successfully', async () => {
            useParams.mockReturnValue({ shareId: 'share789' });

            checkShareLink.mockResolvedValueOnce({
                status: 200,
                data: {
                    type: 'WRITE',
                    fileId: 'file789',
                    fileName: 'document.docx'
                }
            });

            handleWriteShare.mockResolvedValueOnce('newfile123');

            render(<ShareHandler />);

            await waitFor(() => {
                expect(checkShareLink).toHaveBeenCalledWith('share789', 'user123');
            });

            await waitFor(() => {
                expect(handleWriteShare).toHaveBeenCalledWith('share789', 'user123', 'root123');
            });

            await waitFor(() => {
                expect(mockNavigate).toHaveBeenCalledWith('/editor/newfile123');
            });
        });

        test('should navigate to editor with new file ID', async () => {
            useParams.mockReturnValue({ shareId: 'share999' });

            checkShareLink.mockResolvedValueOnce({
                status: 200,
                data: {
                    type: 'WRITE',
                    fileId: 'file999',
                    fileName: 'shared.pdf'
                }
            });

            handleWriteShare.mockResolvedValueOnce('copiedfile456');

            render(<ShareHandler />);

            await waitFor(() => {
                expect(mockNavigate).toHaveBeenCalledWith('/editor/copiedfile456');
            });
        });
    });

    describe('Authentication required', () => {
        test('should redirect to login when authentication needed', async () => {
            useParams.mockReturnValue({ shareId: 'share-auth' });

            checkShareLink.mockResolvedValueOnce({
                status: 401,
                needsAuth: true
            });

            render(<ShareHandler />);

            await waitFor(() => {
                expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
                    'redirectAfterLogin',
                    '/share/share-auth'
                );
            });

            await waitFor(() => {
                expect(mockNavigate).toHaveBeenCalledWith('/login');
            });
        });

        test('should store redirect path before navigating to login', async () => {
            useParams.mockReturnValue({ shareId: 'private-share' });

            checkShareLink.mockResolvedValueOnce({
                status: 401,
                needsAuth: true
            });

            render(<ShareHandler />);

            await waitFor(() => {
                expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
                    'redirectAfterLogin',
                    '/share/private-share'
                );
            });
        });
    });

    describe('Share not found', () => {
        test('should navigate to shareCanceled page when share is not found', async () => {
            useParams.mockReturnValue({ shareId: 'invalid-share' });

            checkShareLink.mockResolvedValueOnce({
                status: 404
            });

            render(<ShareHandler />);

            await waitFor(() => {
                expect(mockNavigate).toHaveBeenCalledWith('/shareCanceled');
            });
        });

        test('should not attempt to process share when status is 404', async () => {
            useParams.mockReturnValue({ shareId: 'deleted-share' });

            checkShareLink.mockResolvedValueOnce({
                status: 404
            });

            render(<ShareHandler />);

            await waitFor(() => {
                expect(mockNavigate).toHaveBeenCalledWith('/shareCanceled');
            });

            expect(handleReadShare).not.toHaveBeenCalled();
            expect(handleWriteShare).not.toHaveBeenCalled();
        });
    });

    describe('Error handling', () => {
        test('should display error message when share check fails', async () => {
            useParams.mockReturnValue({ shareId: 'share123' });

            const errorMessage = 'Network error';
            checkShareLink.mockRejectedValueOnce(new Error(errorMessage));

            render(<ShareHandler />);

            await waitFor(() => {
                expect(screen.getByText(errorMessage)).toBeInTheDocument();
            });
        });

        test('should display generic error when error has no message', async () => {
            useParams.mockReturnValue({ shareId: 'share123' });

            checkShareLink.mockRejectedValueOnce(new Error());

            render(<ShareHandler />);

            await waitFor(() => {
                expect(screen.getByText('Failed to access shared content')).toBeInTheDocument();
            });
        });

        test('should handle error in handleReadShare', async () => {
            useParams.mockReturnValue({ shareId: 'share123' });

            checkShareLink.mockResolvedValueOnce({
                status: 200,
                data: {
                    type: 'READ',
                    fileId: 'file123',
                    fileName: 'document.pdf'
                }
            });

            handleReadShare.mockRejectedValueOnce(new Error('Download failed'));

            render(<ShareHandler />);

            await waitFor(() => {
                expect(screen.getByText('Download failed')).toBeInTheDocument();
            });
        });

        test('should handle error in handleWriteShare', async () => {
            useParams.mockReturnValue({ shareId: 'share123' });

            checkShareLink.mockResolvedValueOnce({
                status: 200,
                data: {
                    type: 'WRITE',
                    fileId: 'file123',
                    fileName: 'document.pdf'
                }
            });

            handleWriteShare.mockRejectedValueOnce(new Error('Save failed'));

            render(<ShareHandler />);

            await waitFor(() => {
                expect(screen.getByText('Save failed')).toBeInTheDocument();
            });
        });

        test('should log error to console', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            useParams.mockReturnValue({ shareId: 'share123' });

            const error = new Error('Test error');
            checkShareLink.mockRejectedValueOnce(error);

            render(<ShareHandler />);

            await waitFor(() => {
                expect(consoleErrorSpy).toHaveBeenCalledWith('Error processing share:', error);
            });

            consoleErrorSpy.mockRestore();
        });
    });

    describe('Component lifecycle', () => {
        test('should only process share once using ref', async () => {
            useParams.mockReturnValue({ shareId: 'share123' });

            checkShareLink.mockResolvedValueOnce({
                status: 200,
                data: {
                    type: 'READ',
                    fileId: 'file123',
                    fileName: 'test.pdf'
                }
            });

            const { rerender } = render(<ShareHandler />);

            await waitFor(() => {
                expect(checkShareLink).toHaveBeenCalledTimes(1);
            });

            // Force re-render
            rerender(<ShareHandler />);

            // Should still only be called once
            expect(checkShareLink).toHaveBeenCalledTimes(1);
        });

        test('should not process if shareId is not available', () => {
            useParams.mockReturnValue({ shareId: null });

            render(<ShareHandler />);

            expect(checkShareLink).not.toHaveBeenCalled();
        });

        test('should get userId from localStorage', async () => {
            useParams.mockReturnValue({ shareId: 'share123' });
            mockLocalStorage.getItem.mockImplementation((key) => {
                if (key === 'userId') return 'custom-user-id';
                return null;
            });

            checkShareLink.mockResolvedValueOnce({
                status: 200,
                data: {
                    type: 'READ',
                    fileId: 'file123',
                    fileName: 'test.pdf'
                }
            });

            render(<ShareHandler />);

            await waitFor(() => {
                expect(checkShareLink).toHaveBeenCalledWith('share123', 'custom-user-id');
            });
        });
    });

    describe('Render states', () => {
        test('should render null when loading is complete and no error', async () => {
            useParams.mockReturnValue({ shareId: 'share123' });

            checkShareLink.mockResolvedValueOnce({
                status: 200,
                data: {
                    type: 'READ',
                    fileId: 'file123',
                    fileName: 'test.pdf'
                }
            });

            handleReadShare.mockResolvedValueOnce();

            const { container } = render(<ShareHandler />);

            await waitFor(() => {
                expect(mockNavigate).toHaveBeenCalled();
            });

            // After navigation, component should render null
            await waitFor(() => {
                expect(container.firstChild).toBeNull();
            });
        });

        test('should not display loading after completion', async () => {
            useParams.mockReturnValue({ shareId: 'share123' });

            checkShareLink.mockResolvedValueOnce({
                status: 404
            });

            render(<ShareHandler />);

            await waitFor(() => {
                expect(mockNavigate).toHaveBeenCalledWith('/shareCanceled');
            });

            await waitFor(() => {
                expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
            });
        });
    });
});