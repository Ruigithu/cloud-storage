package com.ruipeng.cloudstorage.service;

import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.FileVersion;
import com.ruipeng.cloudstorage.entity.PermissionType;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumableUploadService Unit Test")
class ResumableUploadServiceTest {

    @Mock
    private FileMapper fileMapper;

    @Mock
    private FileVersionMapper fileVersionMapper;

    @Mock
    private S3StorageService storageService;

    @Mock
    private FilePermissionService permissionService;

    @InjectMocks
    private ResumableUploadService resumableUploadService;

    // ============ initiateUpload Tests ============

    @Test
    @DisplayName("initiate upload successfully creates file and returns upload info")
    void initiateUpload_success() {
        Long ownerId = 1L;
        Long folderId = 10L;
        String fileName = "test.mp4";
        String mimeType = "video/mp4";
        Long fileSize = 1000000L;
        String uploadId = "test-upload-id-123";

        when(fileMapper.folderExists(folderId)).thenReturn(getTestFolderList());
        doAnswer(invocationOnMock -> {
            File newFile = invocationOnMock.getArgument(0);
            newFile.setId(1L);
            return newFile;
        }).when(fileMapper).insertFile(any(File.class));
        when(storageService.generateStorageKey(anyLong(), anyLong(), anyLong(), anyString(), anyInt()))
                .thenReturn("storage/key/test.mp4");

        InitiateMultipartUploadResult uploadResult = new InitiateMultipartUploadResult();
        uploadResult.setUploadId(uploadId);
        when(storageService.initiateMultipartUpload(anyString())).thenReturn(uploadResult);

        doNothing().when(fileVersionMapper).insertVersion(any(FileVersion.class));
        doNothing().when(permissionService).grantPermission(anyLong(), anyLong(), anyLong(), any(PermissionType.class));

        Map<String, Object> result = resumableUploadService.initiateUpload(
                ownerId, folderId, fileName, mimeType, fileSize
        );

        assertThat(result).isNotNull();
        assertThat(result).containsKeys("fileId", "uploadId");
        assertThat(result.get("uploadId")).isEqualTo(uploadId);
        assertThat(result.get("fileId")).isNotNull();

        verify(fileMapper).folderExists(folderId);
        verify(fileMapper).insertFile(any(File.class));
        verify(storageService).generateStorageKey(anyLong(), anyLong(), anyLong(), anyString(), eq(1));
        verify(storageService).initiateMultipartUpload(anyString());
        verify(fileVersionMapper).insertVersion(any(FileVersion.class));
        verify(permissionService).grantPermission(anyLong(), eq(ownerId), eq(folderId), eq(PermissionType.ADMIN));
    }

