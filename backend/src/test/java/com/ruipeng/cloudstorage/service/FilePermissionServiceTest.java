package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.FilePermission;
import com.ruipeng.cloudstorage.entity.PermissionType;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilePermissionService Unit Test")
class FilePermissionServiceTest {

    @Mock
    private FilePermissionMapper filePermissionMapper;

    @InjectMocks
    private FilePermissionService filePermissionService;


    @Test
    @DisplayName("has admin permission returns true when user has admin permission")
    void hasAdminPermission_userHasAdmin_returnsTrue() {
        Long fileId = 1L;
        Long userId = 1L;
        FilePermission adminPermission = getTestMockPermission(PermissionType.ADMIN);

        when(filePermissionMapper.findByFileIdAndUserId(fileId, userId)).thenReturn(adminPermission);

        boolean result = filePermissionService.hasAdminPermission(fileId, userId);

        assertThat(result).isTrue();
        verify(filePermissionMapper).findByFileIdAndUserId(fileId, userId);
    }

    @Test
    @DisplayName("has admin permission returns false when user has read permission")
    void hasAdminPermission_userHasRead_returnsFalse() {
        Long fileId = 1L;
        Long userId = 1L;
        FilePermission readPermission = getTestMockPermission(PermissionType.READ);

        when(filePermissionMapper.findByFileIdAndUserId(fileId, userId)).thenReturn(readPermission);

        boolean result = filePermissionService.hasAdminPermission(fileId, userId);

        assertThat(result).isFalse();
        verify(filePermissionMapper).findByFileIdAndUserId(fileId, userId);
    }

    @Test
    @DisplayName("has admin permission returns false when user has write permission")
    void hasAdminPermission_userHasWrite_returnsFalse() {
        Long fileId = 1L;
        Long userId = 1L;
        FilePermission writePermission = getTestMockPermission(PermissionType.WRITE);

        when(filePermissionMapper.findByFileIdAndUserId(fileId, userId)).thenReturn(writePermission);

        boolean result = filePermissionService.hasAdminPermission(fileId, userId);

        assertThat(result).isFalse();
        verify(filePermissionMapper).findByFileIdAndUserId(fileId, userId);
    }

    @Test
    @DisplayName("has admin permission returns false when permission not found")
    void hasAdminPermission_permissionNotFound_returnsFalse() {
        Long fileId = 1L;
        Long userId = 1L;

        when(filePermissionMapper.findByFileIdAndUserId(fileId, userId)).thenReturn(null);

        boolean result = filePermissionService.hasAdminPermission(fileId, userId);

        assertThat(result).isFalse();
        verify(filePermissionMapper).findByFileIdAndUserId(fileId, userId);
    }


    @Test
    @DisplayName("has read permission returns true when user has any permission")
    void hasReadPermission_userHasPermission_returnsTrue() {
        Long fileId = 1L;
        Long userId = 1L;
        FilePermission readPermission = getTestMockPermission(PermissionType.READ);

        when(filePermissionMapper.findByFileIdAndUserId(fileId, userId)).thenReturn(readPermission);

        boolean result = filePermissionService.hasReadPermission(fileId, userId);

        assertThat(result).isTrue();
        verify(filePermissionMapper).findByFileIdAndUserId(fileId, userId);
    }

    @Test
    @DisplayName("has read permission returns false when permission not found")
    void hasReadPermission_permissionNotFound_returnsFalse() {
        Long fileId = 1L;
        Long userId = 1L;

        when(filePermissionMapper.findByFileIdAndUserId(fileId, userId)).thenReturn(null);

        boolean result = filePermissionService.hasReadPermission(fileId, userId);

        assertThat(result).isFalse();
        verify(filePermissionMapper).findByFileIdAndUserId(fileId, userId);
    }


    @Test
    @DisplayName("grant permission creates new permission when not exists")
    void grantPermission_permissionNotExists_createsNew() {
        Long fileId = 1L;
        Long userId = 2L;
        Long folderId = 3L;
        PermissionType permissionType = PermissionType.WRITE;

        when(filePermissionMapper.findByFileIdAndUserId(fileId, userId)).thenReturn(null);

        filePermissionService.grantPermission(fileId, userId, folderId, permissionType);

        ArgumentCaptor<FilePermission> captor = ArgumentCaptor.forClass(FilePermission.class);
        verify(filePermissionMapper).findByFileIdAndUserId(fileId, userId);
        verify(filePermissionMapper).insert(captor.capture());

        FilePermission captured = captor.getValue();
        assertThat(captured.getFileId()).isEqualTo(fileId);
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getFolderId()).isEqualTo(folderId);
        assertThat(captured.getPermission()).isEqualTo(permissionType);
        assertThat(captured.getCreatedBy()).isEqualTo(userId);
        assertThat(captured.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("grant permission updates existing permission when exists")
    void grantPermission_permissionExists_updates() {
        Long fileId = 1L;
        Long userId = 2L;
        Long folderId = 3L;
        PermissionType newPermissionType = PermissionType.ADMIN;

        FilePermission existingPermission = getTestMockPermission(PermissionType.READ);
        existingPermission.setFileId(fileId);
        existingPermission.setUserId(userId);

        when(filePermissionMapper.findByFileIdAndUserId(fileId, userId)).thenReturn(existingPermission);

        filePermissionService.grantPermission(fileId, userId, folderId, newPermissionType);

        verify(filePermissionMapper).findByFileIdAndUserId(fileId, userId);
        verify(filePermissionMapper).update(existingPermission);
        verify(filePermissionMapper, never()).insert(any(FilePermission.class));

        assertThat(existingPermission.getPermission()).isEqualTo(newPermissionType);
    }


    @Test
    @DisplayName("grant folder permission creates new permission successfully")
    void grantFolderPermission_success() {
        Long folderId = 1L;
        Long userId = 2L;
        Long createdBy = 3L;
        PermissionType permissionType = PermissionType.WRITE;

        filePermissionService.grantFolderPermission(folderId, userId, permissionType, createdBy);

        ArgumentCaptor<FilePermission> captor = ArgumentCaptor.forClass(FilePermission.class);
        verify(filePermissionMapper).insert(captor.capture());

        FilePermission captured = captor.getValue();
        assertThat(captured.getFolderId()).isEqualTo(folderId);
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getPermission()).isEqualTo(permissionType);
        assertThat(captured.getCreatedBy()).isEqualTo(createdBy);
        assertThat(captured.getCreatedAt()).isNotNull();
    }


