package com.ruipeng.cloudstorage.service;

import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import com.ruipeng.cloudstorage.util.SecurityUtil;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.postgresql.util.PGobject;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FolderS3Service {
    private final FileS3Service fileService;
    private final FilePermissionMapper filePermissionMapper;
    private final FolderMapper folderMapper;
    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final S3StorageService s3StorageService;
    private final User user;

    @Autowired
    public FolderS3Service(FilePermissionMapper filePermissionMapper, FolderMapper folderMapper, FileMapper fileMapper,
                           FileS3Service fileService, FileVersionMapper fileVersionMapper, S3StorageService s3StorageService, User user) {
        this.filePermissionMapper = filePermissionMapper;
        this.folderMapper = folderMapper;
        this.fileMapper = fileMapper;
        this.fileService = fileService;
        this.fileVersionMapper = fileVersionMapper;
        this.s3StorageService = s3StorageService;
        this.user = user;
    }
    public void uploadFolder(MultipartFile[] files, String[] relativePaths, Long userId, Long parentFolderId)
            throws IOException, SQLException, NotFoundException {
        Map<String, Object> uploadInfo = initiateFolderUpload(files, relativePaths, userId, parentFolderId);

    }

    public Map<String, Object> initiateFolderUpload(MultipartFile[] files, String[] relativePaths, Long userId, Long parentFolderId)
            throws IOException, SQLException, NotFoundException {
        if (parentFolderId == null) {
            parentFolderId = initRootFolderForUser(userId);
        }

        Folder parentFolder = folderMapper.findById(parentFolderId);
        if (parentFolder == null) {
            throw new NotFoundException("Parent folder not found");
        }

        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);
        if (rootFolder != null && !rootFolder.getId().equals(parentFolderId)) {
            FilePermission permission = filePermissionMapper.findByFolderIdAndUserId(parentFolderId, userId);
            if (permission == null || permission.getPermission() == PermissionType.READ) {
                throw new AccessDeniedException("No permission to create folder in this location");
            }
        }

        Map<String, Long> pathToFolderIdMap = new HashMap<>();
        pathToFolderIdMap.put("", parentFolderId);


        for (String relativePath : relativePaths) {
            String folderPath = getFolderPath(relativePath);
            if (!folderPath.isEmpty() && !pathToFolderIdMap.containsKey(folderPath)) {
                createFolderStructure(folderPath, pathToFolderIdMap, userId, parentFolderId);
            }
        }


        List<Map<String, Object>> fileUploads = new ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String relativePath = relativePaths[i];
            String folderPath = getFolderPath(relativePath);

            Long targetFolderId = pathToFolderIdMap.get(folderPath);
            if (targetFolderId == null) {
                throw new RuntimeException("Folder does not exist: " + folderPath);
            }

            String fileName = getFileName(relativePath);
            String mimeType = file.getContentType();
            Long fileSize = file.getSize();

            Map<String, Object> uploadInfo = fileService.initiateResumableUpload(
                    userId, targetFolderId, fileName, mimeType, fileSize);


            uploadInfo.put("relativePath", relativePath);
            fileUploads.add(uploadInfo);
        }


        Map<String, Object> result = new HashMap<>();
        result.put("folderStructure", pathToFolderIdMap);
        result.put("fileUploads", fileUploads);
        return result;
    }


    private String getFileName(String relativePath) {
        int lastSeparator = relativePath.lastIndexOf('/');
        return lastSeparator >= 0 ? relativePath.substring(lastSeparator + 1) : relativePath;
    }


    public Map<String, Object> getFolderUploadStatus(List<Long> fileIds) {
        Map<String, Object> status = new HashMap<>();
        List<Map<String, Object>> fileStatuses = new ArrayList<>();

        for (Long fileId : fileIds) {
            Map<String, Object> fileStatus = new HashMap<>();
            FileVersion version = fileVersionMapper.getLatestVersion(fileId);
            File file = fileMapper.getFileById(fileId);

            if (version != null && file != null) {
                fileStatus.put("fileId", fileId);
                fileStatus.put("fileName", file.getName());
                fileStatus.put("size", file.getSize());

                if (version.getUploadId() != null) {
                    List<PartSummary> parts = s3StorageService.listParts(version.getStoragePath(), version.getUploadId());
                    long uploadedBytes = parts.stream().mapToLong(PartSummary::getSize).sum();
                    fileStatus.put("status", "uploading");
                    fileStatus.put("uploadedBytes", uploadedBytes);
                    fileStatus.put("progress", (double) uploadedBytes / file.getSize());
                    fileStatus.put("uploadId", version.getUploadId());
                } else {
                    fileStatus.put("status", "completed");
                    fileStatus.put("progress", 1.0);
                }
            } else {
                fileStatus.put("fileId", fileId);
                fileStatus.put("status", "not_found");
            }

            fileStatuses.add(fileStatus);
        }

        status.put("files", fileStatuses);
        status.put("totalFiles", fileIds.size());
        status.put("completedFiles", fileStatuses.stream().filter(f -> "completed".equals(f.get("status"))).count());

        return status;
    }


    public void completeFolderUpload(List<Map<String, Object>> fileCompletions) {
        for (Map<String, Object> completion : fileCompletions) {
            Long fileId = (Long) completion.get("fileId");
            String uploadId = (String) completion.get("uploadId");
            @SuppressWarnings("unchecked")
            List<PartETag> partETags = (List<PartETag>) completion.get("partETags");

            if (fileId != null && uploadId != null && partETags != null) {
                fileService.completeResumableUpload(fileId, uploadId, partETags);
            }
        }
    }


    public void abortFolderUpload(Long fileId, String uploadId) throws IOException {


        if (fileId != null && uploadId != null) {
            try {
                // 检查文件版本是否存在
                FileVersion version = fileVersionMapper.getLatestVersion(fileId);
                if (version == null || !version.getUploadId().equals(uploadId)) {
                    throw new RuntimeException("Invalid upload ID or file version");
                }

                // 第一步：中止S3分块上传
                try {
                    s3StorageService.abortMultipartUpload(version.getStoragePath(), uploadId);
                    System.out.println("S3 multipart upload aborted successfully");
                } catch (Exception e) {
                    System.err.println("Error aborting S3 multipart upload: " + e.getMessage());
                    throw new RuntimeException("Failed to abort S3 upload: " + e.getMessage(), e);
                }


            } catch (Exception e) {
                System.err.println("Error in abortFolderUpload: " + e.getMessage());
                throw new IOException("Failed to abort folder upload: " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException("fileId and uploadId cannot be null");
        }
    }
    public void uploadFolderSmall(MultipartFile[] files, String[] relativePaths, Long userId, Long parentFolderId)
            throws IOException, SQLException, NotFoundException {
        if (parentFolderId == null) {
            parentFolderId = initRootFolderForUser(userId);
        }

        Folder parentFolder = folderMapper.findById(parentFolderId);
        if (parentFolder == null) {
            throw new NotFoundException("Parent folder not found");
        }

        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);
        if (rootFolder != null && !rootFolder.getId().equals(parentFolderId)) {
            FilePermission permission = filePermissionMapper.findByFolderIdAndUserId(parentFolderId, userId);
            if (permission == null || permission.getPermission() == PermissionType.READ) {
                throw new AccessDeniedException("No permission to create folder in this location");
            }
        }

        Map<String, Long> pathToFolderIdMap = new HashMap<>();
        pathToFolderIdMap.put("", parentFolderId);

        for (String relativePath : relativePaths) {
            String folderPath = getFolderPath(relativePath);
            if (!folderPath.isEmpty() && !pathToFolderIdMap.containsKey(folderPath)) {
                createFolderStructure(folderPath, pathToFolderIdMap, userId, parentFolderId);
            }
        }

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String relativePath = relativePaths[i];
            String folderPath = getFolderPath(relativePath);

            Long targetFolderId = pathToFolderIdMap.get(folderPath);
            if (targetFolderId == null) {
                throw new RuntimeException("Folder does not exist: " + folderPath);
            }

            fileService.uploadFile(file, userId, targetFolderId);
        }
    }


    public Long getRootFolderId(Long userId) throws SQLException {
        return initRootFolderForUser(userId);
    }

    private long initRootFolderForUser(Long userId) throws SQLException {
        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);
        if (rootFolder == null) {
            synchronized (this) {
                rootFolder = folderMapper.findRootFolderByUserId(userId);
                if (rootFolder == null) {
                    Folder newRootFolder = new Folder();
                    newRootFolder.setOwnerId(userId);
                    newRootFolder.setName("Root");
                    newRootFolder.setParentId(null);

                    PGobject ltreePath = new PGobject();
                    ltreePath.setType("ltree");
                    ltreePath.setValue("temp");
                    newRootFolder.setPath(ltreePath);

                    newRootFolder.setCreatedAt(Instant.now());
                    newRootFolder.setUpdatedAt(Instant.now());
                    newRootFolder.setDeleted(false);

                    int rows = folderMapper.insert(newRootFolder);
                    if (rows <= 0) {
                        throw new SQLException("Failed to insert folder");
                    }

                    Folder insertedFolder = folderMapper.findRootFolderByUserId(userId);
                    ltreePath.setValue(insertedFolder.getId().toString());
                    folderMapper.updatePath(insertedFolder.getId(), ltreePath, Instant.now());
                    rootFolder = insertedFolder;

                    FilePermission permission = new FilePermission();
                    permission.setFolderId(insertedFolder.getId());
                    permission.setUserId(userId);
                    permission.setPermission(PermissionType.ADMIN);
                    permission.setCreatedAt(Instant.now());
                    permission.setCreatedBy(userId);
                    filePermissionMapper.insert(permission);
                }
            }
        }
        return rootFolder.getId();
    }

    public Folder createFolder(String name, Long parentId, Long userId) throws NotFoundException, SQLException, AccessDeniedException {
        if (parentId == null) {
            parentId = initRootFolderForUser(userId);
        }

        Folder parentFolder = folderMapper.findById(parentId);
        if (parentFolder == null) {
            throw new NotFoundException("Parent folder not found");
        }

        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);
        if (rootFolder != null && !rootFolder.getId().equals(parentId)) {
            FilePermission permission = filePermissionMapper.findByFolderIdAndUserId(parentId, userId);
            if (permission == null || permission.getPermission() == PermissionType.READ) {
                throw new AccessDeniedException("No permission to create folder in this location");
            }
        }

        Folder folder = new Folder();
        folder.setName(name);
        folder.setParentId(parentId);
        folder.setOwnerId(userId);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());
        folder.setDeleted(false);

        PGobject tempPath = new PGobject();
        tempPath.setType("ltree");
        tempPath.setValue("temp");
        folder.setPath(tempPath);

        int rows = folderMapper.insert(folder);
        if (rows <= 0) {
            throw new SQLException("Failed to insert folder");
        }
        long folderId = folder.getId();

        PGobject ltreePath = new PGobject();
        ltreePath.setType("ltree");
        String parentPathValue = parentFolder.getPath().getValue();
        String newPathValue = rootFolder != null && rootFolder.getId().equals(parentId)
                ? String.valueOf(folderId)
                : parentPathValue + "." + folderId;
        ltreePath.setValue(newPathValue);
        folderMapper.updatePath(folderId, ltreePath, Instant.now());
        folder.setPath(ltreePath);

        FilePermission permission = new FilePermission();
        permission.setFolderId(folderId);
        permission.setUserId(userId);
        permission.setPermission(PermissionType.ADMIN);
        permission.setCreatedAt(Instant.now());
        permission.setCreatedBy(userId);
        filePermissionMapper.insert(permission);

        return folder;
    }

    public List<Folder> getFolders(long userId, long parentId) throws SQLException {
        if (parentId == 0) {
            parentId = initRootFolderForUser(userId);
        }
        return folderMapper.getFoldersByUserIdAndFolderId(userId, parentId);
    }

    public int softDeleteFolder(Long folderId, Long userId) {
        Folder folder = folderMapper.findById(folderId);
        if (folder == null) {
            throw new RuntimeException("Folder does not exist");
        }

        try {
            List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);
            List<Long> folderIds = subFolders.stream().map(Folder::getId).collect(Collectors.toList());
            folderIds.add(folderId);

            List<File> allFiles = fileMapper.findFilesByFolderIds(folderIds, folderId);
            if (allFiles != null) {
                for (File file : allFiles) {
                    fileService.softDeleteFile(file.getId(), userId);
                }
            }

            folder.setDeleted(true);
            folder.setUpdatedAt(Instant.now());
            for (Folder subFolder : subFolders) {
                subFolder.setDeleted(true);
                subFolder.setUpdatedAt(Instant.now());
                folderMapper.updateFolderDeleteStatus(subFolder);
            }
            return folderMapper.updateFolderDeleteStatus(folder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to soft delete folder: " + e.getMessage());
        }
    }

    public int restoreFolder(Long folderId, Long userId) {
        Folder folder = folderMapper.findDeletedById(folderId);
        if (folder == null) {
            throw new RuntimeException("Folder does not exist");
        }
        if (!folder.isDeleted()) {
            throw new RuntimeException("Folder is not deleted");
        }

        try {
            List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);
            List<File> allFiles = fileMapper.findFilesByFolderIds(
                    subFolders.stream().map(Folder::getId).collect(Collectors.toList()),
                    folderId
            );

            for (File file : allFiles) {
                fileService.restoreFile(file.getId(), userId);
            }

            for (Folder subFolder : subFolders) {
                subFolder.setDeleted(false);
                subFolder.setUpdatedAt(Instant.now());
                folderMapper.updateFolderDeleteStatus(subFolder);
            }

            folder.setDeleted(false);
            folder.setUpdatedAt(Instant.now());
            return folderMapper.updateFolderDeleteStatus(folder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to restore folder: " + e.getMessage());
        }
    }

    public int deleteFolder(Long folderId, Long userId) throws IOException {
        Folder folder = folderMapper.findDeletedById(folderId);
        if (folder == null) {
            throw new RuntimeException("Folder does not exist");
        }

        try {
            List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);
            subFolders.add(folderMapper.getFolderByFolderId(folderId));
            List<File> allFiles = fileMapper.findFilesByFolderIds(
                    subFolders.stream().map(Folder::getId).collect(Collectors.toList()),
                    folderId
            );

            for (File file : allFiles) {
                fileService.deleteFile(file.getId(), userId);
            }

            Collections.reverse(subFolders);
            for (Folder subFolder : subFolders) {
                if (subFolder.isDeleted()) {
                    filePermissionMapper.deleteByFolderId(subFolder.getId());
                    folderMapper.deleteFolder(subFolder.getId());
                }
            }

            if (folder.isDeleted()) {
                filePermissionMapper.deleteByFolderId(folderId);
                return folderMapper.deleteFolder(folderId);
            } else {
                throw new RuntimeException("Folder must be soft deleted first");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete folder: " + e.getMessage());
        }
    }

    public DownloadFileInfo downloadFolder(Long folderId, Long userId) throws IOException {
        Folder folder = folderMapper.findById(folderId);
        if (folder == null) {
            throw new RuntimeException("folder does not exist");
        }

        List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);

        List<File> allFiles = fileMapper.findFilesByFolderIds(
                subFolders.stream()
                        .map(Folder::getId)
                        .collect(Collectors.toList()),
                folderId
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // 处理当前文件夹中的文件
            for (File file : allFiles) {
                FileVersion latestVersion = fileVersionMapper.getLatestVersion(file.getId());
                if (latestVersion != null) {
                    DownloadFileInfo fileInfo = fileService.downloadFile(file.getId());
                    byte[] fileContent = ((ByteArrayResource) fileInfo.getResource()).getByteArray();
                    String entryName = generateUniqueEntryName(folder.getName(), file, ""); // 使用文件夹名称
                    ZipEntry entry = new ZipEntry(entryName);
                    zos.putNextEntry(entry);
                    zos.write(fileContent);
                    zos.closeEntry();
                }
            }

            // 处理子文件夹及其内容
            for (Folder subFolder : subFolders) {
               System.out.println(subFolder.getName());
                processSubFolder(subFolder, zos, folder.getName()); // 传递当前文件夹名称
            }
        }

        Resource resource = new ByteArrayResource(baos.toByteArray());
        return new DownloadFileInfo(
                folder.getName() + ".zip",
                "application/zip",
                resource
        );
    }
    private void processSubFolder(Folder folder, ZipOutputStream zos, String parentPath) throws IOException {
        String currentPath = parentPath.isEmpty() ? folder.getName() : parentPath + "/" + folder.getName();

        List<File> files = fileMapper.getFilesByFolderId(folder.getId());
        for (File file : files) {
            FileVersion latestVersion = fileVersionMapper.getLatestVersion(file.getId());
            if (latestVersion != null) {
                DownloadFileInfo fileInfo = fileService.downloadFile(file.getId());
                byte[] fileContent = ((ByteArrayResource) fileInfo.getResource()).getByteArray();
                String entryName = generateUniqueEntryName(currentPath, file, ""); // 使用当前路径
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(fileContent);
                zos.closeEntry();
            }
        }

        // 递归处理子文件夹
        List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folder.getId()); // 使用 parent_id
        for (Folder subFolder : subFolders) {
            processSubFolder(subFolder, zos, currentPath);
        }
    }
    private String generateUniqueEntryName(String parentPath, File file, String baseName) {
        String fileName = file.getName(); // 假设 File 对象有 getName() 方法

        // 构建完整的路径
        String fullPath = parentPath.isEmpty() ? fileName : parentPath + "/" + fileName;

        // 确保名称唯一（如果有重复，添加后缀）
        int counter = 1;
        String uniqueName = fullPath;
        while (usedPaths.contains(uniqueName)) {
            String base = fileName.substring(0, fileName.lastIndexOf("."));
            String extension = fileName.substring(fileName.lastIndexOf("."));
            uniqueName = parentPath.isEmpty() ?
                    (base + "(" + counter + ")" + extension) :
                    (parentPath + "/" + base + "(" + counter + ")" + extension);
            counter++;
        }
        usedPaths.add(uniqueName);

        return uniqueName;
    }

    // 全局 Set 用于跟踪已使用的路径
    private final Set<String> usedPaths = Collections.synchronizedSet(new HashSet<>());


    private String getFolderPath(String relativePath) {
        int lastSeparator = relativePath.lastIndexOf('/');
        return lastSeparator > 0 ? relativePath.substring(0, lastSeparator) : "";
    }

    private void createFolderStructure(String folderPath, Map<String, Long> pathToFolderIdMap, Long userId, Long parentFolderId)
            throws SQLException, NotFoundException, AccessDeniedException {
        String[] folders = folderPath.split("/");
        StringBuilder currentPath = new StringBuilder();
        Long currentParentId = parentFolderId;

        for (String folder : folders) {
            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            }
            currentPath.append(folder);

            String currentPathStr = currentPath.toString();
            if (!pathToFolderIdMap.containsKey(currentPathStr)) {
                Folder newFolder = createFolder(folder, currentParentId, userId);
                pathToFolderIdMap.put(currentPathStr, newFolder.getId());
                currentParentId = newFolder.getId();
            } else {
                currentParentId = pathToFolderIdMap.get(currentPathStr);
            }
        }
    }

    public List<Folder> getAllDeletedFolders(Long userId, Long parentId) throws SQLException {
        if (parentId == null) {
            parentId = initRootFolderForUser(userId);
        }
        return folderMapper.getDeletedFoldersByUserIdAndFolderId(userId, parentId);
    }
}