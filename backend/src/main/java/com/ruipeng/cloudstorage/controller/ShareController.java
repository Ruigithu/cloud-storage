package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.dto.response.ShareInfoResponse;
import com.ruipeng.cloudstorage.dto.response.ShareResponse;
import com.ruipeng.cloudstorage.service.ShareService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for file sharing operations.
 * Handles share creation, retrieval, and management.
 */
@RestController
public class ShareController {
    private static final Logger log = LoggerFactory.getLogger(ShareController.class);

    private final ShareService shareService;

    /**
     * Constructor with dependency injection.
     *
     * @param shareService the share service
     */
    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * Creates a share link for a file.
     *
     * @param fileId the file ID
     * @param userId the user ID
     * @param accessType the access type (read/write)
     * @param expiresAt the expiration date (optional)
     * @return the generated share link
     */
    @PostMapping("/createShareLink")
    public ResponseEntity<?> createShareLink(
            @RequestParam("fileId") Long fileId,
            @RequestParam("userId") Long userId,
            @RequestParam("accessType") String accessType,
            @RequestParam(value = "expiresAt", required = false) String expiresAt) {
        logCreateShareLink(fileId, userId, accessType);

        try {
            ShareResponse response = shareService.createShare(fileId, userId, accessType, expiresAt);
            return buildShareLinkResponse(response.getShareLink());
        } catch (RuntimeException e) {
            log.error("Failed to create share link: fileId={}", fileId, e);
            return buildErrorResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating share link: fileId={}", fileId, e);
            return buildInternalErrorResponse("Failed to create share link");
        }
    }

    /**
     * Gets information about a shared file.
     *
     * @param shareId the share ID
     * @param userId the user ID (optional)
     * @return share information
     */
    @GetMapping("/share/{shareId}")
    public ResponseEntity<?> getSharedFileInfo(
            @PathVariable UUID shareId,
            @RequestParam(value = "userId", required = false) Long userId) {
        logGetShareInfo(shareId, userId);

        if (userId == null) {
            return buildUnauthorizedResponse("Authentication required");
        }

        try {
            ShareInfoResponse response = shareService.getShareInfo(shareId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return handleShareRetrievalError(e);
        } catch (Exception e) {
            log.error("Unexpected error getting share info: shareId={}", shareId, e);
            return buildInternalErrorResponse("Failed to get share info");
        }
    }

    /**
     * Saves a shared file to user's storage.
     *
     * @param shareId the share ID
     * @param userId the user ID
     * @param rootFolderId the root folder ID
     * @return the new file ID
     */
    @PostMapping("/saveShare/{shareId}")
    public ResponseEntity<?> saveSharedFile(
            @PathVariable UUID shareId,
            @RequestParam Long userId,
            @RequestParam Long rootFolderId) {
        logSaveSharedFile(shareId, userId);

        String operationKey = buildOperationKey(shareId, userId);

        if (isAlreadyProcessed(operationKey)) {
            return buildAlreadyProcessedResponse(operationKey);
        }

        markAsProcessing(operationKey);

        try {
            Long newFileId = shareService.saveSharedFile(shareId, userId, rootFolderId);
            cacheResult(operationKey, newFileId);
            return buildSaveSuccessResponse(newFileId);
        } catch (Exception e) {
            log.error("Failed to save shared file: shareId={}", shareId, e);
            clearProcessingMark(operationKey);
            return buildInternalErrorResponse("Failed to save shared file");
        }
    }

    /**
     * Gets all shares created by a user.
     *
     * @param ownerId the owner user ID
     * @return list of share information
     */
    @GetMapping("/getAllSharedFiles")
    public ResponseEntity<?> getAllSharedFiles(@RequestParam("ownerId") Long ownerId) {
        logGetAllShares(ownerId);

        try {
            List<ShareInfoResponse> shares = shareService.getAllUserShares(ownerId);

            if (shares.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            return ResponseEntity.ok(shares);
        } catch (Exception e) {
            log.error("Error fetching shared files: ownerId={}", ownerId, e);
            return buildErrorResponse("Error fetching shared files: " + e.getMessage());
        }
    }

    /**
     * Cancels (deactivates) a share.
     *
     * @param shareId the share ID
     * @param ownerId the owner user ID
     * @return response indicating success or failure
     */
    @PostMapping("/cancelShare")
    public ResponseEntity<?> cancelShare(
            @RequestParam("shareId") UUID shareId,
            @RequestParam("ownerId") Long ownerId) {
        logCancelShare(shareId, ownerId);

        try {
            int row = shareService.cancelShare(shareId, ownerId);

            if (row > 0) {
                return ResponseEntity.ok().build();
            }
            return buildUnauthorizedResponse();
        } catch (RuntimeException e) {
            log.error("Failed to cancel share: shareId={}", shareId, e);
            return buildErrorResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error canceling share: shareId={}", shareId, e);
            return buildInternalErrorResponse("Failed to cancel share link");
        }
    }

    /**
     * Restores a cancelled share.
     *
     * @param shareId the share ID
     * @param ownerId the owner user ID
     * @return response indicating success or failure
     */
    @PostMapping("/restore")
    public ResponseEntity<?> restore(
            @RequestParam("shareId") UUID shareId,
            @RequestParam("ownerId") Long ownerId) {
        logRestoreShare(shareId, ownerId);

        try {
            int row = shareService.restore(shareId, ownerId);

            if (row > 0) {
                return ResponseEntity.ok().build();
            }
            return buildUnauthorizedResponse();
        } catch (RuntimeException e) {
            log.error("Failed to restore share: shareId={}", shareId, e);
            return buildErrorResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error restoring share: shareId={}", shareId, e);
            return buildInternalErrorResponse("Failed to restore share link");
        }
    }

    // ============ Private Helper Methods ============

    /**
     * Builds share link response.
     */
    private ResponseEntity<?> buildShareLinkResponse(String shareLink) {
        return ResponseEntity.ok(Map.of("shareLink", shareLink));
    }

    /**
     * Builds error response with bad request status.
     */
    private ResponseEntity<?> buildErrorResponse(String errorMessage) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", errorMessage));
    }

    /**
     * Builds internal server error response.
     */
    private ResponseEntity<?> buildInternalErrorResponse(String errorMessage) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", errorMessage));
    }

