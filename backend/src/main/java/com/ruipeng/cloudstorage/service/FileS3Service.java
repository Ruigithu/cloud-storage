package com.ruipeng.cloudstorage.service;

import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.FilePermission;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.ShareMapper;
import com.ruipeng.cloudstorage.util.SecurityUtil;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;

import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;


import static org.zwobble.mammoth.internal.conversion.DocumentToHtml.convertToHtml;

@Service
public class FileS3Service {
    private final File file;
    private final ShareMapper shareMapper;
    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FilePermissionMapper filePermissionMapper;
    private final S3StorageService s3StorageService;
    private final FileService fileService;

    @Autowired
    public FileS3Service(FileMapper fileMapper, FileVersionMapper fileVersionMapper, FilePermissionMapper filePermissionMapper,
                         File file, ShareMapper shareMapper, S3StorageService s3StorageService, FileService fileService) {
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.filePermissionMapper = filePermissionMapper;
        this.file = file;
        this.shareMapper = shareMapper;
        this.s3StorageService = s3StorageService;
        this.fileService = fileService;
    }
@Transactional
    public Map<String, Object> initiateResumableUpload(Long ownerId, Long folderId, String fileName, String mimeType, Long fileSize) throws IOException {
        if(folderId==null|| fileMapper.folderExists(folderId).isEmpty()) {
            throw new FileNotFoundException("Folder does not exist");
        }
        System.out.println(1);

        //store meta data
        File newFile = new File();
        newFile.setName(fileName);
        newFile.setMimeType(mimeType);
        newFile.setSize(fileSize);
        newFile.setOwnerId(ownerId);
        newFile.setFolderId(folderId);
        fileMapper.insertFile(newFile);

        System.out.println(2);
        String s3Key = String.format("%s/%s/%s_v1%s",
                ownerId, folderId, newFile.getId(), fileName.substring(fileName.lastIndexOf('.')));

        InitiateMultipartUploadResult initiated = s3StorageService.initiateMultipartUpload(s3Key);
        String uploadId = initiated.getUploadId();
        System.out.println(3);
        //temp
        FileVersion version = new FileVersion();
        version.setFileId(newFile.getId());
        version.setVersionNumber(1);
        version.setStoragePath(s3Key);
        version.setSize(fileSize);
        version.setCreatedBy(ownerId);
        version.setUploadId(uploadId); // 假设 FileVersion 实体新增 uploadId 字段
        fileVersionMapper.insertVersion(version);

        System.out.println(4);
        FilePermission permission = new FilePermission();
        permission.setFileId(newFile.getId());
        permission.setPermission(PermissionType.ADMIN);
        permission.setCreatedBy(ownerId);
        permission.setCreatedAt(Instant.now());
        permission.setUserId(ownerId);
        permission.setFolderId(folderId);

        System.out.println(5);
        FilePermission existingPermission = filePermissionMapper.findByFileIdAndUserId(newFile.getId(), ownerId);
        if (existingPermission == null) {
            filePermissionMapper.insert(permission);
        } else {
            existingPermission.setPermission(PermissionType.ADMIN);
            filePermissionMapper.update(existingPermission);
        }

    Map<String, Object> result = new HashMap<>();
    result.put("fileId", newFile.getId());
    result.put("uploadId", uploadId);
    return result;

    }

    //upload part
    public PartETag uploadPart(Long fileId, String uploadId,int partNumber, byte[]partData) throws IOException{
        FileVersion version = fileVersionMapper.getVersionByFileId(fileId);
        if (version==null || !version.getUploadId().equals(uploadId)){
            throw new RuntimeException("Invalid upload ID or file version");
        }
        String s3Key = version.getStoragePath();
        UploadPartResult uploadPartResult = s3StorageService.uploadPart(s3Key,uploadId,partNumber,partData,partData.length);
        return new PartETag(partNumber,uploadPartResult.getETag());
    }

    public List<PartSummary> listUploadedParts(Long fileId, String uploadId) {
        FileVersion version = fileVersionMapper.getLatestVersion(fileId);
        if (version == null ||version.getUploadId() == null|| !version.getUploadId().equals(uploadId)) {
            throw new RuntimeException("Invalid upload ID or file version");
        }
        return s3StorageService.listParts(version.getStoragePath(), uploadId);
    }


