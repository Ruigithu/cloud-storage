package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import jakarta.transaction.Transactional;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class ShareService {
    private final ShareMapper shareMapper;
    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FileService fileService;
    private final FolderMapper folderMapper;
    private final FolderService folderService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Autowired
    public ShareService(ShareMapper shareMapper, FileMapper fileMapper, FileVersionMapper fileVersionMapper, FileService fileService, FolderMapper folderMapper, FolderService folderService) {
        this.shareMapper = shareMapper;
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.fileService = fileService;
        this.folderMapper = folderMapper;
        this.folderService = folderService;
    }

    public ShareResponse createShare(Long fileId, Long createdBy, String accessType, String expiresAt) {
        // Verify that the file exists and the user has permission
        System.out.println("Step 1");
        File file = fileMapper.findByIdAndUserIdWithAdminPermission(fileId, createdBy);
        System.out.println("Step 2");
        if (file == null) {
            throw new RuntimeException("File not found or no permission");
        }

        // Create a share record
        System.out.println("Step 3");
        Share share = new Share();
        System.out.println("share:" + share.getId() + "," + share.getCreatedAt() + "," + share.isActive());
        share.setFileId(fileId);
        System.out.println("FileId set");
        share.setCreatedBy(createdBy);
        System.out.println("CreatedBy set");
        if (Objects.equals(accessType, "read")) {
            share.setAccessType(PermissionType.READ);
        } else {
            share.setAccessType(PermissionType.WRITE);
        }
        System.out.println("AccessType set");
        System.out.println(expiresAt + " value");

        if (expiresAt != null) {
            share.setExpiresAt(OffsetDateTime.parse(expiresAt));
        }

        System.out.println("share:" + share.getFileId() + " " + share.getCreatedBy() + " " + share.getAccessType() + " " + share.getExpiresAt());
        System.out.println("shareId:" + share.getId().getClass());
        try {
            shareMapper.insert(share);
        } catch (Exception e) {
            e.printStackTrace(); // Print specific error information
        }
        System.out.println("Step 4");

        // Generate a share link
        String shareLink = String.format("%s/share/%s", frontendUrl, share.getId());
        System.out.println(shareLink + " Step 5");

        ShareResponse response = new ShareResponse();
        response.setShareLink(shareLink);
        return response;
    }

    public ShareInfoResponse getShareInfo(UUID shareId, Long userId) {
        System.out.println("Step 1");
        Share share = null;
        try {
            share = shareMapper.findById(shareId);
        } catch (Exception e) {
            System.out.println("Database query exception:");
            e.printStackTrace();  // Print full stack trace
            throw new RuntimeException("Failed to find share");
        }

        System.out.println("Step 2");
        if (share == null) {
            throw new RuntimeException("Share not found");
        }

        System.out.println("Step 3");
        File file = fileMapper.getFileById(share.getFileId());
        if (file == null) {
            throw new RuntimeException("File not found");
        }

        System.out.println("Step 4");
        ShareInfoResponse response = new ShareInfoResponse();

        // Ensure logic matches frontend expectations
        // The frontend determines whether to download or save the shared file based on the "type" value
        response.setType(share.getAccessType()); // Use the access type set at sharing
        response.setFileName(file.getName());
        response.setFilePath(fileVersionMapper.getVersionByFileId(file.getId()).getStoragePath());
        response.setFileId(file.getId());

        System.out.println("Step 5");
        return response;
    }

    public Long saveSharedFile(UUID shareId, Long userId, Long rootFolderId) {
        // 1. Validate share and permissions
        Share share = shareMapper.findById(shareId);
        System.out.println("Step 1");
        if (share == null || !PermissionType.WRITE.equals(share.getAccessType())) {
            throw new RuntimeException("Invalid share");
        }
        System.out.println("Step 2");

        // 2. Retrieve original file information and latest version
        File originalFile = fileMapper.getFileById(share.getFileId());
        System.out.println("Step 3");
        System.out.println(originalFile.getName());
        if (originalFile == null) {
            throw new RuntimeException("Original file not found");
        }

        FileVersion latestVersion = fileVersionMapper.getLatestVersion(originalFile.getId());
        System.out.println("Step 4");
        if (latestVersion == null) {
            throw new RuntimeException("File version not found");
        }
        System.out.println("Step 5");
        try {
            // 3. Read the original file and create a MultipartFile
            Path originalFilePath = Paths.get(latestVersion.getStoragePath());
            String contentType = Files.probeContentType(originalFilePath);

            MultipartFile multipartFile = new CustomMultipartFile(
                    originalFile.getName(),    // File name
                    originalFile.getName(),    // originalFilename
                    contentType,               // Content type
                    Files.readAllBytes(originalFilePath)  // File content
            );

            // 5. Save the file using the existing uploadFile method
            File newFile = fileService.uploadFile(multipartFile, userId, rootFolderId);
            System.out.println("Step 7");
            return newFile.getId();
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy shared file: " + e.getMessage());
        }
    }

    public int cancelShare(UUID shareId, Long userId) {
        return shareMapper.cancelShare(shareId, userId);
    }

    public int restore(UUID shareId, Long userId) {
        return shareMapper.restore(shareId, userId);
    }
}
