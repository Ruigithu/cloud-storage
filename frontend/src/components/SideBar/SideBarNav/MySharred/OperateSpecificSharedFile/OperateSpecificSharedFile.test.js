import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import OperateSpecificSharedFile from './OperateSpecificSharedFile';

// Mock CSS
jest.mock('./OperateSpecificSharedFile.css', () => ({}));

// Mock apiRequest
const mockApiRequest = jest.fn();
jest.mock('../../../../../utils/apiRequest', () => (...args) => mockApiRequest(...args));

// Mock environment variable
process.env.REACT_APP_API_URL = 'http://test-api.com';

describe('OperateSpecificSharedFile', () => {
    const mockProps = {
        shareId: 'share-123',
        userId: 'user-456',
        active: true,
        refresh: jest.fn(),
    };

    beforeEach(() => {
        jest.clearAllMocks();
        global.alert = jest.fn();
    });

    describe('Component Rendering', () => {
        test('should render the component', () => {
            render(<OperateSpecificSharedFile {...mockProps} />);

            const container = document.querySelector('.operate-file-menu-container');
            expect(container).toBeInTheDocument();
        });

        test('should render ellipsis button', () => {
            render(<OperateSpecificSharedFile {...mockProps} />);

            const button = document.querySelector('.fa-ellipsis');
            expect(button).toBeInTheDocument();
            expect(button).toHaveClass('more');
        });

        test('should not show menu initially', () => {
            render(<OperateSpecificSharedFile {...mockProps} />);

            const menu = document.querySelector('.menu-container');
            expect(menu).not.toBeInTheDocument();
        });
    });

    describe('Menu Toggle', () => {
        test('should show menu when button is clicked', () => {
            render(<OperateSpecificSharedFile {...mockProps} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const menu = document.querySelector('.menu-container');
            expect(menu).toBeInTheDocument();
        });

        test('should hide menu when button is clicked again', () => {
            render(<OperateSpecificSharedFile {...mockProps} />);

            const button = document.querySelector('.fa-ellipsis');

            // Show menu
            fireEvent.click(button);
            expect(document.querySelector('.menu-container')).toBeInTheDocument();

            // Hide menu
            fireEvent.click(button);
            expect(document.querySelector('.menu-container')).not.toBeInTheDocument();
        });

        test('should stop propagation when button is clicked', () => {
            const mockStopPropagation = jest.fn();
            render(<OperateSpecificSharedFile {...mockProps} />);

            const button = document.querySelector('.fa-ellipsis');
            const event = new MouseEvent('click', { bubbles: true });
            event.stopPropagation = mockStopPropagation;

            fireEvent(button, event);

            expect(mockStopPropagation).toHaveBeenCalled();
        });
    });

    describe('Click Outside to Close', () => {
        test('should close menu when clicking outside', () => {
            render(<OperateSpecificSharedFile {...mockProps} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            expect(document.querySelector('.menu-container')).toBeInTheDocument();

            // Click outside
            fireEvent.click(document.body);

            expect(document.querySelector('.menu-container')).not.toBeInTheDocument();
        });

        test('should not close menu when clicking inside menu', () => {
            render(<OperateSpecificSharedFile {...mockProps} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const menu = document.querySelector('.menu-container');
            fireEvent.click(menu);

            expect(document.querySelector('.menu-container')).toBeInTheDocument();
        });

        test('should not close menu when clicking the button', () => {
            render(<OperateSpecificSharedFile {...mockProps} />);

            const button = document.querySelector('.fa-ellipsis');

            // Open menu
            fireEvent.click(button);
            expect(document.querySelector('.menu-container')).toBeInTheDocument();

            // Click button again (this toggles it closed, but tests the ref logic)
            fireEvent.click(button);

            // The menu should toggle (close in this case)
            expect(document.querySelector('.menu-container')).not.toBeInTheDocument();
        });
    });

    describe('Menu Content - Active Share', () => {
        test('should show "Cancel Share" option when active is true', () => {
            render(<OperateSpecificSharedFile {...mockProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            expect(screen.getByText('Cancel Share')).toBeInTheDocument();
        });

        test('should not show "Restore" option when active is true', () => {
            render(<OperateSpecificSharedFile {...mockProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            expect(screen.queryByText('Restore')).not.toBeInTheDocument();
        });
    });

    describe('Menu Content - Inactive Share', () => {
        test('should show "Restore" option when active is false', () => {
            render(<OperateSpecificSharedFile {...mockProps} active={false} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            expect(screen.getByText('Restore')).toBeInTheDocument();
        });

        test('should not show "Cancel Share" option when active is false', () => {
            render(<OperateSpecificSharedFile {...mockProps} active={false} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            expect(screen.queryByText('Cancel Share')).not.toBeInTheDocument();
        });
    });

    describe('Cancel Share Functionality', () => {
        test('should call API when Cancel Share is clicked', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
            });

            render(<OperateSpecificSharedFile {...mockProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const cancelOption = screen.getByText('Cancel Share');
            fireEvent.click(cancelOption);

            await waitFor(() => {
                expect(mockApiRequest).toHaveBeenCalledWith(
                    `http://test-api.com/cancelShare?shareId=${mockProps.shareId}&ownerId=${mockProps.userId}`,
                    expect.objectContaining({
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        credentials: 'include',
                    })
                );
            });
        });

        test('should show success alert after canceling share', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
            });

            render(<OperateSpecificSharedFile {...mockProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const cancelOption = screen.getByText('Cancel Share');
            fireEvent.click(cancelOption);

            await waitFor(() => {
                expect(global.alert).toHaveBeenCalledWith('Share canceled successfully');
            });
        });

        test('should show error alert when cancel fails', async () => {
            mockApiRequest.mockResolvedValue({
                ok: false,
                statusText: 'Server Error',
            });

            render(<OperateSpecificSharedFile {...mockProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const cancelOption = screen.getByText('Cancel Share');
            fireEvent.click(cancelOption);

            await waitFor(() => {
                expect(global.alert).toHaveBeenCalledWith(
                    expect.stringContaining('Error cancelling share:')
                );
            });
        });

        test('should handle network error when canceling', async () => {
            mockApiRequest.mockRejectedValue(new Error('Network error'));

            render(<OperateSpecificSharedFile {...mockProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const cancelOption = screen.getByText('Cancel Share');
            fireEvent.click(cancelOption);

            await waitFor(() => {
                expect(global.alert).toHaveBeenCalledWith(
                    expect.stringContaining('Error cancelling share:')
                );
            });
        });
    });

    describe('Restore Functionality', () => {
        test('should call API when Restore is clicked', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
            });

            render(<OperateSpecificSharedFile {...mockProps} active={false} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const restoreOption = screen.getByText('Restore');
            fireEvent.click(restoreOption);

            await waitFor(() => {
                expect(mockApiRequest).toHaveBeenCalledWith(
                    `http://test-api.com/restore?shareId=${mockProps.shareId}&ownerId=${mockProps.userId}`,
                    expect.objectContaining({
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        credentials: 'include',
                    })
                );
            });
        });

        test('should show success alert after restoring share', async () => {
            mockApiRequest.mockResolvedValue({
                ok: true,
            });

            render(<OperateSpecificSharedFile {...mockProps} active={false} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const restoreOption = screen.getByText('Restore');
            fireEvent.click(restoreOption);

            await waitFor(() => {
                expect(global.alert).toHaveBeenCalledWith('Share restored successfully');
            });
        });

        test('should show error alert when restore fails', async () => {
            mockApiRequest.mockResolvedValue({
                ok: false,
                statusText: 'Not Found',
            });

            render(<OperateSpecificSharedFile {...mockProps} active={false} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const restoreOption = screen.getByText('Restore');
            fireEvent.click(restoreOption);

            await waitFor(() => {
                expect(global.alert).toHaveBeenCalledWith(
                    expect.stringContaining('Error restoring file:')
                );
            });
        });

        test('should handle network error when restoring', async () => {
            mockApiRequest.mockRejectedValue(new Error('Connection failed'));

            render(<OperateSpecificSharedFile {...mockProps} active={false} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const restoreOption = screen.getByText('Restore');
            fireEvent.click(restoreOption);

            await waitFor(() => {
                expect(global.alert).toHaveBeenCalledWith(
                    expect.stringContaining('Error restoring file:')
                );
            });
        });
    });

    describe('Props Handling', () => {
        test('should use correct shareId in API calls', async () => {
            const customProps = { ...mockProps, shareId: 'custom-share-999' };
            mockApiRequest.mockResolvedValue({ ok: true });

            render(<OperateSpecificSharedFile {...customProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const cancelOption = screen.getByText('Cancel Share');
            fireEvent.click(cancelOption);

            await waitFor(() => {
                expect(mockApiRequest).toHaveBeenCalledWith(
                    expect.stringContaining('shareId=custom-share-999'),
                    expect.any(Object)
                );
            });
        });

        test('should use correct userId in API calls', async () => {
            const customProps = { ...mockProps, userId: 'custom-user-888' };
            mockApiRequest.mockResolvedValue({ ok: true });

            render(<OperateSpecificSharedFile {...customProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const cancelOption = screen.getByText('Cancel Share');
            fireEvent.click(cancelOption);

            await waitFor(() => {
                expect(mockApiRequest).toHaveBeenCalledWith(
                    expect.stringContaining('ownerId=custom-user-888'),
                    expect.any(Object)
                );
            });
        });
    });

    describe('Event Listeners', () => {
        test('should add click event listener on mount', () => {
            const addEventListenerSpy = jest.spyOn(document, 'addEventListener');

            render(<OperateSpecificSharedFile {...mockProps} />);

            expect(addEventListenerSpy).toHaveBeenCalledWith('click', expect.any(Function));

            addEventListenerSpy.mockRestore();
        });

        test('should remove click event listener on unmount', () => {
            const removeEventListenerSpy = jest.spyOn(document, 'removeEventListener');

            const { unmount } = render(<OperateSpecificSharedFile {...mockProps} />);
            unmount();

            expect(removeEventListenerSpy).toHaveBeenCalledWith('click', expect.any(Function));

            removeEventListenerSpy.mockRestore();
        });
    });

    describe('Menu Styling', () => {
        test('should have correct class names on menu elements', () => {
            render(<OperateSpecificSharedFile {...mockProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const menu = document.querySelector('.menu-container');
            const menuList = document.querySelector('.menu-list');

            expect(menu).toHaveClass('menu-container');
            expect(menuList).toHaveClass('menu-list');
        });

        test('should have correct classes on menu item', () => {
            render(<OperateSpecificSharedFile {...mockProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            const menuItem = screen.getByText('Cancel Share');
            expect(menuItem).toHaveClass('restore', 'more');
        });

        test('should have correct icon color', () => {
            render(<OperateSpecificSharedFile {...mockProps} />);

            const icon = document.querySelector('.fa-ellipsis');
            expect(icon).toHaveStyle({ color: '#bcbdbd' });
        });
    });

    describe('Snapshot', () => {
        test('should match snapshot when menu is closed', () => {
            const { container } = render(<OperateSpecificSharedFile {...mockProps} />);
            expect(container).toMatchSnapshot();
        });

        test('should match snapshot when menu is open with active share', () => {
            const { container } = render(<OperateSpecificSharedFile {...mockProps} active={true} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            expect(container).toMatchSnapshot();
        });

        test('should match snapshot when menu is open with inactive share', () => {
            const { container } = render(<OperateSpecificSharedFile {...mockProps} active={false} />);

            const button = document.querySelector('.fa-ellipsis');
            fireEvent.click(button);

            expect(container).toMatchSnapshot();
        });
    });
});