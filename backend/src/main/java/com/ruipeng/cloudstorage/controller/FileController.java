package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.dto.DownloadFileInfo;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.service.FileS3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for file operations.
 * Handles file CRUD, upload, download, and deletion.
 *
 * All business logic is delegated to FileService.
 */
@RestController
@Tag(name = "File Management", description = "Operating file interface")
public class FileController {
    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileS3Service fileService;

    /**
     * Constructor with dependency injection.
     *
     * @param fileService the file service
     */
    public FileController(FileS3Service fileService) {
        this.fileService = fileService;
    }

    /**
     * Gets all files in the root folder for a user.
     *
     * @param ownerId the owner user ID
     * @return list of files in root folder
     */
    @GetMapping("/getRootFiles")
    @Operation(summary = "get files in the root folder",
            description = "get root folderId, search the files in root folder and return the list of these files")
    @Parameter(name = "ownerId",description = "specified ownerId", required = true,example = "1")
    public List<File> getRootFiles(@RequestParam Long ownerId) {
        logGetRootFiles(ownerId);
        return fileService.getRootFiles(ownerId);
    }

    /**
     * Gets all files in a specific folder.
     *
     * @param folderId the folder ID
     * @param ownerId the owner user ID
     * @return list of files in the folder
     */
    @GetMapping("/getAllFiles")
    @Operation(summary = "Get files from specified folder", description = "standardize the folderId, search the files in the folder and return the list of these files")
    public ResponseEntity<List<File>> getAllFiles(
            @RequestParam long folderId,
            @RequestParam long ownerId) {
        logGetAllFiles(folderId, ownerId);

        Long actualFolderId = normalizeFolderId(folderId);
        List<File> files = fileService.getFilesByFolder(ownerId, actualFolderId);

        return ResponseEntity.ok().body(files);
    }

    /**
     * Gets file details by file ID and owner ID.
     *
     * @param ownerId the owner user ID
     * @param fileId the file ID
     * @return file details or file content
     */
    @GetMapping("/getFileByUserIdAndFileId")
    public ResponseEntity<?> getFileByUserIdAndFileId(
            @RequestParam long ownerId,
            @RequestParam long fileId) {
        logGetFileDetails(fileId, ownerId);

        try {
            Map<String, Object> response = fileService.getFileDetailsAsMap(fileId, ownerId);

            if (isWordDocument(response)) {
                return buildJsonResponse(response);
            }

            if (isTextFile(response)) {
                return buildJsonResponse(response);
            }

            return buildBinaryResponse(response);

        } catch (Exception e) {
            log.error("Error getting file details: fileId={}, ownerId={}", fileId, ownerId, e);
            return buildErrorResponse(e.getMessage());
        }
    }

