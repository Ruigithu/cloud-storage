package com.ruipeng.cloudstorage.service;

import com.amazonaws.services.s3.model.PartETag;
import com.ruipeng.cloudstorage.dto.DownloadFileInfo;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.exception.InsufficientPermissionException;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FileS3Service Unit Test")
class FileS3ServiceTest {

    @Mock private FileMapper fileMapper;
    @Mock private FileVersionMapper fileVersionMapper;
    @Mock private FilePermissionMapper filePermissionMapper;
    @Mock private ShareMapper shareMapper;
    @Mock private S3StorageService storageService;
    @Mock private FilePermissionService permissionService;
    @Mock private FolderHelperService folderHelperService;
    @Mock private ResumableUploadService resumableUploadService;

    @InjectMocks
    private FileS3Service fileS3Service;

    @Test
    @DisplayName("get root files successfully")
    void getRootFiles_success() {
        Long ownerId = 1L;
        Long rootFolderId = 100L;
        List<File> expectedFiles = Arrays.asList(getTestFile(1L), getTestFile(2L));

        when(folderHelperService.getRootFolderId(ownerId)).thenReturn(rootFolderId);
        when(fileMapper.getFilesByUserIdAndFolderId(ownerId, rootFolderId)).thenReturn(expectedFiles);

        List<File> result = fileS3Service.getRootFiles(ownerId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedFiles);

        verify(folderHelperService).getRootFolderId(ownerId);
        verify(fileMapper).getFilesByUserIdAndFolderId(ownerId, rootFolderId);
    }


    @Test
    @DisplayName("get files by folder successfully")
    void getFilesByFolder_success() {
        Long ownerId = 1L;
        Long folderId = 10L;
        List<File> expectedFiles = Arrays.asList(getTestFile(1L), getTestFile(2L));

        when(fileMapper.getFilesByUserIdAndFolderId(ownerId, folderId)).thenReturn(expectedFiles);

        List<File> result = fileS3Service.getFilesByFolder(ownerId, folderId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedFiles);

        verify(fileMapper).getFilesByUserIdAndFolderId(ownerId, folderId);
    }


    @Test
    @DisplayName("upload file successfully")
    void uploadFile_success() {
        MultipartFile multipartFile = getTestMultipartFile();
        Long ownerId = 1L;
        Long folderId = 10L;

        when(fileMapper.folderExists(folderId)).thenReturn(getTestFolder());
        doAnswer(invocation->{
            File file = invocation.getArgument(0);
            file.setId(1L);
            return null;
        }).when(fileMapper).insertFile(any(File.class));
        when(storageService.generateStorageKey(anyLong(), anyLong(), anyLong(), anyString(), anyInt()))
                .thenReturn("s3/path/to/file.txt");
        doNothing().when(storageService).uploadFile(any(MultipartFile.class), anyString());
        doNothing().when(fileVersionMapper).insertVersion(any(FileVersion.class));
        doNothing().when(permissionService).grantPermission(anyLong(), anyLong(), anyLong(), any(PermissionType.class));

        File result = fileS3Service.uploadFile(multipartFile, ownerId, folderId);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test.txt");
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getFolderId()).isEqualTo(folderId);

