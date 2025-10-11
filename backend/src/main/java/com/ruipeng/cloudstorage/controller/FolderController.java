package com.ruipeng.cloudstorage.controller;

import com.amazonaws.services.s3.model.PartETag;
import com.ruipeng.cloudstorage.dto.DownloadFileInfo;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.Folder;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.service.FileS3Service;
import com.ruipeng.cloudstorage.service.FolderS3Service;
import org.apache.ibatis.javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.*;

/**
 * Controller for folder operations.
 * Handles folder CRUD, upload, download, and deletion.
 */
@RestController
public class FolderController {
    private static final Logger log = LoggerFactory.getLogger(FolderController.class);

    private final FolderS3Service folderService;
    private final FileS3Service fileService;
    private final FileMapper fileMapper;

    /**
     * Constructor with dependency injection.
     *
     * @param folderService the folder service
     * @param fileService the file service
     * @param fileMapper the file mapper
     */
    public FolderController(FolderS3Service folderService,
                            FileS3Service fileService,
                            FileMapper fileMapper) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.fileMapper = fileMapper;
    }

    /**
     * Gets root folders for a user.
     *
     * @param userId the user ID
     * @return root folder information
     */
    @GetMapping("/getRootFolders")
    public Map<String, Object> getRootFolders(@RequestParam Long userId) throws SQLException {
        logGetRootFolders(userId);

        Long rootFolderId = folderService.getRootFolderId(userId);
        List<Folder> folders = folderService.getFoldersByParent(userId, rootFolderId);

        return buildRootFolderResponse(rootFolderId, folders);
    }

    /**
     * Creates a new folder.
     *
     * @param name the folder name
     * @param parentId the parent folder ID
     * @param userId the user ID
     * @return the created folder
     */
    @PostMapping("/createFolder")
    public ResponseEntity<Folder> createFolder(
            @RequestParam("name") String name,
            @RequestParam("parentId") Long parentId,
            @RequestParam("userId") Long userId)
            throws SQLException, NotFoundException, AccessDeniedException {
        logCreateFolder(name, parentId);

        Folder folder = folderService.createFolder(name, parentId, userId);
        return ResponseEntity.ok(folder);
    }

    /**
     * Gets all folders under a parent folder.
     *
     * @param parentId the parent folder ID (optional)
     * @param userId the user ID
     * @return list of folders or root folder info
     */
    @GetMapping("/getAllFolders")
    public ResponseEntity<?> getAllFolders(
            @RequestParam(required = false) Long parentId,
            @RequestParam Long userId) {
        logGetAllFolders(parentId, userId);

        try {
            if (parentId == null) {
                return buildRootFoldersResponse(userId);
            }

            List<Folder> folders = folderService.getFoldersByParent(userId, parentId);
            return ResponseEntity.ok(folders);

        } catch (Exception e) {
            log.error("Error getting folders: parentId={}, userId={}", parentId, userId, e);
            return buildErrorResponse("Error getting folders: " + e.getMessage());
        }
    }

    /**
     * Soft deletes a folder.
     *
     * @param folderId the folder ID
     * @param ownerId the owner user ID
     * @return response indicating success or failure
     */
    @DeleteMapping("/softDeleteFolder")
    public ResponseEntity<?> softDeleteFolder(
            @RequestParam("folderId") long folderId,
            @RequestParam("userId") long ownerId) throws IOException {
        logSoftDeleteFolder(folderId);

        int result = folderService.softDeleteFolder(folderId, ownerId);

        if (result > 0) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Permanently deletes a folder.
     *
     * @param folderId the folder ID
     * @param ownerId the owner user ID
     * @return response indicating success or failure
     */
    @DeleteMapping("/deleteFolder")
    public ResponseEntity<?> deleteFolder(
            @RequestParam("folderId") long folderId,
            @RequestParam("userId") long ownerId) throws IOException {
        logDeleteFolder(folderId);

        int result = folderService.deleteFolder(folderId, ownerId);

        if (result > 0) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Gets all deleted folders for a user.
     *
     * @param parentId the parent folder ID
     * @param userId the user ID
     * @return list of deleted folders
     */
    @GetMapping("/getAllDeletedFolders")
    public ResponseEntity<List<Folder>> getAllDeletedFolders(
            @RequestParam long parentId,
            @RequestParam long userId) throws SQLException {
        logGetDeletedFolders(userId);

        List<Folder> folders = folderService.getAllDeletedFolders(userId, parentId);
        return ResponseEntity.ok().body(folders);
    }

    /**
     * Restores a deleted folder.
     *
     * @param folderId the folder ID
     * @param ownerId the owner user ID
     * @return response indicating success or failure
     */
    @PostMapping("/restoreFolder")
    public ResponseEntity<?> restoreFolder(
            @RequestParam long folderId,
            @RequestParam long ownerId) {
        logRestoreFolder(folderId);

        int result = folderService.restoreFolder(folderId, ownerId);

        if (result > 0) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Downloads a folder as a ZIP file.
     *
     * @param folderId the folder ID
     * @param userId the user ID
     * @return the folder content as ZIP
     */
    @GetMapping("/downloadFolder")
    public ResponseEntity<Resource> downloadFolder(
            @RequestParam("folderId") Long folderId,
            @RequestParam("userId") Long userId) {
        logDownloadFolder(folderId);

        try {
            DownloadFileInfo downloadInfo = folderService.downloadFolder(folderId, userId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            createZipContentDisposition(downloadInfo.getFileName()))
                    .body(downloadInfo.getResource());
        } catch (Exception e) {
            log.error("Failed to download folder: folderId={}", folderId, e);
            throw new RuntimeException("fail downloading folder " + e.getMessage());
        }
    }

    /**
     * Uploads a small folder (synchronous).
     *
     * @param files the files to upload
     * @param relativePaths the relative paths of files
     * @param parentFolderId the parent folder ID
     * @param userId the user ID
     * @return response indicating success or failure
     */
    @PostMapping("/uploadFolder")
    public ResponseEntity<?> uploadFolder(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("paths") String[] relativePaths,
            @RequestParam("folderId") long parentFolderId,
            @RequestParam("userId") long userId) throws IOException {
        logUploadFolder(files.length, parentFolderId);

        if (files == null || files.length == 0) {
            return buildBadRequestResponse("No files uploaded");
        }

        try {
            folderService.uploadFolderSmall(files, relativePaths, userId, parentFolderId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to upload folder", e);
            return buildErrorResponse("fail uploading folder " + e.getMessage());
        }
    }

    /**
     * Initiates a multipart folder upload.
     *
     * @param files the files metadata
     * @param relativePaths the relative paths
     * @param parentFolderId the parent folder ID
     * @param userId the user ID
     * @return upload initialization information
     */
    @PostMapping("/folders-initiate-upload")
    public ResponseEntity<?> initiateUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("relativePaths") String[] relativePaths,
            @RequestParam(value = "parentFolderId", required = false) long parentFolderId,
            @RequestParam("userId") long userId) {
        logInitiateFolderUpload(files.length);

        try {
            Map<String, Object> uploadInfo = folderService.initiateFolderUpload(
                    files, relativePaths, userId, parentFolderId);

            return ResponseEntity.ok(uploadInfo);
        } catch (Exception e) {
            log.error("Failed to initiate folder upload", e);
            return buildErrorResponse("Failed to initiate folder upload: " + e.getMessage());
        }
    }

    /**
     * Uploads a part of a file in multipart upload.
     *
     * @param fileId the file ID
     * @param uploadId the upload ID
     * @param partNumber the part number
     * @param file the file part
     * @return the part ETag
     */
    @PostMapping("/folders-upload-part")
    public ResponseEntity<?> uploadPart(
            @RequestParam("fileId") Long fileId,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber,
            @RequestParam("file") MultipartFile file) {
        logUploadPart(fileId, partNumber);

        try {
            PartETag partETag = fileService.uploadPart(fileId, uploadId, partNumber, file.getBytes());
            return ResponseEntity.ok(partETag);
        } catch (Exception e) {
            log.error("Failed to upload part: fileId={}, partNumber={}", fileId, partNumber, e);
            return buildErrorResponse("Failed to upload part: " + e.getMessage());
        }
    }

    /**
     * Gets the upload status of files in a folder.
     *
     * @param fileIds the list of file IDs
     * @return upload status information
     */
    @GetMapping("/folders-upload-status")
    public ResponseEntity<?> getUploadStatus(@RequestParam("fileIds") List<Long> fileIds) {
        logGetUploadStatus(fileIds.size());

        try {
            Map<String, Object> status = folderService.getFolderUploadStatus(fileIds);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get upload status", e);
            return buildErrorResponse("Failed to get upload status: " + e.getMessage());
        }
    }

    /**
     * Completes a multipart folder upload.
     *
     * @param fileCompletions the list of file completion information
     * @return response indicating success
     */
    @PostMapping("/folders-complete-upload")
    public ResponseEntity<?> completeUpload(
            @RequestBody List<Map<String, Object>> fileCompletions) {
        logCompleteUpload(fileCompletions.size());

        try {
            folderService.completeFolderUpload(fileCompletions);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to complete upload", e);
            return buildErrorResponse("Failed to complete upload: " + e.getMessage());
        }
    }

    /**
     * Aborts a multipart folder upload.
     *
     * @param fileIds the array of file IDs
     * @param uploadIds the array of upload IDs
     * @param parentFolderId the parent folder ID
     * @param userId the user ID
     * @return list of abort results
     */
    @PostMapping("/folders-abort-upload")
    public ResponseEntity<?> abortUpload(
            @RequestParam(value = "fileIds", required = false) Long[] fileIds,
            @RequestParam(value = "uploadIds", required = false) String[] uploadIds,
            @RequestParam(value = "parentFolderId", required = false) Long parentFolderId,
            @RequestParam(value = "userId", required = false) Long userId) {
        logAbortUpload(fileIds != null ? fileIds.length : 0);

        if (!validateAbortParams(fileIds, uploadIds)) {
            return ResponseEntity.badRequest().body("Invalid request parameters");
        }

        List<Map<String, String>> results = processAbortRequests(fileIds, uploadIds, userId);
        Long minFolderId = findMinimumFolderId(fileIds, parentFolderId, userId);

        cleanupFolderAfterAbort(minFolderId, userId);

        return ResponseEntity.ok(results);
    }

    // ============ Private Helper Methods ============

    /**
     * Builds root folder response map.
     */
    private Map<String, Object> buildRootFolderResponse(Long rootFolderId, List<Folder> folders) {
        Map<String, Object> response = new HashMap<>();
        response.put("rootFolderId", rootFolderId);
        response.put("folders", folders);
        return response;
    }

    /**
     * Builds root folders response entity.
     */
    private ResponseEntity<?> buildRootFoldersResponse(Long userId) throws SQLException {
        Long rootId = folderService.getRootFolderId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("rootFolderId", rootId);
        response.put("folders", folderService.getFoldersByParent(userId, rootId));
        return ResponseEntity.ok(response);
    }

    /**
     * Builds error response entity.
     */
    private ResponseEntity<?> buildErrorResponse(String errorMessage) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(errorMessage));
    }

    /**
     * Builds bad request response entity.
     */
    private ResponseEntity<?> buildBadRequestResponse(String errorMessage) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorMessage);
    }

    /**
     * Creates Content-Disposition header for ZIP file.
     */
    private String createZipContentDisposition(String fileName) {
        return "attachment; filename=\"" +
                URLEncoder.encode(fileName + ".zip", StandardCharsets.UTF_8) + "\"";
    }

    /**
     * Validates abort upload parameters.
     */
    private boolean validateAbortParams(Long[] fileIds, String[] uploadIds) {
        return fileIds != null && uploadIds != null && fileIds.length == uploadIds.length;
    }

    /**
     * Processes abort requests for multiple files.
     */
    private List<Map<String, String>> processAbortRequests(Long[] fileIds, String[] uploadIds, Long userId) {
        List<Map<String, String>> results = new ArrayList<>();

        for (int i = 0; i < fileIds.length; i++) {
            Map<String, String> result = abortSingleUpload(fileIds[i], uploadIds[i]);
            results.add(result);
        }

        return results;
    }

    /**
     * Aborts a single file upload.
     */
    private Map<String, String> abortSingleUpload(Long fileId, String uploadId) {
        try {
            if (fileId != null && uploadId != null) {
                folderService.abortFolderUpload(fileId, uploadId);
                return createSuccessResult(fileId);
            }
            return createFailureResult(fileId, "Missing fileId or uploadId");
        } catch (Exception e) {
            log.error("Failed to abort upload: fileId={}", fileId, e);
            return createFailureResult(fileId, e.getMessage());
        }
    }

    /**
     * Creates success result map.
     */
    private Map<String, String> createSuccessResult(Long fileId) {
        return Map.of("fileId", String.valueOf(fileId), "status", "success");
    }

    /**
     * Creates failure result map.
     */
    private Map<String, String> createFailureResult(Long fileId, String error) {
        return Map.of(
                "fileId", String.valueOf(fileId),
                "status", "failed",
                "error", error
        );
    }

    /**
     * Finds the minimum folder ID from file list.
     */
    private Long findMinimumFolderId(Long[] fileIds, Long parentFolderId, Long userId) {
        long minFolderId = Long.MAX_VALUE;

        for (Long fileId : fileIds) {
            File file = fileMapper.getFileByUserIdAndFileId(userId, fileId);
            if (file != null && file.getFolderId() != parentFolderId) {
                minFolderId = Math.min(file.getFolderId(), minFolderId);
            }
        }

        return minFolderId;
    }

    /**
     * Cleans up folder after aborting upload.
     */
    private void cleanupFolderAfterAbort(Long folderId, Long userId) {
        try {
            folderService.softDeleteFolder(folderId, userId);
            folderService.deleteFolder(folderId, userId);
        } catch (Exception e) {
            log.error("Failed to cleanup folder after abort: folderId={}", folderId, e);
            throw new RuntimeException("Folder cleanup failed: " + e.getMessage(), e);
        }
    }

    // ============ Logging Methods ============

    private void logGetRootFolders(Long userId) {
        log.info("Getting root folders for user: {}", userId);
    }

    private void logCreateFolder(String name, Long parentId) {
        log.info("Creating folder: name={}, parentId={}", name, parentId);
    }

    private void logGetAllFolders(Long parentId, Long userId) {
        log.info("Getting folders: parentId={}, userId={}", parentId, userId);
    }

    private void logSoftDeleteFolder(long folderId) {
        log.info("Soft deleting folder: {}", folderId);
    }

    private void logDeleteFolder(long folderId) {
        log.info("Permanently deleting folder: {}", folderId);
    }

    private void logGetDeletedFolders(long userId) {
        log.info("Getting deleted folders for user: {}", userId);
    }

    private void logRestoreFolder(long folderId) {
        log.info("Restoring folder: {}", folderId);
    }

    private void logDownloadFolder(Long folderId) {
        log.info("Downloading folder: {}", folderId);
    }

    private void logUploadFolder(int fileCount, long parentFolderId) {
        log.info("Uploading folder with {} files to parent: {}", fileCount, parentFolderId);
    }

    private void logInitiateFolderUpload(int fileCount) {
        log.info("Initiating folder upload with {} files", fileCount);
    }

    private void logUploadPart(Long fileId, int partNumber) {
        log.debug("Uploading part: fileId={}, partNumber={}", fileId, partNumber);
    }

    private void logGetUploadStatus(int fileCount) {
        log.info("Getting upload status for {} files", fileCount);
    }

    private void logCompleteUpload(int fileCount) {
        log.info("Completing upload for {} files", fileCount);
    }

    private void logAbortUpload(int fileCount) {
        log.info("Aborting upload for {} files", fileCount);
    }

    /**
     * Error response class.
     */
    private static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}