import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { useNavigate } from 'react-router-dom';
import Home from './home';
import { useFileNavigation } from '../../hooks/useFileNavigation';
import { getFilesAndFolders } from '../../services/fileService';
import { getUserInfo } from '../../services/authService';

// Mock react-router-dom
jest.mock('react-router-dom', () => ({
    useNavigate: jest.fn()
}));

// Mock custom hook
jest.mock('../../hooks/useFileNavigation', () => ({
    useFileNavigation: jest.fn()
}));

// Mock services
jest.mock('../../services/fileService', () => ({
    getFilesAndFolders: jest.fn()
}));

jest.mock('../../services/authService', () => ({
    getUserInfo: jest.fn()
}));

// Mock components
jest.mock('../../components/Layout/Header', () => {
    return function Header({ onFileUploadSuccess }) {
        return (
            <div data-testid="header">
                Header
                <button onClick={onFileUploadSuccess}>Upload Success</button>
            </div>
        );
    };
});

jest.mock('../../components/SideBar/SideBar', () => {
    return function Sidebar() {
        return <div data-testid="sidebar">Sidebar</div>;
    };
});

jest.mock('../../components/Navigation/FolderPath', () => {
    return function FolderPath({ navigationPath, onBackward, onPathClick }) {
        return (
            <div data-testid="folder-path">
                FolderPath: {navigationPath.map(p => p.name).join(' > ')}
                <button onClick={onBackward}>Back</button>
                <button onClick={() => onPathClick(navigationPath[0])}>Path Click</button>
            </div>
        );
    };
});

jest.mock('../../components/FileList/FolderRow', () => {
    return function FolderRow({ folder, onClick }) {
        return (
            <tr data-testid={`folder-${folder.id}`}>
                <td onClick={() => onClick(folder.id)}>{folder.name}</td>
            </tr>
        );
    };
});

jest.mock('../../components/FileList/FileRow', () => {
    return function FileRow({ file, onClick }) {
        return (
            <tr data-testid={`file-${file.id}`}>
                <td onClick={() => onClick(file.id)}>{file.name}</td>
            </tr>
        );
    };
});

jest.mock('../../components/Button/OperateSpecificFile/OperateSpecificFile', () => {
    return function OperateSpecificFile() {
        return <div>File Actions</div>;
    };
});

jest.mock('../../components/Button/OperateSpecificFolder/OperateSpecificFolder', () => {
    return function OperateSpecificFolder() {
        return <div>Folder Actions</div>;
    };
});

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

