package com.ruipeng.cloudstorage.service;

import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.ruipeng.cloudstorage.dto.DownloadFileInfo;
import com.ruipeng.cloudstorage.dto.response.FolderResponse;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.ibatis.javassist.NotFoundException;

/**
 * Service for managing folder operations.
 *
 * Responsibilities:
 * - Folder CRUD operations
 * - Folder hierarchy management
 * - Folder download (as ZIP)
 */
@Service
public class FolderS3Service {
    private static final Logger log = LoggerFactory.getLogger(FolderS3Service.class);

    private final FolderMapper folderMapper;
    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FilePermissionMapper filePermissionMapper;
    private final FilePermissionService permissionService;
    private final FileS3Service fileService;
    private final ResumableUploadService resumableUploadService;
    private final S3StorageService storageService;
    private final FolderHelperService folderHelperService;

    public FolderS3Service(FolderMapper folderMapper,
                         FileMapper fileMapper,
                         FileVersionMapper fileVersionMapper,
                         FilePermissionMapper filePermissionMapper,
                         FilePermissionService permissionService,
                         FileS3Service fileService,
                           ResumableUploadService resumableUploadService,
                           S3StorageService storageService,
                           FolderHelperService folderHelperService) {
        this.folderMapper = folderMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.fileMapper = fileMapper;
        this.filePermissionMapper = filePermissionMapper;
        this.permissionService = permissionService;
        this.fileService = fileService;
        this.resumableUploadService = resumableUploadService;
        this.storageService=storageService;
        this.folderHelperService=folderHelperService;
    }

    /**
     * Gets the root folder ID for a user, creating it if necessary.
     */
    public Long getRootFolderId(Long userId) {
        return folderHelperService.getRootFolderId(userId);
    }

    /**
     * Gets root folder information including subfolders.
     */
    public FolderResponse getRootFolderInfo(Long userId) {
        Long rootFolderId = folderHelperService.getRootFolderId(userId);
        List<Folder> folders = getFoldersByParent(userId, rootFolderId);

        return FolderResponse.builder()
                .rootFolderId(rootFolderId)
                .folders(folders)
                .build();
    }

    /**
     * Gets all folders under a parent folder.
     */
    public List<Folder> getFoldersByParent(Long userId, Long parentId) {
        Long actualParentId = parentId == 0 ? folderHelperService.getRootFolderId(userId) : parentId;
        return folderMapper.getFoldersByUserIdAndFolderId(userId, actualParentId);
    }

    /**
     * Creates a new folder.
     */
    @Transactional
    public Folder createFolder(String name, Long parentId, Long userId) {
        Long actualParentId = parentId == null ? folderHelperService.getRootFolderId(userId) : parentId;

        Folder parentFolder = findFolderById(actualParentId);
        validateFolderPermission(actualParentId, userId);

        Folder newFolder = buildNewFolder(name, actualParentId, userId);
        insertFolder(newFolder);
        updateFolderPath(newFolder, parentFolder, userId);
        grantFolderPermission(newFolder.getId(), userId);

        log.info("Folder created: id={}, name={}, parentId={}",
                newFolder.getId(), name, actualParentId);
        return newFolder;
    }




    /**
     * Downloads a folder and its contents as a ZIP file.
     */
    public DownloadFileInfo downloadFolder(Long folderId, Long userId) {
        Folder folder = findFolderById(folderId);

        try {
            byte[] zipData = createZipArchive(folder);
            Resource resource = new ByteArrayResource(zipData);

            return new DownloadFileInfo(
                    folder.getName() + ".zip",
                    "application/zip",
                    resource
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to create ZIP archive", e);
        }
    }

    /**
     * Gets all deleted folders for a user.
     */
    public List<Folder> getAllDeletedFolders(Long userId, Long parentId) {
        Long actualParentId = parentId == null ? folderHelperService.getRootFolderId(userId) : parentId;  // 改这里
        return folderMapper.getDeletedFoldersByUserIdAndFolderId(userId, actualParentId);
    }


    @Transactional
    protected synchronized Long initializeRootFolder(Long userId) {
        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);

        if (rootFolder == null) {
            rootFolder = createRootFolder(userId);
        }

        return rootFolder.getId();
    }

