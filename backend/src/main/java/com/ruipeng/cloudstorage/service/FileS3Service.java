package com.ruipeng.cloudstorage.service;

import com.amazonaws.services.s3.model.PartETag;
import com.ruipeng.cloudstorage.dto.DownloadFileInfo;
import com.ruipeng.cloudstorage.dto.response.FileResponse;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.exception.InsufficientPermissionException;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing file operations.
 *
 * Responsibilities:
 * - Basic file CRUD operations
 * - File upload and download
 * - File version management
 * - Permission validation
 */
@Service
public class FileS3Service {
    private static final Logger log = LoggerFactory.getLogger(FileS3Service.class);

    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FilePermissionMapper filePermissionMapper;
    private final ShareMapper shareMapper;
    private final S3StorageService storageService;
    private final FilePermissionService permissionService;
    private final FolderHelperService folderHelperService;
    private final ResumableUploadService resumableUploadService;

    public FileS3Service(FileMapper fileMapper,
                       FileVersionMapper fileVersionMapper,
                       FilePermissionMapper filePermissionMapper,
                       ShareMapper shareMapper,
                       S3StorageService storageService,
                       FilePermissionService permissionService,
                       FolderHelperService folderHelperService,
                       ResumableUploadService resumableUploadService) {
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.filePermissionMapper = filePermissionMapper;
        this.shareMapper = shareMapper;
        this.storageService = storageService;
        this.permissionService = permissionService;
        this.folderHelperService = folderHelperService;
        this.resumableUploadService = resumableUploadService;
    }

    /**
     * Retrieves all files in the root folder for a user.
     */
    public List<File> getRootFiles(Long ownerId) {
        Long rootFolderId = folderHelperService.getRootFolderId(ownerId);
        return getFilesByFolder(ownerId, rootFolderId);
    }

    /**
     * Retrieves all files in a specific folder.
     */
    public List<File> getFilesByFolder(Long ownerId, Long folderId) {
        return fileMapper.getFilesByUserIdAndFolderId(ownerId, folderId);
    }

    /**
     * Gets detailed information about a file.
     */
    public FileResponse getFileDetails(Long fileId, Long ownerId) {
        File file = findFileById(fileId);
        FileVersion latestVersion = getLatestVersion(fileId);

        byte[] fileContent = downloadFileContent(latestVersion.getStoragePath());

        return buildFileResponse(file, latestVersion, fileContent);
    }

    /**
     * Uploads a new file.
     */
    @Transactional
    public File uploadFile(MultipartFile file, Long ownerId, Long folderId) {
        validateFolderExists(folderId);
        validateFileName(file.getOriginalFilename());

        File newFile = createFileMetadata(file, ownerId, folderId);
        saveFile(newFile);

        String s3Key = uploadFileToStorage(file, newFile, folderId, ownerId, 1);
        createFileVersion(newFile.getId(), s3Key, file.getSize(), ownerId, 1,fileVersionMapper);
        grantFilePermission(newFile.getId(), ownerId, folderId);

        log.info("File uploaded: id={}, name={}", newFile.getId(), newFile.getName());
        return newFile;
    }

    /**
     * Uploads a new version of an existing file.
     */
    @Transactional
    public File uploadNewVersion(MultipartFile file, Long ownerId, Long fileId) {
        File existingFile = findFileById(fileId);
        validateUserPermission(fileId, ownerId);

        int newVersionNumber = getNextVersionNumber(fileId);
        updateFileMetadata(existingFile, file);

        String s3Key = uploadNewVersionToStorage(file, existingFile, newVersionNumber);
        createFileVersion(fileId, s3Key, file.getSize(), ownerId, newVersionNumber,fileVersionMapper);

        log.info("New version uploaded: fileId={}, version={}", fileId, newVersionNumber);
        return existingFile;
    }

    /**
     * Downloads a file.
     */
    public DownloadFileInfo downloadFile(Long fileId) {
        File file = findFileById(fileId);
        FileVersion latestVersion = getLatestVersion(fileId);

        byte[] fileContent = downloadFileContent(latestVersion.getStoragePath());
        Resource resource = new ByteArrayResource(fileContent);

        return new DownloadFileInfo(file.getName(), file.getMimeType(), resource);
    }


