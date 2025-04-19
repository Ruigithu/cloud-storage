import React, {act} from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Provider } from 'react-redux';
import { BrowserRouter } from 'react-router-dom';
import { configureStore } from '@reduxjs/toolkit';
import userReducer from '../../components/Tool/UserInfo/userSlice';
import Home from './Home';

// Mock fetch API
global.fetch = jest.fn();

// Mock environment variables
process.env.REACT_APP_API_URL = 'http://test-api.example.com';

// Mock localStorage
const localStorageMock = (() => {
    let store = {};
    return {
        getItem: jest.fn(key => store[key] || null),
        setItem: jest.fn((key, value) => {
            // 确保实际更新store值
            store[key] = String(value);
        }),
        removeItem: jest.fn(key => {
            delete store[key];
        }),
        clear: jest.fn(() => {
            store = {};
        }),
    };
})();
Object.defineProperty(window, 'localStorage', { value: localStorageMock });

// Mock navigate function
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

// Mock SVG imports
jest.mock('../../assets/images/cloudversify-brands-solid.svg', () => 'mock-drive-icon.svg');

// Setup Redux store for testing
const createTestStore = () =>
    configureStore({
        reducer: {
            user: userReducer,
        },
    });

// Helper function to render component with all required providers
const renderHomeComponent = () => {
    const store = createTestStore();
    return render(
        <Provider store={store}>
            <BrowserRouter>
                <Home />
            </BrowserRouter>
        </Provider>
    );
};

