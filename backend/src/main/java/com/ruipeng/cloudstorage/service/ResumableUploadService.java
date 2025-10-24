package com.ruipeng.cloudstorage.service;

import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling resumable (multipart) file uploads.
 *
 * Supports large file uploads with the ability to:
 * - Initialize multipart uploads
 * - Upload individual parts
 * - Track upload progress
 * - Complete or abort uploads
 */
@Service
public class ResumableUploadService {
    private static final Logger log = LoggerFactory.getLogger(ResumableUploadService.class);

    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final S3StorageService storageService;
    private final FilePermissionService permissionService;
    private final FilePermissionService filePermissionService;
    private final FilePermissionMapper filePermissionMapper;

    public ResumableUploadService(FileMapper fileMapper,
                                  FileVersionMapper fileVersionMapper,
                                  S3StorageService storageService,
                                  FilePermissionService permissionService, FilePermissionService filePermissionService, FilePermissionMapper filePermissionMapper) {
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.storageService = storageService;
        this.permissionService = permissionService;
        this.filePermissionService = filePermissionService;
        this.filePermissionMapper = filePermissionMapper;
    }

    /**
     * Initiates a resumable upload session.
     *
     * @return Map containing fileId and uploadId
     */
    @Transactional
    public Map<String, Object> initiateUpload(Long ownerId, Long folderId,
                                              String fileName, String mimeType,
                                              Long fileSize) {
        validateFolderExists(folderId);

        File newFile = createFileMetadata(fileName, mimeType, fileSize, ownerId, folderId);
        saveFile(newFile);

        String s3Key = generateStorageKey(ownerId, folderId, newFile.getId(), fileName);
        String uploadId = initiateMultipartUpload(s3Key);

        createPendingFileVersion(newFile.getId(), s3Key, fileSize, ownerId, uploadId);
        grantFilePermission(newFile.getId(), ownerId, folderId);

        log.info("Resumable upload initiated: fileId={}, uploadId={}",
                newFile.getId(), uploadId);

        return createInitResponse(newFile.getId(), uploadId);
    }

    /**
     * Uploads a single part of a multipart upload.
     */
    public PartETag uploadPart(Long fileId, String uploadId,
                               int partNumber, byte[] partData) {
        FileVersion version = findVersionByFileIdAndUploadId(fileId, uploadId);

        PartETag partETag = storageService.uploadPart(
                version.getStoragePath(),
                uploadId,
                partNumber,
                partData
        );

        log.debug("Part uploaded: fileId={}, partNumber={}", fileId, partNumber);
        return partETag;
    }

    /**
     * Lists all uploaded parts for a multipart upload.
     */
    public List<PartSummary> listUploadedParts(Long fileId, String uploadId) {
        FileVersion version = findVersionByFileIdAndUploadId(fileId, uploadId);
        return storageService.listUploadedParts(version.getStoragePath(), uploadId);
    }

    /**
     * Completes a multipart upload.
     */
    @Transactional
    public void completeUpload(Long fileId, String uploadId, List<PartETag> partETags) {
        FileVersion version = findVersionByFileIdAndUploadId(fileId, uploadId);

        storageService.completeMultipartUpload(
                version.getStoragePath(),
                uploadId,
                partETags
        );

        markUploadAsComplete(version);

        log.info("Resumable upload completed: fileId={}", fileId);
    }

    /**
     * Aborts a multipart upload and cleans up resources.
     */
    @Transactional
    public void abortUpload(Long fileId, String uploadId, Long userId) {
        FileVersion version = findVersionByFileIdAndUploadId(fileId, uploadId);

        storageService.abortMultipartUpload(version.getStoragePath(), uploadId);
        cleanupAbortedUpload(fileId, userId);

        log.info("Resumable upload aborted: fileId={}", fileId);
    }

    // ============ Private Helper Methods ============

    private void validateFolderExists(Long folderId) {
        if (folderId == null || fileMapper.folderExists(folderId).isEmpty()) {
            throw new ResourceNotFoundException("Folder", folderId);
        }
    }

    private File createFileMetadata(String fileName, String mimeType,
                                    Long fileSize, Long ownerId, Long folderId) {
        File file = new File();
        file.setName(fileName);
        file.setMimeType(mimeType);
        file.setSize(fileSize);
        file.setOwnerId(ownerId);
        file.setFolderId(folderId);
        file.setCreatedAt(Instant.now());
        file.setUpdatedAt(Instant.now());
        return file;
    }

    private void saveFile(File file) {
        fileMapper.insertFile(file);
    }

    private String generateStorageKey(Long ownerId, Long folderId,
                                      Long fileId, String fileName) {
        return storageService.generateStorageKey(ownerId, folderId, fileId, fileName, 1);
    }

    private String initiateMultipartUpload(String s3Key) {
        InitiateMultipartUploadResult result = storageService.initiateMultipartUpload(s3Key);
        return result.getUploadId();
    }

    private void createPendingFileVersion(Long fileId, String storagePath,
                                          Long fileSize, Long ownerId, String uploadId) {
        FileVersion version = new FileVersion();
        version.setFileId(fileId);
        version.setVersionNumber(1);
        version.setStoragePath(storagePath);
        version.setSize(fileSize);
        version.setCreatedBy(ownerId);
        version.setUploadId(uploadId);
        version.setCreatedAt(Instant.now());

        fileVersionMapper.insertVersion(version);
    }

    private void grantFilePermission(Long fileId, Long ownerId, Long folderId) {
        permissionService.grantPermission(fileId, ownerId, folderId, PermissionType.ADMIN);
    }

    private Map<String, Object> createInitResponse(Long fileId, String uploadId) {
        Map<String, Object> response = new HashMap<>();
        response.put("fileId", fileId);
        response.put("uploadId", uploadId);
        return response;
    }

    private FileVersion findVersionByFileIdAndUploadId(Long fileId, String uploadId) {
        FileVersion version = fileVersionMapper.getLatestVersion(fileId);

        if (version == null || version.getUploadId() == null) {
            throw new ResourceNotFoundException("File version for file", fileId);
        }

        if (!version.getUploadId().equals(uploadId)) {
            throw new IllegalArgumentException("Invalid upload ID for file: " + fileId);
        }

        return version;
    }

    private void markUploadAsComplete(FileVersion version) {
        version.setUploadId(null); // Clear uploadId to indicate completion
        fileVersionMapper.updateUploadStatus(version);
    }

    private void cleanupAbortedUpload(Long fileId, Long userId) {
        File file = fileMapper.getFileById(fileId);
        if (file != null) {
            // Soft delete first
            file.setDeleted(true);
            file.setUpdatedAt(Instant.now());
            fileMapper.updateFileDeleteStatus(file);

            // Then permanently delete
            filePermissionMapper.deleteByFileId(fileId);
            fileVersionMapper.deleteByFileId(fileId);
            fileMapper.deleteFile(fileId);
        }
    }

}

