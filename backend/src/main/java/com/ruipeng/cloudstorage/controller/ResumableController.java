package com.ruipeng.cloudstorage.controller;

import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.ruipeng.cloudstorage.dto.request.CompleteUploadRequest;
import com.ruipeng.cloudstorage.service.ResumableUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for resumable file uploads.
 * Handles multipart upload operations.
 */
@RestController
@RequestMapping("/resumable")
public class ResumableController {
    private static final Logger log = LoggerFactory.getLogger(ResumableController.class);

    private final ResumableUploadService uploadService;

    /**
     * Constructor with dependency injection.
     *
     * @param uploadService the resumable upload service
     */
    public ResumableController(ResumableUploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * Initiates a resumable upload session.
     *
     * @param ownerId the owner user ID
     * @param folderId the target folder ID
     * @param fileName the file name
     * @param mimeType the file MIME type
     * @param fileSize the file size
     * @return upload initialization information
     */
    @PostMapping("/init")
    public ResponseEntity<?> initiateUpload(
            @RequestParam("ownerId") Long ownerId,
            @RequestParam("folderId") Long folderId,
            @RequestParam("fileName") String fileName,
            @RequestParam("mimeType") String mimeType,
            @RequestParam("fileSize") Long fileSize) {
        logInitiateUpload(fileName, fileSize);

        try {
            Map<String, Object> result = uploadService.initiateUpload(
                    ownerId, folderId, fileName, mimeType, fileSize
            );

            logInitiateSuccess(result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error initiating upload for file: {}", fileName, e);
            return buildErrorResponse("Error initiating upload: " + e.getMessage());
        }
    }

    /**
     * Uploads a single part of a multipart upload.
     *
     * @param fileId the file ID
     * @param uploadId the upload ID
     * @param partNumber the part number
     * @param part the file part
     * @return the part ETag
     */
    @PostMapping("/part")
    public ResponseEntity<PartETag> uploadPart(
            @RequestParam("fileId") Long fileId,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber,
            @RequestPart("part") MultipartFile part) throws Exception {
        logUploadPart(fileId, partNumber);

        byte[] partData = part.getBytes();
        PartETag partETag = uploadService.uploadPart(fileId, uploadId, partNumber, partData);

        return ResponseEntity.ok(partETag);
    }

    /**
     * Lists all uploaded parts for a multipart upload.
     *
     * @param fileId the file ID
     * @param uploadId the upload ID
     * @return list of uploaded parts
     */
    @GetMapping("/parts")
    public ResponseEntity<List<PartSummary>> listParts(
            @RequestParam("fileId") Long fileId,
            @RequestParam("uploadId") String uploadId) {
        logListParts(fileId);

        List<PartSummary> parts = uploadService.listUploadedParts(fileId, uploadId);
        return ResponseEntity.ok(parts);
    }

    /**
     * Completes a multipart upload.
     *
     * @param request the completion request with part information
     * @return success message
     */
    @PostMapping("/complete")
    public ResponseEntity<String> completeUpload(@RequestBody CompleteUploadRequest request) {
        logCompleteUpload(request.getFileId());

        try {
            List<PartETag> awsPartETags = convertToAwsPartETags(request);
            uploadService.completeUpload(request.getFileId(), request.getUploadId(), awsPartETags);

            return ResponseEntity.ok("Upload completed successfully");
        } catch (Exception e) {
            log.error("Error completing upload: fileId={}", request.getFileId(), e);
            return ResponseEntity.status(500)
                    .body("Error completing upload: " + e.getMessage());
        }
    }

    /**
     * Aborts a multipart upload.
     *
     * @param fileId the file ID
     * @param uploadId the upload ID
     * @param folderId the folder ID (unused but kept for compatibility)
     * @param userId the user ID
     * @return success message
     */
    @PostMapping("/abort")
    public ResponseEntity<String> abortUpload(
            @RequestParam("fileId") Long fileId,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("folderId") Long folderId,
            @RequestParam("userId") Long userId) {
        logAbortUpload(fileId);

        try {
            uploadService.abortUpload(fileId, uploadId, userId);
            return ResponseEntity.ok("Upload aborted successfully");
        } catch (Exception e) {
            log.error("Error aborting upload: fileId={}", fileId, e);
            return ResponseEntity.status(500)
                    .body("Error aborting upload: " + e.getMessage());
        }
    }

    // ============ Private Helper Methods ============

    /**
     * Converts DTO PartETags to AWS PartETags.
     */
    private List<PartETag> convertToAwsPartETags(CompleteUploadRequest request) {
        return request.getPartETags().stream()
                .map(dto -> new PartETag(dto.getPartNumber(), dto.geteTag()))
                .collect(Collectors.toList());
    }

    /**
     * Builds error response.
     */
    private ResponseEntity<?> buildErrorResponse(String errorMessage) {
        return ResponseEntity.status(500).body(errorMessage);
    }

    // ============ Logging Methods ============

    private void logInitiateUpload(String fileName, Long fileSize) {
        log.info("Initiating upload: fileName={}, size={}", fileName, fileSize);
    }

    private void logInitiateSuccess(Map<String, Object> result) {
        log.info("Upload initiated successfully with result: {}", result);
    }

    private void logUploadPart(Long fileId, int partNumber) {
        log.debug("Uploading part: fileId={}, partNumber={}", fileId, partNumber);
    }

    private void logListParts(Long fileId) {
        log.debug("Listing parts: fileId={}", fileId);
    }

    private void logCompleteUpload(Long fileId) {
        log.info("Completing upload: fileId={}", fileId);
    }

    private void logAbortUpload(Long fileId) {
        log.info("Aborting upload: fileId={}", fileId);
    }
}