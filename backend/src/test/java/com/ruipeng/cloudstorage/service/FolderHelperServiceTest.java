package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.Folder;
import com.ruipeng.cloudstorage.entity.PermissionType;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;

import java.sql.SQLException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FolderHelperService Unit Test")
class FolderHelperServiceTest {

    @Mock
    private FolderMapper folderMapper;

    @Mock
    private FilePermissionService permissionService;

    @InjectMocks
    private FolderHelperService folderHelperService;

    // ============ getRootFolderId Tests ============

    @Test
    @DisplayName("get root folder id returns existing folder id when root folder exists")
    void getRootFolderId_rootFolderExists_returnsExistingId() {
        Long userId = 1L;
        Folder existingRootFolder = getTestMockRootFolder(userId, 100L);

        when(folderMapper.findRootFolderByUserId(userId)).thenReturn(existingRootFolder);

        Long result = folderHelperService.getRootFolderId(userId);

        assertThat(result).isEqualTo(100L);
        verify(folderMapper).findRootFolderByUserId(userId);
        verify(folderMapper, never()).insert(any(Folder.class));
        verify(permissionService, never()).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());
    }

    @Test
    @DisplayName("get root folder id creates new folder when root folder does not exist")
    void getRootFolderId_rootFolderNotExists_createsNewFolder() {
        Long userId = 1L;
        Long newFolderId = 200L;
        Folder newRootFolder = getTestMockRootFolder(userId, newFolderId);

        when(folderMapper.findRootFolderByUserId(userId))
                .thenReturn(null)
                .thenReturn(newRootFolder);
        when(folderMapper.insert(any(Folder.class))).thenReturn(1);
        doNothing().when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        doNothing().when(permissionService).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());

        Long result = folderHelperService.getRootFolderId(userId);

        assertThat(result).isEqualTo(newFolderId);

        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        verify(folderMapper, times(2)).findRootFolderByUserId(userId);
        verify(folderMapper).insert(folderCaptor.capture());
        verify(folderMapper).updatePath(eq(newFolderId), any(PGobject.class), any(Instant.class));
        verify(permissionService).grantFolderPermission(newFolderId, userId, PermissionType.ADMIN, userId);

        Folder capturedFolder = folderCaptor.getValue();
        assertThat(capturedFolder.getOwnerId()).isEqualTo(userId);
        assertThat(capturedFolder.getName()).isEqualTo("Root");
        assertThat(capturedFolder.getParentId()).isNull();
        assertThat(capturedFolder.isDeleted()).isFalse();
        assertThat(capturedFolder.getCreatedAt()).isNotNull();
        assertThat(capturedFolder.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("get root folder id throws exception when folder insertion fails")
    void getRootFolderId_insertionFails_throwsException() {
        Long userId = 1L;

        when(folderMapper.findRootFolderByUserId(userId)).thenReturn(null);
        when(folderMapper.insert(any(Folder.class))).thenReturn(0);

        assertThatThrownBy(() -> folderHelperService.getRootFolderId(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to insert root folder");

        verify(folderMapper).findRootFolderByUserId(userId);
        verify(folderMapper).insert(any(Folder.class));
        verify(folderMapper, never()).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        verify(permissionService, never()).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());
    }

    @Test
    @DisplayName("get root folder id throws exception when inserted folder cannot be retrieved")
    void getRootFolderId_cannotRetrieveInsertedFolder_throwsException() {
        Long userId = 1L;

        when(folderMapper.findRootFolderByUserId(userId))
                .thenReturn(null)
                .thenReturn(null);
        when(folderMapper.insert(any(Folder.class))).thenReturn(1);

        assertThatThrownBy(() -> folderHelperService.getRootFolderId(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to retrieve created root folder");

        verify(folderMapper, times(2)).findRootFolderByUserId(userId);
        verify(folderMapper).insert(any(Folder.class));
        verify(folderMapper, never()).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        verify(permissionService, never()).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());
    }

    @Test
    @DisplayName("get root folder id throws exception when path update fails")
    void getRootFolderId_pathUpdateFails_throwsException() {
        Long userId = 1L;
        Long newFolderId = 200L;
        Folder newRootFolder = getTestMockRootFolder(userId, newFolderId);

        when(folderMapper.findRootFolderByUserId(userId))
                .thenReturn(null)
                .thenReturn(newRootFolder);
        when(folderMapper.insert(any(Folder.class))).thenReturn(1);
        doThrow(new RuntimeException("Database error")).when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));

        assertThatThrownBy(() -> folderHelperService.getRootFolderId(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(folderMapper, times(2)).findRootFolderByUserId(userId);
        verify(folderMapper).insert(any(Folder.class));
        verify(folderMapper).updatePath(eq(newFolderId), any(PGobject.class), any(Instant.class));
        verify(permissionService, never()).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());
    }

    @Test
    @DisplayName("get root folder id verifies folder properties are set correctly")
    void getRootFolderId_verifyFolderProperties() {
        Long userId = 5L;
        Long newFolderId = 300L;
        Folder newRootFolder = getTestMockRootFolder(userId, newFolderId);

        when(folderMapper.findRootFolderByUserId(userId))
                .thenReturn(null)
                .thenReturn(newRootFolder);
        when(folderMapper.insert(any(Folder.class))).thenReturn(1);
        doNothing().when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        doNothing().when(permissionService).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());

        Long result = folderHelperService.getRootFolderId(userId);

        assertThat(result).isEqualTo(newFolderId);

        ArgumentCaptor<Folder> folderCaptor = ArgumentCaptor.forClass(Folder.class);
        verify(folderMapper).insert(folderCaptor.capture());

        Folder capturedFolder = folderCaptor.getValue();
        assertThat(capturedFolder.getOwnerId()).isEqualTo(userId);
        assertThat(capturedFolder.getName()).isEqualTo("Root");
        assertThat(capturedFolder.getParentId()).isNull();
        assertThat(capturedFolder.getPath()).isNotNull();
        assertThat(capturedFolder.isDeleted()).isFalse();
        assertThat(capturedFolder.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
        assertThat(capturedFolder.getUpdatedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("get root folder id verifies path is updated with folder id")
    void getRootFolderId_verifyPathUpdate() {
        Long userId = 1L;
        Long newFolderId = 400L;
        Folder newRootFolder = getTestMockRootFolder(userId, newFolderId);

        when(folderMapper.findRootFolderByUserId(userId))
                .thenReturn(null)
                .thenReturn(newRootFolder);
        when(folderMapper.insert(any(Folder.class))).thenReturn(1);
        doNothing().when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        doNothing().when(permissionService).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());

        folderHelperService.getRootFolderId(userId);

        ArgumentCaptor<Long> folderIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<PGobject> pathCaptor = ArgumentCaptor.forClass(PGobject.class);
        ArgumentCaptor<Instant> timestampCaptor = ArgumentCaptor.forClass(Instant.class);

        verify(folderMapper).updatePath(folderIdCaptor.capture(), pathCaptor.capture(), timestampCaptor.capture());

        assertThat(folderIdCaptor.getValue()).isEqualTo(newFolderId);
        assertThat(pathCaptor.getValue().getType()).isEqualTo("ltree");
        assertThat(pathCaptor.getValue().getValue()).isEqualTo(newFolderId.toString());
        assertThat(timestampCaptor.getValue()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("get root folder id verifies permission is granted correctly")
    void getRootFolderId_verifyPermissionGrant() {
        Long userId = 1L;
        Long newFolderId = 500L;
        Folder newRootFolder = getTestMockRootFolder(userId, newFolderId);

        when(folderMapper.findRootFolderByUserId(userId))
                .thenReturn(null)
                .thenReturn(newRootFolder);
        when(folderMapper.insert(any(Folder.class))).thenReturn(1);
        doNothing().when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        doNothing().when(permissionService).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());

        folderHelperService.getRootFolderId(userId);

        ArgumentCaptor<Long> folderIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<PermissionType> permissionTypeCaptor = ArgumentCaptor.forClass(PermissionType.class);
        ArgumentCaptor<Long> createdByCaptor = ArgumentCaptor.forClass(Long.class);

        verify(permissionService).grantFolderPermission(
                folderIdCaptor.capture(),
                userIdCaptor.capture(),
                permissionTypeCaptor.capture(),
                createdByCaptor.capture()
        );

        assertThat(folderIdCaptor.getValue()).isEqualTo(newFolderId);
        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
        assertThat(permissionTypeCaptor.getValue()).isEqualTo(PermissionType.ADMIN);
        assertThat(createdByCaptor.getValue()).isEqualTo(userId);
    }

    @Test
    @DisplayName("get root folder id handles multiple consecutive calls correctly")
    void getRootFolderId_multipleCalls_handlesCorrectly() {
        Long userId = 1L;
        Long existingFolderId = 600L;
        Folder existingRootFolder = getTestMockRootFolder(userId, existingFolderId);

        when(folderMapper.findRootFolderByUserId(userId)).thenReturn(existingRootFolder);

        Long result1 = folderHelperService.getRootFolderId(userId);
        Long result2 = folderHelperService.getRootFolderId(userId);

        assertThat(result1).isEqualTo(existingFolderId);
        assertThat(result2).isEqualTo(existingFolderId);

        verify(folderMapper, times(2)).findRootFolderByUserId(userId);
        verify(folderMapper, never()).insert(any(Folder.class));
    }

    @Test
    @DisplayName("get root folder id creates different folders for different users")
    void getRootFolderId_differentUsers_createsDifferentFolders() {
        Long userId1 = 1L;
        Long userId2 = 2L;
        Long folderId1 = 100L;
        Long folderId2 = 200L;

        Folder folder1 = getTestMockRootFolder(userId1, folderId1);
        Folder folder2 = getTestMockRootFolder(userId2, folderId2);

        when(folderMapper.findRootFolderByUserId(userId1))
                .thenReturn(null)
                .thenReturn(folder1);
        when(folderMapper.findRootFolderByUserId(userId2))
                .thenReturn(null)
                .thenReturn(folder2);
        when(folderMapper.insert(any(Folder.class))).thenReturn(1);
        doNothing().when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        doNothing().when(permissionService).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());

        Long result1 = folderHelperService.getRootFolderId(userId1);
        Long result2 = folderHelperService.getRootFolderId(userId2);

        assertThat(result1).isEqualTo(folderId1);
        assertThat(result2).isEqualTo(folderId2);

        verify(folderMapper, times(2)).findRootFolderByUserId(userId1);
        verify(folderMapper, times(2)).findRootFolderByUserId(userId2);
        verify(permissionService).grantFolderPermission(folderId1, userId1, PermissionType.ADMIN, userId1);
        verify(permissionService).grantFolderPermission(folderId2, userId2, PermissionType.ADMIN, userId2);
    }

    // ============ Helper Methods ============

    private Folder getTestMockRootFolder(Long userId, Long folderId) {
        Folder folder = new Folder();
        folder.setId(folderId);
        folder.setOwnerId(userId);
        folder.setName("Root");
        folder.setParentId(null);
        folder.setDeleted(false);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());

        try {
            PGobject path = new PGobject();
            path.setType("ltree");
            path.setValue(folderId.toString());
            folder.setPath(path);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return folder;
    }
}