describe('Home Component', () => {
    beforeEach(() => {
        jest.clearAllMocks();
        localStorageMock.clear();
    });

    // 1. Rendering Tests
    describe('Component Rendering', () => {
        test('renders basic UI elements', async () => {
            // 首先设置对getUserInfo的响应
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ userId: '123', userName: 'TestUser' }),
                })
            );

            // 然后设置对getRootFiles的响应
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([]),
                })
            );

            // 最后设置对getRootFolders的响应
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({
                        rootFolderId: 'root1',
                        folders: []
                    }),
                })
            );

            renderHomeComponent();

            // Check for header elements
            expect(screen.getByAltText('drive-icon')).toBeInTheDocument();
            expect(screen.getByPlaceholderText(/search in the drive/i)).toBeInTheDocument();

            // Check for table headers
            expect(screen.getByText('Name')).toBeInTheDocument();
            expect(screen.getByText('Last Modified')).toBeInTheDocument();
            expect(screen.getByText('Size')).toBeInTheDocument();
        });

        test('displays "No files found" when files array is empty', async () => {
            // 设置完整的API调用链
            // 首先设置对getUserInfo的响应
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({
                        userId: '123',
                        userName: 'TestUser'
                    }),
                })
            );

            // 然后设置对getRootFiles的响应
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([]),
                })
            );

            // 最后设置对getRootFolders的响应
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({
                        rootFolderId: '456',
                        folders: []
                    }),
                })
            );

            renderHomeComponent();

            // Wait for the "No files found" message to appear
            await waitFor(() => {
                expect(screen.getByText('No files found')).toBeInTheDocument();
            });
        });
    });

    // 2. API Call Tests
    describe('API Calls and Data Loading', () => {
        test('fetches user info and files/folders on component mount', async () => {
            // 设置串联的API响应
            // getUserInfo
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ userId: '123', userName: 'TestUser' }),
                })
            );

            // getRootFiles
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([
                        { id: 'file1', name: 'Test File 1', size: 1024, mimeType: 'application/pdf' }
                    ]),
                })
            );

            // getRootFolders
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({
                        rootFolderId: 'root1',
                        folders: [
                            { id: 'folder1', name: 'Test Folder 1', updatedAt: '2023-01-01' }
                        ]
                    }),
                })
            );

            renderHomeComponent();

            // 验证所有API调用
            await waitFor(() => {
                const calls = global.fetch.mock.calls;
                expect(calls.length).toBe(5);

                // 第一个调用应该是获取用户信息
                expect(calls[0][0]).toBe('http://test-api.example.com/getUserInfo');

                // 第二个调用应该使用正确的userId获取文件
                expect(calls[1][0]).toBe('http://test-api.example.com/getRootFiles?ownerId=123');

                // 第三个调用应该使用正确的userId获取文件夹
                expect(calls[2][0]).toBe('http://test-api.example.com/getRootFolders?userId=123');
            });

            // 验证localStorage被更新
            expect(localStorageMock.setItem).toHaveBeenCalledWith('userId', '123');
            expect(localStorageMock.setItem).toHaveBeenCalledWith('123', 'TestUser');

            // 验证组件显示了文件和文件夹
            await waitFor(() => {
                expect(screen.getByText('Test File 1')).toBeInTheDocument();
                expect(screen.getByText('Test Folder 1')).toBeInTheDocument();
            });
        });
    });

    // 3. User Interaction Tests
    describe('User Interactions', () => {
        beforeEach(async () => {
            // 设置API响应链
            // getUserInfo
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ userId: '123', userName: 'TestUser' }),
                })
            );

            // getRootFiles
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([]),
                })
            );

            // getRootFolders
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({
                        rootFolderId: 'root1',
                        folders: [
                            { id: 'folder1', name: 'Test Folder 1', updatedAt: '2023-01-01' }
                        ]
                    }),
                })
            );
        });

        test('clicking on a folder updates navigation path and fetches its content', async () => {
            renderHomeComponent();

            // 等待文件夹渲染
            await waitFor(() => {
                expect(screen.getByText('Test Folder 1')).toBeInTheDocument();
            });

            // 清除之前的fetch调用
            global.fetch.mockClear();

            // 设置子文件夹内容的模拟响应
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([]),
                })
            );

            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({
                        folders: [
                            { id: 'subfolder1', name: 'Subfolder 1', updatedAt: '2023-01-02' }
                        ]
                    }),
                })
            );

            // 点击文件夹
            fireEvent.click(screen.getByText('Test Folder 1'));

            renderHomeComponent();

            // 验证导航路径更新
            await waitFor(() => {
                expect(screen.getByText('Subfolder 1')).toBeInTheDocument();
            });
        });

        test('clicking on a file navigates to editor page', async () => {
            // Update mock to include a file
            global.fetch.mockImplementation((url) => {
                if (url.includes('getAllFiles') || url.includes('getRootFiles')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve([
                            { id: 'file1', name: 'Test Document.doc', size: 1024, mimeType: 'application/msword' }
                        ]),
                    });
                } else if (url.includes('getAllFolders') || url.includes('getRootFolders')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve([
                            { id: 'folder1', name: 'Test Folder', updatedAt: '2023-01-01' }
                        ]),
                    });
                }
                // 处理其他API调用
                return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
            });

            renderHomeComponent();

            // Wait for file to be rendered
            await waitFor(() => {
                expect(screen.getByText('Test Document.doc')).toBeInTheDocument();
            });

            // Click on the file
            fireEvent.click(screen.getByTestId('file-name-file1'));

            // Verify navigation to editor
            expect(mockNavigate).toHaveBeenCalledWith('/editor/file1');
        });

        test('backward navigation button works correctly', async () => {
            renderHomeComponent();

            // Setup mock for folder content
            global.fetch.mockImplementation((url) => {
                if (url.includes('getAllFolders') || url.includes('getRootFolders')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            folders: [{ id: 'subfolder1', name: 'Subfolder 1', updatedAt: '2023-01-02' }],
                            rootFolderId: 'root1'
                        }),
                    });
                } else if (url.includes('getAllFiles') || url.includes('getRootFiles')) {
                    // 针对文件请求返回数组格式
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve([])  // 空数组或包含文件的数组
                    });
                } else {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve([])
                    });
                }
            });

            // Wait for folder to be rendered and click on it
            await waitFor(() => {
                expect(screen.getByText('Test Folder 1')).toBeInTheDocument();
            });
            fireEvent.click(screen.getByText('Test Folder 1'));

            // Verify subfolder is displayed after clicking parent folder
            await waitFor(() => {
                expect(screen.getByText('Subfolder 1')).toBeInTheDocument();
            });

            // Clear previous fetch calls
            global.fetch.mockClear();

            // Setup mock for parent folder content
            global.fetch.mockImplementation(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({
                        files: [],
                        folders: [{ id: 'folder1', name: 'Test Folder 1', updatedAt: '2023-01-01' }],
                        rootFolderId: 'root1'  // 添加这个属性以保持一致性
                    }),
                })
            );
            // Click backward button
            const backButton = await screen.findByTestId('backward-icon');
            fireEvent.click(backButton);


            // Verify we're back to showing the parent folder content
            await waitFor(() => {
                expect(screen.getByText('Test Folder 1')).toBeInTheDocument();
                // Subfolder should no longer be in the document
                expect(screen.queryByText('Subfolder 1')).not.toBeInTheDocument();
            });
        });

    // 4. Helper Function Tests
    describe('Helper Functions', () => {
        test('formatFileSize correctly formats different file sizes', () => {
            // We need to access the component's internal function
            // This requires a different approach - we can test this indirectly

            // Mock files with different sizes
            global.fetch.mockImplementationOnce(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({ userId: '123', userName: 'TestUser' }),
                })
            );

            global.fetch.mockImplementation(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([
                        { id: 'file1', name: 'Small File', size: 1024, mimeType: 'text/plain' },
                        { id: 'file2', name: 'Medium File', size: 1048576, mimeType: 'text/plain' }, // 1MB
                        { id: 'file3', name: 'Large File', size: 1073741824, mimeType: 'text/plain' }, // 1GB
                    ]),
                })
            );

            renderHomeComponent();

            // Check if the formatted sizes are displayed correctly
            waitFor(() => {
                expect(screen.getByText('1 KB')).toBeInTheDocument();
                expect(screen.getByText('1 MB')).toBeInTheDocument();
                expect(screen.getByText('1 GB')).toBeInTheDocument();
            });
        });

        test('getFileIcon returns correct icon classes for different file types', async () => {
            // Mock files with different types
            global.fetch.mockImplementation((url) => {
                if (url.includes('getAllFolders') || url.includes('getRootFolders')) {
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve({
                            folders: [{ id: 'subfolder1', name: 'Subfolder 1', updatedAt: '2023-01-02' }],
                            rootFolderId: 'root1'
                        }),
                    });
                } else {
                    // 确保其他所有API调用（包括文件相关的）返回一个空数组而不是undefined
                    return Promise.resolve({
                        ok: true,
                        json: () => Promise.resolve([])
                    });
                }
            });

            global.fetch.mockImplementation(() =>
                Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([
                        { id: 'file1', name: 'Image File', size: 1024, mimeType: 'image/jpeg' },
                        { id: 'file2', name: 'PDF File', size: 2048, mimeType: 'application/pdf' },
                        { id: 'file3', name: 'Word File', size: 3072, mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' },
                    ]),
                })
            );

            renderHomeComponent();

            // Check if the correct icons are displayed
            await waitFor(() => {
                const imageIcon = document.querySelector('.fa-image');
                const pdfIcon = document.querySelector('.fa-file-pdf');
                const wordIcon = document.querySelector('.fa-file-word');

                expect(imageIcon).toBeInTheDocument();
                expect(pdfIcon).toBeInTheDocument();
                expect(wordIcon).toBeInTheDocument();
            });
        });
    });
});
});