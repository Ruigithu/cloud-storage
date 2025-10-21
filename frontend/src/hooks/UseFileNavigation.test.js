import { renderHook, act } from '@testing-library/react';
import { useFileNavigation } from './useFileNavigation';

describe('useFileNavigation', () => {
    describe('Initial state', () => {
        test('should initialize with default values', () => {
            const { result } = renderHook(() => useFileNavigation());

            expect(result.current.rootFolderId).toBeNull();
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' }
            ]);
        });

        test('should initialize with custom folder id', () => {
            const { result } = renderHook(() => useFileNavigation('folder123'));

            expect(result.current.rootFolderId).toBe('folder123');
            expect(result.current.navigationPath).toEqual([
                { id: 'folder123', name: 'root' }
            ]);
        });

        test('should initialize with custom folder id and name', () => {
            const { result } = renderHook(() => useFileNavigation('folder456', 'Documents'));

            expect(result.current.rootFolderId).toBe('folder456');
            expect(result.current.navigationPath).toEqual([
                { id: 'folder456', name: 'Documents' }
            ]);
        });
    });

    describe('handleFolderClick', () => {
        test('should add folder to navigation path', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
            });

            expect(result.current.rootFolderId).toBe('folder1');
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' },
                { id: 'folder1', name: 'Folder 1' }
            ]);
        });

        test('should handle nested folder navigation', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
            });

            act(() => {
                result.current.handleFolderClick('folder2', 'Folder 2');
            });

            expect(result.current.rootFolderId).toBe('folder2');
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' },
                { id: 'folder1', name: 'Folder 1' },
                { id: 'folder2', name: 'Folder 2' }
            ]);
        });

        test('should update rootFolderId when clicking folder', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('newFolder', 'New Folder');
            });

            expect(result.current.rootFolderId).toBe('newFolder');
        });

        test('should preserve previous navigation history', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
                result.current.handleFolderClick('folder2', 'Folder 2');
                result.current.handleFolderClick('folder3', 'Folder 3');
            });

            expect(result.current.navigationPath).toHaveLength(4);
            expect(result.current.navigationPath[3]).toEqual({
                id: 'folder3',
                name: 'Folder 3'
            });
        });
    });

    describe('handleBackward', () => {
        test('should navigate back one level', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
                result.current.handleFolderClick('folder2', 'Folder 2');
            });

            act(() => {
                result.current.handleBackward();
            });

            expect(result.current.rootFolderId).toBe('folder1');
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' },
                { id: 'folder1', name: 'Folder 1' }
            ]);
        });

        test('should not navigate back from root', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleBackward();
            });

            expect(result.current.rootFolderId).toBeNull();
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' }
            ]);
        });

        test('should navigate to root when going back from first subfolder', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
            });

            act(() => {
                result.current.handleBackward();
            });

            expect(result.current.rootFolderId).toBeNull();
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' }
            ]);
        });

        test('should handle multiple backward navigations', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
                result.current.handleFolderClick('folder2', 'Folder 2');
                result.current.handleFolderClick('folder3', 'Folder 3');
            });

            act(() => {
                result.current.handleBackward();
                result.current.handleBackward();
            });

            expect(result.current.rootFolderId).toBe('folder1');
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' },
                { id: 'folder1', name: 'Folder 1' }
            ]);
        });

        test('should update rootFolderId correctly when going back', () => {
            const { result } = renderHook(() => useFileNavigation('initial', 'Initial'));

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
            });

            act(() => {
                result.current.handleBackward();
            });

            expect(result.current.rootFolderId).toBe('initial');
        });
    });

    describe('handlePathClick', () => {
        test('should navigate to clicked path segment', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
                result.current.handleFolderClick('folder2', 'Folder 2');
                result.current.handleFolderClick('folder3', 'Folder 3');
            });

            act(() => {
                result.current.handlePathClick(1); // Click on 'Folder 1'
            });

            expect(result.current.rootFolderId).toBe('folder1');
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' },
                { id: 'folder1', name: 'Folder 1' }
            ]);
        });

        test('should navigate to root when clicking first segment', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
                result.current.handleFolderClick('folder2', 'Folder 2');
            });

            act(() => {
                result.current.handlePathClick(0); // Click on root
            });

            expect(result.current.rootFolderId).toBeNull();
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' }
            ]);
        });

        test('should handle clicking current path segment', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
            });

            const pathLength = result.current.navigationPath.length;

            act(() => {
                result.current.handlePathClick(pathLength - 1);
            });

            expect(result.current.rootFolderId).toBe('folder1');
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' },
                { id: 'folder1', name: 'Folder 1' }
            ]);
        });

        test('should navigate to middle path segment', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
                result.current.handleFolderClick('folder2', 'Folder 2');
                result.current.handleFolderClick('folder3', 'Folder 3');
                result.current.handleFolderClick('folder4', 'Folder 4');
            });

            act(() => {
                result.current.handlePathClick(2); // Click on 'Folder 2'
            });

            expect(result.current.rootFolderId).toBe('folder2');
            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' },
                { id: 'folder1', name: 'Folder 1' },
                { id: 'folder2', name: 'Folder 2' }
            ]);
        });
    });

    describe('setRootFolderId', () => {
        test('should allow manual setting of rootFolderId', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.setRootFolderId('customFolder');
            });

            expect(result.current.rootFolderId).toBe('customFolder');
        });
    });

    describe('setNavigationPath', () => {
        test('should allow manual setting of navigation path', () => {
            const { result } = renderHook(() => useFileNavigation());

            const customPath = [
                { id: 'root', name: 'Root' },
                { id: 'custom', name: 'Custom' }
            ];

            act(() => {
                result.current.setNavigationPath(customPath);
            });

            expect(result.current.navigationPath).toEqual(customPath);
        });
    });

    describe('Complex navigation scenarios', () => {
        test('should handle forward and backward navigation', () => {
            const { result } = renderHook(() => useFileNavigation());

            // Go forward
            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
                result.current.handleFolderClick('folder2', 'Folder 2');
            });

            expect(result.current.navigationPath).toHaveLength(3);

            // Go back
            act(() => {
                result.current.handleBackward();
            });

            expect(result.current.navigationPath).toHaveLength(2);

            // Go forward again (new path)
            act(() => {
                result.current.handleFolderClick('folder3', 'Folder 3');
            });

            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' },
                { id: 'folder1', name: 'Folder 1' },
                { id: 'folder3', name: 'Folder 3' }
            ]);
        });

        test('should handle path click after forward navigation', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'Folder 1');
                result.current.handleFolderClick('folder2', 'Folder 2');
                result.current.handleFolderClick('folder3', 'Folder 3');
            });

            act(() => {
                result.current.handlePathClick(1);
            });

            expect(result.current.rootFolderId).toBe('folder1');
            expect(result.current.navigationPath).toHaveLength(2);

            // Navigate forward from new position
            act(() => {
                result.current.handleFolderClick('folder4', 'Folder 4');
            });

            expect(result.current.navigationPath).toEqual([
                { id: 1, name: 'root' },
                { id: 'folder1', name: 'Folder 1' },
                { id: 'folder4', name: 'Folder 4' }
            ]);
        });
    });

    describe('Edge cases', () => {
        test('should handle empty folder name', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', '');
            });

            expect(result.current.navigationPath[1]).toEqual({
                id: 'folder1',
                name: ''
            });
        });

        test('should handle numeric folder ids', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick(123, 'Folder 123');
            });

            expect(result.current.rootFolderId).toBe(123);
        });

        test('should handle special characters in folder name', () => {
            const { result } = renderHook(() => useFileNavigation());

            act(() => {
                result.current.handleFolderClick('folder1', 'My Folder / Photos & Videos');
            });

            expect(result.current.navigationPath[1].name).toBe('My Folder / Photos & Videos');
        });
    });
});