    @Test
    @DisplayName("has folder permission returns true when user has admin and requires read")
    void hasFolderPermission_userHasAdmin_requiresRead_returnsTrue() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission adminPermission = getTestMockPermission(PermissionType.ADMIN);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(adminPermission);

        boolean result = filePermissionService.hasFolderPermission(folderId, userId, PermissionType.READ);

        assertThat(result).isTrue();
        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("has folder permission returns true when user has admin and requires write")
    void hasFolderPermission_userHasAdmin_requiresWrite_returnsTrue() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission adminPermission = getTestMockPermission(PermissionType.ADMIN);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(adminPermission);

        boolean result = filePermissionService.hasFolderPermission(folderId, userId, PermissionType.WRITE);

        assertThat(result).isTrue();
        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("has folder permission returns true when user has admin and requires admin")
    void hasFolderPermission_userHasAdmin_requiresAdmin_returnsTrue() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission adminPermission = getTestMockPermission(PermissionType.ADMIN);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(adminPermission);

        boolean result = filePermissionService.hasFolderPermission(folderId, userId, PermissionType.ADMIN);

        assertThat(result).isTrue();
        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("has folder permission returns true when user has write and requires read")
    void hasFolderPermission_userHasWrite_requiresRead_returnsTrue() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission writePermission = getTestMockPermission(PermissionType.WRITE);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(writePermission);

        boolean result = filePermissionService.hasFolderPermission(folderId, userId, PermissionType.READ);

        assertThat(result).isTrue();
        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("has folder permission returns true when user has write and requires write")
    void hasFolderPermission_userHasWrite_requiresWrite_returnsTrue() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission writePermission = getTestMockPermission(PermissionType.WRITE);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(writePermission);

        boolean result = filePermissionService.hasFolderPermission(folderId, userId, PermissionType.WRITE);

        assertThat(result).isTrue();
        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("has folder permission returns false when user has write but requires admin")
    void hasFolderPermission_userHasWrite_requiresAdmin_returnsFalse() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission writePermission = getTestMockPermission(PermissionType.WRITE);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(writePermission);

        boolean result = filePermissionService.hasFolderPermission(folderId, userId, PermissionType.ADMIN);

        assertThat(result).isFalse();
        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("has folder permission returns true when user has read and requires read")
    void hasFolderPermission_userHasRead_requiresRead_returnsTrue() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission readPermission = getTestMockPermission(PermissionType.READ);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(readPermission);

        boolean result = filePermissionService.hasFolderPermission(folderId, userId, PermissionType.READ);

        assertThat(result).isTrue();
        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("has folder permission returns false when user has read but requires write")
    void hasFolderPermission_userHasRead_requiresWrite_returnsFalse() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission readPermission = getTestMockPermission(PermissionType.READ);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(readPermission);

        boolean result = filePermissionService.hasFolderPermission(folderId, userId, PermissionType.WRITE);

        assertThat(result).isFalse();
        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("has folder permission returns false when permission not found")
    void hasFolderPermission_permissionNotFound_returnsFalse() {
        Long folderId = 1L;
        Long userId = 1L;

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(null);

        boolean result = filePermissionService.hasFolderPermission(folderId, userId, PermissionType.READ);

        assertThat(result).isFalse();
        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    // ============ validateFolderAdminPermission Tests ============

    @Test
    @DisplayName("validate folder admin permission succeeds when user has admin")
    void validateFolderAdminPermission_userHasAdmin_success() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission adminPermission = getTestMockPermission(PermissionType.ADMIN);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(adminPermission);

        assertThatCode(() -> filePermissionService.validateFolderAdminPermission(folderId, userId))
                .doesNotThrowAnyException();

        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("validate folder admin permission throws exception when user lacks admin")
    void validateFolderAdminPermission_userLacksAdmin_throwsException() {
        Long folderId = 1L;
        Long userId = 1L;
        FilePermission readPermission = getTestMockPermission(PermissionType.READ);

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(readPermission);

        assertThatThrownBy(() -> filePermissionService.validateFolderAdminPermission(folderId, userId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("No admin permission for folder: " + folderId);

        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    @Test
    @DisplayName("validate folder admin permission throws exception when permission not found")
    void validateFolderAdminPermission_permissionNotFound_throwsException() {
        Long folderId = 1L;
        Long userId = 1L;

        when(filePermissionMapper.findByFolderIdAndUserId(folderId, userId)).thenReturn(null);

        assertThatThrownBy(() -> filePermissionService.validateFolderAdminPermission(folderId, userId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("No admin permission for folder: " + folderId);

        verify(filePermissionMapper).findByFolderIdAndUserId(folderId, userId);
    }

    // ============ Helper Methods ============

    private FilePermission getTestMockPermission(PermissionType permissionType) {
        FilePermission permission = new FilePermission();
        permission.setId(1L);
        permission.setPermission(permissionType);
        permission.setCreatedAt(Instant.now());
        permission.setCreatedBy(1L);
        return permission;
    }
}