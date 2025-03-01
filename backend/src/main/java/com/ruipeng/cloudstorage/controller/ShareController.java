package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import com.ruipeng.cloudstorage.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
public class ShareController {
    private final FileMapper fileMapper;
    private final User user;
    private ShareService shareService;
    private ShareMapper shareMapper;
    private FileVersionMapper fileVersionMapper;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    public ShareController(ShareService shareService, ShareMapper shareMapper, FileMapper fileMapper, FileVersionMapper fileVersionMapper, User user) {
        this.shareService = shareService;
        this.shareMapper = shareMapper;
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.user = user;
    }
    @Value("${frontend.url}")
    private String frontendUrl;

    @PostMapping("/createShareLink")
    public ResponseEntity<?> createShareLink(
            @RequestParam("fileId") Long fileId,
            @RequestParam("userId") Long userId,
            @RequestParam("accessType") String accessType,
            @RequestParam(value = "expiresAt", required = false) String expiresAt
    ) {
        try {
            System.out.println("fileId:"+fileId+" userId: "+userId+" access: "+accessType+" expires: "+expiresAt);
            ShareResponse response = shareService.createShare(fileId, userId, accessType, expiresAt);
            return ResponseEntity.ok(Map.of("shareLink", response.getShareLink()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create share link"));
        }
    }

    @GetMapping("/share/{shareId}")
    public ResponseEntity<?> getSharedFileInfo(
            @PathVariable UUID shareId,
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        System.out.println(shareId + "," + userId);
        try {
            if (userId == null) {
                // 可以返回一个简单的错误消息而不是完整的响应
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Authentication required");
            }
            ShareInfoResponse response = shareService.getShareInfo(shareId, userId);
            System.out.println("执行到这一步了");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().equals("Share not found") || e.getMessage().equals("File not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get share info"));
        }
    }

    @PostMapping("/saveShare/{shareId}")
    public ResponseEntity<?> saveSharedFile(
            @PathVariable UUID shareId,
            @RequestParam Long userId,
            @RequestParam Long rootFolderId
    ) {
        String operationKey = shareId.toString() + "_" + userId;
        Cache processedCache = cacheManager.getCache("processedRequests");
        Cache resultCache = cacheManager.getCache("requestResults");

        if (processedCache.get(operationKey) != null) {
            Long existingFileId = resultCache.get(operationKey, Long.class);
            return ResponseEntity.ok(Map.of("newFileId", existingFileId, "note", "Already processed"));
        }

        processedCache.put(operationKey, true); // 先放置缓存，避免重复请求

        try {
            Long newFileId = shareService.saveSharedFile(shareId, userId, rootFolderId);
            resultCache.put(operationKey, newFileId);
            return ResponseEntity.ok(Map.of("newFileId", newFileId));
        } catch (Exception e) {
            processedCache.evict(operationKey); // 如果失败，清除缓存，允许重试
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save shared file"));
        }

    }

    @GetMapping("/getAllSharedFiles")
    public ResponseEntity<?> getAllSharedFiles(@RequestParam("ownerId") Long userId) {

        try {
            List<Share> shares = shareMapper.findByUserId(userId);
            List<ShareInfoResponse> shareInfoResponses = new ArrayList<>();
            for (Share share : shares) {
                FileVersion latestVersion = fileVersionMapper.getLatestVersion(share.getFileId());
                File file = fileMapper.getFileByUserIdAndFileId(userId, latestVersion.getFileId());
                ShareInfoResponse shareInfo = new ShareInfoResponse();

                shareInfo.setId(share.getId());
                shareInfo.setFileId(latestVersion.getFileId());
                shareInfo.setFilePath(latestVersion.getStoragePath());
                shareInfo.setFileName(file.getName());
                shareInfo.setShareLink(String.format("%s/share/%s",
                        frontendUrl,
                        share.getId()));
                shareInfo.setExpiresAt(share.getExpiresAt());
                shareInfo.setType(share.getAccessType());
                shareInfo.setActive(share.isActive());
                shareInfoResponses.add(shareInfo);


            }

            if (shareInfoResponses.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(shareInfoResponses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching shared files: " + e.getMessage());
        }
    }

    @PostMapping("/cancelShare")
    public ResponseEntity<?> cancelShare(
            @RequestParam("shareId") UUID shareId,
            @RequestParam("ownerId") Long userId
    ) {
        try {
           int row =shareService.cancelShare(shareId,userId);
           if (row>0){
               return ResponseEntity.ok().build();
           }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create share link"));
        }
    }

    @PostMapping("/restore")
    public ResponseEntity<?> restore(
            @RequestParam("shareId") UUID shareId,
            @RequestParam("ownerId") Long userId
    ) {
        try {
            int row =shareService.restore(shareId,userId);
            if (row>0){
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create share link"));
        }
    }
}
