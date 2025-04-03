package com.ruipeng.cloudstorage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FileS3Service {
    private final File file;
    private final ShareMapper shareMapper;
    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FilePermissionMapper filePermissionMapper;
    private final S3StorageService s3StorageService;

    @Autowired
    public FileS3Service(FileMapper fileMapper, FileVersionMapper fileVersionMapper, FilePermissionMapper filePermissionMapper,
                         File file, ShareMapper shareMapper, S3StorageService s3StorageService) {
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.filePermissionMapper = filePermissionMapper;
        this.file = file;
        this.shareMapper = shareMapper;
        this.s3StorageService = s3StorageService;
    }

    public File uploadFile(MultipartFile file, Long ownerId, Long folderId) throws IOException {
        System.out.println("folderId:"+folderId);
        if (folderId == null || fileMapper.folderExists(folderId).isEmpty()) {
            throw new RuntimeException("Folder does not exist");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }

        String subPath = "";
        String fileName = originalFilename;

        if (originalFilename.contains("/") || originalFilename.contains("\\")) {
            Path fullPath = Paths.get(originalFilename);
            fileName = fullPath.getFileName().toString();
            if (fullPath.getParent() != null) {
                subPath = fullPath.getParent().toString();
            }
        }

        String fileExtension = "";
        String nameWithoutExtension = fileName;

        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            nameWithoutExtension = fileName.substring(0, lastDotIndex);
            fileExtension = fileName.substring(lastDotIndex);
        }

        // 1. Insert file metadata
        File newFile = new File();
        newFile.setName(fileName);
        newFile.setFolderId(folderId);
        newFile.setOwnerId(ownerId);
        newFile.setMimeType(file.getContentType());
        newFile.setSize(file.getSize());
        fileMapper.insertFile(newFile);

        // 2. Generate S3 key and upload
        int versionNumber = 1;
        String s3Key = String.format("%s/%s/%s/%s_v%d%s",
                ownerId.toString(),
                folderId.toString(),
                subPath.replace("\\", "/"), // Ensure consistent path separator
                newFile.getId().toString(),
                versionNumber,
                fileExtension);
        s3StorageService.uploadFile(file, s3Key);

        // 3. Insert file version
        FileVersion version = new FileVersion();
        version.setFileId(newFile.getId());
        version.setVersionNumber(versionNumber);
        version.setStoragePath(s3Key);
        version.setSize(file.getSize());
        version.setCreatedBy(ownerId);
        fileVersionMapper.insertVersion(version);

        // 4. Set permissions
        FilePermission permission = new FilePermission();
        permission.setFileId(newFile.getId());
        permission.setPermission(PermissionType.ADMIN);
        permission.setCreatedBy(ownerId);
        permission.setCreatedAt(Instant.now());
        permission.setUserId(ownerId);
        permission.setFolderId(folderId);

        FilePermission existingPermission = filePermissionMapper.findByFileIdAndUserId(newFile.getId(), ownerId);
        if (existingPermission == null) {
            filePermissionMapper.insert(permission);
        } else {
            existingPermission.setPermission(PermissionType.ADMIN);
            filePermissionMapper.update(existingPermission);
        }

        return newFile;
    }

    public File uploadNewVersion(MultipartFile file, Long ownerId, Long fileId) throws IOException {
        File existingFile = fileMapper.getFileById(fileId);
        if (existingFile == null) {
            throw new RuntimeException("File does not exist");
        }

        FilePermission permission = filePermissionMapper.findByFileIdAndUserId(fileId, ownerId);
        if (permission == null || permission.getPermission() != PermissionType.ADMIN) {
            throw new RuntimeException("No permission");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }

        String subPath = "";
        String fileName = originalFilename;

        if (originalFilename.contains("/") || originalFilename.contains("\\")) {
            Path fullPath = Paths.get(originalFilename);
            fileName = fullPath.getFileName().toString();
            if (fullPath.getParent() != null) {
                subPath = fullPath.getParent().toString();
            }
        }

        String fileExtension = "";
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            fileExtension = fileName.substring(lastDotIndex);
        }

        existingFile.setMimeType(file.getContentType());
        existingFile.setSize(file.getSize());
        fileMapper.updateFile(existingFile);

        int versionNumber = fileVersionMapper.getLatestVersionNumber(fileId) + 1;
        String s3Key = String.format("%s/%s/%s/%s_v%d%s",
                ownerId.toString(),
                existingFile.getFolderId(),
                subPath.replace("\\", "/"),
                fileId.toString(),
                versionNumber,
                fileExtension);
        s3StorageService.uploadFile(file, s3Key);

        FileVersion version = new FileVersion();
        version.setFileId(fileId);
        version.setVersionNumber(versionNumber);
        version.setStoragePath(s3Key);
        version.setSize(file.getSize());
        version.setCreatedBy(ownerId);
        fileVersionMapper.insertVersion(version);

        return existingFile;
    }

    public List<File> getFiles(long ownerId, long folderId) {
        return fileMapper.getFilesByUserIdAndFolderId(ownerId, folderId);
    }

    public int softDeleteFile(Long fileId, Long userId) {
        File file = fileMapper.getFileById(fileId);
        if (file == null) {
            throw new RuntimeException("File does not exist");
        }

        file.setDeleted(true);
        file.setUpdatedAt(Instant.now());
        return fileMapper.updateFileDeleteStatus(file);
    }

    public int restoreFile(Long fileId, Long userId) {
        File file = fileMapper.getDeletedFileById(fileId);
        if (file == null) {
            throw new RuntimeException("File does not exist");
        }
        if (!file.isDeleted()) {
            throw new RuntimeException("File is not deleted");
        }

        file.setDeleted(false);
        file.setUpdatedAt(Instant.now());
        return fileMapper.updateFileDeleteStatus(file);
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupSoftDeletedFiles() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<File> filesToDelete = fileMapper.getSoftDeletedFilesBefore(thirtyDaysAgo);

        for (File file : filesToDelete) {
            try {
                deleteFile(file.getId(), file.getOwnerId());
            } catch (IOException e) {
                System.out.println("Failed to delete file: " + file.getId() + " - " + e.getMessage());
            }
        }
    }

    public int deleteFile(Long fileId, Long userId) throws IOException {
        File file = fileMapper.getDeletedFileById(fileId);
        if (file == null) {
            throw new RuntimeException("File does not exist");
        }

        List<FileVersion> versions = fileVersionMapper.getVersionsByFileId(fileId);
        for (FileVersion version : versions) {
            String s3Key = version.getStoragePath();
            if (s3StorageService.doesFileExist(s3Key)) {
                s3StorageService.deleteFile(s3Key);
            }
        }

        filePermissionMapper.deleteByFileId(fileId);
        fileVersionMapper.deleteByFileId(fileId);
        if (!shareMapper.findByFileIdAndCreatedBy(fileId, userId).isEmpty()) {
            shareMapper.deleteByFileIdAndCreatedBy(fileId, userId);
        }
        return fileMapper.deleteFile(fileId);
    }

    public DownloadFileInfo downloadFile(Long fileId) throws IOException {
        File file = fileMapper.getFileById(fileId);
        if (file == null) {
            throw new RuntimeException("File does not exist");
        }

        FileVersion latestVersion = fileVersionMapper.getLatestVersion(fileId);
        if (latestVersion == null) {
            throw new RuntimeException("File version does not exist");
        }

        String s3Key = latestVersion.getStoragePath();
        byte[] fileContent = s3StorageService.downloadFile(s3Key);
        Resource resource = new ByteArrayResource(fileContent);

        return new DownloadFileInfo(
                file.getName(),
                file.getMimeType(),
                resource
        );
    }

    public List<File> getAllDeletedFiles(long ownerId, long folderId) {
        List<File> deletedFiles = fileMapper.getDeletedFilesByUserIdAndFolderId(ownerId, folderId);
        deletedFiles.addAll(fileMapper.getDeletedFileByUserId(ownerId));
        return deletedFiles;
    }

    public ResponseEntity<?> getFileByUserIdAndFileId(long ownerId, long fileId) {
        try {
            FileVersion version = fileVersionMapper.getLatestVersion(fileId);
            if (version == null) {
                return ResponseEntity.notFound().build();
            }

            File file = fileMapper.getFileByUserIdAndFileId(ownerId, fileId);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }

            String s3Key = version.getStoragePath();
            if (!s3StorageService.doesFileExist(s3Key)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = s3StorageService.downloadFile(s3Key);
            String contentType = file.getMimeType() != null ? file.getMimeType() : "application/octet-stream";

            if (isWordDocument(contentType, version.getStoragePath())) {
                try {
                    String convertedText;
                    if (version.getStoragePath().endsWith(".docx")) {
                        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileContent));
                        StringBuilder text = new StringBuilder();
                        for (XWPFParagraph paragraph : document.getParagraphs()) {
                            for (XWPFRun run : paragraph.getRuns()) {
                                text.append(run.getText(0)).append(" ");
                            }
                            text.append("\n");
                        }
                        convertedText = text.toString();
                        document.close();
                    } else {
                        HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(fileContent));
                        convertedText = document.getDocumentText();
                        document.close();
                    }
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(convertedText);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to convert file: " + e.getMessage());
                }
            } else if (contentType.startsWith("text/") || contentType.equals("application/json")) {
                String textContent = new String(fileContent, StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(textContent);
            } else {
                String fileName = s3Key.substring(s3Key.lastIndexOf('/') + 1);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(fileContent);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading file: " + e.getMessage());
        }
    }

    private boolean isWordDocument(String contentType, String filePath) {
        return contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equals("application/msword") ||
                filePath.endsWith(".docx") ||
                filePath.endsWith(".doc");
    }
}