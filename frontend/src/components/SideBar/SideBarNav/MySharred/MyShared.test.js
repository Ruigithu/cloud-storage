import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import MyShared from './MyShared';

// Mock Sidebar component
jest.mock('../../SideBar', () => {
    return function MockSidebar() {
        return <div data-testid="sidebar">Sidebar</div>;
    };
});

// Mock OperateSpecificSharedFile component
jest.mock('./OperateSpecificSharedFile/OperateSpecificSharedFile', () => {
    return function MockOperateSpecificSharedFile({ shareId, userId, active }) {
        return (
            <div data-testid="operate-file" data-share-id={shareId} data-user-id={userId} data-active={active}>
                Operate Menu
            </div>
        );
    };
});

// Mock CSS
jest.mock('./MyShared.css', () => ({}));

// Mock image import
jest.mock('../../../../assets/images/cloudversify-brands-solid.svg', () => 'drive-icon.svg');

// Mock apiRequest
const mockApiRequest = jest.fn();
jest.mock('../../../../utils/apiRequest', () => (...args) => mockApiRequest(...args));

// Mock localStorage
const mockLocalStorage = {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
};

Object.defineProperty(window, 'localStorage', {
    value: mockLocalStorage,
    writable: true,
});

// Mock environment variable
process.env.REACT_APP_API_URL = 'http://test-api.com';