    /**
     * Builds unauthorized response.
     */
    private ResponseEntity<?> buildUnauthorizedResponse(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(message);
    }

    /**
     * Builds unauthorized response without message.
     */
    private ResponseEntity<?> buildUnauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Handles share retrieval errors.
     */
    private ResponseEntity<?> handleShareRetrievalError(RuntimeException e) {
        String errorMessage = e.getMessage();

        if (isNotFoundError(errorMessage)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return buildErrorResponse(errorMessage);
    }

    /**
     * Checks if error is a not found error.
     */
    private boolean isNotFoundError(String errorMessage) {
        return "Share not found".equals(errorMessage) || "File not found".equals(errorMessage);
    }

    /**
     * Builds operation key for idempotency check.
     */
    private String buildOperationKey(UUID shareId, Long userId) {
        return shareId.toString() + "_" + userId;
    }

    /**
     * Checks if operation already processed (placeholder).
     */
    private boolean isAlreadyProcessed(String operationKey) {
        // Implementation would check cache
        return false;
    }

    /**
     * Marks operation as processing (placeholder).
     */
    private void markAsProcessing(String operationKey) {
        // Implementation would update cache
    }

    /**
     * Caches operation result (placeholder).
     */
    private void cacheResult(String operationKey, Long fileId) {
        // Implementation would cache result
    }

    /**
     * Clears processing mark (placeholder).
     */
    private void clearProcessingMark(String operationKey) {
        // Implementation would clear cache
    }

    /**
     * Builds already processed response.
     */
    private ResponseEntity<?> buildAlreadyProcessedResponse(String operationKey) {
        // Get cached file ID
        Long existingFileId = 0L; // Placeholder
        return ResponseEntity.ok(Map.of(
                "newFileId", existingFileId,
                "note", "Already processed"
        ));
    }

    /**
     * Builds save success response.
     */
    private ResponseEntity<?> buildSaveSuccessResponse(Long newFileId) {
        return ResponseEntity.ok(Map.of("newFileId", newFileId));
    }

    // ============ Logging Methods ============

    private void logCreateShareLink(Long fileId, Long userId, String accessType) {
        log.info("Creating share link: fileId={}, userId={}, accessType={}",
                fileId, userId, accessType);
    }

    private void logGetShareInfo(UUID shareId, Long userId) {
        log.info("Getting share info: shareId={}, userId={}", shareId, userId);
    }

    private void logSaveSharedFile(UUID shareId, Long userId) {
        log.info("Saving shared file: shareId={}, userId={}", shareId, userId);
    }

    private void logGetAllShares(Long ownerId) {
        log.info("Getting all shares for user: {}", ownerId);
    }

    private void logCancelShare(UUID shareId, Long ownerId) {
        log.info("Canceling share: shareId={}, ownerId={}", shareId, ownerId);
    }

    private void logRestoreShare(UUID shareId, Long ownerId) {
        log.info("Restoring share: shareId={}, ownerId={}", shareId, ownerId);
    }
}