    /**
     * Gets all deleted files for a user.
     */
    public List<File> getAllDeletedFiles(Long ownerId, Long folderId) {
        List<File> deletedFiles = fileMapper.getDeletedFilesByUserIdAndFolderId(ownerId, folderId);
        deletedFiles.addAll(fileMapper.getDeletedFileByUserId(ownerId));
        return deletedFiles;
    }

    // ============ Private Helper Methods ============

    private File findFileById(Long fileId) {
        File file = fileMapper.getFileById(fileId);
        if (file == null) {
            throw new ResourceNotFoundException("File", fileId);
        }
        return file;
    }

    private File findDeletedFileById(Long fileId) {
        File file = fileMapper.getDeletedFileById(fileId);
        if (file == null) {
            throw new ResourceNotFoundException("Deleted file", fileId);
        }
        return file;
    }

    private FileVersion getLatestVersion(Long fileId) {
        FileVersion version = fileVersionMapper.getLatestVersion(fileId);
        if (version == null) {
            throw new ResourceNotFoundException("File version for file", fileId);
        }
        return version;
    }

    private void validateFolderExists(Long folderId) {
        if (folderId == null || fileMapper.folderExists(folderId).isEmpty()) {
            throw new ResourceNotFoundException("Folder", folderId);
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }
    }

    private void validateUserPermission(Long fileId, Long userId) {
        if (!permissionService.hasAdminPermission(fileId, userId)) {
            throw new InsufficientPermissionException("No permission to update this file");
        }
    }

    private void validateFileIsDeleted(File file) {
        if (!file.isDeleted()) {
            throw new IllegalStateException("File is not deleted");
        }
    }

    private File createFileMetadata(MultipartFile file, Long ownerId, Long folderId) {
        File newFile = new File();
        newFile.setName(file.getOriginalFilename());
        newFile.setFolderId(folderId);
        newFile.setOwnerId(ownerId);
        newFile.setMimeType(file.getContentType());
        newFile.setSize(file.getSize());
        return newFile;
    }

    private void saveFile(File file) {
        fileMapper.insertFile(file);
    }

    private String uploadFileToStorage(MultipartFile file, File newFile,
                                       Long folderId, Long ownerId, int version) {
        String s3Key = storageService.generateStorageKey(
                ownerId, folderId, newFile.getId(),
                file.getOriginalFilename(), version
        );

        storageService.uploadFile(file, s3Key);
        return s3Key;
    }

    private void createFileVersion(Long fileId, String storagePath,
                                   Long size, Long createdBy, int versionNumber,FileVersionMapper fileVersionMapper) {
        DocumentConversionService.createNewFileVersion(fileId, storagePath, size, createdBy, versionNumber, fileVersionMapper);
    }

    private void grantFilePermission(Long fileId, Long userId, Long folderId) {
        permissionService.grantPermission(fileId, userId, folderId, PermissionType.ADMIN);
    }

    private int getNextVersionNumber(Long fileId) {
        int currentVersion = fileVersionMapper.getLatestVersionNumber(fileId);
        return currentVersion + 1;
    }

    private void updateFileMetadata(File file, MultipartFile uploadedFile) {
        file.setMimeType(uploadedFile.getContentType());
        file.setSize(uploadedFile.getSize());
        file.setUpdatedAt(Instant.now());
        fileMapper.updateFile(file);
    }

    private String uploadNewVersionToStorage(MultipartFile file, File existingFile, int version) {
        FileVersion latestVersion = getLatestVersion(existingFile.getId());
        String previousPath = latestVersion.getStoragePath();

        String s3Key = storageService.generateVersionedStorageKey(previousPath, version);
        storageService.uploadFile(file, s3Key);

        return s3Key;
    }

