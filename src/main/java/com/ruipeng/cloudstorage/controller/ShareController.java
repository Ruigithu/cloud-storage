package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.entity.Share;
import com.ruipeng.cloudstorage.entity.ShareInfoResponse;
import com.ruipeng.cloudstorage.entity.ShareResponse;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import com.ruipeng.cloudstorage.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ShareController {
    private ShareService shareService;
    private ShareMapper shareMapper;

    @Autowired
    public ShareController(ShareService shareService, ShareMapper shareMapper) {
        this.shareService = shareService;
        this.shareMapper = shareMapper;
    }

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
        System.out.println("hahahahahahahahahahahahahahahahahahahah");
        System.out.println(shareId+","+userId);
        try {
            ShareInfoResponse response = shareService.getShareInfo(shareId, userId);
            System.out.println("执行到这一步了");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
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
            @RequestParam Long userId
    ) {
        try {
            System.out.println("进来保存啦");
            System.out.println("shareId:"+shareId+" userId:"+userId);
            Long newFileId = shareService.saveSharedFile(shareId, userId);
            System.out.println("找到share啦");
            return ResponseEntity.ok(Map.of("newFileId", newFileId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save shared file"));
        }
    }

    @GetMapping("/getAllSharedFiles")
    public ResponseEntity<?> getAllSharedFiles(@RequestParam("ownerId") Long userId) {

        try {
            List<Share> shares = shareMapper.findByUserId(userId);

            if (shares.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(shares);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error fetching shared files: " + e.getMessage());
        }
    }
}
