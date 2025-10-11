package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.dto.response.ShareInfoResponse;
import com.ruipeng.cloudstorage.dto.response.ShareResponse;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.exception.ShareLinkException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing file sharing operations.
 *
 * Responsibilities:
 * - Creating and managing share links
 * - Validating share permissions
 * - Copying shared files to user's storage
 */
@Service
public class ShareService {
    private static final Logger log = LoggerFactory.getLogger(ShareService.class);

    private final ShareMapper shareMapper;
    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final S3StorageService storageService;
    private final FileS3Service fileS3Service;
    private final FolderHelperService folderHelperService;


    @Value("${frontend.url}")
    private String frontendUrl;

    public ShareService(ShareMapper shareMapper,
                        FileMapper fileMapper,
                        FileVersionMapper fileVersionMapper,
                        S3StorageService storageService,
                        FileS3Service fileS3Service,
                        @Value("${frontend.url}") String frontendUrl, FolderHelperService folderHelperService) {
        this.shareMapper = shareMapper;
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.storageService = storageService;
        this.fileS3Service = fileS3Service;
        this.frontendUrl = frontendUrl;
        this.folderHelperService = folderHelperService;
    }

    /**
     * Creates a new share link for a file.
     */
    @Transactional
    public ShareResponse createShare(Long fileId, Long createdBy,
                                     String accessType, String expiresAt) {
        validateUserHasPermission(fileId, createdBy);

        Share share = buildShare(fileId, createdBy, accessType, expiresAt);
        saveShare(share);

        String shareLink = generateShareLink(share.getId());

        log.info("Share created: id={}, fileId={}, type={}",
                share.getId(), fileId, accessType);

        return createShareResponse(shareLink);
    }

    /**
     * Retrieves information about a shared file.
     */
    public ShareInfoResponse getShareInfo(UUID shareId, Long userId) {
        Share share = findShareById(shareId);
        validateShareIsActive(share);

        File file = findFileById(share.getFileId());
        FileVersion version = getLatestVersion(file.getId());

        return buildShareInfoResponse(share, file, version);
    }

    /**
     * Saves a shared file to the user's storage.
     */
    @Transactional
    public Long saveSharedFile(UUID shareId, Long userId, Long rootFolderId) {
        Share share = findShareById(shareId);
        validateShareHasWriteAccess(share);

        File originalFile = findFileById(share.getFileId());
        FileVersion latestVersion = getLatestVersion(originalFile.getId());

        File newFile = copyFileToUserStorage(originalFile, latestVersion, userId, rootFolderId);

        log.info("Shared file saved: shareId={}, newFileId={}, userId={}",
                shareId, newFile.getId(), userId);

        return newFile.getId();
    }
    /**
            * Cancels a share and returns affected rows.
     */
    public int cancelShare(UUID shareId, Long userId) {
        int updated = shareMapper.cancelShare(shareId, userId);
        if (updated == 0) {
            throw new ShareLinkException("Failed to cancel share or no permission");
        }
        log.info("Share cancelled: id={}", shareId);
        return updated;
    }

    /**
     * Restores a cancelled share and returns affected rows.
     */
    public int restore(UUID shareId, Long userId) {
        int updated = shareMapper.restore(shareId, userId);
        if (updated == 0) {
            throw new ShareLinkException("Failed to restore share or no permission");
        }
        log.info("Share restored: id={}", shareId);
        return updated;
    }

    /**
     * Gets all shares created by a user.
     */
    public List<ShareInfoResponse> getAllUserShares(Long userId) {
        List<Share> shares = shareMapper.findByUserId(userId);

        List<ShareInfoResponse> responses = new ArrayList<>();
        for (Share share : shares) {
            try {
                ShareInfoResponse response = buildShareResponse(share, userId);
                responses.add(response);
            } catch (Exception e) {
                log.warn("Failed to build share response for shareId={}", share.getId(), e);
            }
        }

        return responses;
    }


    // ============ Private Helper Methods ============

    private void validateUserHasPermission(Long fileId, Long userId) {
        File file = fileMapper.findByIdAndUserIdWithAdminPermission(fileId, userId);
        if (file == null) {
            throw new ShareLinkException("File not found or no permission");
        }
    }

