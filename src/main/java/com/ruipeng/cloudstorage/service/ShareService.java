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
        // 验证文件存在且用户有权限
        System.out.println("执行到1");
        File file = fileMapper.findByIdAndUserIdWithAdminPermission(fileId, createdBy);
        System.out.println("执行到2");
        if (file == null) {
            throw new RuntimeException("File not found or no permission");
        }

        // 创建分享记录
        System.out.println("执行到3");
        Share share = new Share();
        System.out.println("share:"+share.getId()+","+share.getCreatedAt()+","+share.isActive());
        share.setFileId(fileId);
        System.out.println("插完fileId");
        share.setCreatedBy(createdBy);
        System.out.println("插完createdBy");
        if (Objects.equals(accessType, "read")) {
            share.setAccessType(PermissionType.READ);
        }else {
            share.setAccessType(PermissionType.WRITE);
        }
        System.out.println("插完accessType");
        System.out.println(expiresAt+"是什么");

        if (expiresAt != null) {
            share.setExpiresAt(OffsetDateTime.parse(expiresAt));
        }

        System.out.println("share:"+share.getFileId()+" "+share.getCreatedBy()+" "+share.getAccessType()+" "+share.getExpiresAt());
        System.out.println("shareId:"+share.getId().getClass());
        try {
            shareMapper.insert(share);
        } catch (Exception e) {
            e.printStackTrace(); // 打印具体错误信息
        }
        System.out.println("执行到4");

        // 生成分享链接
        String shareLink = String.format("%s/share/%s",
                frontendUrl,
                share.getId());
        System.out.println(shareLink+" 执行到5");

        ShareResponse response = new ShareResponse();
        response.setShareLink(shareLink);
        return response;
    }



    public ShareInfoResponse getShareInfo(UUID shareId, Long userId) {
        System.out.println("执行到1");
        Share share = null;
        try {
            share = shareMapper.findById(shareId);
        } catch (Exception e) {
            System.out.println("数据库查询发生异常：");
            e.printStackTrace();  // 打印完整堆栈信息
        }
        System.out.println("执行到2");
        System.out.println(share);
        System.out.println(share.getAccessType()+" "+share.getExpiresAt());
        if (share == null) {
            throw new RuntimeException("Share not found or expired");
        }
        System.out.println("执行到3");
        File file = fileMapper.getFileById(share.getFileId());
        if (file == null) {
            throw new RuntimeException("File not found");
        }
        System.out.println("执行到4");
        ShareInfoResponse response = new ShareInfoResponse();
        response.setType(share.getAccessType().name());
        response.setRequiresAuth(PermissionType.WRITE.equals(share.getAccessType()) && userId == null);
        response.setFileName(file.getName());
        response.setFilePath(fileVersionMapper.getVersionByFileId(file.getId()).getStoragePath());
        response.setFileId(file.getId());
        System.out.println("执行到5");
        return response;
    }

    public Long saveSharedFile(UUID shareId, Long userId) {
        // 1. 验证分享和权限
        Share share = shareMapper.findById(shareId);
        System.out.println("执行到1");
        if (share == null || !PermissionType.WRITE.equals(share.getAccessType())) {
            throw new RuntimeException("Invalid share");
        }
        System.out.println("执行到2");
        // 2. 获取原始文件信息和最新版本
        File originalFile = fileMapper.getFileById(share.getFileId());
        System.out.println("执行到3");
        System.out.println(originalFile.getName());
        if (originalFile == null) {
            throw new RuntimeException("Original file not found");
        }

        FileVersion latestVersion = fileVersionMapper.getLatestVersion(originalFile.getId());
        System.out.println("执行到4");
        if (latestVersion == null) {
            throw new RuntimeException("File version not found");
        }
        System.out.println("执行到5");
        try {
            // 3. 读取原始文件并创建MultipartFile
            Path originalFilePath = Paths.get(latestVersion.getStoragePath());
            String contentType = Files.probeContentType(originalFilePath);

            MultipartFile multipartFile = new CustomMultipartFile(
                    originalFile.getName(),    // 文件名
                    originalFile.getName(),    // originalFilename
                    contentType,              // content type
                    Files.readAllBytes(originalFilePath)  // 文件内容
            );

            // 4. 获取或创建用户的根目录ID
            Long userRootFolderId = folderMapper.getUserRootFolderId(userId);
            if (userRootFolderId == null) {
                Folder folder = folderService.createFolder("temp", 1L, userId);
//                folderService.softDeleteFolder(folder.getId(),userId);
//                folderService.deleteFolder(folder.getId(),userId);
                System.out.println("创建好根目录啦");
            }
            System.out.println(userId+","+userRootFolderId);
            System.out.println("执行到6");


            // 5. 使用现有的uploadFile方法保存文件

            File newFile = fileService.uploadFile(multipartFile, userId, userRootFolderId);
            System.out.println("执行到7");
            return newFile.getId();

        } catch (IOException e) {
            throw new RuntimeException("Failed to copy shared file: " + e.getMessage());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }


}