describe('Home', () => {
    let mockNavigate;
    let mockSetRootFolderId;
    let mockSetNavigationPath;
    let mockHandleFolderClick;
    let mockHandleBackward;
    let mockHandlePathClick;

    beforeEach(() => {
        jest.clearAllMocks();

        mockNavigate = jest.fn();
        useNavigate.mockReturnValue(mockNavigate);

        mockSetRootFolderId = jest.fn();
        mockSetNavigationPath = jest.fn();
        mockHandleFolderClick = jest.fn();
        mockHandleBackward = jest.fn();
        mockHandlePathClick = jest.fn();

        useFileNavigation.mockReturnValue({
            rootFolderId: null,
            setRootFolderId: mockSetRootFolderId,
            navigationPath: [{ id: 'root', name: 'root' }],
            setNavigationPath: mockSetNavigationPath,
            handleFolderClick: mockHandleFolderClick,
            handleBackward: mockHandleBackward,
            handlePathClick: mockHandlePathClick
        });

        mockLocalStorage.getItem.mockImplementation((key) => {
            if (key === 'userId') return 'user123';
            if (key === 'rootFolderId') return 'root123';
            return null;
        });
    });

    describe('Initial render and user info fetch', () => {
        test('should fetch user info on mount', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(getUserInfo).toHaveBeenCalled();
            });
        });

        test('should store user info in localStorage', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user456',
                userName: 'johndoe'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(mockLocalStorage.setItem).toHaveBeenCalledWith('userId', 'user456');
                expect(mockLocalStorage.setItem).toHaveBeenCalledWith('user456', 'johndoe');
            });
        });

        test('should handle getUserInfo error gracefully', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            getUserInfo.mockRejectedValueOnce(new Error('Network error'));

            render(<Home />);

            await waitFor(() => {
                expect(consoleErrorSpy).toHaveBeenCalledWith(
                    'Failed to fetch user info:',
                    expect.any(Error)
                );
            });

            consoleErrorSpy.mockRestore();
        });
    });

    describe('Files and folders fetching', () => {
        test('should fetch files and folders on mount', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [
                    { id: 'file1', name: 'document.pdf' }
                ],
                folders: [
                    { id: 'folder1', name: 'Documents' }
                ]
            });

            render(<Home />);

            await waitFor(() => {
                expect(getFilesAndFolders).toHaveBeenCalledWith(null, 'user123');
            });
        });

        test('should display fetched files and folders', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [
                    { id: 'file1', name: 'document.pdf' },
                    { id: 'file2', name: 'image.png' }
                ],
                folders: [
                    { id: 'folder1', name: 'Documents' },
                    { id: 'folder2', name: 'Images' }
                ]
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('folder-folder1')).toBeInTheDocument();
                expect(screen.getByTestId('folder-folder2')).toBeInTheDocument();
                expect(screen.getByTestId('file-file1')).toBeInTheDocument();
                expect(screen.getByTestId('file-file2')).toBeInTheDocument();
            });
        });

        test('should set root folder ID when not set', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: [],
                rootFolderId: 'root-folder-123'
            });

            render(<Home />);

            await waitFor(() => {
                expect(mockSetRootFolderId).toHaveBeenCalledWith('root-folder-123');
                expect(mockLocalStorage.setItem).toHaveBeenCalledWith('rootFolderId', 'root-folder-123');
                expect(mockSetNavigationPath).toHaveBeenCalledWith([
                    { id: 'root-folder-123', name: 'root' }
                ]);
            });
        });

        test('should handle fetch error gracefully', async () => {
            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockRejectedValueOnce(new Error('Network error'));

            render(<Home />);

            await waitFor(() => {
                expect(consoleErrorSpy).toHaveBeenCalledWith(
                    'Error fetching data:',
                    expect.any(Error)
                );
            });

            consoleErrorSpy.mockRestore();
        });

        test('should display "No files found" message when no files', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByText('No files found')).toBeInTheDocument();
            });
        });

        test('should not display "No files found" when files exist', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [{ id: 'file1', name: 'test.pdf' }],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.queryByText('No files found')).not.toBeInTheDocument();
            });
        });
    });

    describe('File and folder interactions', () => {
        test('should navigate to editor when file is clicked', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [{ id: 'file1', name: 'document.pdf' }],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('file-file1')).toBeInTheDocument();
            });

            fireEvent.click(screen.getByText('document.pdf'));

            expect(mockNavigate).toHaveBeenCalledWith('/editor/file1');
        });

        test('should call handleFolderClick when folder is clicked', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: [{ id: 'folder1', name: 'Documents' }]
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('folder-folder1')).toBeInTheDocument();
            });

            fireEvent.click(screen.getByText('Documents'));

            expect(mockHandleFolderClick).toHaveBeenCalledWith('folder1');
        });
    });

    describe('File upload', () => {
        test('should refresh files after upload success', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders
                .mockResolvedValueOnce({
                    files: [],
                    folders: []
                })
                .mockResolvedValueOnce({
                    files: [{ id: 'newfile', name: 'uploaded.pdf' }],
                    folders: []
                });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('header')).toBeInTheDocument();
            });

            // Trigger upload success
            fireEvent.click(screen.getByText('Upload Success'));

            await waitFor(() => {
                expect(getFilesAndFolders).toHaveBeenCalledTimes(2);
            });
        });
    });

    describe('Navigation', () => {
        test('should call handleBackward when back button is clicked', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('folder-path')).toBeInTheDocument();
            });

            fireEvent.click(screen.getByText('Back'));

            expect(mockHandleBackward).toHaveBeenCalled();
        });

        test('should call handlePathClick when path is clicked', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('folder-path')).toBeInTheDocument();
            });

            fireEvent.click(screen.getByText('Path Click'));

            expect(mockHandlePathClick).toHaveBeenCalled();
        });
    });

    describe('Component layout', () => {
        test('should render Header component', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('header')).toBeInTheDocument();
            });
        });

        test('should render Sidebar component', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('sidebar')).toBeInTheDocument();
            });
        });

        test('should render FolderPath component', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('folder-path')).toBeInTheDocument();
            });
        });

        test('should render file table with headers', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByText('Name')).toBeInTheDocument();
                expect(screen.getByText('Last Modified')).toBeInTheDocument();
                expect(screen.getByText('Size')).toBeInTheDocument();
            });
        });
    });

    describe('Effect dependencies', () => {
        test('should refetch when rootFolderId changes', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValue({
                files: [],
                folders: []
            });

            const { rerender } = render(<Home />);

            await waitFor(() => {
                expect(getFilesAndFolders).toHaveBeenCalledTimes(1);
            });

            // Change rootFolderId
            useFileNavigation.mockReturnValue({
                rootFolderId: 'newfolder123',
                setRootFolderId: mockSetRootFolderId,
                navigationPath: [{ id: 'newfolder123', name: 'New Folder' }],
                setNavigationPath: mockSetNavigationPath,
                handleFolderClick: mockHandleFolderClick,
                handleBackward: mockHandleBackward,
                handlePathClick: mockHandlePathClick
            });

            rerender(<Home />);

            await waitFor(() => {
                expect(getFilesAndFolders).toHaveBeenCalledTimes(2);
            });
        });

        test('should not fetch if userId is not available', async () => {
            getUserInfo.mockResolvedValueOnce(null);
            mockLocalStorage.getItem.mockReturnValue(null);

            const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation();

            render(<Home />);

            await waitFor(() => {
                expect(consoleErrorSpy).toHaveBeenCalledWith('No userId available');
            });

            consoleErrorSpy.mockRestore();
        });
    });

    describe('localStorage integration', () => {
        test('should pass userId from localStorage to Header', async () => {
            getUserInfo.mockResolvedValueOnce({
                userId: 'user123',
                userName: 'testuser'
            });

            getFilesAndFolders.mockResolvedValueOnce({
                files: [],
                folders: []
            });

            mockLocalStorage.getItem.mockReturnValue('stored-user-123');

            render(<Home />);

            await waitFor(() => {
                expect(screen.getByTestId('header')).toBeInTheDocument();
            });

            // Header should receive userId from localStorage
            expect(mockLocalStorage.getItem).toHaveBeenCalledWith('userId');
        });
    });
});