    private Share buildShare(Long fileId, Long createdBy,
                             String accessType, String expiresAt) {
        Share share = new Share();
        share.setFileId(fileId);
        share.setCreatedBy(createdBy);
        share.setAccessType(parseAccessType(accessType));

        if (expiresAt != null && !expiresAt.isEmpty()) {
            share.setExpiresAt(OffsetDateTime.parse(expiresAt));
        }

        return share;
    }

    private PermissionType parseAccessType(String accessType) {
        return "read".equalsIgnoreCase(accessType)
                ? PermissionType.READ
                : PermissionType.WRITE;
    }

    private void saveShare(Share share) {
        shareMapper.insert(share);
    }

    private String generateShareLink(UUID shareId) {
        return String.format("%s/share/%s", frontendUrl, shareId);
    }

    private ShareResponse createShareResponse(String shareLink) {
        ShareResponse response = new ShareResponse();
        response.setShareLink(shareLink);
        return response;
    }

    private Share findShareById(UUID shareId) {
        Share share = shareMapper.findById(shareId);
        if (share == null) {
            throw new ResourceNotFoundException("Share not found");
        }
        return share;
    }

    private void validateShareIsActive(Share share) {
        if (!share.isActive()) {
            throw new ShareLinkException("Share link is not active");
        }

        if (share.getExpiresAt() != null &&
                share.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ShareLinkException("Share link has expired");
        }
    }

    private void validateShareHasWriteAccess(Share share) {
        if (!PermissionType.WRITE.equals(share.getAccessType())) {
            throw new ShareLinkException("Share does not allow saving files");
        }
    }

    private File findFileById(Long fileId) {
        File file = fileMapper.getFileById(fileId);
        if (file == null) {
            throw new ResourceNotFoundException("File", fileId);
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

    private ShareInfoResponse buildShareInfoResponse(Share share, File file,
                                                     FileVersion version) {
        ShareInfoResponse response = new ShareInfoResponse();
        response.setId(share.getId());
        response.setFileId(file.getId());
        response.setFileName(file.getName());
        response.setFilePath(version.getStoragePath());
        response.setType(share.getAccessType());
        response.setExpiresAt(share.getExpiresAt());
        response.setActive(share.isActive());
        return response;
    }

    private ShareInfoResponse buildShareResponse(Share share, Long userId) {
        FileVersion latestVersion = getLatestVersion(share.getFileId());
        File file = fileMapper.getFileByUserIdAndFileId(userId, latestVersion.getFileId());

        if (file == null) {
            log.warn("File not found for share: shareId={}, fileId={}",
                    share.getId(), share.getFileId());
            return null;
        }

        ShareInfoResponse response = new ShareInfoResponse();
        response.setId(share.getId());
        response.setFileId(latestVersion.getFileId());
        response.setFilePath(latestVersion.getStoragePath());
        response.setFileName(file.getName());
        response.setShareLink(generateShareLink(share.getId()));
        response.setExpiresAt(share.getExpiresAt());
        response.setType(share.getAccessType());
        response.setActive(share.isActive());

        return response;
    }

    private File copyFileToUserStorage(File originalFile, FileVersion originalVersion,
                                       Long userId, Long folderId) {
        // This is a placeholder - actual implementation would need to:
        // 1. Download the original file from S3
        // 2. Upload it as a new file for the user
        // 3. Create appropriate metadata and permissions
            try {
                byte[] fileContent = storageService.downloadFile(originalVersion.getStoragePath());

                log.debug("Downloaded shared file: {} ({} bytes)",
                        originalFile.getName(), fileContent.length);

                MultipartFile multipartFile = new MockMultipartFile(
                        "file",
                        originalFile.getName(),
                        originalFile.getMimeType(),
                        fileContent
                );

                File newFile = fileS3Service.uploadFile(multipartFile, userId, folderHelperService.getRootFolderId(userId));

                log.info("File copied successfully using FileS3Service: originalFileId={}, newFileId={}, userId={}",
                        originalFile.getId(), newFile.getId(), userId);

                return newFile;

            } catch (Exception e) {
                log.error("Failed to copy shared file: originalFileId={}, userId={}",
                        originalFile.getId(), userId, e);
                throw new RuntimeException("Failed to save shared file to your storage: " + e.getMessage(), e);
            }
        }
    }
