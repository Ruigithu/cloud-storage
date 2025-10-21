import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Sidebar from './SideBar';

// Mock CSS import
jest.mock('./SideBar.css', () => ({}));

// Helper function to render with Router
const renderWithRouter = (component) => {
    return render(<BrowserRouter>{component}</BrowserRouter>);
};

describe('Sidebar', () => {
    test('should render the sidebar component', () => {
        renderWithRouter(<Sidebar />);

        const sidebar = document.querySelector('.aside-nav');
        expect(sidebar).toBeInTheDocument();
    });

    test('should render all navigation items', () => {
        renderWithRouter(<Sidebar />);

        expect(screen.getByText('Home')).toBeInTheDocument();
        expect(screen.getByText('My Shared')).toBeInTheDocument();
        expect(screen.getByText('Bin')).toBeInTheDocument();
    });

    test('should render correct number of navigation links', () => {
        renderWithRouter(<Sidebar />);

        const navLinks = document.querySelectorAll('.aside-button');
        expect(navLinks).toHaveLength(3);
    });

    test('should render Home link with correct path', () => {
        renderWithRouter(<Sidebar />);

        const homeLink = screen.getByText('Home').closest('a');
        expect(homeLink).toHaveAttribute('href', '/home');
    });

    test('should render My Shared link with correct path', () => {
        renderWithRouter(<Sidebar />);

        const sharedLink = screen.getByText('My Shared').closest('a');
        expect(sharedLink).toHaveAttribute('href', '/my-shared');
    });

    test('should render Bin link with correct path', () => {
        renderWithRouter(<Sidebar />);

        const binLink = screen.getByText('Bin').closest('a');
        expect(binLink).toHaveAttribute('href', '/bin');
    });

    test('should render home icon', () => {
        renderWithRouter(<Sidebar />);

        const homeIcon = document.querySelector('.fa-house');
        expect(homeIcon).toBeInTheDocument();
    });

    test('should render share icon', () => {
        renderWithRouter(<Sidebar />);

        const shareIcon = document.querySelector('.fa-share');
        expect(shareIcon).toBeInTheDocument();
    });

    test('should render trash icon', () => {
        renderWithRouter(<Sidebar />);

        const trashIcon = document.querySelector('.fa-trash');
        expect(trashIcon).toBeInTheDocument();
    });

    test('should have correct icon colors', () => {
        renderWithRouter(<Sidebar />);

        const icons = document.querySelectorAll('i[style*="color"]');
        icons.forEach(icon => {
            expect(icon).toHaveStyle({ color: '#8a8a8a' });
        });
    });

    test('should render links with aside-button class', () => {
        renderWithRouter(<Sidebar />);

        const links = document.querySelectorAll('.aside-button');
        expect(links.length).toBeGreaterThan(0);

        links.forEach(link => {
            expect(link).toHaveClass('aside-button');
        });
    });

    test('should have correct structure for each nav item', () => {
        renderWithRouter(<Sidebar />);

        const navItems = document.querySelectorAll('.aside-button');

        navItems.forEach(item => {
            const icon = item.querySelector('i');
            const text = item.querySelector('span:last-child');

            expect(icon).toBeInTheDocument();
            expect(text).toBeInTheDocument();
        });
    });

    test('should match snapshot', () => {
        const { container } = renderWithRouter(<Sidebar />);
        expect(container).toMatchSnapshot();
    });
});