    private Folder createRootFolder(Long userId) {
        try {
            Folder rootFolder = buildRootFolder(userId);
            insertFolder(rootFolder);

            Folder inserted = folderMapper.findRootFolderByUserId(userId);
            updateRootFolderPath(inserted);
            grantFolderPermission(inserted.getId(), userId);

            return inserted;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create root folder", e);
        }
    }

    private Folder buildRootFolder(Long userId) throws SQLException {
        Folder folder = new Folder();
        folder.setOwnerId(userId);
        folder.setName("Root");
        folder.setParentId(null);
        folder.setPath(createTempPath());
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());
        folder.setDeleted(false);
        return folder;
    }

    private Folder buildNewFolder(String name, Long parentId, Long userId) {
        Folder folder = new Folder();
        folder.setName(name);
        folder.setParentId(parentId);
        folder.setOwnerId(userId);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());
        folder.setDeleted(false);

        try {
            folder.setPath(createTempPath());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create folder path", e);
        }

        return folder;
    }

    private PGobject createTempPath() throws SQLException {
        PGobject path = new PGobject();
        path.setType("ltree");
        path.setValue("temp");
        return path;
    }

    private void insertFolder(Folder folder) {
        int rows = folderMapper.insert(folder);
        if (rows <= 0) {
            throw new RuntimeException("Failed to insert folder");
        }
    }

    private void updateFolderPath(Folder folder, Folder parent, Long userId) {
        try {
            PGobject path = buildFolderPath(folder, parent, userId);
            folderMapper.updatePath(folder.getId(), path, Instant.now());
            folder.setPath(path);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update folder path", e);
        }
    }

    private void updateRootFolderPath(Folder rootFolder) {
        try {
            PGobject path = new PGobject();
            path.setType("ltree");
            path.setValue(rootFolder.getId().toString());
            folderMapper.updatePath(rootFolder.getId(), path, Instant.now());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update root folder path", e);
        }
    }

    private PGobject buildFolderPath(Folder folder, Folder parent, Long userId)
            throws SQLException {
        PGobject path = new PGobject();
        path.setType("ltree");

        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);
        String parentPathValue = parent.getPath().getValue();

        String newPathValue = (rootFolder != null && rootFolder.getId().equals(parent.getId()))
                ? String.valueOf(folder.getId())
                : parentPathValue + "." + folder.getId();

        path.setValue(newPathValue);
        return path;
    }

    private void grantFolderPermission(Long folderId, Long userId) {
        permissionService.grantFolderPermission(
                folderId, userId, PermissionType.ADMIN, userId
        );
    }

    private Folder findFolderById(Long folderId) {
        Folder folder = folderMapper.findById(folderId);
        if (folder == null) {
            throw new ResourceNotFoundException("Folder", folderId);
        }
        return folder;
    }

    private Folder findDeletedFolderById(Long folderId) {
        Folder folder = folderMapper.findDeletedById(folderId);
        if (folder == null) {
            throw new ResourceNotFoundException("Deleted folder", folderId);
        }
        return folder;
    }

    private void validateFolderPermission(Long folderId, Long userId) {
        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);

        if (rootFolder != null && !rootFolder.getId().equals(folderId)) {
            if (!permissionService.hasFolderPermission(folderId, userId, PermissionType.WRITE)) {
                throw new SecurityException("No permission to create folder here");
            }
        }
    }

    private List<Folder> findAllSubFolders(Folder folder) {
        return folderMapper.findSubFolders(folder.getPath(), folder.getId());
    }

    private List<Long> collectFolderIds(List<Folder> subFolders, Long parentId) {
        List<Long> folderIds = subFolders.stream()
                .map(Folder::getId)
                .collect(Collectors.toList());
        folderIds.add(parentId);
        return folderIds;
    }

    private void softDeleteFilesInFolders(List<Long> folderIds, Long userId) {
        List<File> files = fileMapper.findFilesByFolderIds(folderIds, folderIds.get(0));
        for (File file : files) {
            fileService.softDeleteFile(file.getId(), userId);
        }
    }

    private void softDeleteFolderHierarchy(Folder folder, List<Folder> subFolders) {
        markFolderAsDeleted(folder);

        for (Folder subFolder : subFolders) {
            markFolderAsDeleted(subFolder);
        }
    }

    private void markFolderAsDeleted(Folder folder) {
        folder.setDeleted(true);
        folder.setUpdatedAt(Instant.now());
        folderMapper.updateFolderDeleteStatus(folder);
    }

    private void restoreFilesInFolders(List<Folder> subFolders, Long folderId, Long userId) {
        List<Long> folderIds = subFolders.stream()
                .map(Folder::getId)
                .collect(Collectors.toList());

        List<File> files = fileMapper.findFilesByFolderIds(folderIds, folderId);
        for (File file : files) {
            fileService.restoreFile(file.getId(), userId);
        }
    }

    private void restoreFolderHierarchy(Folder folder, List<Folder> subFolders) {
        for (Folder subFolder : subFolders) {
            markFolderAsRestored(subFolder);
        }
        markFolderAsRestored(folder);
    }

    private void markFolderAsRestored(Folder folder) {
        folder.setDeleted(false);
        folder.setUpdatedAt(Instant.now());
        folderMapper.updateFolderDeleteStatus(folder);
    }

    private void deleteFilesInFolders(List<Folder> folders, Long folderId, Long userId) throws IOException {
        List<Long> folderIds = folders.stream()
                .map(Folder::getId)
                .collect(Collectors.toList());

        List<File> files = fileMapper.findFilesByFolderIds(folderIds, folderId);
        for (File file : files) {
            fileService.deleteFile(file.getId(), userId);
        }
    }

    private void deleteFolderHierarchy(List<Folder> subFolders, Long folderId) {
        Collections.reverse(subFolders);

        for (Folder subFolder : subFolders) {
            if (subFolder.isDeleted()) {
                filePermissionMapper.deleteByFolderId(subFolder.getId());
                folderMapper.deleteFolder(subFolder.getId());
            }
        }
    }

    private byte[] createZipArchive(Folder folder) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Set<String> usedPaths = Collections.synchronizedSet(new HashSet<>());

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            addFolderToZip(folder, zos, folder.getName(), usedPaths);
        }

        return baos.toByteArray();
    }

    private void addFolderToZip(Folder folder, ZipOutputStream zos,
                                String currentPath, Set<String> usedPaths)
            throws IOException {
        addFilesToZip(folder, zos, currentPath, usedPaths);
        addSubFoldersToZip(folder, zos, currentPath, usedPaths);
    }

    private void addFilesToZip(Folder folder, ZipOutputStream zos,
                               String currentPath, Set<String> usedPaths)
            throws IOException {
        List<File> files = fileMapper.getFilesByFolderId(folder.getId());

        for (File file : files) {
            addFileToZip(file, zos, currentPath, usedPaths);
        }
    }

    private void addFileToZip(File file, ZipOutputStream zos,
                              String currentPath, Set<String> usedPaths)
            throws IOException {
        DownloadFileInfo fileInfo = fileService.downloadFile(file.getId());
        byte[] content = ((ByteArrayResource) fileInfo.getResource()).getByteArray();

        String entryName = generateUniqueEntryName(currentPath, file.getName(), usedPaths);

        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }

    private void addSubFoldersToZip(Folder folder, ZipOutputStream zos,
                                    String currentPath, Set<String> usedPaths)
            throws IOException {
        List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folder.getId());

        for (Folder subFolder : subFolders) {
            String newPath = currentPath + "/" + subFolder.getName();
            addFolderToZip(subFolder, zos, newPath, usedPaths);
        }
    }

    private String generateUniqueEntryName(String parentPath, String fileName,
                                           Set<String> usedPaths) {
        String fullPath = parentPath.isEmpty() ? fileName : parentPath + "/" + fileName;

        String uniqueName = fullPath;
        int counter = 1;

        while (usedPaths.contains(uniqueName)) {
            uniqueName = generateNumberedFileName(parentPath, fileName, counter);
            counter++;
        }

        usedPaths.add(uniqueName);
        return uniqueName;
    }

    private String generateNumberedFileName(String parentPath, String fileName, int counter) {
        int lastDotIndex = fileName.lastIndexOf(".");

        if (lastDotIndex > 0) {
            String base = fileName.substring(0, lastDotIndex);
            String extension = fileName.substring(lastDotIndex);
            String numbered = base + "(" + counter + ")" + extension;
            return parentPath.isEmpty() ? numbered : parentPath + "/" + numbered;
        }

        String numbered = fileName + "(" + counter + ")";
        return parentPath.isEmpty() ? numbered : parentPath + "/" + numbered;
    }
    /**
     * Soft deletes a folder and returns affected rows.
     *
     * @param folderId the folder ID
     * @param userId the user ID
     * @return number of affected rows
     */
    public int softDeleteFolder(Long folderId, Long userId) throws IOException {
        Folder folder = findFolderById(folderId);

        List<Folder> subFolders = findAllSubFolders(folder);
        List<Long> folderIds = collectFolderIds(subFolders, folderId);

        softDeleteFilesInFolders(folderIds, userId);
        softDeleteFolderHierarchy(folder, subFolders);

        log.info("Folder soft deleted: id={}", folderId);
        return 1; // Return success indicator
    }

    /**
     * Permanently deletes a folder and returns affected rows.
     *
     * @param folderId the folder ID
     * @param userId the user ID
     * @return number of affected rows
     */
    public int deleteFolder(Long folderId, Long userId) throws IOException {
        Folder folder = findDeletedFolderById(folderId);

        List<Folder> subFolders = findAllSubFolders(folder);
        subFolders.add(folder);

        deleteFilesInFolders(subFolders, folderId, userId);
        deleteFolderHierarchy(subFolders, folderId);

        log.info("Folder permanently deleted: id={}", folderId);
        return 1;
    }

    /**
     * Restores a deleted folder and returns affected rows.
     *
     * @param folderId the folder ID
     * @param userId the user ID
     * @return number of affected rows
     */
    public int restoreFolder(Long folderId, Long userId) {
        Folder folder = findDeletedFolderById(folderId);

        List<Folder> subFolders = findAllSubFolders(folder);
        restoreFilesInFolders(subFolders, folderId, userId);
        restoreFolderHierarchy(folder, subFolders);

        log.info("Folder restored: id={}", folderId);
        return 1;
    }

    // Add these methods to FolderService.java

    /**
     * Uploads a small folder synchronously.
     * Creates folder structure and uploads all files directly.
     *
     * @param files the files to upload
     * @param relativePaths the relative paths of files
     * @param userId the user ID
     * @param parentFolderId the parent folder ID
     * @throws IOException if file upload fails
     * @throws SQLException if database operation fails
     * @throws NotFoundException if parent folder not found
     */
    public void uploadFolderSmall(MultipartFile[] files, String[] relativePaths,
                                  Long userId, Long parentFolderId)
            throws IOException, SQLException, NotFoundException {
        Long actualParentId = ensureParentFolder(userId, parentFolderId);

        validateParentFolder(actualParentId);
        validateFolderPermission(actualParentId, userId);

        Map<String, Long> pathToFolderMap = createFolderStructure(relativePaths, userId, actualParentId);

        uploadFilesToFolders(files, relativePaths, pathToFolderMap, userId);

        log.info("Small folder uploaded successfully: {} files, userId={}", files.length, userId);
    }

    /**
     * Initiates a folder upload for large folders (multipart).
     * Creates folder structure and initiates multipart uploads for all files.
     *
     * @param files the files metadata
     * @param relativePaths the relative paths
     * @param userId the user ID
     * @param parentFolderId the parent folder ID
     * @return upload initialization information
     * @throws IOException if initialization fails
     * @throws SQLException if database operation fails
     * @throws NotFoundException if parent folder not found
     */
    public Map<String, Object> initiateFolderUpload(MultipartFile[] files,
                                                    String[] relativePaths,
                                                    Long userId,
                                                    Long parentFolderId)
            throws IOException, SQLException, NotFoundException {
        Long actualParentId = ensureParentFolder(userId, parentFolderId);

        validateParentFolder(actualParentId);
        validateFolderPermission(actualParentId, userId);

        Map<String, Long> pathToFolderMap = createFolderStructure(relativePaths, userId, actualParentId);

        List<Map<String, Object>> fileUploads = initiateFileUploads(
                files, relativePaths, pathToFolderMap, userId
        );

        return buildUploadInitResponse(pathToFolderMap, fileUploads);
    }



    /**
     * Completes a folder upload by completing all file uploads.
     *
     * @param fileCompletions list of file completion information
     */
    public void completeFolderUpload(List<Map<String, Object>> fileCompletions) {
        for (Map<String, Object> completion : fileCompletions) {
            completeFileUpload(completion);
        }

        log.info("Folder upload completed: {} files", fileCompletions.size());
    }

    /**
     * Aborts a folder upload by aborting a single file's multipart upload.
     *
     * @param fileId the file ID
     * @param uploadId the upload ID
     * @throws IOException if abort operation fails
     */
    public void abortFolderUpload(Long fileId, String uploadId) throws IOException {
        validateFileAndUploadId(fileId, uploadId);

        FileVersion version = getVersionByFileAndUpload(fileId, uploadId);

        abortMultipartUpload(version.getStoragePath(), uploadId);

        log.info("Folder upload aborted: fileId={}, uploadId={}", fileId, uploadId);
    }

