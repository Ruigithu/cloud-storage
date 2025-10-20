package com.ruipeng.cloudstorage.service;

import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.ruipeng.cloudstorage.dto.DownloadFileInfo;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import org.apache.ibatis.javassist.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.postgresql.util.PGobject;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FolderS3Service Unit Test")
class FolderS3ServiceTest {

    @Mock private FolderMapper folderMapper;
    @Mock private FileMapper fileMapper;
    @Mock private FileVersionMapper fileVersionMapper;
    @Mock private FilePermissionMapper filePermissionMapper;
    @Mock private FilePermissionService permissionService;
    @Mock private FileS3Service fileService;
    @Mock private ResumableUploadService resumableUploadService;
    @Mock private S3StorageService storageService;
    @Mock private FolderHelperService folderHelperService;

    @InjectMocks
    private FolderS3Service folderS3Service;

    // ============ getRootFolderId Tests ============

    @Test
    @DisplayName("get root folder id successfully")
    void getRootFolderId_success() {
        Long userId = 1L;
        Long expectedRootFolderId = 100L;

        when(folderHelperService.getRootFolderId(userId)).thenReturn(expectedRootFolderId);

        Long result = folderS3Service.getRootFolderId(userId);

        assertThat(result).isEqualTo(expectedRootFolderId);
        verify(folderHelperService).getRootFolderId(userId);
    }

    @Test
    @DisplayName("get folders by parent successfully")
    void getFoldersByParent_success() {
        Long userId = 1L;
        Long parentId = 10L;
        List<Folder> expectedFolders = new ArrayList<>(Arrays.asList(getTestFolder(1L), getTestFolder(2L)));

        when(folderMapper.getFoldersByUserIdAndFolderId(userId, parentId)).thenReturn(expectedFolders);

        List<Folder> result = folderS3Service.getFoldersByParent(userId, parentId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).isEqualTo(expectedFolders);

        verify(folderMapper).getFoldersByUserIdAndFolderId(userId, parentId);
    }

    @Test
    @DisplayName("get folders by parent with zero parent id uses root folder")
    void getFoldersByParent_zeroParentId_usesRootFolder() {
        Long userId = 1L;
        Long parentId = 0L;
        Long rootFolderId = 100L;
        List<Folder> expectedFolders = new ArrayList<>(Arrays.asList(getTestFolder(1L)));

        when(folderHelperService.getRootFolderId(userId)).thenReturn(rootFolderId);
        when(folderMapper.getFoldersByUserIdAndFolderId(userId, rootFolderId)).thenReturn(expectedFolders);

        List<Folder> result = folderS3Service.getFoldersByParent(userId, parentId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verify(folderHelperService).getRootFolderId(userId);
        verify(folderMapper).getFoldersByUserIdAndFolderId(userId, rootFolderId);
    }

    // ============ createFolder Tests ============

    @Test
    @DisplayName("create folder successfully")
    void createFolder_success() throws SQLException {
        String folderName = "Test Folder";
        Long parentId = 10L;
        Long userId = 1L;
        Folder parentFolder = getTestFolder(parentId);
        Folder rootFolder = getTestFolder(100L);

        when(folderMapper.findById(parentId)).thenReturn(parentFolder);
        when(folderMapper.findRootFolderByUserId(userId)).thenReturn(rootFolder);
        when(permissionService.hasFolderPermission(parentId, userId, PermissionType.WRITE)).thenReturn(true);
        when(folderMapper.insert(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(1L);
            return 1;
        });
        doNothing().when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        doNothing().when(permissionService).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());

        Folder result = folderS3Service.createFolder(folderName, parentId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(folderName);
        assertThat(result.getParentId()).isEqualTo(parentId);
        assertThat(result.getOwnerId()).isEqualTo(userId);

        verify(folderMapper).findById(parentId);
        verify(folderMapper).insert(any(Folder.class));
        verify(permissionService).grantFolderPermission(anyLong(), eq(userId), eq(PermissionType.ADMIN), eq(userId));
    }

    @Test
    @DisplayName("create folder with null parent id uses root folder")
    void createFolder_nullParentId_usesRootFolder() throws SQLException {
        String folderName = "Test Folder";
        Long parentId = null;
        Long userId = 1L;
        Long rootFolderId = 100L;
        Folder rootFolder = getTestFolder(rootFolderId);

        when(folderHelperService.getRootFolderId(userId)).thenReturn(rootFolderId);
        when(folderMapper.findById(rootFolderId)).thenReturn(rootFolder);
        when(folderMapper.findRootFolderByUserId(userId)).thenReturn(rootFolder);
        when(permissionService.hasFolderPermission(rootFolderId, userId, PermissionType.WRITE)).thenReturn(true);
        when(folderMapper.insert(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(1L);
            return 1;
        });
        doNothing().when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        doNothing().when(permissionService).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());

        Folder result = folderS3Service.createFolder(folderName, parentId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getParentId()).isEqualTo(rootFolderId);

        verify(folderHelperService).getRootFolderId(userId);
    }

