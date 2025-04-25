package com.ruipeng.cloudstorage.controller;


import com.amazonaws.services.s3.model.PartETag;
import com.ruipeng.cloudstorage.entity.DownloadFileInfo;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.Folder;
import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.service.FileS3Service;
import com.ruipeng.cloudstorage.service.FolderS3Service;
import org.apache.ibatis.javassist.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FolderController {

    private final User user;
    private final FileMapper fileMapper;
    private final File file;
    private FolderS3Service folderService;
    private FileS3Service fileService;

    @Autowired
    public FolderController(FolderS3Service folderService, FileS3Service fileService, User user, FileMapper fileMapper, File file) {
        this.folderService = folderService;
        this.fileService = fileService;
        this.user = user;
        this.fileMapper = fileMapper;
        this.file = file;
    }
    @GetMapping("/getRootFolders")
    public Map<String, Object> getRootFolders(@RequestParam Long userId) throws SQLException {
        System.out.println("hello");
        Long rootFolderId = folderService.getRootFolderId(userId);
        List<Folder> folders = folderService.getFolders(userId, rootFolderId);

        Map<String, Object> response = new HashMap<>();
        response.put("rootFolderId", rootFolderId);
        response.put("folders", folders);

        return response;
    }

    @PostMapping("/createFolder")
    public ResponseEntity<Folder> createFolder(@RequestParam("name")String name,@RequestParam("parentId")Long parentId,@RequestParam("userId") Long userId) throws SQLException, NotFoundException, AccessDeniedException {
       Folder folder = folderService.createFolder(name, parentId, userId);
        return ResponseEntity.ok(folder);
    }

    @GetMapping("/getAllFolders")
    public ResponseEntity<?> getAllFolders(
            @RequestParam(required = false) Long parentId,
            @RequestParam Long userId) {
        try {
            if (parentId == null) {
                Long rootId = folderService.getRootFolderId(userId);
                Map<String, Object> response = new HashMap<>();
                response.put("rootFolderId", rootId);
                response.put("folders", folderService.getFolders(userId, rootId));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(folderService.getFolders(userId, parentId));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting folders: " + e.getMessage());
        }
    }

    @DeleteMapping  ("/deleteFolder")
    public ResponseEntity<?> deleteFolder(@RequestParam("folderId") long folderId,@RequestParam("userId")long ownerId) throws IOException {
        int i = folderService.deleteFolder(folderId, ownerId);
        if (i>0){
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }


    @DeleteMapping  ("/softDeleteFolder")
    public ResponseEntity<?> softDeleteFolder(@RequestParam("folderId") long folderId,@RequestParam("userId")long ownerId) throws IOException {
        int i = folderService.softDeleteFolder(folderId, ownerId);
        if (i>0){
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }



    @GetMapping("/getAllDeletedFolders")
    public ResponseEntity<List<Folder>> getAllDeletedFolders(@RequestParam long parentId, @RequestParam long userId) throws SQLException {

        List<Folder> folders = folderService.getAllDeletedFolders(userId,parentId);
        return ResponseEntity.ok().body(folders);
    }

    @PostMapping("/restoreFolder")
    public ResponseEntity<?>restoreFile(@RequestParam long folderId,@RequestParam long ownerId) {
        int i = folderService.restoreFolder(folderId, ownerId);
        if (i>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }


    @GetMapping("/downloadFolder")
    public ResponseEntity<Resource> downloadFolder(@RequestParam("folderId") Long folderId, @RequestParam("userId") Long userId) {
        try {
            DownloadFileInfo downloadInfo = folderService.downloadFolder(folderId, userId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + URLEncoder.encode(downloadInfo.getFileName() + ".zip", "UTF-8") + "\"")
                    .body(downloadInfo.getResource());
        } catch (Exception e) {
            throw new RuntimeException("fail downloading folder " + e.getMessage());
        }
    }

    @PostMapping("/uploadFolder")
    public ResponseEntity<?> uploadFolder(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("paths") String[] relativePaths,
            @RequestParam("folderId") long parentFolderId,
            @RequestParam("userId")long userId) throws IOException {

        if (files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No files uploaded");
        }

        try {
            folderService.uploadFolderSmall(files, relativePaths, userId, parentFolderId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("fail uploading folder " + e.getMessage());
        }
    }

    @PostMapping("/folders-initiate-upload")
    public ResponseEntity<?> initiateUpload(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("relativePaths") String[] relativePaths,
            @RequestParam(value = "parentFolderId", required = false) long parentFolderId,
            @RequestParam("userId")long userId) {

        try {

            Map<String, Object> uploadInfo = folderService.initiateFolderUpload(
                    files, relativePaths, userId, parentFolderId);

            return ResponseEntity.ok(uploadInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to initiate folder upload: " + e.getMessage()));
        }
    }

    @PostMapping("/folders-upload-part")
    public ResponseEntity<?> uploadPart(
            @RequestParam("fileId") Long fileId,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber,
            @RequestParam("file") MultipartFile file) {

        try {
            PartETag partETag = fileService.uploadPart(fileId, uploadId, partNumber, file.getBytes());
            return ResponseEntity.ok(partETag);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to upload part: " + e.getMessage()));
        }
    }

    @GetMapping("/folders-upload-status")
    public ResponseEntity<?> getUploadStatus(
            @RequestParam("fileIds") List<Long> fileIds) {

        try {
            Map<String, Object> status = folderService.getFolderUploadStatus(fileIds);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to get upload status: " + e.getMessage()));
        }
    }

    @PostMapping("/folders-complete-upload")
    public ResponseEntity<?> completeUpload(
            @RequestBody List<Map<String, Object>> fileCompletions) {

        try {
            folderService.completeFolderUpload(fileCompletions);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to complete upload: " + e.getMessage()));
        }
    }

    @PostMapping("/folders-abort-upload")
    public ResponseEntity<?> abortUpload(
            @RequestParam(value = "fileIds", required = false) Long[] fileIds,
            @RequestParam(value = "uploadIds", required = false) String[] uploadIds,
            @RequestParam(value = "parentFolderId", required = false) Long parentFolderId,
            @RequestParam(value = "userId", required = false) Long userId) {

        System.out.println("=== 接收到取消上传请求(FormData) ===");
        System.out.println("文件ID数量: " + (fileIds != null ? fileIds.length : 0));

        List<Map<String, String>> results = new ArrayList<>();

        if (fileIds == null || uploadIds == null || fileIds.length != uploadIds.length) {
            return ResponseEntity.badRequest().body("Invalid request parameters");
        }
        long minFolderId=Integer.MAX_VALUE;
        for (int i = 0; i < fileIds.length; i++) {
                Long fileId = fileIds[i];
                long folderId = fileMapper.getFileByUserIdAndFileId(userId, fileId).getFolderId();
                if (folderId!=parentFolderId) {
                    minFolderId = Math.min(folderId,minFolderId);
                }
        }

        for (int i = 0; i < fileIds.length; i++) {
            try {
                Long fileId = fileIds[i];
                String uploadId = uploadIds[i];

                System.out.println("处理取消请求项: fileId=" + fileId + ", uploadId=" + uploadId
                         + ", userId=" + userId+", minFolderId=" + minFolderId);

                if (fileId != null && uploadId != null) {
                    System.out.println("准备调用 abortFolderUpload 方法...");
                    try {
                        folderService.abortFolderUpload(fileId, uploadId);
                        System.out.println("abortFolderUpload 方法调用成功");
                        results.add(Map.of("fileId", String.valueOf(fileId), "status", "success"));
                    } catch (Exception e) {
                        System.err.println("abortFolderUpload 方法调用异常: " + e);
                        e.printStackTrace();
                        results.add(Map.of("fileId", String.valueOf(fileId), "status", "failed", "error", e.getMessage()));
                    }
                } else {
                    System.err.println("fileId 或 uploadId 为空");
                    results.add(Map.of("fileId", String.valueOf(fileId), "status", "failed", "error", "Missing fileId or uploadId"));
                }
            } catch (Exception e) {
                System.err.println("处理取消请求项异常: " + e);
                e.printStackTrace();
                results.add(Map.of("index", String.valueOf(i), "status", "failed", "error", e.getMessage()));
            }
        }

        // 第二步：软删除文件夹
        try {
            int i = folderService.softDeleteFolder(minFolderId, userId);
            if (i==0) {
                System.err.println("Soft delete folder failed");
                throw new RuntimeException("Failed to soft delete folder");
            }
            System.out.println("Folder soft deleted successfully");
        } catch (Exception e) {
            System.err.println("Error during soft delete: " + e.getMessage());
            throw new RuntimeException("Folder soft delete failed: " + e.getMessage(), e);
        }

        // 第三步：物理删除文件夹
        try {
            int i = folderService.deleteFolder(minFolderId, userId);
            if (i==0) {
                System.err.println("Delete folder failed");
                throw new RuntimeException("Failed to delete folder");
            }
            System.out.println("Folder deleted successfully");
        } catch (Exception e) {
            System.err.println("Error during folder deletion: " + e.getMessage());
            throw new RuntimeException("Folder deletion failed: " + e.getMessage(), e);
        }
        System.out.println("处理完成，返回结果: " + results);
        return ResponseEntity.ok(results);
    }


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
