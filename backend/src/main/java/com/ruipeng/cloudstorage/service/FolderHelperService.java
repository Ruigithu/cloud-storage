package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.Folder;
import com.ruipeng.cloudstorage.entity.PermissionType;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Instant;

@Service
public class FolderHelperService {
    private static final Logger log = LoggerFactory.getLogger(FolderHelperService.class);

    private final FolderMapper folderMapper;
    private final FilePermissionService permissionService;

    public FolderHelperService(FolderMapper folderMapper,
                               FilePermissionService permissionService) {
        this.folderMapper = folderMapper;
        this.permissionService = permissionService;
    }

    /**
     * 获取用户的根文件夹ID,如果不存在则创建
     */
    @Transactional
    public synchronized Long getRootFolderId(Long userId) {
        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);

        if (rootFolder == null) {
            rootFolder = createRootFolder(userId);
        }

        return rootFolder.getId();
    }

    // ============ Private Helper Methods ============
    //operate the database many times
    private Folder createRootFolder(Long userId) {
        try {
            Folder rootFolder = buildRootFolder(userId);
            insertFolder(rootFolder);

            Folder inserted = folderMapper.findRootFolderByUserId(userId);
            if (inserted == null) {
                throw new RuntimeException("Failed to retrieve created root folder");
            }

            updateRootFolderPath(inserted);
            grantFolderPermission(inserted.getId(), userId);

            log.info("Root folder created for user: {}", userId);
            return inserted;
        } catch (SQLException e) {
            log.error("Failed to create root folder for user: {}", userId, e);
            throw new RuntimeException("Failed to create root folder", e);
        }
    }

    private Folder buildRootFolder(Long userId) throws SQLException {
        Folder folder = new Folder();
        folder.setOwnerId(userId);
        folder.setName("Root");
        folder.setParentId(null);
        folder.setPath(createTempPath());
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());
        folder.setDeleted(false);
        return folder;
    }

    private PGobject createTempPath() throws SQLException {
        PGobject path = new PGobject();
        path.setType("ltree");
        path.setValue("temp");
        return path;
    }

    private void insertFolder(Folder folder) {
        int rows = folderMapper.insert(folder);
        if (rows <= 0) {
            throw new RuntimeException("Failed to insert root folder");
        }
    }

    private void updateRootFolderPath(Folder rootFolder) {
        try {
            PGobject path = new PGobject();
            path.setType("ltree");
            path.setValue(rootFolder.getId().toString());
            folderMapper.updatePath(rootFolder.getId(), path, Instant.now());
        } catch (SQLException e) {
            log.error("Failed to update root folder path: {}", rootFolder.getId(), e);
            throw new RuntimeException("Failed to update root folder path", e);
        }
    }

    private void grantFolderPermission(Long folderId, Long userId) {
        permissionService.grantFolderPermission(
                folderId, userId, PermissionType.ADMIN, userId
        );
    }
}