    private byte[] downloadFileContent(String storagePath) {
        try {
            return storageService.downloadFile(storagePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to download file content", e);
        }
    }

    private FileResponse buildFileResponse(File file, FileVersion version, byte[] content) {
        boolean isWord = isWordDocument(file.getMimeType(), version.getStoragePath());
        String textContent = isTextFile(file.getMimeType()) ?
                new String(content, StandardCharsets.UTF_8) : null;

        return FileResponse.builder()
                .fileId(file.getId())
                .fileName(file.getName())
                .mimeType(file.getMimeType())
                .size(file.getSize())
                .versionId(version.getId())
                .isWordDocument(isWord)
                .content(textContent)
                .build();
    }

    private boolean isWordDocument(String mimeType, String path) {
        return mimeType != null && (
                mimeType.contains("application/msword") ||
                        mimeType.contains("wordprocessingml.document") ||
                        path.endsWith(".doc") || path.endsWith(".docx")
        );
    }

    private boolean isTextFile(String mimeType) {
        return mimeType != null && (
                mimeType.startsWith("text/") ||
                        mimeType.equals("application/json")
        );
    }

    private void deleteFileVersions(Long fileId) {
        List<FileVersion> versions = fileVersionMapper.getVersionsByFileId(fileId);
        for (FileVersion version : versions) {
            storageService.deleteFile(version.getStoragePath());
        }
        fileVersionMapper.deleteByFileId(fileId);
    }

    private void deleteFilePermissions(Long fileId) {
        filePermissionMapper.deleteByFileId(fileId);
    }

    private void deleteFileShares(Long fileId, Long userId) {
        if (!shareMapper.findByFileIdAndCreatedBy(fileId, userId).isEmpty()) {
            shareMapper.deleteByFileIdAndCreatedBy(fileId, userId);
        }
    }

    /**
     * Gets file details as a Map for Controller response.
     * This is needed for backward compatibility with the original API.
     *
     * @param fileId the file ID
     * @param ownerId the owner user ID
     * @return map containing file details and content
     */
    public Map<String, Object> getFileDetailsAsMap(Long fileId, Long ownerId) {
        File file = findFileById(fileId);
        FileVersion latestVersion = getLatestVersion(fileId);

        byte[] fileContent = downloadFileContent(latestVersion.getStoragePath());

        Map<String, Object> response = new HashMap<>();
        response.put("fileId", file.getId());
        response.put("fileName", file.getName());
        response.put("mimeType", file.getMimeType());
        response.put("size", file.getSize());
        response.put("versionId", latestVersion.getId());

        boolean isWord = isWordDocument(file.getMimeType(), latestVersion.getStoragePath());
        response.put("isWordDocument", isWord);

        if (isTextFile(file.getMimeType())) {
            response.put("content", new String(fileContent, StandardCharsets.UTF_8));
        } else {
            response.put("binaryContent", fileContent);
        }

        return response;
    }

    /**
     * Soft deletes a file and returns affected rows.
     *
     * @param fileId the file ID
     * @param userId the user ID
     * @return number of affected rows
     */
    public int softDeleteFile(Long fileId, Long userId) {
        File file = findFileById(fileId);
        file.setDeleted(true);
        file.setUpdatedAt(Instant.now());

        int updated = fileMapper.updateFileDeleteStatus(file);
        if (updated == 0) {
            throw new RuntimeException("Failed to soft delete file");
        }

        log.info("File soft deleted: id={}", fileId);
        return updated;
    }

    /**
     * Permanently deletes a file and returns affected rows.
     *
     * @param fileId the file ID
     * @param userId the user ID
     * @return number of affected rows
     */
    public int deleteFile(Long fileId, Long userId) throws IOException {
        File file = findDeletedFileById(fileId);

        deleteFileVersions(fileId);
        deleteFilePermissions(fileId);
        deleteFileShares(fileId, userId);

        int deleted = fileMapper.deleteFile(fileId);
        if (deleted == 0) {
            throw new RuntimeException("Failed to delete file");
        }

        log.info("File permanently deleted: id={}", fileId);
        return deleted;
    }

    /**
     * Restores a deleted file and returns affected rows.
     *
     * @param fileId the file ID
     * @param userId the user ID
     * @return number of affected rows
     */
    public int restoreFile(Long fileId, Long userId) {
        File file = findDeletedFileById(fileId);
        validateFileIsDeleted(file);

        file.setDeleted(false);
        file.setUpdatedAt(Instant.now());

        int updated = fileMapper.updateFileDeleteStatus(file);
        if (updated == 0) {
            throw new RuntimeException("Failed to restore file");
        }

        log.info("File restored: id={}", fileId);
        return updated;
    }

    /**
     * Uploads a part of a multipart upload.
     * This is a convenience method for FolderController.
     *
     * @param fileId the file ID
     * @param uploadId the upload ID
     * @param partNumber the part number
     * @param partData the part data
     * @return the part ETag
     */
    public PartETag uploadPart(Long fileId, String uploadId, int partNumber, byte[] partData) {
        return resumableUploadService.uploadPart(fileId, uploadId, partNumber, partData);
    }
}