        verify(fileMapper).folderExists(folderId);
        verify(fileMapper).insertFile(any(File.class));
        verify(storageService).uploadFile(any(MultipartFile.class), anyString());
        verify(fileVersionMapper).insertVersion(any(FileVersion.class));
        verify(permissionService).grantPermission(anyLong(), anyLong(), eq(folderId), eq(PermissionType.ADMIN));
    }

    @Test
    @DisplayName("failed to upload file because folder not found")
    void uploadFile_folderNotFound_throwsException() {
        MultipartFile multipartFile = getTestMultipartFile();
        Long ownerId = 1L;
        Long folderId = 999L;

        when(fileMapper.folderExists(folderId)).thenThrow(new ResourceNotFoundException("Folder", folderId));

        assertThatThrownBy(() -> fileS3Service.uploadFile(multipartFile, ownerId, folderId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Folder");

        verify(fileMapper).folderExists(folderId);
        verify(fileMapper, never()).insertFile(any(File.class));
    }

    @Test
    @DisplayName("failed to upload file because file name is null")
    void uploadFile_nullFileName_throwsException() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        Long ownerId = 1L;
        Long folderId = 10L;

        when(fileMapper.folderExists(folderId)).thenReturn(getTestFolder());

        assertThatThrownBy(() -> fileS3Service.uploadFile(multipartFile, ownerId, folderId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File name cannot be null or empty");

        verify(fileMapper).folderExists(folderId);
        verify(fileMapper, never()).insertFile(any(File.class));
    }


    @Test
    @DisplayName("upload new version successfully")
    void uploadNewVersion_success() {
        MultipartFile multipartFile = getTestMultipartFile();
        Long ownerId = 1L;
        Long fileId = 1L;
        File existingFile = getTestFile(fileId);
        FileVersion latestVersion = getTestFileVersion();

        when(fileMapper.getFileById(fileId)).thenReturn(existingFile);
        when(permissionService.hasAdminPermission(fileId, ownerId)).thenReturn(true);
        when(fileVersionMapper.getLatestVersionNumber(fileId)).thenReturn(1);
        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(latestVersion);
        when(storageService.generateVersionedStorageKey(anyString(), anyInt())).thenReturn("s3/path/to/file_v2.txt");
        doNothing().when(storageService).uploadFile(any(MultipartFile.class), anyString());
        when(fileMapper.updateFile(any(File.class))).thenReturn(1);
        doNothing().when(fileVersionMapper).insertVersion(any(FileVersion.class));

        File result = fileS3Service.uploadNewVersion(multipartFile, ownerId, fileId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(fileId);
        assertThat(result.getMimeType()).isEqualTo(multipartFile.getContentType());

        verify(fileMapper).getFileById(fileId);
        verify(permissionService).hasAdminPermission(fileId, ownerId);
        verify(fileVersionMapper).getLatestVersionNumber(fileId);
        verify(storageService).uploadFile(any(MultipartFile.class), anyString());
        verify(fileMapper).updateFile(any(File.class));
        verify(fileVersionMapper).insertVersion(any(FileVersion.class));
    }

    @Test
    @DisplayName("failed to upload new version because file not found")
    void uploadNewVersion_fileNotFound_throwsException() {
        MultipartFile multipartFile = getTestMultipartFile();
        Long ownerId = 1L;
        Long fileId = 999L;

        when(fileMapper.getFileById(fileId)).thenReturn(null);

        assertThatThrownBy(() -> fileS3Service.uploadNewVersion(multipartFile, ownerId, fileId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File");

        verify(fileMapper).getFileById(fileId);
        verify(permissionService, never()).hasAdminPermission(anyLong(), anyLong());
    }

    @Test
    @DisplayName("failed to upload new version because insufficient permission")
    void uploadNewVersion_insufficientPermission_throwsException() {
        MultipartFile multipartFile = getTestMultipartFile();
        Long ownerId = 1L;
        Long fileId = 1L;
        File existingFile = getTestFile(fileId);

        when(fileMapper.getFileById(fileId)).thenReturn(existingFile);
        when(permissionService.hasAdminPermission(fileId, ownerId)).thenReturn(false);

        assertThatThrownBy(() -> fileS3Service.uploadNewVersion(multipartFile, ownerId, fileId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessageContaining("No permission to update this file");

        verify(fileMapper).getFileById(fileId);
        verify(permissionService).hasAdminPermission(fileId, ownerId);
        verify(storageService, never()).uploadFile(any(MultipartFile.class), anyString());
    }

    // ============ downloadFile Tests ============

    @Test
    @DisplayName("download file successfully")
    void downloadFile_success() throws IOException {
        Long fileId = 1L;
        File file = getTestFile(fileId);
        FileVersion latestVersion = getTestFileVersion();
        byte[] fileContent = "test content".getBytes();

        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(latestVersion);
        when(storageService.downloadFile(latestVersion.getStoragePath())).thenReturn(fileContent);

        DownloadFileInfo result = fileS3Service.downloadFile(fileId);

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo(file.getName());
        assertThat(result.getMimeType()).isEqualTo(file.getMimeType());
        assertThat(result.getResource()).isNotNull();

        verify(fileMapper).getFileById(fileId);
        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).downloadFile(latestVersion.getStoragePath());
    }

    @Test
    @DisplayName("failed to download file because file not found")
    void downloadFile_fileNotFound_throwsException() {
        Long fileId = 999L;

        when(fileMapper.getFileById(fileId)).thenReturn(null);

        assertThatThrownBy(() -> fileS3Service.downloadFile(fileId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File");

        verify(fileMapper).getFileById(fileId);
        verify(fileVersionMapper, never()).getLatestVersion(anyLong());
    }

    @Test
    @DisplayName("failed to download file because file version not found")
    void downloadFile_fileVersionNotFound_throwsException() {
        Long fileId = 1L;
        File file = getTestFile(fileId);

        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(null);

        assertThatThrownBy(() -> fileS3Service.downloadFile(fileId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File version for file");

        verify(fileMapper).getFileById(fileId);
        verify(fileVersionMapper).getLatestVersion(fileId);
    }

    @Test
    @DisplayName("failed to download file because storage error")
    void downloadFile_storageError_throwsException() throws IOException {
        Long fileId = 1L;
        File file = getTestFile(fileId);
        FileVersion latestVersion = getTestFileVersion();

        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(latestVersion);
        when(storageService.downloadFile(latestVersion.getStoragePath())).thenThrow(new IOException("Storage error"));

        assertThatThrownBy(() -> fileS3Service.downloadFile(fileId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to download file content");

        verify(fileMapper).getFileById(fileId);
        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).downloadFile(latestVersion.getStoragePath());
    }


    @Test
    @DisplayName("get all deleted files successfully")
    void getAllDeletedFiles_success() {
        Long ownerId = 1L;
        Long folderId = 10L;
        List<File> deletedFilesInFolder = new ArrayList<>(Arrays.asList(getTestFile(1L)));
        List<File> deletedFilesByUser = new ArrayList<>(Arrays.asList(getTestFile(2L), getTestFile(3L)));

        when(fileMapper.getDeletedFilesByUserIdAndFolderId(ownerId, folderId)).thenReturn(deletedFilesInFolder);
        when(fileMapper.getDeletedFileByUserId(ownerId)).thenReturn(deletedFilesByUser);

        List<File> result = fileS3Service.getAllDeletedFiles(ownerId, folderId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);

        verify(fileMapper).getDeletedFilesByUserIdAndFolderId(ownerId, folderId);
        verify(fileMapper).getDeletedFileByUserId(ownerId);
    }


    @Test
    @DisplayName("get file details as map successfully for text file")
    void getFileDetailsAsMap_textFile_success() throws IOException {
        Long fileId = 1L;
        Long ownerId = 1L;
        File file = getTestFile(fileId);
        file.setMimeType("text/plain");
        FileVersion latestVersion = getTestFileVersion();
        byte[] fileContent = "test content".getBytes();

        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(latestVersion);
        when(storageService.downloadFile(latestVersion.getStoragePath())).thenReturn(fileContent);

        Map<String, Object> result = fileS3Service.getFileDetailsAsMap(fileId, ownerId);

        assertThat(result).isNotNull();
        assertThat(result.get("fileId")).isEqualTo(fileId);
        assertThat(result.get("fileName")).isEqualTo(file.getName());
        assertThat(result.get("mimeType")).isEqualTo(file.getMimeType());
        assertThat(result.get("isWordDocument")).isEqualTo(false);
        assertThat(result.get("content")).isEqualTo("test content");
        assertThat(result.get("binaryContent")).isNull();

        verify(fileMapper).getFileById(fileId);
        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).downloadFile(latestVersion.getStoragePath());
    }

    @Test
    @DisplayName("get file details as map successfully for binary file")
    void getFileDetailsAsMap_binaryFile_success() throws IOException {
        Long fileId = 1L;
        Long ownerId = 1L;
        File file = getTestFile(fileId);
        file.setMimeType("image/png");
        FileVersion latestVersion = getTestFileVersion();
        byte[] fileContent = new byte[]{1, 2, 3, 4};

        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(latestVersion);
        when(storageService.downloadFile(latestVersion.getStoragePath())).thenReturn(fileContent);

        Map<String, Object> result = fileS3Service.getFileDetailsAsMap(fileId, ownerId);

        assertThat(result).isNotNull();
        assertThat(result.get("fileId")).isEqualTo(fileId);
        assertThat(result.get("isWordDocument")).isEqualTo(false);
        assertThat(result.get("content")).isNull();
        assertThat(result.get("binaryContent")).isEqualTo(fileContent);

        verify(fileMapper).getFileById(fileId);
        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).downloadFile(latestVersion.getStoragePath());
    }

    @Test
    @DisplayName("get file details as map successfully for word document")
    void getFileDetailsAsMap_wordDocument_success() throws IOException {
        Long fileId = 1L;
        Long ownerId = 1L;
        File file = getTestFile(fileId);
        file.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        FileVersion latestVersion = getTestFileVersion();
        latestVersion.setStoragePath("path/to/file.docx");
        byte[] fileContent = new byte[]{1, 2, 3, 4};

        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(latestVersion);
        when(storageService.downloadFile(latestVersion.getStoragePath())).thenReturn(fileContent);

        Map<String, Object> result = fileS3Service.getFileDetailsAsMap(fileId, ownerId);

        assertThat(result).isNotNull();
        assertThat(result.get("isWordDocument")).isEqualTo(true);

        verify(fileMapper).getFileById(fileId);
        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).downloadFile(latestVersion.getStoragePath());
    }

    // ============ softDeleteFile Tests ============

    @Test
    @DisplayName("soft delete file successfully")
    void softDeleteFile_success() {
        Long fileId = 1L;
        Long userId = 1L;
        File file = getTestFile(fileId);

        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileMapper.updateFileDeleteStatus(any(File.class))).thenReturn(1);

        int result = fileS3Service.softDeleteFile(fileId, userId);

        assertThat(result).isEqualTo(1);

        verify(fileMapper).getFileById(fileId);
        verify(fileMapper).updateFileDeleteStatus(argThat(f -> f.isDeleted()));
    }

    @Test
    @DisplayName("failed to soft delete file because file not found")
    void softDeleteFile_fileNotFound_throwsException() {
        Long fileId = 999L;
        Long userId = 1L;

        when(fileMapper.getFileById(fileId)).thenReturn(null);

        assertThatThrownBy(() -> fileS3Service.softDeleteFile(fileId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File");

        verify(fileMapper).getFileById(fileId);
        verify(fileMapper, never()).updateFileDeleteStatus(any(File.class));
    }

    @Test
    @DisplayName("failed to soft delete file because update failed")
    void softDeleteFile_updateFailed_throwsException() {
        Long fileId = 1L;
        Long userId = 1L;
        File file = getTestFile(fileId);

        when(fileMapper.getFileById(fileId)).thenReturn(file);
        when(fileMapper.updateFileDeleteStatus(any(File.class))).thenReturn(0);

        assertThatThrownBy(() -> fileS3Service.softDeleteFile(fileId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to soft delete file");

        verify(fileMapper).getFileById(fileId);
        verify(fileMapper).updateFileDeleteStatus(any(File.class));
    }


    @Test
    @DisplayName("delete file permanently successfully")
    void deleteFile_success() throws IOException {
        Long fileId = 1L;
        Long userId = 1L;
        File file = getTestFile(fileId);
        file.setDeleted(true);
        List<FileVersion> versions = Arrays.asList(getTestFileVersion());

        when(fileMapper.getDeletedFileById(fileId)).thenReturn(file);
        when(fileVersionMapper.getVersionsByFileId(fileId)).thenReturn(versions);
        doNothing().when(storageService).deleteFile(anyString());
        doNothing().when(fileVersionMapper).deleteByFileId(fileId);
        doNothing().when(filePermissionMapper).deleteByFileId(fileId);
        when(shareMapper.findByFileIdAndCreatedBy(fileId, userId)).thenReturn(Collections.emptyList());
        when(fileMapper.deleteFile(fileId)).thenReturn(1);

        int result = fileS3Service.deleteFile(fileId, userId);

        assertThat(result).isEqualTo(1);

        verify(fileMapper).getDeletedFileById(fileId);
        verify(fileVersionMapper).getVersionsByFileId(fileId);
        verify(storageService).deleteFile(anyString());
        verify(fileVersionMapper).deleteByFileId(fileId);
        verify(filePermissionMapper).deleteByFileId(fileId);
        verify(fileMapper).deleteFile(fileId);
    }

    @Test
    @DisplayName("delete file with shares successfully")
    void deleteFile_withShares_success() throws IOException {
        Long fileId = 1L;
        Long userId = 1L;
        File file = getTestFile(fileId);
        file.setDeleted(true);
        List<FileVersion> versions = Arrays.asList(getTestFileVersion());
        List<Share> shares = Arrays.asList(new Share());

        when(fileMapper.getDeletedFileById(fileId)).thenReturn(file);
        when(fileVersionMapper.getVersionsByFileId(fileId)).thenReturn(versions);
        doNothing().when(storageService).deleteFile(anyString());
        doNothing().when(fileVersionMapper).deleteByFileId(fileId);
        doNothing().when(filePermissionMapper).deleteByFileId(fileId);
        when(shareMapper.findByFileIdAndCreatedBy(fileId, userId)).thenReturn(shares);
        doNothing().when(shareMapper).deleteByFileIdAndCreatedBy(fileId, userId);
        when(fileMapper.deleteFile(fileId)).thenReturn(1);

        int result = fileS3Service.deleteFile(fileId, userId);

        assertThat(result).isEqualTo(1);

        verify(shareMapper).deleteByFileIdAndCreatedBy(fileId, userId);
        verify(fileMapper).deleteFile(fileId);
    }

    @Test
    @DisplayName("failed to delete file because deleted file not found")
    void deleteFile_fileNotFound_throwsException() {
        Long fileId = 999L;
        Long userId = 1L;

        when(fileMapper.getDeletedFileById(fileId)).thenReturn(null);

        assertThatThrownBy(() -> fileS3Service.deleteFile(fileId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Deleted file");

        verify(fileMapper).getDeletedFileById(fileId);
        verify(fileMapper, never()).deleteFile(anyLong());
    }

    // ============ restoreFile Tests ============

    @Test
    @DisplayName("restore file successfully")
    void restoreFile_success() {
        Long fileId = 1L;
        Long userId = 1L;
        File file = getTestFile(fileId);
        file.setDeleted(true);

        when(fileMapper.getDeletedFileById(fileId)).thenReturn(file);
        when(fileMapper.updateFileDeleteStatus(any(File.class))).thenReturn(1);

        int result = fileS3Service.restoreFile(fileId, userId);

        assertThat(result).isEqualTo(1);

        verify(fileMapper).getDeletedFileById(fileId);
        verify(fileMapper).updateFileDeleteStatus(argThat(f -> !f.isDeleted()));
    }

    @Test
    @DisplayName("failed to restore file because file not found")
    void restoreFile_fileNotFound_throwsException() {
        Long fileId = 999L;
        Long userId = 1L;

        when(fileMapper.getDeletedFileById(fileId)).thenReturn(null);

        assertThatThrownBy(() -> fileS3Service.restoreFile(fileId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Deleted file");

        verify(fileMapper).getDeletedFileById(fileId);
        verify(fileMapper, never()).updateFileDeleteStatus(any(File.class));
    }

    @Test
    @DisplayName("failed to restore file because file is not deleted")
    void restoreFile_fileNotDeleted_throwsException() {
        Long fileId = 1L;
        Long userId = 1L;
        File file = getTestFile(fileId);
        file.setDeleted(false);

        when(fileMapper.getDeletedFileById(fileId)).thenReturn(file);

        assertThatThrownBy(() -> fileS3Service.restoreFile(fileId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("File is not deleted");

        verify(fileMapper).getDeletedFileById(fileId);
        verify(fileMapper, never()).updateFileDeleteStatus(any(File.class));
    }

    @Test
    @DisplayName("failed to restore file because update failed")
    void restoreFile_updateFailed_throwsException() {
        Long fileId = 1L;
        Long userId = 1L;
        File file = getTestFile(fileId);
        file.setDeleted(true);

        when(fileMapper.getDeletedFileById(fileId)).thenReturn(file);
        when(fileMapper.updateFileDeleteStatus(any(File.class))).thenReturn(0);

        assertThatThrownBy(() -> fileS3Service.restoreFile(fileId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to restore file");

        verify(fileMapper).getDeletedFileById(fileId);
        verify(fileMapper).updateFileDeleteStatus(any(File.class));
    }

    // ============ uploadPart Tests ============

    @Test
    @DisplayName("upload part successfully")
    void uploadPart_success() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        int partNumber = 1;
        byte[] partData = "part data".getBytes();
        PartETag expectedPartETag = new PartETag(partNumber, "etag-123");

        when(resumableUploadService.uploadPart(fileId, uploadId, partNumber, partData)).thenReturn(expectedPartETag);

        PartETag result = fileS3Service.uploadPart(fileId, uploadId, partNumber, partData);

        assertThat(result).isNotNull();
        assertThat(result.getPartNumber()).isEqualTo(partNumber);
        assertThat(result.getETag()).isEqualTo("etag-123");

        verify(resumableUploadService).uploadPart(fileId, uploadId, partNumber, partData);
    }

    // ============ Helper Methods ============

    private File getTestFile(Long id) {
        File file = new File();
        file.setId(id);
        file.setName("test.txt");
        file.setMimeType("text/plain");
        file.setSize(1024L);
        file.setOwnerId(1L);
        file.setFolderId(10L);
        file.setDeleted(false);
        file.setCreatedAt(Instant.now());
        file.setUpdatedAt(Instant.now());
        return file;
    }

    private FileVersion getTestFileVersion() {
        FileVersion version = new FileVersion();
        version.setId(1L);
        version.setFileId(1L);
        version.setVersionNumber(1);
        version.setStoragePath("s3/path/to/file.txt");
        version.setSize(1024L);
        version.setCreatedBy(1L);
        version.setCreatedAt(Instant.now());
        return version;
    }

    private MultipartFile getTestMultipartFile() {
        MultipartFile multipartFile = mock(MultipartFile.class);
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getContentType()).thenReturn("text/plain");
        when(multipartFile.getSize()).thenReturn(1024L);
        return multipartFile;
    }

    private List<File> getTestFolder() {
        List<File> list = new ArrayList<>();
        list.add(getTestFile(1L));
        return list;
    }
}
