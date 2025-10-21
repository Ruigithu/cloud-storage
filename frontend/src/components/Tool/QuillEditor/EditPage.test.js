import { render, screen } from '@testing-library/react';
import { useParams } from 'react-router-dom';

// Mock react-router-dom BEFORE importing components
jest.mock('react-router-dom', () => ({
    useParams: jest.fn(),
}));

// Mock QuillEditor component BEFORE importing EditorPage
jest.mock('./QuillEditor', () => ({
    __esModule: true,
    default: jest.fn((props) => (
        <div data-testid="quill-editor" data-document-id={props.documentId} data-user-id={props.userId}>
            Mocked QuillEditor
        </div>
    ))
}));

// Now import components AFTER mocks
import EditorPage from './EditPage';
import QuillEditor from './QuillEditor';

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

describe('EditorPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should render the component', () => {
        useParams.mockReturnValue({ fileId: 'test-file-123' });
        mockLocalStorage.getItem.mockReturnValue('test-user-456');

        render(<EditorPage />);

        expect(screen.getByTestId('quill-editor')).toBeInTheDocument();
    });

    test('should pass fileId from URL params to QuillEditor', () => {
        const testFileId = 'document-123';
        useParams.mockReturnValue({ fileId: testFileId });
        mockLocalStorage.getItem.mockReturnValue('user-456');

        render(<EditorPage />);

        expect(QuillEditor).toHaveBeenCalledWith(
            expect.objectContaining({
                documentId: testFileId,
            }),
            expect.anything()
        );
    });

    test('should pass userId from localStorage to QuillEditor', () => {
        const testUserId = 'user-789';
        useParams.mockReturnValue({ fileId: 'file-123' });
        mockLocalStorage.getItem.mockReturnValue(testUserId);

        render(<EditorPage />);

        expect(QuillEditor).toHaveBeenCalledWith(
            expect.objectContaining({
                userId: testUserId,
            }),
            expect.anything()
        );
    });

    test('should get userId from localStorage with correct key', () => {
        useParams.mockReturnValue({ fileId: 'file-123' });
        mockLocalStorage.getItem.mockReturnValue('user-123');

        render(<EditorPage />);

        expect(mockLocalStorage.getItem).toHaveBeenCalledWith('userId');
    });

    test('should render with h-screen class on container div', () => {
        useParams.mockReturnValue({ fileId: 'file-123' });
        mockLocalStorage.getItem.mockReturnValue('user-123');

        const { container } = render(<EditorPage />);

        const divElement = container.querySelector('.h-screen');
        expect(divElement).toBeInTheDocument();
    });

    test('should handle missing fileId param', () => {
        useParams.mockReturnValue({});
        mockLocalStorage.getItem.mockReturnValue('user-123');

        render(<EditorPage />);

        expect(QuillEditor).toHaveBeenCalledWith(
            expect.objectContaining({
                documentId: undefined,
            }),
            expect.anything()
        );
    });

    test('should handle null userId from localStorage', () => {
        useParams.mockReturnValue({ fileId: 'file-123' });
        mockLocalStorage.getItem.mockReturnValue(null);

        render(<EditorPage />);

        expect(QuillEditor).toHaveBeenCalledWith(
            expect.objectContaining({
                userId: null,
            }),
            expect.anything()
        );
    });

    test('should match snapshot', () => {
        useParams.mockReturnValue({ fileId: 'file-123' });
        mockLocalStorage.getItem.mockReturnValue('user-123');

        const { container } = render(<EditorPage />);

        expect(container).toMatchSnapshot();
    });
});