    @Test
    @DisplayName("failed to create folder because parent folder not found")
    void createFolder_parentNotFound_throwsException() {
        String folderName = "Test Folder";
        Long parentId = 999L;
        Long userId = 1L;

        when(folderMapper.findById(parentId)).thenReturn(null);

        assertThatThrownBy(() -> folderS3Service.createFolder(folderName, parentId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Folder");

        verify(folderMapper).findById(parentId);
        verify(folderMapper, never()).insert(any(Folder.class));
    }

    @Test
    @DisplayName("failed to create folder because insufficient permission")
    void createFolder_insufficientPermission_throwsException() {
        String folderName = "Test Folder";
        Long parentId = 10L;
        Long userId = 1L;
        Folder parentFolder = getTestFolder(parentId);
        Folder rootFolder = getTestFolder(100L);

        when(folderMapper.findById(parentId)).thenReturn(parentFolder);
        when(folderMapper.findRootFolderByUserId(userId)).thenReturn(rootFolder);
        when(permissionService.hasFolderPermission(parentId, userId, PermissionType.WRITE)).thenReturn(false);

        assertThatThrownBy(() -> folderS3Service.createFolder(folderName, parentId, userId))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("No permission to create folder here");

        verify(folderMapper).findById(parentId);
        verify(permissionService).hasFolderPermission(parentId, userId, PermissionType.WRITE);
        verify(folderMapper, never()).insert(any(Folder.class));
    }

    @Test
    @DisplayName("failed to create folder because insert failed")
    void createFolder_insertFailed_throwsException() throws SQLException {
        String folderName = "Test Folder";
        Long parentId = 10L;
        Long userId = 1L;
        Folder parentFolder = getTestFolder(parentId);
        Folder rootFolder = getTestFolder(100L);

        when(folderMapper.findById(parentId)).thenReturn(parentFolder);
        when(folderMapper.findRootFolderByUserId(userId)).thenReturn(rootFolder);
        when(permissionService.hasFolderPermission(parentId, userId, PermissionType.WRITE)).thenReturn(true);
        when(folderMapper.insert(any(Folder.class))).thenReturn(0);

        assertThatThrownBy(() -> folderS3Service.createFolder(folderName, parentId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to insert folder");

        verify(folderMapper).insert(any(Folder.class));
    }

    // ============ downloadFolder Tests ============

    @Test
    @DisplayName("download folder successfully")
    void downloadFolder_success() throws IOException {
        Long folderId = 1L;
        Long userId = 1L;
        Folder folder = getTestFolder(folderId);
        folder.setName("TestFolder");

        List<File> files = new ArrayList<>(Arrays.asList(getTestFile(1L)));
        List<Folder> subFolders = new ArrayList<>();
        DownloadFileInfo fileInfo = new DownloadFileInfo("test.txt", "text/plain",
                new ByteArrayResource("content".getBytes()));

        when(folderMapper.findById(folderId)).thenReturn(folder);
        when(fileMapper.getFilesByFolderId(folderId)).thenReturn(files);
        when(folderMapper.findSubFolders(any(PGobject.class), eq(folderId))).thenReturn(subFolders);
        when(fileService.downloadFile(anyLong())).thenReturn(fileInfo);

        DownloadFileInfo result = folderS3Service.downloadFolder(folderId, userId);

        assertThat(result).isNotNull();
        assertThat(result.getFileName()).isEqualTo("TestFolder.zip");
        assertThat(result.getMimeType()).isEqualTo("application/zip");
        assertThat(result.getResource()).isNotNull();

        verify(folderMapper).findById(folderId);
        verify(fileMapper).getFilesByFolderId(folderId);
    }

    @Test
    @DisplayName("failed to download folder because folder not found")
    void downloadFolder_folderNotFound_throwsException() {
        Long folderId = 999L;
        Long userId = 1L;

        when(folderMapper.findById(folderId)).thenReturn(null);

        assertThatThrownBy(() -> folderS3Service.downloadFolder(folderId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Folder");

        verify(folderMapper).findById(folderId);
    }

    // ============ getAllDeletedFolders Tests ============

    @Test
    @DisplayName("get all deleted folders successfully")
    void getAllDeletedFolders_success() {
        Long userId = 1L;
        Long parentId = 10L;
        List<Folder> deletedFolders = new ArrayList<>(Arrays.asList(getTestFolder(1L), getTestFolder(2L)));

        when(folderMapper.getDeletedFoldersByUserIdAndFolderId(userId, parentId)).thenReturn(deletedFolders);

        List<Folder> result = folderS3Service.getAllDeletedFolders(userId, parentId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);

        verify(folderMapper).getDeletedFoldersByUserIdAndFolderId(userId, parentId);
    }

    @Test
    @DisplayName("get all deleted folders with null parent id uses root folder")
    void getAllDeletedFolders_nullParentId_usesRootFolder() {
        Long userId = 1L;
        Long parentId = null;
        Long rootFolderId = 100L;
        List<Folder> deletedFolders = new ArrayList<>(Arrays.asList(getTestFolder(1L)));

        when(folderHelperService.getRootFolderId(userId)).thenReturn(rootFolderId);
        when(folderMapper.getDeletedFoldersByUserIdAndFolderId(userId, rootFolderId)).thenReturn(deletedFolders);

        List<Folder> result = folderS3Service.getAllDeletedFolders(userId, parentId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verify(folderHelperService).getRootFolderId(userId);
        verify(folderMapper).getDeletedFoldersByUserIdAndFolderId(userId, rootFolderId);
    }

    // ============ softDeleteFolder Tests ============

    @Test
    @DisplayName("soft delete folder successfully")
    void softDeleteFolder_success() throws IOException {
        Long folderId = 1L;
        Long userId = 1L;
        Folder folder = getTestFolder(folderId);
        List<Folder> subFolders = new ArrayList<>();
        List<File> files = new ArrayList<>();

        when(folderMapper.findById(folderId)).thenReturn(folder);
        when(folderMapper.findSubFolders(any(PGobject.class), eq(folderId))).thenReturn(subFolders);
        when(fileMapper.findFilesByFolderIds(anyList(), anyLong())).thenReturn(files);
        when(fileService.softDeleteFile(anyLong(), anyLong())).thenReturn(1);
        doNothing().when(folderMapper).updateFolderDeleteStatus(any(Folder.class));

        int result = folderS3Service.softDeleteFolder(folderId, userId);

        assertThat(result).isEqualTo(1);

        verify(folderMapper).findById(folderId);
        verify(folderMapper).updateFolderDeleteStatus(argThat(f -> f.isDeleted()));
    }

    @Test
    @DisplayName("failed to soft delete folder because folder not found")
    void softDeleteFolder_folderNotFound_throwsException() {
        Long folderId = 999L;
        Long userId = 1L;

        when(folderMapper.findById(folderId)).thenReturn(null);

        assertThatThrownBy(() -> folderS3Service.softDeleteFolder(folderId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Folder");

        verify(folderMapper).findById(folderId);
        verify(folderMapper, never()).updateFolderDeleteStatus(any(Folder.class));
    }

    // ============ deleteFolder Tests ============

    @Test
    @DisplayName("delete folder permanently successfully")
    void deleteFolder_success() throws IOException {
        Long folderId = 1L;
        Long userId = 1L;
        Folder folder = getTestFolder(folderId);
        folder.setDeleted(true);
        List<Folder> subFolders = new ArrayList<>();
        List<File> files = new ArrayList<>();

        when(folderMapper.findDeletedById(folderId)).thenReturn(folder);
        when(folderMapper.findSubFolders(any(PGobject.class), eq(folderId))).thenReturn(subFolders);
        when(fileMapper.findFilesByFolderIds(anyList(), anyLong())).thenReturn(files);
        when(fileService.deleteFile(anyLong(), anyLong())).thenReturn(1);
        doNothing().when(filePermissionMapper).deleteByFolderId(anyLong());
        doNothing().when(folderMapper).deleteFolder(anyLong());

        int result = folderS3Service.deleteFolder(folderId, userId);

        assertThat(result).isEqualTo(1);

        verify(folderMapper).findDeletedById(folderId);
    }

    @Test
    @DisplayName("failed to delete folder because deleted folder not found")
    void deleteFolder_folderNotFound_throwsException() {
        Long folderId = 999L;
        Long userId = 1L;

        when(folderMapper.findDeletedById(folderId)).thenReturn(null);

        assertThatThrownBy(() -> folderS3Service.deleteFolder(folderId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Deleted folder");

        verify(folderMapper).findDeletedById(folderId);
        verify(folderMapper, never()).deleteFolder(anyLong());
    }


    @Test
    @DisplayName("restore folder successfully")
    void restoreFolder_success() {
        Long folderId = 1L;
        Long userId = 1L;
        Folder folder = getTestFolder(folderId);
        folder.setDeleted(true);
        List<Folder> subFolders = new ArrayList<>();
        List<File> files = new ArrayList<>();

        when(folderMapper.findDeletedById(folderId)).thenReturn(folder);
        when(folderMapper.findSubFolders(any(PGobject.class), eq(folderId))).thenReturn(subFolders);
        when(fileMapper.findFilesByFolderIds(anyList(), anyLong())).thenReturn(files);
        when(fileService.restoreFile(anyLong(), anyLong())).thenReturn(1);
        doNothing().when(folderMapper).updateFolderDeleteStatus(any(Folder.class));

        int result = folderS3Service.restoreFolder(folderId, userId);

        assertThat(result).isEqualTo(1);

        verify(folderMapper).findDeletedById(folderId);
        verify(folderMapper).updateFolderDeleteStatus(argThat(f -> !f.isDeleted()));
    }

    @Test
    @DisplayName("failed to restore folder because deleted folder not found")
    void restoreFolder_folderNotFound_throwsException() {
        Long folderId = 999L;
        Long userId = 1L;

        when(folderMapper.findDeletedById(folderId)).thenReturn(null);

        assertThatThrownBy(() -> folderS3Service.restoreFolder(folderId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Deleted folder");

        verify(folderMapper).findDeletedById(folderId);
        verify(folderMapper, never()).updateFolderDeleteStatus(any(Folder.class));
    }

    // ============ uploadFolderSmall Tests ============

    @Test
    @DisplayName("upload small folder successfully")
    void uploadFolderSmall_success() throws IOException, SQLException, NotFoundException {
        Long userId = 1L;
        Long parentFolderId = 10L;
        MultipartFile[] files = new MultipartFile[]{getTestMultipartFile()};
        String[] relativePaths = new String[]{"folder1/test.txt"};

        Folder parentFolder = getTestFolder(parentFolderId);
        Folder newFolder = getTestFolder(20L);

        when(folderMapper.findById(parentFolderId)).thenReturn(parentFolder);
        when(folderMapper.findRootFolderByUserId(userId)).thenReturn(getTestFolder(100L));
        when(permissionService.hasFolderPermission(parentFolderId, userId, PermissionType.WRITE)).thenReturn(true);
        when(folderMapper.insert(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(20L);
            return 1;
        });
        doNothing().when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        doNothing().when(permissionService).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());
        when(fileMapper.folderExists(anyLong())).thenReturn(getTestFolderList());
        doAnswer(invocation -> {
            File file = invocation.getArgument(0);
            file.setId(1L);
            return null;
        }).when(fileMapper).insertFile(any(File.class));
        when(storageService.generateStorageKey(anyLong(), anyLong(), anyLong(), anyString(), anyInt()))
                .thenReturn("s3/path/to/file.txt");
        doNothing().when(storageService).uploadFile(any(MultipartFile.class), anyString());
        doNothing().when(fileVersionMapper).insertVersion(any(FileVersion.class));
        doNothing().when(permissionService).grantPermission(anyLong(), anyLong(), anyLong(), any(PermissionType.class));

        folderS3Service.uploadFolderSmall(files, relativePaths, userId, parentFolderId);

        verify(folderMapper,times(2)).findById(parentFolderId);
        verify(folderMapper, atLeastOnce()).insert(any(Folder.class));
    }

    @Test
    @DisplayName("failed to upload small folder because parent folder not found")
    void uploadFolderSmall_parentNotFound_throwsException() {
        Long userId = 1L;
        Long parentFolderId = 999L;
        MultipartFile[] files = new MultipartFile[]{getTestMultipartFile()};
        String[] relativePaths = new String[]{"folder1/test.txt"};

        when(folderMapper.findById(parentFolderId)).thenReturn(null);

        assertThatThrownBy(() -> folderS3Service.uploadFolderSmall(files, relativePaths, userId, parentFolderId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Parent folder not found");

        verify(folderMapper).findById(parentFolderId);
    }


    @Test
    @DisplayName("initiate folder upload successfully")
    void initiateFolderUpload_success() throws IOException, SQLException, NotFoundException {
        Long userId = 1L;
        Long parentFolderId = 10L;
        MultipartFile[] files = new MultipartFile[]{getTestMultipartFile()};
        String[] relativePaths = new String[]{"folder1/test.txt"};

        Folder parentFolder = getTestFolder(parentFolderId);
        Map<String, Object> uploadInfo = new HashMap<>();
        uploadInfo.put("fileId", 1L);
        uploadInfo.put("uploadId", "upload-123");

        when(folderMapper.findById(parentFolderId)).thenReturn(parentFolder);
        when(folderMapper.findRootFolderByUserId(userId)).thenReturn(getTestFolder(100L));
        when(permissionService.hasFolderPermission(parentFolderId, userId, PermissionType.WRITE)).thenReturn(true);
        when(folderMapper.insert(any(Folder.class))).thenAnswer(invocation -> {
            Folder folder = invocation.getArgument(0);
            folder.setId(20L);
            return 1;
        });
        doNothing().when(folderMapper).updatePath(anyLong(), any(PGobject.class), any(Instant.class));
        doNothing().when(permissionService).grantFolderPermission(anyLong(), anyLong(), any(PermissionType.class), anyLong());
        when(resumableUploadService.initiateUpload(anyLong(), anyLong(), anyString(), anyString(), anyLong()))
                .thenReturn(uploadInfo);

        Map<String, Object> result = folderS3Service.initiateFolderUpload(files, relativePaths, userId, parentFolderId);

        assertThat(result).isNotNull();
        assertThat(result).containsKeys("folderStructure", "fileUploads");

        verify(folderMapper,times(2)).findById(parentFolderId);
        verify(resumableUploadService).initiateUpload(anyLong(), anyLong(), anyString(), anyString(), anyLong());
    }


    @Test
    @DisplayName("complete folder upload successfully")
    void completeFolderUpload_success() {
        List<Map<String, Object>> fileCompletions = new ArrayList<>();
        Map<String, Object> completion = new HashMap<>();
        completion.put("fileId", 1L);
        completion.put("uploadId", "upload-123");
        completion.put("partETags", Arrays.asList(new PartETag(1, "etag")));
        fileCompletions.add(completion);

        doNothing().when(resumableUploadService).completeUpload(anyLong(), anyString(), anyList());

        folderS3Service.completeFolderUpload(fileCompletions);

        verify(resumableUploadService).completeUpload(eq(1L), eq("upload-123"), anyList());
    }

    // ============ abortFolderUpload Tests ============

    @Test
    @DisplayName("abort folder upload successfully")
    void abortFolderUpload_success() throws IOException {
        Long fileId = 1L;
        String uploadId = "upload-123";
        FileVersion version = getTestFileVersion();
        version.setUploadId(uploadId);

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);
        doNothing().when(storageService).abortMultipartUpload(anyString(), anyString());

        folderS3Service.abortFolderUpload(fileId, uploadId);

        verify(fileVersionMapper).getLatestVersion(fileId);
        verify(storageService).abortMultipartUpload(version.getStoragePath(), uploadId);
    }

    @Test
    @DisplayName("failed to abort folder upload because file id is null")
    void abortFolderUpload_nullFileId_throwsException() {
        Long fileId = null;
        String uploadId = "upload-123";

        assertThatThrownBy(() -> folderS3Service.abortFolderUpload(fileId, uploadId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileId and uploadId cannot be null");
    }

    @Test
    @DisplayName("failed to abort folder upload because upload id is null")
    void abortFolderUpload_nullUploadId_throwsException() {
        Long fileId = 1L;
        String uploadId = null;

        assertThatThrownBy(() -> folderS3Service.abortFolderUpload(fileId, uploadId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileId and uploadId cannot be null");
    }

    @Test
    @DisplayName("failed to abort folder upload because version not found")
    void abortFolderUpload_versionNotFound_throwsException() {
        Long fileId = 1L;
        String uploadId = "upload-123";

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(null);

        assertThatThrownBy(() -> folderS3Service.abortFolderUpload(fileId, uploadId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File version not found");

        verify(fileVersionMapper).getLatestVersion(fileId);
    }

    @Test
    @DisplayName("failed to abort folder upload because invalid upload id")
    void abortFolderUpload_invalidUploadId_throwsException() {
        Long fileId = 1L;
        String uploadId = "upload-123";
        FileVersion version = getTestFileVersion();
        version.setUploadId("different-upload-id");

        when(fileVersionMapper.getLatestVersion(fileId)).thenReturn(version);

        assertThatThrownBy(() -> folderS3Service.abortFolderUpload(fileId, uploadId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid upload ID for file");

        verify(fileVersionMapper).getLatestVersion(fileId);
    }

    // ============ getFolderUploadStatus Tests ============

    @Test
    @DisplayName("get folder upload status successfully")
    void getFolderUploadStatus_success() {
        List<Long> fileIds = Arrays.asList(1L, 2L);
        FileVersion version1 = getTestFileVersion();
        version1.setUploadId("upload-123");
        FileVersion version2 = getTestFileVersion();
        version2.setUploadId(null);
        File file1 = getTestFile(1L);
        File file2 = getTestFile(2L);
        List<PartSummary> parts = new ArrayList<>();

        when(fileVersionMapper.getLatestVersion(1L)).thenReturn(version1);
        when(fileVersionMapper.getLatestVersion(2L)).thenReturn(version2);
        when(fileMapper.getFileById(1L)).thenReturn(file1);
        when(fileMapper.getFileById(2L)).thenReturn(file2);
        when(storageService.listUploadedParts(anyString(), anyString())).thenReturn(parts);

        Map<String, Object> result = folderS3Service.getFolderUploadStatus(fileIds);

        assertThat(result).isNotNull();
        assertThat(result).containsKeys("files", "totalFiles", "completedFiles");
        assertThat(result.get("totalFiles")).isEqualTo(2);

        verify(fileVersionMapper, times(2)).getLatestVersion(anyLong());
        verify(fileMapper, times(2)).getFileById(anyLong());
    }

    @Test
    @DisplayName("get folder upload status with empty file list")
    void getFolderUploadStatus_emptyList_success() {
        List<Long> fileIds = new ArrayList<>();

        Map<String, Object> result = folderS3Service.getFolderUploadStatus(fileIds);

        assertThat(result).isNotNull();
        assertThat(result.get("totalFiles")).isEqualTo(0);
        assertThat(result.get("completedFiles")).isEqualTo(0L);
    }

    // ============ Helper Methods ============

    private Folder getTestFolder(Long id) {
        Folder folder = new Folder();
        folder.setId(id);
        folder.setName("Test Folder");
        folder.setOwnerId(1L);
        folder.setParentId(10L);
        folder.setDeleted(false);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());

        try {
            PGobject path = new PGobject();
            path.setType("ltree");
            path.setValue(id.toString());
            folder.setPath(path);
        } catch (SQLException e) {
        }

        return folder;
    }

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
    private List<File> getTestFolderList() {
        List<File> list = new ArrayList<>();
        list.add(getTestFile(1L));
        return list;
    }
}