// Helper function to render with Router
const renderWithRouter = (component) => {
    return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('MyShared', () => {
    const mockUserId = 'user-123';
    const mockSharedFiles = [
        {
            id: 'share-1',
            fileName: 'Document1.pdf',
            shareLink: 'https://share.link/abc123',
            expiresAt: '2025-12-31',
            type: 'view',
            active: true,
        },
        {
            id: 'share-2',
            fileName: 'Spreadsheet.xlsx',
            shareLink: 'https://share.link/def456',
            expiresAt: '2025-11-30',
            type: 'edit',
            active: true,
        },
        {
            id: 'share-3',
            fileName: 'Canceled.docx',
            shareLink: 'https://share.link/ghi789',
            expiresAt: '2025-10-15',
            type: 'view',
            active: false,
        },
    ];

    beforeEach(() => {
        jest.clearAllMocks();
        mockLocalStorage.getItem.mockReturnValue(mockUserId);
    });

    describe('Component Rendering', () => {
        test('should render the component', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            expect(screen.getByTestId('sidebar')).toBeInTheDocument();
        });

        test('should render header with drive icon', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            const driveIcon = document.querySelector('.drive-icon');
            expect(driveIcon).toBeInTheDocument();
            expect(driveIcon).toHaveAttribute('alt', 'drive-icon');
        });

        test('should render search bar', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            const searchBox = screen.getByPlaceholderText('search in the drive');
            expect(searchBox).toBeInTheDocument();
        });

        test('should render table headers', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(screen.getByText('FileName')).toBeInTheDocument();
                expect(screen.getByText('ShareLink')).toBeInTheDocument();
                expect(screen.getByText('Expires At')).toBeInTheDocument();
                expect(screen.getByText('Authorization')).toBeInTheDocument();
            });
        });
    });

    describe('Data Fetching', () => {
        test('should fetch shared files on mount', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(mockApiRequest).toHaveBeenCalledWith(
                    `http://test-api.com/getAllSharedFiles?ownerId=${mockUserId}`,
                    expect.objectContaining({
                        method: 'GET',
                        headers: { 'Content-Type': 'application/json' },
                        credentials: 'include',
                    })
                );
            });
        });

        test('should get userId from localStorage', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(mockLocalStorage.getItem).toHaveBeenCalledWith('userId');
            });
        });

        test('should handle API error gracefully', async () => {
            const consoleError = jest.spyOn(console, 'error').mockImplementation(() => {});

            mockApiRequest.mockRejectedValue(new Error('API Error'));

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(consoleError).toHaveBeenCalledWith(
                    'Error fetching data:',
                    expect.any(Error)
                );
            });

            consoleError.mockRestore();
        });

        test('should not fetch if userId is null', async () => {
            mockLocalStorage.getItem.mockReturnValue(null);

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(mockApiRequest).not.toHaveBeenCalled();
            });
        });
    });

    describe('Data Display', () => {
        test('should display shared files data', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(screen.getByText('Document1.pdf')).toBeInTheDocument();
                expect(screen.getByText('Spreadsheet.xlsx')).toBeInTheDocument();
                expect(screen.getByText('Canceled.docx')).toBeInTheDocument();
            });
        });

        test('should display share links', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(screen.getByText('https://share.link/abc123')).toBeInTheDocument();
                expect(screen.getByText('https://share.link/def456')).toBeInTheDocument();
            });
        });

        test('should display expiration dates for active shares', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(screen.getByText('2025-12-31')).toBeInTheDocument();
                expect(screen.getByText('2025-11-30')).toBeInTheDocument();
            });
        });

        test('should display "canceled" for inactive shares', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                const canceledText = screen.getByText('canceled');
                expect(canceledText).toBeInTheDocument();
                expect(canceledText).toHaveStyle({ color: 'red' });
            });
        });

        test('should display authorization types', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                const viewElements = screen.getAllByText('view');
                const editElements = screen.getAllByText('edit');

                expect(viewElements.length).toBeGreaterThan(0);
                expect(editElements.length).toBeGreaterThan(0);
            });
        });

        test('should render OperateSpecificSharedFile for each share', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                const operateMenus = screen.getAllByTestId('operate-file');
                expect(operateMenus).toHaveLength(3);
            });
        });

        test('should pass correct props to OperateSpecificSharedFile', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                const firstOperateMenu = screen.getAllByTestId('operate-file')[0];
                expect(firstOperateMenu).toHaveAttribute('data-share-id', 'share-1');
                expect(firstOperateMenu).toHaveAttribute('data-user-id', mockUserId);
                expect(firstOperateMenu).toHaveAttribute('data-active', 'true');
            });
        });
    });

    describe('Empty State', () => {
        test('should show "No files found" when shares array is empty', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(screen.getByText('No files found')).toBeInTheDocument();
            });
        });

        test('should show "No files found" when shares is null', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => null,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(screen.getByText('No files found')).toBeInTheDocument();
            });
        });

        test('should not show table rows when no data', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                const tableRows = document.querySelectorAll('.share-data');
                expect(tableRows).toHaveLength(0);
            });
        });
    });

    describe('Table Structure', () => {
        test('should render table with correct class', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            const table = document.querySelector('.file-table');
            expect(table).toBeInTheDocument();
        });

        test('should render sort icons in headers', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                const sortIcons = document.querySelectorAll('.fa-sort');
                expect(sortIcons.length).toBeGreaterThan(0);
            });
        });

        test('should render correct number of table rows', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            renderWithRouter(<MyShared />);

            await waitFor(() => {
                const tableRows = document.querySelectorAll('.share-data');
                expect(tableRows).toHaveLength(3);
            });
        });
    });

    describe('Layout Structure', () => {
        test('should have correct container structure', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            const { container } = renderWithRouter(<MyShared />);

            expect(container.querySelector('.container')).toBeInTheDocument();
            expect(container.querySelector('header')).toBeInTheDocument();
            expect(container.querySelector('.home-main-content')).toBeInTheDocument();
        });

        test('should render sidebar in navigation', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            const nav = document.querySelector('nav');
            expect(nav).toBeInTheDocument();
            expect(screen.getByTestId('sidebar')).toBeInTheDocument();
        });

        test('should render main content area', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            renderWithRouter(<MyShared />);

            const mainContent = document.querySelector('.main-content');
            expect(mainContent).toBeInTheDocument();
        });
    });

    describe('Snapshot', () => {
        test('should match snapshot with data', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => mockSharedFiles,
            });

            const { container } = renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(screen.getByText('Document1.pdf')).toBeInTheDocument();
            });

            expect(container).toMatchSnapshot();
        });

        test('should match snapshot without data', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
                json: async () => [],
            });

            const { container } = renderWithRouter(<MyShared />);

            await waitFor(() => {
                expect(screen.getByText('No files found')).toBeInTheDocument();
            });

            expect(container).toMatchSnapshot();
        });
    });
});