import { render, screen } from '@testing-library/react';
import ShareCanceled from './ShareCanceled';

describe('ShareCanceled', () => {
    test('should render the component', () => {
        render(<ShareCanceled />);

        const heading = screen.getByRole('heading', { level: 1 });
        expect(heading).toBeInTheDocument();
    });

    test('should display the correct error message', () => {
        render(<ShareCanceled />);

        const message = screen.getByText(/Sorry,this share link has been canceled!/i);
        expect(message).toBeInTheDocument();
    });

    test('should render h1 element with correct text', () => {
        render(<ShareCanceled />);

        const heading = screen.getByRole('heading', { level: 1 });
        expect(heading).toHaveTextContent('Sorry,this share link has been canceled!');
    });

    test('should render the container div', () => {
        const { container } = render(<ShareCanceled />);

        const divElement = container.querySelector('div');
        expect(divElement).toBeInTheDocument();
    });

    test('should match snapshot', () => {
        const { container } = render(<ShareCanceled />);
        expect(container).toMatchSnapshot();
    });
});