    /**
     * Uploads a new file.
     *
     * @param file the file to upload
     * @param ownerId the owner user ID
     * @param folderId the target folder ID
     * @return the uploaded file details
     */
    @PostMapping("/upload")
    public ResponseEntity<File> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") long ownerId,
            @RequestParam("folderId") long folderId) throws IOException {
        logUploadFile(file.getOriginalFilename(), folderId);

        Long actualFolderId = normalizeFolderId(folderId);
        File uploadedFile = fileService.uploadFile(file, ownerId, actualFolderId);

        return ResponseEntity.ok().body(uploadedFile);
    }

    /**
     * Uploads a new version of an existing file.
     *
     * @param file the new file version
     * @param ownerId the owner user ID
     * @param fileId the file ID
     * @return response with upload result
     */
    @PostMapping("/uploadNewFile")
    public ResponseEntity<?> uploadNewFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerId") long ownerId,
            @RequestParam("fileId") long fileId) {
        logUploadNewVersion(fileId);

        try {
            File updatedFile = fileService.uploadNewVersion(file, ownerId, fileId);
            Map<String, Object> response = buildUploadSuccessResponse(updatedFile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Unexpected error uploading new version: fileId={}", fileId, e);
            return buildUploadErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Downloads a file.
     *
     * @param fileId the file ID
     * @return the file content as resource
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("fileId") Long fileId) {
        logDownloadFile(fileId);

        try {
            DownloadFileInfo downloadInfo = fileService.downloadFile(fileId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(downloadInfo.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            createContentDisposition(downloadInfo.getFileName()))
                    .body(downloadInfo.getResource());
        } catch (Exception e) {
            log.error("Failed to download file: fileId={}", fileId, e);
            throw new RuntimeException("fail downloading file " + e.getMessage());
        }
    }

    /**
     * Soft deletes a file (moves to trash).
     *
     * @param fileId the file ID
     * @param userId the user ID
     * @return response indicating success or failure
     */
    @DeleteMapping("/softDeleteFile")
    public ResponseEntity<?> softDeleteFile(
            @RequestParam long fileId,
            @RequestParam long userId) throws IOException {
        logSoftDeleteFile(fileId);

        int result = fileService.softDeleteFile(fileId, userId);

        if (result > 0) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Permanently deletes a file.
     *
     * @param fileId the file ID
     * @param userId the user ID
     * @return response indicating success or failure
     */
    @DeleteMapping("/deleteFile")
    public ResponseEntity<?> deleteFile(
            @RequestParam long fileId,
            @RequestParam long userId) throws IOException {
        logDeleteFile(fileId);

        int result = fileService.deleteFile(fileId, userId);

        if (result > 0) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    /**
     * Gets all deleted files for a user.
     *
     * @param ownerId the owner user ID
     * @param folderId the folder ID
     * @return list of deleted files
     */
    @GetMapping("/getAllDeletedFiles")
    public ResponseEntity<List<File>> getAllDeletedFiles(
            @RequestParam long ownerId,
            @RequestParam long folderId) {
        logGetDeletedFiles(ownerId);

        Long actualFolderId = normalizeFolderId(folderId);
        List<File> files = fileService.getAllDeletedFiles(ownerId, actualFolderId);

        return ResponseEntity.ok().body(files);
    }

    /**
     * Restores a deleted file.
     *
     * @param fileId the file ID
     * @param ownerId the owner user ID
     * @return response indicating success or failure
     */
    @PostMapping("/restoreFile")
    public ResponseEntity<?> restoreFile(
            @RequestParam long fileId,
            @RequestParam long ownerId) {
        logRestoreFile(fileId);

        int result = fileService.restoreFile(fileId, ownerId);

        if (result > 0) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    // ============ Private Helper Methods ============

    /**
     * Normalizes folder ID, converting -1 to 0 for root folder.
     */
    private Long normalizeFolderId(long folderId) {
        return folderId == -1 ? 0 : folderId;
    }

    /**
     * Checks if response contains a Word document.
     */
    private boolean isWordDocument(Map<String, Object> response) {
        Boolean isWord = (Boolean) response.get("isWordDocument");
        return isWord != null && isWord;
    }

    /**
     * Checks if response contains text content.
     */
    private boolean isTextFile(Map<String, Object> response) {
        return response.containsKey("content") && response.get("content") != null;
    }

    /**
     * Builds JSON response for metadata.
     */
    private ResponseEntity<?> buildJsonResponse(Map<String, Object> response) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    /**
     * Builds binary response for file download.
     */
    private ResponseEntity<?> buildBinaryResponse(Map<String, Object> response) {
        String mimeType = (String) response.get("mimeType");
        String fileName = (String) response.get("fileName");
        byte[] content = (byte[]) response.get("binaryContent");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(content);
    }

    /**
     * Builds error response.
     */
    private ResponseEntity<?> buildErrorResponse(String errorMessage) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected error: " + errorMessage);
    }

    /**
     * Builds upload success response.
     */
    private Map<String, Object> buildUploadSuccessResponse(File file) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "File uploaded successfully");
        response.put("fileId", file.getId());
        response.put("fileName", file.getName());
        response.put("mimeType", file.getMimeType());
        response.put("size", file.getSize());
        return response;
    }

    /**
     * Builds upload error response.
     */
    private ResponseEntity<?> buildUploadErrorResponse(String errorMessage) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", errorMessage));
    }

    /**
     * Creates Content-Disposition header value.
     */
    private String createContentDisposition(String fileName) {
        return "attachment; filename=\"" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8) + "\"";
    }

    // ============ Logging Methods ============

    private void logGetRootFiles(Long ownerId) {
        log.info("Getting root files for owner: {}", ownerId);
    }

    private void logGetAllFiles(long folderId, long ownerId) {
        log.info("Getting files for folder: {} owner: {}", folderId, ownerId);
    }

    private void logGetFileDetails(long fileId, long ownerId) {
        log.info("Getting file details: fileId={}, ownerId={}", fileId, ownerId);
    }

    private void logUploadFile(String filename, long folderId) {
        log.info("Uploading file: {} to folder: {}", filename, folderId);
    }

    private void logUploadNewVersion(long fileId) {
        log.info("Uploading new version for file: {}", fileId);
    }

    private void logDownloadFile(Long fileId) {
        log.info("Downloading file: {}", fileId);
    }

    private void logSoftDeleteFile(long fileId) {
        log.info("Soft deleting file: {}", fileId);
    }

    private void logDeleteFile(long fileId) {
        log.info("Permanently deleting file: {}", fileId);
    }

    private void logGetDeletedFiles(long ownerId) {
        log.info("Getting deleted files for owner: {}", ownerId);
    }

    private void logRestoreFile(long fileId) {
        log.info("Restoring file: {}", fileId);
    }
}