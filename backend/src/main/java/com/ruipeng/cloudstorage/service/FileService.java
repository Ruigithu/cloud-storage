package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.dto.DownloadFileInfo;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class FileService {
    private final File file;
    private final ShareMapper shareMapper;
    private FileMapper fileMapper;
    private FileVersionMapper fileVersionMapper;
    private FilePermissionMapper filePermissionMapper;
    private final ThreadPoolTaskExecutor taskExecutor;

    @Value("${file.storage.path}")
    private String baseStoragePath;

    @Autowired
    private PlatformTransactionManager transactionManager;


    public FileService(FileMapper fileMapper,
                       FileVersionMapper fileVersionMapper,
                       FilePermissionMapper filePermissionMapper,
                       File file,
                       ShareMapper shareMapper,
                       ThreadPoolTaskExecutor taskExecutor) {
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.filePermissionMapper = filePermissionMapper;
        this.file = file;
        this.shareMapper = shareMapper;
        this.taskExecutor = taskExecutor;
    }

    @Transactional
    public File uploadFile(MultipartFile file, Long ownerId, Long folderId) throws IOException {
        if (folderId == null && fileMapper.folderExists(folderId).isEmpty()) {
            throw new RuntimeException("folder not exist");
        }
        long startTime = System.currentTimeMillis();

        String originalFilename = file.getOriginalFilename();


        String subPath = "";
        String fileName = originalFilename;


        if (originalFilename != null && (originalFilename.contains("/") || originalFilename.contains("\\"))) {
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
        // 1. files 表table
        File newFile = new File();
        newFile.setName(fileName); // save original file name(extension included)
        newFile.setFolderId(folderId);
        newFile.setOwnerId(ownerId);
        newFile.setMimeType(file.getContentType());
        newFile.setSize(file.getSize());
        fileMapper.insertFile(newFile);


        // 2. version table
        int versionNumber = 1;


        // construct new name
        String newFileName = String.format("%s_v%d%s", nameWithoutExtension, versionNumber, fileExtension);

        // make sure no repeating
        Path directoryPath;
        if (subPath.isEmpty()) {
            directoryPath = Paths.get(baseStoragePath, ownerId.toString(), Long.toString(folderId));
        } else {
            directoryPath = Paths.get(baseStoragePath, ownerId.toString(), Long.toString(folderId), subPath);
        }

        System.out.println("directoryPath:" + directoryPath);
        Path filePath = directoryPath.resolve(newFileName);
//
//        // construct parent folder
//        try {
//            Files.createDirectories(directoryPath);
//        } catch (IOException e) {
//            throw new IOException("Failed to create directories: " + directoryPath, e);
//        }
//
//        // save file
//        try {
//            file.transferTo(filePath.toFile());
//        } catch (IOException e) {
//            throw new IOException("Failed to store file at: " + filePath, e);
//        }
//
////          file_versions
//        FileVersion version = new FileVersion();
//        version.setFileId(newFile.getId());
//        version.setVersionNumber(versionNumber);
//        version.setStoragePath(filePath.toString());
//        version.setSize(file.getSize());
//        version.setCreatedBy(ownerId);
//        fileVersionMapper.insertVersion(version);
//
////         permission table
//        FilePermission permission = new FilePermission();
//        permission.setFileId(newFile.getId());
//        permission.setPermission(PermissionType.ADMIN);
//        permission.setCreatedBy(ownerId);
//        permission.setCreatedAt(Instant.now());
//        permission.setUserId(ownerId);
//        permission.setFolderId(folderId);
//
//        try {
//            FilePermission existingPermission = filePermissionMapper.findByFileIdAndUserId(newFile.getId(), ownerId);
//            if (existingPermission==null) {
//                filePermissionMapper.insert(permission);
//            } else {
//                existingPermission.setPermission(PermissionType.ADMIN);
//                filePermissionMapper.update(existingPermission);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException();
//        }

        CompletableFuture.runAsync(() -> {
            long asyncStartTime = System.currentTimeMillis();
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.execute(status -> {
                try {
                    Files.createDirectories(directoryPath); // 创建目录
                    file.transferTo(filePath.toFile());     // 保存文件到本地
                    // 插入 file_versions 表
                    FileVersion version = new FileVersion();
                    version.setFileId(newFile.getId());
                    version.setVersionNumber(versionNumber);
                    version.setStoragePath(filePath.toString());
                    version.setSize(file.getSize());
                    version.setCreatedBy(ownerId);
                    fileVersionMapper.insertVersion(version);

                    FilePermission permission = new FilePermission();
                    permission.setFileId(newFile.getId());
                    permission.setPermission(PermissionType.ADMIN);
                    permission.setCreatedBy(ownerId);
                    permission.setCreatedAt(Instant.now());
                    permission.setUserId(ownerId);
                    permission.setFolderId(folderId);

                    FilePermission existingPermission = filePermissionMapper.findByFileIdAndUserId(newFile.getId(), ownerId);
                    if (existingPermission==null) {
                        filePermissionMapper.insert(permission);
                    } else {
                        existingPermission.setPermission(PermissionType.ADMIN);
                        filePermissionMapper.update(existingPermission);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

              return null;
          });

            long asyncEndTime = System.currentTimeMillis();
            System.out.println("Async Task Time: " + (asyncEndTime - asyncStartTime) + " ms");
        }, taskExecutor);

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        System.out.println("Main Thread Upload Time: " + duration + " ms");

        return newFile;
    }

    public File uploadNewVersion(MultipartFile file, Long ownerId, Long fileId) throws IOException {

        // 1. if exist
        File existingFile = fileMapper.getFileById(fileId);
        if (existingFile == null) {
            throw new RuntimeException("file not exist");
        }

        // 2. verify permission
        FilePermission permission = filePermissionMapper.findByFileIdAndUserId(fileId, ownerId);
        if (permission == null || !(permission.getPermission() == PermissionType.ADMIN)) {
            throw new RuntimeException("no permission");
        }

        String originalFilename = file.getOriginalFilename();
        Long folderId = existingFile.getFolderId();

        String subPath = "";
        String fileName = originalFilename;


        if (originalFilename != null && (originalFilename.contains("/") || originalFilename.contains("\\"))) {
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

        // 3. update file data
        try {
            existingFile.setMimeType(file.getContentType());
            existingFile.setSize(file.getSize());
            int result = fileMapper.updateFile(existingFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 4. new version
        int versionNumber = fileVersionMapper.getLatestVersionNumber(fileId) + 1;

        // new filename
        String newFileName = String.format("%s_v%d%s", nameWithoutExtension, versionNumber, fileExtension);

        Path directoryPath;
        if (subPath.isEmpty()) {
            directoryPath = Paths.get(baseStoragePath, ownerId.toString(), Long.toString(folderId));
        } else {
            directoryPath = Paths.get(baseStoragePath, ownerId.toString(), Long.toString(folderId), subPath);
        }


        Path filePath = directoryPath.resolve(newFileName);


        try {
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            throw new IOException("Failed to create directories: " + directoryPath, e);
        }

        // save
        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            throw new IOException("Failed to store file at: " + filePath, e);
        }

        // 5. file_versions table
        FileVersion version = new FileVersion();
        version.setFileId(fileId);
        version.setVersionNumber(versionNumber);
        version.setStoragePath(filePath.toString());
        version.setSize(file.getSize());
        version.setCreatedBy(ownerId);
        fileVersionMapper.insertVersion(version);


        return existingFile;
    }

    public List<File> getFiles(long ownerId,long folderId){
        return fileMapper.getFilesByUserIdAndFolderId(ownerId,folderId);
    }


    //soft delete
    public int softDeleteFile(Long fileId, Long userId) {

        File file = fileMapper.getFileById(fileId);
        if (file == null) {
            throw new RuntimeException("file not exist");
        }

        // 2. verify permission
//        FilePermission permission = filePermissionMapper.getPermission(fileId, userId);
//        if (permission == null || !permission.getPermission().equals(PermissionType.ADMIN)) {
//            throw new RuntimeException("no permission");
//        }
        FilePermission permission = filePermissionMapper.getPermission(fileId, userId);
        if (permission != null) {
            System.out.println("Permission type: " + permission.getPermission());
            System.out.println("Is ADMIN? " + (permission.getPermission() == PermissionType.ADMIN));
            System.out.println("Equals ADMIN? " + permission.getPermission().equals(PermissionType.ADMIN));
            System.out.println("Permission class: " + permission.getPermission().getClass());
            System.out.println("ADMIN class: " + PermissionType.ADMIN.getClass());
        }
        System.out.println("finish checking");

        // 3. update the soft delete status
        file.setDeleted(true);
        file.setUpdatedAt(Instant.now());

        return fileMapper.updateFileDeleteStatus(file);
    }

    //restore
    public int restoreFile(Long fileId, Long userId) {
        File file = fileMapper.getDeletedFileById(fileId);
        if (file == null) {
            throw new RuntimeException("file not exist");
        }


        FilePermission permission = filePermissionMapper.getPermission(fileId, userId);
        if (permission != null) {
            System.out.println("Permission type: " + permission.getPermission());
            System.out.println("Is ADMIN? " + (permission.getPermission() == PermissionType.ADMIN));
            System.out.println("Equals ADMIN? " + permission.getPermission().equals(PermissionType.ADMIN));
            System.out.println("Permission class: " + permission.getPermission().getClass());
            System.out.println("ADMIN class: " + PermissionType.ADMIN.getClass());
        }
        System.out.println("finish checking");

        if (!file.isDeleted()) {
            throw new RuntimeException("not deleted");
        }

        file.setDeleted(false);
        file.setUpdatedAt(Instant.now());

        return fileMapper.updateFileDeleteStatus(file);
    }

    // delete files soft-deleted more than 30 days
    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanupSoftDeletedFiles() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<File> filesToDelete = fileMapper.getSoftDeletedFilesBefore(thirtyDaysAgo);

        for (File file : filesToDelete) {
            try {
                deleteFile(file.getId(), file.getOwnerId());
                System.out.println("Successfully deleted file: {}"+file.getId());
            } catch (IOException e) {
                System.out.println("Failed to delete file: {}"+file.getId()+e);
            }
        }
    }


    public int deleteFile(Long fileId, Long userId) throws IOException {

            File file = fileMapper.getDeletedFileById(fileId);
            if (file == null) {
                throw new RuntimeException("file not exist");
            }

            // 2. admin
            FilePermission permission = filePermissionMapper.getPermission(fileId, userId);
            if (permission != null) {
                System.out.println("Permission type: " + permission.getPermission());
                System.out.println("Is ADMIN? " + (permission.getPermission() == PermissionType.ADMIN));
                System.out.println("Equals ADMIN? " + permission.getPermission().equals(PermissionType.ADMIN));
                System.out.println("Permission class: " + permission.getPermission().getClass());
                System.out.println("ADMIN class: " + PermissionType.ADMIN.getClass());
            }
            System.out.println("finish checking");

            List<FileVersion> versions = fileVersionMapper.getVersionsByFileId(fileId);


            // 4. physically delete
            for (FileVersion version : versions) {

                Path filePath = Paths.get(version.getStoragePath());
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    throw new IOException("fail deleting " + filePath, e);
                }
            }

            // 5. delete data in other tables
            filePermissionMapper.deleteByFileId(fileId);  // delete permission data
            fileVersionMapper.deleteByFileId(fileId);     // delete version data
        //delete share data
            if (!shareMapper.findByFileIdAndCreatedBy(fileId,userId).isEmpty()){
                shareMapper.deleteByFileIdAndCreatedBy(fileId,userId);
            }
            return fileMapper.deleteFile(fileId);                // delete file
        }


    public DownloadFileInfo downloadFile(Long fileId) throws IOException {
        File file = fileMapper.getFileById(fileId);
        if (file == null) {
            throw new RuntimeException("file not exist");
        }
        // 2. new version
        FileVersion latestVersion = fileVersionMapper.getLatestVersion(fileId);
        if (latestVersion == null) {
            throw new RuntimeException("file version does not exist");
        }
        // 3. get the local path
        Path filePath = Paths.get(latestVersion.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("file does not exist in local storage");
        }
        // 4. construct resources
        Resource resource = new FileSystemResource(filePath.toFile());
        return new DownloadFileInfo(
                file.getName(),
                file.getMimeType(),
                resource
        );
    }


    public List<File> getAllDeletedFiles( long ownerId,long folderId) {

        List<File> deletedFoldersAndFiles = fileMapper.getDeletedFilesByUserIdAndFolderId(ownerId, folderId);
        deletedFoldersAndFiles.addAll(fileMapper.getDeletedFileByUserId(ownerId));
        return deletedFoldersAndFiles;
    }

    public ResponseEntity<?> getFileByUserIdAndFileId(long ownerId, long fileId) {

        try {
            FileVersion version = fileVersionMapper.getLatestVersion(fileId);
            if (version == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(version.getStoragePath());
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }


            byte[] fileContent = Files.readAllBytes(filePath);


            File file= fileMapper.getFileByUserIdAndFileId(ownerId, fileId);

            if (isWordDocument(contentType, version.getStoragePath())) {
                // Word
                try {
                    String convertedText;
                    if (version.getStoragePath().endsWith(".docx")) {
                        // docx
                        XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileContent));
                        StringBuilder text = new StringBuilder();
                        for (XWPFParagraph paragraph : document.getParagraphs()) {
                            List<XWPFRun> runs = paragraph.getRuns();
                            for (XWPFRun run : runs) {
                                text.append(run.getText(0)).append(" ");
                            }
                            text.append("\n");
                        }
                        convertedText = text.toString();
                        System.out.println("converted text: " + convertedText);
                        document.close();
//                    } else if (isZipFile(fileContent)) {
//                        return handleZipFile(fileContent, ownerId, fileId);
                    } else {
                        // doc
                        HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(fileContent));
                        convertedText = document.getDocumentText();
                        document.close();
                    }
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(convertedText);
                } catch (Exception e) {

                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("fail converting: " + e.getMessage());
                }
            } else if (contentType.startsWith("text/") || contentType.equals("application/json")) {
                // text
                String textContent = new String(fileContent, StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(textContent);
            } else {
                String fileName = Paths.get(version.getStoragePath()).getFileName().toString();
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + fileName + "\"")
                        .body(fileContent);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading file: " + e.getMessage());
        }

    }

    private boolean isWordDocument(String contentType, String filePath) {

        return contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || // docx
                contentType.equals("application/msword") || // doc
                filePath.endsWith(".docx") ||
                filePath.endsWith(".doc");
    }
}
