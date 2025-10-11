package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.FilePermission;
import com.ruipeng.cloudstorage.entity.PermissionType;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service for managing file and folder permissions.
 *
 * Handles:
 * - Permission checks
 * - Permission grants and revokes
 * - Permission queries
 */
@Service
public class FilePermissionService {
    private static final Logger log = LoggerFactory.getLogger(FilePermissionService.class);

    private final FilePermissionMapper filePermissionMapper;

    public FilePermissionService(FilePermissionMapper filePermissionMapper) {
        this.filePermissionMapper = filePermissionMapper;
    }

    /**
     * Checks if a user has admin permission for a file.
     */
    public boolean hasAdminPermission(Long fileId, Long userId) {
        FilePermission permission = findPermission(fileId, userId);
        return permission != null && permission.getPermission() == PermissionType.ADMIN;
    }

    /**
     * Checks if a user has at least read permission for a file.
     */
    public boolean hasReadPermission(Long fileId, Long userId) {
        FilePermission permission = findPermission(fileId, userId);
        return permission != null;
    }

    /**
     * Grants a permission to a user for a file.
     */
    @Transactional
    public void grantPermission(Long fileId, Long userId, Long folderId,
                                PermissionType permissionType) {
        FilePermission existing = findPermission(fileId, userId);

        if (existing != null) {
            updateExistingPermission(existing, permissionType);
        } else {
            createNewPermission(fileId, userId, folderId, permissionType);
        }

        log.info("Permission granted: fileId={}, userId={}, type={}",
                fileId, userId, permissionType);
    }

    /**
     * Grants folder permission to a user.
     */
    @Transactional
    public void grantFolderPermission(Long folderId, Long userId,
                                      PermissionType permissionType, Long createdBy) {
        FilePermission permission = createFolderPermission(
                folderId, userId, permissionType, createdBy
        );
        filePermissionMapper.insert(permission);

        log.info("Folder permission granted: folderId={}, userId={}, type={}",
                folderId, userId, permissionType);
    }

    /**
     * Checks if user has permission for a folder.
     */
    public boolean hasFolderPermission(Long folderId, Long userId,
                                       PermissionType requiredPermission) {
        FilePermission permission = filePermissionMapper.findByFolderIdAndUserId(folderId, userId);

        if (permission == null) {
            return false;
        }

        return isPermissionSufficient(permission.getPermission(), requiredPermission);
    }

    /**
     * Validates that a user has admin permission for a folder.
     */
    public void validateFolderAdminPermission(Long folderId, Long userId) {
        if (!hasFolderPermission(folderId, userId, PermissionType.ADMIN)) {
            throw new SecurityException("No admin permission for folder: " + folderId);
        }
    }

    // ============ Private Helper Methods ============

    private FilePermission findPermission(Long fileId, Long userId) {
        return filePermissionMapper.findByFileIdAndUserId(fileId, userId);
    }

    private void updateExistingPermission(FilePermission permission,
                                          PermissionType newType) {
        permission.setPermission(newType);
        filePermissionMapper.update(permission);
    }

    private void createNewPermission(Long fileId, Long userId, Long folderId,
                                     PermissionType permissionType) {
        FilePermission permission = new FilePermission();
        permission.setFileId(fileId);
        permission.setUserId(userId);
        permission.setFolderId(folderId);
        permission.setPermission(permissionType);
        permission.setCreatedBy(userId);
        permission.setCreatedAt(Instant.now());

        filePermissionMapper.insert(permission);
    }

    private FilePermission createFolderPermission(Long folderId, Long userId,
                                                  PermissionType permissionType,
                                                  Long createdBy) {
        FilePermission permission = new FilePermission();
        permission.setFolderId(folderId);
        permission.setUserId(userId);
        permission.setPermission(permissionType);
        permission.setCreatedAt(Instant.now());
        permission.setCreatedBy(createdBy);
        return permission;
    }

    private boolean isPermissionSufficient(PermissionType current,
                                           PermissionType required) {
        if (current == PermissionType.ADMIN) {
            return true;
        }
        if (current == PermissionType.WRITE && required != PermissionType.ADMIN) {
            return true;
        }
        return current == required;
    }
}
