import { useState, useCallback } from 'react';

export function useFileNavigation(initialFolderId = null, initialFolderName = 'root') {
    const [rootFolderId, setRootFolderId] = useState(initialFolderId);
    const [navigationPath, setNavigationPath] = useState([
        { id: initialFolderId || 1, name: initialFolderName }
    ]);

    const handleFolderClick = useCallback((folderId, folderName) => {
        setRootFolderId(folderId);
        setNavigationPath(prev => [...prev, { id: folderId, name: folderName }]);
    }, []);

    const handleBackward = useCallback(() => {
        if (navigationPath.length > 1) {
            const newPath = navigationPath.slice(0, -1);
            setNavigationPath(newPath);
            setRootFolderId(newPath[newPath.length - 1].id);
        }
    }, [navigationPath]);

    const handlePathClick = useCallback((index) => {
        const newPath = navigationPath.slice(0, index + 1);
        setNavigationPath(newPath);
        setRootFolderId(newPath[newPath.length - 1].id);
    }, [navigationPath]);

    return {
        rootFolderId,
        setRootFolderId,
        navigationPath,
        setNavigationPath,
        handleFolderClick,
        handleBackward,
        handlePathClick
    };
}