    public void completeResumableUpload(Long fileId, String uploadId, List<PartETag> partETags) {
        FileVersion version = fileVersionMapper.getLatestVersion(fileId);
        if (version == null || !version.getUploadId().equals(uploadId)) {
            throw new RuntimeException("Invalid upload ID or file version");
        }
        s3StorageService.completeMultipartUpload(version.getStoragePath(), uploadId, partETags);
        // 更新版本状态
        version.setUploadId(null); // 清除 uploadId，表示上传完成
        fileVersionMapper.updateUploadStatus(version);
    }


    public void abortResumableUpload(Long fileId, String uploadId ) throws IOException {
        FileVersion version = fileVersionMapper.getLatestVersion(fileId);
        if (version == null || !version.getUploadId().equals(uploadId)) {
            throw new RuntimeException("Invalid upload ID or file version");
        }
        s3StorageService.abortMultipartUpload(version.getStoragePath(), uploadId);
        softDeleteFile(fileId, SecurityUtil.getCurrentUserId());
        deleteFile(fileId, SecurityUtil.getCurrentUserId());
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
        // 1. Validate file exists and check permissions
        File existingFile = fileMapper.getFileById(fileId);
        if (existingFile == null) {
            throw new RuntimeException("File does not exist");
        }

        FilePermission permission = filePermissionMapper.findByFileIdAndUserId(fileId, ownerId);
        if (permission == null || permission.getPermission() != PermissionType.ADMIN) {
            throw new RuntimeException("No permission to update this file");
        }

        // 2. Update file metadata but retain original file ID
        String originalMimeType = existingFile.getMimeType();
        String newMimeType = file.getContentType();

        // Preserve original mime type for certain file formats when uploading updated content
        if (originalMimeType != null && originalMimeType.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document") &&
                (newMimeType.contains("text/html") || newMimeType.contains("application/json"))) {
            // Keep the original DOCX mime type
            newMimeType = originalMimeType;
        } else if (originalMimeType != null && originalMimeType.contains("application/msword") &&
                (newMimeType.contains("text/html") || newMimeType.contains("application/json"))) {
            // Keep the original DOC mime type
            newMimeType = originalMimeType;
        }

        existingFile.setMimeType(newMimeType);
        existingFile.setSize(file.getSize());
        existingFile.setUpdatedAt(Instant.now());

        // 3. Get the latest version number and increment
        int currentVersionNumber = fileVersionMapper.getLatestVersionNumber(fileId);
        int newVersionNumber = currentVersionNumber + 1;

        // 4. Generate new S3 key with version number
        FileVersion latestVersion = fileVersionMapper.getLatestVersion(fileId);
        String previousPath = latestVersion.getStoragePath();

        // Ensure extension is preserved correctly
        String extension = getFileExtension(file.getOriginalFilename());
        if (extension == null || extension.isEmpty()) {
            extension = getFileExtensionFromMimeType(newMimeType);
        }

        // Preserve original extension for certain file types
        if (originalMimeType != null &&
                (originalMimeType.contains("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                        originalMimeType.contains("application/msword"))) {
            String originalExtension = previousPath.substring(previousPath.lastIndexOf('.'));
            extension = originalExtension;
        }

        String s3Key;
        if (previousPath.contains("_v")) {
            s3Key = previousPath.replaceAll("_v\\d+\\.", "_v" + newVersionNumber + ".");
        } else {
            String basePath = previousPath.substring(0, previousPath.lastIndexOf('.'));
            s3Key = basePath + "_v" + newVersionNumber + extension;
        }

        // 5. Upload file to S3
        s3StorageService.uploadFile(file, s3Key);

        // 6. Insert new file version record
        FileVersion version = new FileVersion();
        version.setFileId(fileId);
        version.setVersionNumber(newVersionNumber);
        version.setStoragePath(s3Key);
        version.setSize(file.getSize());
        version.setCreatedBy(ownerId);
        version.setCreatedAt(Instant.now());
        fileVersionMapper.insertVersion(version);

        // 7. Update database with new file metadata
        fileMapper.updateFile(existingFile);

        return existingFile;
    }

    private String getFileExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return ".txt";

        Map<String, String> mimeToExt = Map.of(
                "application/json", ".json",
                "text/plain", ".txt",
                "text/html", ".html",
                "image/png", ".png",
                "image/jpeg", ".jpg",
                "application/pdf", ".pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx",
                "application/msword", ".doc"
        );

        return mimeToExt.getOrDefault(mimeType, ".txt");
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
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




}