// ============ Private Helper Methods ============

    /**
     * Ensures parent folder exists, creates root if needed.
     */
    private Long ensureParentFolder(Long userId, Long parentFolderId) {
        if (parentFolderId == null) {
            return folderHelperService.getRootFolderId(userId);
        }
        return parentFolderId;
    }

    /**
     * Validates parent folder exists.
     */
    private void validateParentFolder(Long parentFolderId) throws NotFoundException {
        Folder parentFolder = folderMapper.findById(parentFolderId);
        if (parentFolder == null) {
            throw new NotFoundException("Parent folder not found");
        }
    }


    /**
     * Creates folder structure from relative paths.
     *
     * @return map of path to folder ID
     */
    private Map<String, Long> createFolderStructure(String[] relativePaths, Long userId, Long parentId)
            throws SQLException, NotFoundException {
        Map<String, Long> pathToFolderMap = new HashMap<>();
        pathToFolderMap.put("", parentId);

        for (String relativePath : relativePaths) {
            String folderPath = extractFolderPath(relativePath);
            if (!folderPath.isEmpty() && !pathToFolderMap.containsKey(folderPath)) {
                createNestedFolders(folderPath, pathToFolderMap, userId, parentId);
            }
        }

        return pathToFolderMap;
    }

    /**
     * Extracts folder path from relative path.
     */
    private String extractFolderPath(String relativePath) {
        int lastSeparator = relativePath.lastIndexOf('/');
        return lastSeparator > 0 ? relativePath.substring(0, lastSeparator) : "";
    }

    /**
     * Creates nested folder structure.
     */
    private void createNestedFolders(String folderPath, Map<String, Long> pathToFolderMap,
                                     Long userId, Long parentId)
            throws SQLException, NotFoundException {
        String[] folders = folderPath.split("/");
        StringBuilder currentPath = new StringBuilder();
        Long currentParentId = parentId;

        for (String folder : folders) {
            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            }
            currentPath.append(folder);

            String currentPathStr = currentPath.toString();
            if (!pathToFolderMap.containsKey(currentPathStr)) {
                Folder newFolder = createFolder(folder, currentParentId, userId);
                pathToFolderMap.put(currentPathStr, newFolder.getId());
                currentParentId = newFolder.getId();
            } else {
                currentParentId = pathToFolderMap.get(currentPathStr);
            }
        }
    }

    /**
     * Uploads all files to their target folders.
     */
    private void uploadFilesToFolders(MultipartFile[] files, String[] relativePaths,
                                      Map<String, Long> pathToFolderMap, Long userId)
            throws IOException {
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String relativePath = relativePaths[i];

            String folderPath = extractFolderPath(relativePath);
            Long targetFolderId = pathToFolderMap.get(folderPath);

            if (targetFolderId == null) {
                throw new RuntimeException("Target folder not found: " + folderPath);
            }

            fileService.uploadFile(file, userId, targetFolderId);
        }
    }

    /**
     * Initiates multipart uploads for all files.
     */
    private List<Map<String, Object>> initiateFileUploads(MultipartFile[] files,
                                                          String[] relativePaths,
                                                          Map<String, Long> pathToFolderMap,
                                                          Long userId)
            throws IOException {
        List<Map<String, Object>> fileUploads = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            String relativePath = relativePaths[i];

            String folderPath = extractFolderPath(relativePath);
            Long targetFolderId = pathToFolderMap.get(folderPath);

            if (targetFolderId == null) {
                throw new RuntimeException("Target folder not found: " + folderPath);
            }

            String fileName = extractFileName(relativePath);
            Map<String, Object> uploadInfo = resumableUploadService.initiateUpload(
                    userId, targetFolderId, fileName, file.getContentType(), file.getSize()
            );

            uploadInfo.put("relativePath", relativePath);
            fileUploads.add(uploadInfo);
        }

        return fileUploads;
    }

    /**
     * Extracts file name from relative path.
     */
    private String extractFileName(String relativePath) {
        int lastSeparator = relativePath.lastIndexOf('/');
        return lastSeparator >= 0 ? relativePath.substring(lastSeparator + 1) : relativePath;
    }

    /**
     * Builds upload initialization response.
     */
    private Map<String, Object> buildUploadInitResponse(Map<String, Long> pathToFolderMap,
                                                        List<Map<String, Object>> fileUploads) {
        Map<String, Object> result = new HashMap<>();
        result.put("folderStructure", pathToFolderMap);
        result.put("fileUploads", fileUploads);
        return result;
    }

    /**
     * Gets upload status for a single file.
     */
    private Map<String, Object> getFileUploadStatus(Long fileId) {
        Map<String, Object> fileStatus = new HashMap<>();

        try {
            FileVersion version = fileVersionMapper.getLatestVersion(fileId);
            File file = fileMapper.getFileById(fileId);

            if (version != null && file != null) {
                populateFileStatus(fileStatus, version, file);
            } else {
                fileStatus.put("fileId", fileId);
                fileStatus.put("status", "not_found");
            }
        } catch (Exception e) {
            log.warn("Error getting upload status for fileId={}", fileId, e);
            fileStatus.put("fileId", fileId);
            fileStatus.put("status", "error");
            fileStatus.put("error", e.getMessage());
        }

        return fileStatus;
    }

    /**
     * Populates file status information.
     */
    private void populateFileStatus(Map<String, Object> fileStatus, FileVersion version, File file) {
        fileStatus.put("fileId", file.getId());
        fileStatus.put("fileName", file.getName());
        fileStatus.put("size", file.getSize());

        if (version.getUploadId() != null) {
            List<PartSummary> parts = storageService.listUploadedParts(
                    version.getStoragePath(),
                    version.getUploadId()
            );
            long uploadedBytes = calculateUploadedBytes(parts);

            fileStatus.put("status", "uploading");
            fileStatus.put("uploadedBytes", uploadedBytes);
            fileStatus.put("progress", calculateProgress(uploadedBytes, file.getSize()));
            fileStatus.put("uploadId", version.getUploadId());
        } else {
            fileStatus.put("status", "completed");
            fileStatus.put("progress", 1.0);
        }
    }

    /**
     * Calculates total uploaded bytes from parts.
     */
    private long calculateUploadedBytes(List<PartSummary> parts) {
        return parts.stream().mapToLong(PartSummary::getSize).sum();
    }

    /**
     * Calculates upload progress.
     */
    private double calculateProgress(long uploadedBytes, long totalBytes) {
        if (totalBytes == 0) {
            return 0.0;
        }
        return (double) uploadedBytes / totalBytes;
    }

    /**
     * Builds status response for all files.
     */
    private Map<String, Object> buildStatusResponse(List<Map<String, Object>> fileStatuses, int totalFiles) {
        Map<String, Object> status = new HashMap<>();
        status.put("files", fileStatuses);
        status.put("totalFiles", totalFiles);
        status.put("completedFiles", countCompletedFiles(fileStatuses));
        return status;
    }

    /**
     * Counts completed files.
     */
    private long countCompletedFiles(List<Map<String, Object>> fileStatuses) {
        return fileStatuses.stream()
                .filter(f -> "completed".equals(f.get("status")))
                .count();
    }

    /**
     * Completes a single file upload.
     */
    private void completeFileUpload(Map<String, Object> completion) {
        Long fileId = getLongValue(completion, "fileId");
        String uploadId = getStringValue(completion, "uploadId");
        List<PartETag> partETags = extractPartETags(completion);

        if (fileId != null && uploadId != null && partETags != null) {
            resumableUploadService.completeUpload(fileId, uploadId, partETags);
        }
    }

    /**
     * Extracts Long value from map.
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        return null;
    }

    /**
     * Extracts String value from map.
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Extracts PartETag list from completion map.
     */
    @SuppressWarnings("unchecked")
    private List<PartETag> extractPartETags(Map<String, Object> completion) {
        Object partETagsObj = completion.get("partETags");
        if (partETagsObj instanceof List) {
            return (List<PartETag>) partETagsObj;
        }
        return null;
    }

    /**
     * Validates file ID and upload ID.
     */
    private void validateFileAndUploadId(Long fileId, String uploadId) {
        if (fileId == null || uploadId == null) {
            throw new IllegalArgumentException("fileId and uploadId cannot be null");
        }
    }

    /**
     * Gets file version by file ID and upload ID.
     */
    private FileVersion getVersionByFileAndUpload(Long fileId, String uploadId) {
        FileVersion version = fileVersionMapper.getLatestVersion(fileId);

        if (version == null || version.getUploadId() == null) {
            throw new ResourceNotFoundException("File version not found for fileId: " + fileId);
        }

        if (!version.getUploadId().equals(uploadId)) {
            throw new IllegalArgumentException("Invalid upload ID for file: " + fileId);
        }

        return version;
    }

    /**
     * Aborts multipart upload in storage.
     */
    private void abortMultipartUpload(String storagePath, String uploadId) {
        storageService.abortMultipartUpload(storagePath, uploadId);
    }

    /**
     * Gets the upload status for multiple files.
     *
     * @param fileIds list of file IDs
     * @return upload status information
     */

    public Map<String, Object> getFolderUploadStatus(List<Long> fileIds) {
        List<Map<String, Object>> fileStatuses = new ArrayList<>();

        for (Long fileId : fileIds) {
            Map<String, Object> fileStatus = getFileUploadStatus(fileId);
            fileStatuses.add(fileStatus);
        }

        return buildStatusResponse(fileStatuses, fileIds.size());
    }

}