    @Test
    @DisplayName("initiate upload throws exception when folder does not exist")
    void initiateUpload_folderNotExists_throwsException() {
        Long ownerId = 1L;
        Long folderId = 999L;
        String fileName = "test.mp4";
        String mimeType = "video/mp4";
        Long fileSize = 1000000L;

        when(fileMapper.folderExists(folderId)).thenThrow(new ResourceNotFoundException("Folder", folderId));

        assertThatThrownBy(() -> resumableUploadService.initiateUpload(
                ownerId, folderId, fileName, mimeType, fileSize
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Folder");

        verify(fileMapper).folderExists(folderId);
        verify(fileMapper, never()).insertFile(any(File.class));
        verify(storageService, never()).initiateMultipartUpload(anyString());
    }

    @Test
    @DisplayName("initiate upload throws exception when folder id is null")
    void initiateUpload_folderIdNull_throwsException() {
        Long ownerId = 1L;
        Long folderId = null;
        String fileName = "test.mp4";
        String mimeType = "video/mp4";
        Long fileSize = 1000000L;

        assertThatThrownBy(() -> resumableUploadService.initiateUpload(
                ownerId, folderId, fileName, mimeType, fileSize
        ))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileMapper, never()).insertFile(any(File.class));
    }

    @Test
    @DisplayName("initiate upload verifies file metadata is set correctly")
    void initiateUpload_verifyFileMetadata() {
        Long ownerId = 2L;
        Long folderId = 20L;
        String fileName = "document.pdf";
        String mimeType = "application/pdf";
        Long fileSize = 500000L;
        String uploadId = "upload-123";

        when(fileMapper.folderExists(folderId)).thenReturn(getTestFolderList());
        doAnswer(invocationOnMock -> {
            File newFile = invocationOnMock.getArgument(0);
            newFile.setId(1L);
            return newFile;
        }).when(fileMapper).insertFile(any(File.class));
        when(storageService.generateStorageKey(anyLong(), anyLong(), anyLong(), anyString(), anyInt()))
                .thenReturn("storage/key/document.pdf");

        InitiateMultipartUploadResult uploadResult = new InitiateMultipartUploadResult();
        uploadResult.setUploadId(uploadId);
        when(storageService.initiateMultipartUpload(anyString())).thenReturn(uploadResult);

        doNothing().when(fileVersionMapper).insertVersion(any(FileVersion.class));
        doNothing().when(permissionService).grantPermission(anyLong(), anyLong(), anyLong(), any(PermissionType.class));

        resumableUploadService.initiateUpload(ownerId, folderId, fileName, mimeType, fileSize);

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileMapper).insertFile(fileCaptor.capture());

        File capturedFile = fileCaptor.getValue();
        assertThat(capturedFile.getName()).isEqualTo(fileName);
        assertThat(capturedFile.getMimeType()).isEqualTo(mimeType);
        assertThat(capturedFile.getSize()).isEqualTo(fileSize);
        assertThat(capturedFile.getOwnerId()).isEqualTo(ownerId);
        assertThat(capturedFile.getFolderId()).isEqualTo(folderId);
        assertThat(capturedFile.getCreatedAt()).isNotNull();
        assertThat(capturedFile.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("initiate upload verifies file version is created with upload id")
    void initiateUpload_verifyFileVersionCreation() {
        Long ownerId = 1L;
        Long folderId = 10L;
        String fileName = "video.mp4";
        String mimeType = "video/mp4";
        Long fileSize = 2000000L;
        String uploadId = "multipart-upload-id";
        String storageKey = "path/to/video.mp4";

        when(fileMapper.folderExists(folderId)).thenReturn(getTestFolderList());
        doAnswer(invocationOnMock -> {
            File newFile = invocationOnMock.getArgument(0);
            newFile.setId(1L);
            return newFile;
        }).when(fileMapper).insertFile(any(File.class));
        when(storageService.generateStorageKey(anyLong(), anyLong(), anyLong(), anyString(), anyInt()))
                .thenReturn(storageKey);

        InitiateMultipartUploadResult uploadResult = new InitiateMultipartUploadResult();
        uploadResult.setUploadId(uploadId);
        when(storageService.initiateMultipartUpload(anyString())).thenReturn(uploadResult);

        doNothing().when(fileVersionMapper).insertVersion(any(FileVersion.class));
        doNothing().when(permissionService).grantPermission(anyLong(), anyLong(), anyLong(), any(PermissionType.class));

        resumableUploadService.initiateUpload(ownerId, folderId, fileName, mimeType, fileSize);

        ArgumentCaptor<FileVersion> versionCaptor = ArgumentCaptor.forClass(FileVersion.class);
        verify(fileVersionMapper).insertVersion(versionCaptor.capture());

        FileVersion capturedVersion = versionCaptor.getValue();
        assertThat(capturedVersion.getVersionNumber()).isEqualTo(1);
        assertThat(capturedVersion.getStoragePath()).isEqualTo(storageKey);
        assertThat(capturedVersion.getSize()).isEqualTo(fileSize);
        assertThat(capturedVersion.getCreatedBy()).isEqualTo(ownerId);
        assertThat(capturedVersion.getUploadId()).isEqualTo(uploadId);
        assertThat(capturedVersion.getCreatedAt()).isNotNull();
    }

    // ============ uploadPart Tests ============

    @Test
    @DisplayName("upload part successfully uploads data and returns part etag")
    void uploadPart_success() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        int partNumber = 1;
        byte[] partData = new byte[]{1, 2, 3, 4, 5};
        String storageKey = "path/to/file.mp4";

        FileVersion version = getTestMockFileVersion(fileId, uploadId, storageKey);
        PartETag expectedETag = new PartETag(partNumber, "etag-123");

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);
        when(storageService.uploadPart(storageKey, uploadId, partNumber, partData))
                .thenReturn(expectedETag);

        PartETag result = resumableUploadService.uploadPart(fileId, uploadId, partNumber, partData);

        assertThat(result).isNotNull();
        assertThat(result.getPartNumber()).isEqualTo(partNumber);
        assertThat(result.getETag()).isEqualTo("etag-123");

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).uploadPart(storageKey, uploadId, partNumber, partData);
    }

    @Test
    @DisplayName("upload part throws exception when file version not found")
    void uploadPart_versionNotFound_throwsException() {
        Long fileId = 999L;
        String uploadId = "upload-123";
        int partNumber = 1;
        byte[] partData = new byte[]{1, 2, 3};

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(null);

        assertThatThrownBy(() -> resumableUploadService.uploadPart(fileId, uploadId, partNumber, partData))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File version for file");

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService, never()).uploadPart(anyString(), anyString(), anyInt(), any(byte[].class));
    }

    @Test
    @DisplayName("upload part throws exception when upload id is null")
    void uploadPart_uploadIdNull_throwsException() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        int partNumber = 1;
        byte[] partData = new byte[]{1, 2, 3};
        String storageKey = "path/to/file.mp4";

        FileVersion version = getTestMockFileVersion(fileId, null, storageKey);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);

        assertThatThrownBy(() -> resumableUploadService.uploadPart(fileId, uploadId, partNumber, partData))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService, never()).uploadPart(anyString(), anyString(), anyInt(), any(byte[].class));
    }

    @Test
    @DisplayName("upload part throws exception when upload id does not match")
    void uploadPart_uploadIdMismatch_throwsException() {
        Long fileId = 1L;
        String requestUploadId = "upload-123";
        String actualUploadId = "upload-456";
        int partNumber = 1;
        byte[] partData = new byte[]{1, 2, 3};
        String storageKey = "path/to/file.mp4";

        FileVersion version = getTestMockFileVersion(fileId, actualUploadId, storageKey);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);

        assertThatThrownBy(() -> resumableUploadService.uploadPart(fileId, requestUploadId, partNumber, partData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid upload ID");

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService, never()).uploadPart(anyString(), anyString(), anyInt(), any(byte[].class));
    }

    // ============ listUploadedParts Tests ============

    @Test
    @DisplayName("list uploaded parts returns list of part summaries")
    void listUploadedParts_success() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        String storageKey = "path/to/file.mp4";

        FileVersion version = getTestMockFileVersion(fileId, uploadId, storageKey);
        List<PartSummary> expectedParts = Arrays.asList(
                createPartSummary(1, 1000L),
                createPartSummary(2, 1000L),
                createPartSummary(3, 500L)
        );

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);
        when(storageService.listUploadedParts(storageKey, uploadId)).thenReturn(expectedParts);

        List<PartSummary> result = resumableUploadService.listUploadedParts(fileId, uploadId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPartNumber()).isEqualTo(1);
        assertThat(result.get(1).getPartNumber()).isEqualTo(2);
        assertThat(result.get(2).getPartNumber()).isEqualTo(3);

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).listUploadedParts(storageKey, uploadId);
    }

    @Test
    @DisplayName("list uploaded parts throws exception when version not found")
    void listUploadedParts_versionNotFound_throwsException() {
        Long fileId = 999L;
        String uploadId = "upload-123";

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(null);

        assertThatThrownBy(() -> resumableUploadService.listUploadedParts(fileId, uploadId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService, never()).listUploadedParts(anyString(), anyString());
    }

    @Test
    @DisplayName("list uploaded parts returns empty list when no parts uploaded")
    void listUploadedParts_noParts_returnsEmptyList() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        String storageKey = "path/to/file.mp4";

        FileVersion version = getTestMockFileVersion(fileId, uploadId, storageKey);
        List<PartSummary> emptyList = Collections.emptyList();

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);
        when(storageService.listUploadedParts(storageKey, uploadId)).thenReturn(emptyList);

        List<PartSummary> result = resumableUploadService.listUploadedParts(fileId, uploadId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).listUploadedParts(storageKey, uploadId);
    }

    // ============ completeUpload Tests ============

    @Test
    @DisplayName("complete upload successfully completes multipart upload")
    void completeUpload_success() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        String storageKey = "path/to/file.mp4";
        List<PartETag> partETags = Arrays.asList(
                new PartETag(1, "etag1"),
                new PartETag(2, "etag2")
        );

        FileVersion version = getTestMockFileVersion(fileId, uploadId, storageKey);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);
        doNothing().when(storageService).completeMultipartUpload(storageKey, uploadId, partETags);
        doNothing().when(fileVersionMapper).updateUploadStatus(any(FileVersion.class));

        resumableUploadService.completeUpload(fileId, uploadId, partETags);

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).completeMultipartUpload(storageKey, uploadId, partETags);
        verify(fileVersionMapper).updateUploadStatus(version);
        assertThat(version.getUploadId()).isNull();
    }

    @Test
    @DisplayName("complete upload throws exception when version not found")
    void completeUpload_versionNotFound_throwsException() {
        Long fileId = 999L;
        String uploadId = "upload-123";
        List<PartETag> partETags = Arrays.asList(new PartETag(1, "etag1"));

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(null);

        assertThatThrownBy(() -> resumableUploadService.completeUpload(fileId, uploadId, partETags))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService, never()).completeMultipartUpload(anyString(), anyString(), anyList());
        verify(fileVersionMapper, never()).updateUploadStatus(any(FileVersion.class));
    }

    @Test
    @DisplayName("complete upload throws exception when upload id mismatch")
    void completeUpload_uploadIdMismatch_throwsException() {
        Long fileId = 1L;
        String requestUploadId = "upload-123";
        String actualUploadId = "upload-456";
        String storageKey = "path/to/file.mp4";
        List<PartETag> partETags = Arrays.asList(new PartETag(1, "etag1"));

        FileVersion version = getTestMockFileVersion(fileId, actualUploadId, storageKey);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);

        assertThatThrownBy(() -> resumableUploadService.completeUpload(fileId, requestUploadId, partETags))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid upload ID");

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService, never()).completeMultipartUpload(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("complete upload clears upload id from version")
    void completeUpload_clearsUploadId() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        String storageKey = "path/to/file.mp4";
        List<PartETag> partETags = Arrays.asList(new PartETag(1, "etag1"));

        FileVersion version = getTestMockFileVersion(fileId, uploadId, storageKey);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);
        doNothing().when(storageService).completeMultipartUpload(storageKey, uploadId, partETags);
        doNothing().when(fileVersionMapper).updateUploadStatus(any(FileVersion.class));

        resumableUploadService.completeUpload(fileId, uploadId, partETags);

        ArgumentCaptor<FileVersion> versionCaptor = ArgumentCaptor.forClass(FileVersion.class);
        verify(fileVersionMapper).updateUploadStatus(versionCaptor.capture());

        FileVersion capturedVersion = versionCaptor.getValue();
        assertThat(capturedVersion.getUploadId()).isNull();
    }

    // ============ abortUpload Tests ============

    @Test
    @DisplayName("abort upload successfully aborts and cleans up resources")
    void abortUpload_success() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        Long userId = 1L;
        String storageKey = "path/to/file.mp4";

        FileVersion version = getTestMockFileVersion(fileId, uploadId, storageKey);
        File file = getTestMockFile(fileId);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);
        doNothing().when(storageService).abortMultipartUpload(storageKey, uploadId);
        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileMapper.updateFileDeleteStatus(any(File.class))).thenReturn(1);
        when(fileMapper.deleteFile(fileId)).thenReturn(1);
        doNothing().when(fileVersionMapper).deleteByFileId(fileId);

        resumableUploadService.abortUpload(fileId, uploadId, userId);

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).abortMultipartUpload(storageKey, uploadId);
        verify(fileMapper).getFileById(fileId);
        verify(fileMapper).updateFileDeleteStatus(file);
        verify(fileMapper).deleteFile(fileId);
        verify(fileVersionMapper).deleteByFileId(fileId);
        assertThat(file.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("abort upload throws exception when version not found")
    void abortUpload_versionNotFound_throwsException() {
        Long fileId = 999L;
        String uploadId = "upload-123";
        Long userId = 1L;

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(null);

        assertThatThrownBy(() -> resumableUploadService.abortUpload(fileId, uploadId, userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService, never()).abortMultipartUpload(anyString(), anyString());
        verify(fileMapper, never()).deleteFile(anyLong());
    }

    @Test
    @DisplayName("abort upload handles cleanup gracefully when file not found")
    void abortUpload_fileNotFound_handlesGracefully() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        Long userId = 1L;
        String storageKey = "path/to/file.mp4";

        FileVersion version = getTestMockFileVersion(fileId, uploadId, storageKey);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);
        doNothing().when(storageService).abortMultipartUpload(storageKey, uploadId);
        when(fileMapper.getFileById(fileId)).thenReturn(null);

        resumableUploadService.abortUpload(fileId, uploadId, userId);

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).abortMultipartUpload(storageKey, uploadId);
        verify(fileMapper).getFileById(fileId);
        verify(fileMapper, never()).updateFileDeleteStatus(any(File.class));
        verify(fileMapper, never()).deleteFile(anyLong());
        verify(fileVersionMapper, never()).deleteByFileId(anyLong());
    }

    @Test
    @DisplayName("abort upload marks file as deleted before permanent deletion")
    void abortUpload_markAsDeletedFirst() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        Long userId = 1L;
        String storageKey = "path/to/file.mp4";

        FileVersion version = getTestMockFileVersion(fileId, uploadId, storageKey);
        File file = getTestMockFile(fileId);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);
        doNothing().when(storageService).abortMultipartUpload(storageKey, uploadId);
        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileMapper.updateFileDeleteStatus(any(File.class))).thenReturn(1);

        when(fileMapper.deleteFile(fileId)).thenReturn(1);
        doNothing().when(fileVersionMapper).deleteByFileId(fileId);

        resumableUploadService.abortUpload(fileId, uploadId, userId);

        ArgumentCaptor<File> fileCaptor = ArgumentCaptor.forClass(File.class);
        verify(fileMapper).updateFileDeleteStatus(fileCaptor.capture());

        File capturedFile = fileCaptor.getValue();
        assertThat(capturedFile.isDeleted()).isTrue();
        assertThat(capturedFile.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("abort upload throws exception when upload id mismatch")
    void abortUpload_uploadIdMismatch_throwsException() {
        Long fileId = 1L;
        String requestUploadId = "upload-123";
        String actualUploadId = "upload-456";
        Long userId = 1L;
        String storageKey = "path/to/file.mp4";

        FileVersion version = getTestMockFileVersion(fileId, actualUploadId, storageKey);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);

        assertThatThrownBy(() -> resumableUploadService.abortUpload(fileId, requestUploadId, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid upload ID");

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService, never()).abortMultipartUpload(anyString(), anyString());
    }

    // ============ Helper Methods ============

    private FileVersion getTestMockFileVersion(Long fileId, String uploadId, String storagePath) {
        FileVersion version = new FileVersion();
        version.setId(1L);
        version.setFileId(fileId);
        version.setVersionNumber(1);
        version.setStoragePath(storagePath);
        version.setSize(1000000L);
        version.setUploadId(uploadId);
        version.setCreatedAt(Instant.now());
        version.setCreatedBy(1L);
        return version;
    }

    private File getTestMockFile(Long fileId) {
        File file = new File();
        file.setId(fileId);
        file.setName("test.mp4");
        file.setMimeType("video/mp4");
        file.setSize(1000000L);
        file.setOwnerId(1L);
        file.setFolderId(10L);
        file.setDeleted(false);
        file.setCreatedAt(Instant.now());
        file.setUpdatedAt(Instant.now());
        return file;
    }

    private PartSummary createPartSummary(int partNumber, long size) {
        PartSummary summary = new PartSummary();
        summary.setPartNumber(partNumber);
        summary.setSize(size);
        summary.setETag("etag-" + partNumber);
        return summary;
    }

    private List<File> getTestFolderList() {
        List<File> list = new ArrayList<>();
        list.add(getTestMockFile(1L));
        return list;
    }
}