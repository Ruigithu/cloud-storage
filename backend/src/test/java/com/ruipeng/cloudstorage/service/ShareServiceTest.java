package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.dto.response.ShareInfoResponse;
import com.ruipeng.cloudstorage.dto.response.ShareResponse;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.exception.ShareLinkException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ShareService Unit Test")
class ShareServiceTest {

    @Mock
    private ShareMapper shareMapper;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private FileVersionMapper fileVersionMapper;

    @Mock
    private S3StorageService storageService;

    @Mock
    private FileS3Service fileS3Service;

    @Mock
    private FolderHelperService folderHelperService;

    private ShareService shareService;

    private static final String FRONTEND_URL = "https://example.com";
    private static final Long TEST_USER_ID = 1L;
    private static final Long TEST_FILE_ID = 100L;
    private static final UUID TEST_SHARE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

    @BeforeEach
    void setUp() {
        shareService = new ShareService(
                shareMapper,
                fileMapper,
                fileVersionMapper,
                storageService,
                fileS3Service,
                FRONTEND_URL,
                folderHelperService
        );
    }

    // ============ createShare Tests ============

    @Test
    @DisplayName("create share successfully creates share link")
    void createShare_success() {
        Long fileId = TEST_FILE_ID;
        Long createdBy = TEST_USER_ID;
        String accessType = "read";
        String expiresAt = "2025-12-31T23:59:59Z";

        File mockFile = createMockFile(fileId, "test.pdf");

        when(fileMapper.findByIdAndUserIdWithAdminPermission(fileId, createdBy)).thenReturn(mockFile);
        when(shareMapper.insert(any(Share.class))).thenReturn(1);

        ShareResponse result = shareService.createShare(fileId, createdBy, accessType, expiresAt);

        assertThat(result).isNotNull();
        assertThat(result.getShareLink()).isNotNull();
        assertThat(result.getShareLink()).startsWith(FRONTEND_URL + "/share/");

        ArgumentCaptor<Share> shareCaptor = ArgumentCaptor.forClass(Share.class);
        verify(fileMapper).findByIdAndUserIdWithAdminPermission(fileId, createdBy);
        verify(shareMapper).insert(shareCaptor.capture());

        Share capturedShare = shareCaptor.getValue();
        assertThat(capturedShare.getFileId()).isEqualTo(fileId);
        assertThat(capturedShare.getCreatedBy()).isEqualTo(createdBy);
        assertThat(capturedShare.getAccessType()).isEqualTo(PermissionType.READ);
        assertThat(capturedShare.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("create share with write access type")
    void createShare_writeAccess_success() {
        Long fileId = TEST_FILE_ID;
        Long createdBy = TEST_USER_ID;
        String accessType = "write";
        String expiresAt = null;

        File mockFile = createMockFile(fileId, "test.pdf");

        when(fileMapper.findByIdAndUserIdWithAdminPermission(fileId, createdBy)).thenReturn(mockFile);
        when(shareMapper.insert(any(Share.class))).thenReturn(1);

        ShareResponse result = shareService.createShare(fileId, createdBy, accessType, expiresAt);

        assertThat(result).isNotNull();

        ArgumentCaptor<Share> shareCaptor = ArgumentCaptor.forClass(Share.class);
        verify(shareMapper).insert(shareCaptor.capture());

        Share capturedShare = shareCaptor.getValue();
        assertThat(capturedShare.getAccessType()).isEqualTo(PermissionType.WRITE);
        assertThat(capturedShare.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("create share without expiration date")
    void createShare_noExpiration_success() {
        Long fileId = TEST_FILE_ID;
        Long createdBy = TEST_USER_ID;
        String accessType = "read";
        String expiresAt = null;

        File mockFile = createMockFile(fileId, "test.pdf");

        when(fileMapper.findByIdAndUserIdWithAdminPermission(fileId, createdBy)).thenReturn(mockFile);
        when(shareMapper.insert(any(Share.class))).thenReturn(1);

        ShareResponse result = shareService.createShare(fileId, createdBy, accessType, expiresAt);

        assertThat(result).isNotNull();

        ArgumentCaptor<Share> shareCaptor = ArgumentCaptor.forClass(Share.class);
        verify(shareMapper).insert(shareCaptor.capture());

        Share capturedShare = shareCaptor.getValue();
        assertThat(capturedShare.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("create share throws exception when user has no permission")
    void createShare_noPermission_throwsException() {
        Long fileId = TEST_FILE_ID;
        Long createdBy = TEST_USER_ID;
        String accessType = "read";
        String expiresAt = null;

        when(fileMapper.findByIdAndUserIdWithAdminPermission(fileId, createdBy)).thenReturn(null);

        assertThatThrownBy(() -> shareService.createShare(fileId, createdBy, accessType, expiresAt))
                .isInstanceOf(ShareLinkException.class)
                .hasMessageContaining("File not found or no permission");

        verify(fileMapper).findByIdAndUserIdWithAdminPermission(fileId, createdBy);
        verify(shareMapper, never()).insert(any(Share.class));
    }

    @Test
    @DisplayName("create share verifies share link format")
    void createShare_verifyShareLinkFormat() {
        Long fileId = TEST_FILE_ID;
        Long createdBy = TEST_USER_ID;
        String accessType = "read";
        String expiresAt = null;

        File mockFile = createMockFile(fileId, "test.pdf");

        when(fileMapper.findByIdAndUserIdWithAdminPermission(fileId, createdBy)).thenReturn(mockFile);
        when(shareMapper.insert(any(Share.class))).thenReturn(1);

        ShareResponse result = shareService.createShare(fileId, createdBy, accessType, expiresAt);

        assertThat(result.getShareLink())
                .matches(FRONTEND_URL + "/share/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    // ============ getShareInfo Tests ============

    @Test
    @DisplayName("get share info returns share information successfully")
    void getShareInfo_success() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.READ, true);
        File mockFile = createMockFile(TEST_FILE_ID, "shared-file.pdf");
        FileVersion mockVersion = createMockFileVersion(TEST_FILE_ID, "path/to/file.pdf");

        when(shareMapper.findById(shareId)).thenReturn(mockShare);
        when(fileMapper.getFileById(TEST_FILE_ID)).thenReturn(mockFile);
        when(fileVersionMapper.getLatestVersion(TEST_FILE_ID)).thenReturn(mockVersion);

        ShareInfoResponse result = shareService.getShareInfo(shareId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(shareId);
        assertThat(result.getFileId()).isEqualTo(TEST_FILE_ID);
        assertThat(result.getFileName()).isEqualTo("shared-file.pdf");
        assertThat(result.getFilePath()).isEqualTo("path/to/file.pdf");
        assertThat(result.getType()).isEqualTo(PermissionType.READ);
        assertThat(result.isActive()).isTrue();

        verify(shareMapper).findById(shareId);
        verify(fileMapper).getFileById(TEST_FILE_ID);
        verify(fileVersionMapper).getLatestVersion(TEST_FILE_ID);
    }

    @Test
    @DisplayName("get share info throws exception when share not found")
    void getShareInfo_shareNotFound_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        when(shareMapper.findById(shareId)).thenReturn(null);

        assertThatThrownBy(() -> shareService.getShareInfo(shareId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Share not found");

        verify(shareMapper).findById(shareId);
        verify(fileMapper, never()).getFileById(anyLong());
    }

    @Test
    @DisplayName("get share info throws exception when share is not active")
    void getShareInfo_shareNotActive_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.READ, false);

        when(shareMapper.findById(shareId)).thenReturn(mockShare);

        assertThatThrownBy(() -> shareService.getShareInfo(shareId, userId))
                .isInstanceOf(ShareLinkException.class)
                .hasMessageContaining("Share link is not active");

        verify(shareMapper).findById(shareId);
        verify(fileMapper, never()).getFileById(anyLong());
    }

    @Test
    @DisplayName("get share info throws exception when share has expired")
    void getShareInfo_shareExpired_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.READ, true);
        mockShare.setExpiresAt(OffsetDateTime.now().minusDays(1));

        when(shareMapper.findById(shareId)).thenReturn(mockShare);

        assertThatThrownBy(() -> shareService.getShareInfo(shareId, userId))
                .isInstanceOf(ShareLinkException.class)
                .hasMessageContaining("Share link has expired");

        verify(shareMapper).findById(shareId);
        verify(fileMapper, never()).getFileById(anyLong());
    }

    @Test
    @DisplayName("get share info throws exception when file not found")
    void getShareInfo_fileNotFound_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.READ, true);

        when(shareMapper.findById(shareId)).thenReturn(mockShare);
        when(fileMapper.getFileById(TEST_FILE_ID)).thenReturn(null);

        assertThatThrownBy(() -> shareService.getShareInfo(shareId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File");

        verify(shareMapper).findById(shareId);
        verify(fileMapper).getFileById(TEST_FILE_ID);
        verify(fileVersionMapper, never()).getLatestVersion(anyLong());
    }

    @Test
    @DisplayName("get share info throws exception when file version not found")
    void getShareInfo_versionNotFound_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.READ, true);
        File mockFile = createMockFile(TEST_FILE_ID, "shared-file.pdf");

        when(shareMapper.findById(shareId)).thenReturn(mockShare);
        when(fileMapper.getFileById(TEST_FILE_ID)).thenReturn(mockFile);
        when(fileVersionMapper.getLatestVersion(TEST_FILE_ID)).thenReturn(null);

        assertThatThrownBy(() -> shareService.getShareInfo(shareId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File version for file");

        verify(shareMapper).findById(shareId);
        verify(fileMapper).getFileById(TEST_FILE_ID);
        verify(fileVersionMapper).getLatestVersion(TEST_FILE_ID);
    }

    // ============ saveSharedFile Tests ============

    @Test
    @DisplayName("save shared file successfully copies file to user storage")
    void saveSharedFile_success() throws IOException {
        UUID shareId = TEST_SHARE_ID;
        Long userId = 2L;
        Long rootFolderId = 10L;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.WRITE, true);
        File mockOriginalFile = createMockFile(TEST_FILE_ID, "original.pdf");
        FileVersion mockVersion = createMockFileVersion(TEST_FILE_ID, "path/to/original.pdf");
        File mockNewFile = createMockFile(200L, "original.pdf");
        byte[] fileContent = "test file content".getBytes();

        when(shareMapper.findById(shareId)).thenReturn(mockShare);
        when(fileMapper.getFileById(TEST_FILE_ID)).thenReturn(mockOriginalFile);
        when(fileVersionMapper.getLatestVersion(TEST_FILE_ID)).thenReturn(mockVersion);
        when(storageService.downloadFile("path/to/original.pdf")).thenReturn(fileContent);
        when(folderHelperService.getRootFolderId(userId)).thenReturn(rootFolderId);
        when(fileS3Service.uploadFile(any(MultipartFile.class), eq(userId), eq(rootFolderId)))
                .thenReturn(mockNewFile);

        Long result = shareService.saveSharedFile(shareId, userId, rootFolderId);

        assertThat(result).isEqualTo(200L);

        verify(shareMapper).findById(shareId);
        verify(fileMapper).getFileById(TEST_FILE_ID);
        verify(fileVersionMapper).getLatestVersion(TEST_FILE_ID);
        verify(storageService).downloadFile("path/to/original.pdf");
        verify(fileS3Service).uploadFile(any(MultipartFile.class), eq(userId), eq(rootFolderId));
    }

    @Test
    @DisplayName("save shared file throws exception when share not found")
    void saveSharedFile_shareNotFound_throwsException() throws IOException {
        UUID shareId = TEST_SHARE_ID;
        Long userId = 2L;
        Long rootFolderId = 10L;

        when(shareMapper.findById(shareId)).thenReturn(null);

        assertThatThrownBy(() -> shareService.saveSharedFile(shareId, userId, rootFolderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Share not found");

        verify(shareMapper).findById(shareId);
        verify(storageService, never()).downloadFile(anyString());
    }

    @Test
    @DisplayName("save shared file throws exception when share has no write access")
    void saveSharedFile_noWriteAccess_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = 2L;
        Long rootFolderId = 10L;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.READ, true);

        when(shareMapper.findById(shareId)).thenReturn(mockShare);

        assertThatThrownBy(() -> shareService.saveSharedFile(shareId, userId, rootFolderId))
                .isInstanceOf(ShareLinkException.class)
                .hasMessageContaining("Share does not allow saving files");

        verify(shareMapper).findById(shareId);
        verify(fileMapper, never()).getFileById(anyLong());
    }

    @Test
    @DisplayName("save shared file throws exception when original file not found")
    void saveSharedFile_fileNotFound_throwsException() throws IOException {
        UUID shareId = TEST_SHARE_ID;
        Long userId = 2L;
        Long rootFolderId = 10L;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.WRITE, true);

        when(shareMapper.findById(shareId)).thenReturn(mockShare);
        when(fileMapper.getFileById(TEST_FILE_ID)).thenReturn(null);

        assertThatThrownBy(() -> shareService.saveSharedFile(shareId, userId, rootFolderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File");

        verify(shareMapper).findById(shareId);
        verify(fileMapper).getFileById(TEST_FILE_ID);
        verify(storageService, never()).downloadFile(anyString());
    }

    @Test
    @DisplayName("save shared file throws exception when download fails")
    void saveSharedFile_downloadFails_throwsException() throws IOException {
        UUID shareId = TEST_SHARE_ID;
        Long userId = 2L;
        Long rootFolderId = 10L;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.WRITE, true);
        File mockOriginalFile = createMockFile(TEST_FILE_ID, "original.pdf");
        FileVersion mockVersion = createMockFileVersion(TEST_FILE_ID, "path/to/original.pdf");

        when(shareMapper.findById(shareId)).thenReturn(mockShare);
        when(fileMapper.getFileById(TEST_FILE_ID)).thenReturn(mockOriginalFile);
        when(fileVersionMapper.getLatestVersion(TEST_FILE_ID)).thenReturn(mockVersion);
        when(storageService.downloadFile("path/to/original.pdf"))
                .thenThrow(new IOException("Download failed"));

        assertThatThrownBy(() -> shareService.saveSharedFile(shareId, userId, rootFolderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save shared file to your storage");

        verify(shareMapper).findById(shareId);
        verify(storageService).downloadFile("path/to/original.pdf");
        verify(fileS3Service, never()).uploadFile(any(MultipartFile.class), anyLong(), anyLong());
    }

    @Test
    @DisplayName("save shared file throws exception when upload fails")
    void saveSharedFile_uploadFails_throwsException() throws IOException {
        UUID shareId = TEST_SHARE_ID;
        Long userId = 2L;
        Long rootFolderId = 10L;

        Share mockShare = createMockShare(shareId, TEST_FILE_ID, PermissionType.WRITE, true);
        File mockOriginalFile = createMockFile(TEST_FILE_ID, "original.pdf");
        FileVersion mockVersion = createMockFileVersion(TEST_FILE_ID, "path/to/original.pdf");
        byte[] fileContent = "test content".getBytes();

        when(shareMapper.findById(shareId)).thenReturn(mockShare);
        when(fileMapper.getFileById(TEST_FILE_ID)).thenReturn(mockOriginalFile);
        when(fileVersionMapper.getLatestVersion(TEST_FILE_ID)).thenReturn(mockVersion);
        when(storageService.downloadFile("path/to/original.pdf")).thenReturn(fileContent);
        when(folderHelperService.getRootFolderId(userId)).thenReturn(rootFolderId);
        when(fileS3Service.uploadFile(any(MultipartFile.class), eq(userId), eq(rootFolderId)))
                .thenThrow(new RuntimeException("Upload failed"));

        assertThatThrownBy(() -> shareService.saveSharedFile(shareId, userId, rootFolderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to save shared file to your storage");

        verify(storageService).downloadFile("path/to/original.pdf");
        verify(fileS3Service).uploadFile(any(MultipartFile.class), eq(userId), eq(rootFolderId));
    }

    // ============ cancelShare Tests ============

    @Test
    @DisplayName("cancel share successfully cancels the share")
    void cancelShare_success() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        when(shareMapper.cancelShare(shareId, userId)).thenReturn(1);

        int result = shareService.cancelShare(shareId, userId);

        assertThat(result).isEqualTo(1);
        verify(shareMapper).cancelShare(shareId, userId);
    }

    @Test
    @DisplayName("cancel share throws exception when no rows updated")
    void cancelShare_noRowsUpdated_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        when(shareMapper.cancelShare(shareId, userId)).thenReturn(0);

        assertThatThrownBy(() -> shareService.cancelShare(shareId, userId))
                .isInstanceOf(ShareLinkException.class)
                .hasMessageContaining("Failed to cancel share or no permission");

        verify(shareMapper).cancelShare(shareId, userId);
    }

    @Test
    @DisplayName("cancel share throws exception when user has no permission")
    void cancelShare_noPermission_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = 999L;

        when(shareMapper.cancelShare(shareId, userId)).thenReturn(0);

        assertThatThrownBy(() -> shareService.cancelShare(shareId, userId))
                .isInstanceOf(ShareLinkException.class)
                .hasMessageContaining("Failed to cancel share or no permission");

        verify(shareMapper).cancelShare(shareId, userId);
    }

    // ============ restore Tests ============

    @Test
    @DisplayName("restore share successfully restores the share")
    void restore_success() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        when(shareMapper.restore(shareId, userId)).thenReturn(1);

        int result = shareService.restore(shareId, userId);

        assertThat(result).isEqualTo(1);
        verify(shareMapper).restore(shareId, userId);
    }

    @Test
    @DisplayName("restore share throws exception when no rows updated")
    void restore_noRowsUpdated_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = TEST_USER_ID;

        when(shareMapper.restore(shareId, userId)).thenReturn(0);

        assertThatThrownBy(() -> shareService.restore(shareId, userId))
                .isInstanceOf(ShareLinkException.class)
                .hasMessageContaining("Failed to restore share or no permission");

        verify(shareMapper).restore(shareId, userId);
    }

    @Test
    @DisplayName("restore share throws exception when user has no permission")
    void restore_noPermission_throwsException() {
        UUID shareId = TEST_SHARE_ID;
        Long userId = 999L;

        when(shareMapper.restore(shareId, userId)).thenReturn(0);

        assertThatThrownBy(() -> shareService.restore(shareId, userId))
                .isInstanceOf(ShareLinkException.class)
                .hasMessageContaining("Failed to restore share or no permission");

        verify(shareMapper).restore(shareId, userId);
    }

    // ============ getAllUserShares Tests ============

    @Test
    @DisplayName("get all user shares returns list of shares successfully")
    void getAllUserShares_success() {
        Long userId = TEST_USER_ID;
        UUID shareId1 = UUID.randomUUID();
        UUID shareId2 = UUID.randomUUID();

        Share share1 = createMockShare(shareId1, 1L, PermissionType.READ, true);
        Share share2 = createMockShare(shareId2, 2L, PermissionType.WRITE, true);

        FileVersion version1 = createMockFileVersion(1L, "path1.pdf");
        FileVersion version2 = createMockFileVersion(2L, "path2.pdf");

        File file1 = createMockFile(1L, "file1.pdf");
        File file2 = createMockFile(2L, "file2.pdf");

        when(shareMapper.findByUserId(userId)).thenReturn(Arrays.asList(share1, share2));
        when(fileVersionMapper.getLatestVersion(1L)).thenReturn(version1);
        when(fileVersionMapper.getLatestVersion(2L)).thenReturn(version2);
        when(fileMapper.getFileByUserIdAndFileId(userId, 1L)).thenReturn(file1);
        when(fileMapper.getFileByUserIdAndFileId(userId, 2L)).thenReturn(file2);

        List<ShareInfoResponse> result = shareService.getAllUserShares(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFileId()).isEqualTo(1L);
        assertThat(result.get(0).getFileName()).isEqualTo("file1.pdf");
        assertThat(result.get(1).getFileId()).isEqualTo(2L);
        assertThat(result.get(1).getFileName()).isEqualTo("file2.pdf");

        verify(shareMapper).findByUserId(userId);
        verify(fileVersionMapper).getLatestVersion(1L);
        verify(fileVersionMapper).getLatestVersion(2L);
    }

    @Test
    @DisplayName("get all user shares returns empty list when no shares found")
    void getAllUserShares_noShares_returnsEmpty() {
        Long userId = TEST_USER_ID;

        when(shareMapper.findByUserId(userId)).thenReturn(Collections.emptyList());

        List<ShareInfoResponse> result = shareService.getAllUserShares(userId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(shareMapper).findByUserId(userId);
        verify(fileVersionMapper, never()).getLatestVersion(anyLong());
    }

    @Test
    @DisplayName("get all user shares skips share when file not found")
    void getAllUserShares_fileNotFound_skipsShare() {
        Long userId = TEST_USER_ID;
        UUID shareId = UUID.randomUUID();

        Share share = createMockShare(shareId, TEST_FILE_ID, PermissionType.READ, true);
        FileVersion version = createMockFileVersion(TEST_FILE_ID, "path.pdf");

        when(shareMapper.findByUserId(userId)).thenReturn(Arrays.asList(share));
        when(fileVersionMapper.getLatestVersion(TEST_FILE_ID)).thenReturn(version);
        when(fileMapper.getFileByUserIdAndFileId(userId, TEST_FILE_ID)).thenReturn(null);

        List<ShareInfoResponse> result = shareService.getAllUserShares(userId);


        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isNull();

        verify(shareMapper).findByUserId(userId);
        verify(fileVersionMapper).getLatestVersion(TEST_FILE_ID);
        verify(fileMapper).getFileByUserIdAndFileId(userId, TEST_FILE_ID);
    }

    @Test
    @DisplayName("get all user shares handles exception gracefully")
    void getAllUserShares_exceptionOccurs_handlesGracefully() {
        Long userId = TEST_USER_ID;
        UUID shareId1 = UUID.randomUUID();
        UUID shareId2 = UUID.randomUUID();

        Share share1 = createMockShare(shareId1, 1L, PermissionType.READ, true);
        Share share2 = createMockShare(shareId2, 2L, PermissionType.WRITE, true);

        FileVersion version2 = createMockFileVersion(2L, "path2.pdf");
        File file2 = createMockFile(2L, "file2.pdf");

        when(shareMapper.findByUserId(userId)).thenReturn(Arrays.asList(share1, share2));
        when(fileVersionMapper.getLatestVersion(1L)).thenThrow(new RuntimeException("Database error"));
        when(fileVersionMapper.getLatestVersion(2L)).thenReturn(version2);
        when(fileMapper.getFileByUserIdAndFileId(userId, 2L)).thenReturn(file2);

        List<ShareInfoResponse> result = shareService.getAllUserShares(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFileId()).isEqualTo(2L);

        verify(shareMapper).findByUserId(userId);
        verify(fileVersionMapper).getLatestVersion(1L);
        verify(fileVersionMapper).getLatestVersion(2L);
    }

    @Test
    @DisplayName("get all user shares verifies share link format")
    void getAllUserShares_verifyShareLinkFormat() {
        Long userId = TEST_USER_ID;
        UUID shareId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        Share share = createMockShare(shareId, TEST_FILE_ID, PermissionType.READ, true);
        FileVersion version = createMockFileVersion(TEST_FILE_ID, "path.pdf");
        File file = createMockFile(TEST_FILE_ID, "file.pdf");

        when(shareMapper.findByUserId(userId)).thenReturn(Arrays.asList(share));
        when(fileVersionMapper.getLatestVersion(TEST_FILE_ID)).thenReturn(version);
        when(fileMapper.getFileByUserIdAndFileId(userId, TEST_FILE_ID)).thenReturn(file);

        List<ShareInfoResponse> result = shareService.getAllUserShares(userId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShareLink())
                .isEqualTo(FRONTEND_URL + "/share/" + shareId);
    }

    // ============ Helper Methods ============

    private File createMockFile(Long id, String name) {
        File file = new File();
        file.setId(id);
        file.setName(name);
        file.setMimeType("application/pdf");
        file.setSize(1000L);
        file.setOwnerId(TEST_USER_ID);
        file.setFolderId(10L);
        file.setDeleted(false);
        file.setCreatedAt(Instant.now());
        file.setUpdatedAt(Instant.now());
        return file;
    }

    private Share createMockShare(UUID id, Long fileId, PermissionType accessType, boolean active) {
        Share share = new Share();
        share.setId(id);
        share.setFileId(fileId);
        share.setCreatedBy(TEST_USER_ID);
        share.setAccessType(accessType);
        share.setActive(active);
        share.setCreatedAt(OffsetDateTime.now());
        share.setExpiresAt(null);
        return share;
    }

    private FileVersion createMockFileVersion(Long fileId, String storagePath) {
        FileVersion version = new FileVersion();
        version.setId(1L);
        version.setFileId(fileId);
        version.setVersionNumber(1);
        version.setStoragePath(storagePath);
        version.setSize(1000L);
        version.setCreatedAt(Instant.now());
        version.setCreatedBy(TEST_USER_ID);
        return version;
    }
}