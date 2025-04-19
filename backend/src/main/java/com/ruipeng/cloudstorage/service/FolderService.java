package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.mappers.FolderMapper;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.postgresql.util.PGobject;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FolderService {
    private  FileService fileService;
    private FilePermissionMapper filePermissionMapper;
    private FolderMapper folderMapper;
    private FileMapper fileMapper;
    private FileVersionMapper fileVersionMapper;



    @Autowired
    public FolderService(FilePermissionMapper filePermissionMapper, FolderMapper folderMapper, FileMapper fileMapper, FileService fileService, FileVersionMapper fileVersionMapper) {
        this.filePermissionMapper = filePermissionMapper;
        this.folderMapper = folderMapper;
        this.fileMapper = fileMapper;
        this.fileService = fileService;
        this.fileVersionMapper = fileVersionMapper;
    }

    public FolderService() {
    }
    public Long getRootFolderId(Long userId) throws SQLException {
        return initRootFolderForUser(userId);
    }

    private long initRootFolderForUser(Long userId) throws SQLException {
        // Check if root folder exists for this user
        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);
        System.out.println(1);
        if (rootFolder == null) {
            synchronized (this) {  // Double-checked locking pattern
                rootFolder = folderMapper.findRootFolderByUserId(userId);
                if (rootFolder == null) {
                    // Create root folder for this user
                    Folder newRootFolder = new Folder();
                    // Let the database generate the ID
                    newRootFolder.setOwnerId(userId);
                    newRootFolder.setName("Root");
                    newRootFolder.setParentId(null); // Root has no parent

                    // Set up the ltree path - will be updated after insert
                    PGobject ltreePath = new PGobject();
                    ltreePath.setType("ltree");
                    ltreePath.setValue("temp"); // Temporary value
                    newRootFolder.setPath(ltreePath);

                    newRootFolder.setCreatedAt(Instant.now());
                    newRootFolder.setUpdatedAt(Instant.now());
                    newRootFolder.setDeleted(false);

                    int rows = folderMapper.insert(newRootFolder);
                    System.out.println(2);
                    if (rows <= 0) {
                        throw new SQLException("Failed to insert folder");
                    }
                    System.out.println(3);
                    Folder insertedFolder =folderMapper.findRootFolderByUserId(userId);

                    // Update path with the actual ID
                    ltreePath.setValue(insertedFolder.getId().toString());
                    folderMapper.updatePath(insertedFolder.getId(), ltreePath, Instant.now());
                    rootFolder=insertedFolder;
                    System.out.println("rootfolder"+rootFolder.getId());
                    // Create permission record
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
            Long rootId = initRootFolderForUser(userId);
            parentId = rootId;
        }

        // get parent folder
        Folder parentFolder = folderMapper.findById(parentId);
        if (parentFolder == null) {
            throw new NotFoundException("Parent folder not found");
        }

        //  if it is root folder
        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);
        // not root folder verify permission
        if (rootFolder != null && !rootFolder.getId().equals(parentId)) {
            FilePermission permission = filePermissionMapper.findByFolderIdAndUserId(parentId, userId);
            if (permission == null || permission.getPermission() == PermissionType.READ) {
                throw new AccessDeniedException("No permission to create folder in this location");
            }
        }


        // crate new folder
        Folder folder = new Folder();
        folder.setName(name);
        folder.setParentId(parentId);
        folder.setOwnerId(userId);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());
        folder.setDeleted(false);

        // temp path
        PGobject tempPath = new PGobject();
        tempPath.setType("ltree");
        tempPath.setValue("temp");
        folder.setPath(tempPath);

        //get the id after insert
        int rows = folderMapper.insert(folder);
        if (rows <= 0) {
            throw new SQLException("Failed to insert folder");
        }
        long folderId = folder.getId();


        // construct path
        try {
            PGobject ltreePath = new PGobject();
            ltreePath.setType("ltree");

            String parentPathValue = parentFolder.getPath().getValue();
            String newPathValue;
            if (rootFolder != null && rootFolder.getId().equals(parentId)) {
                // if it is root，path is id
                newPathValue = String.valueOf(folderId);
            } else {
                // pr concat
                newPathValue = parentPathValue + "." + folderId;
            }

            ltreePath.setValue(newPathValue);

            // update new path
            folderMapper.updatePath(folderId, ltreePath, Instant.now());

            // update new path
            folder.setPath(ltreePath);
        } catch (SQLException e) {
            throw e;
        }


        // permission table
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

    //soft delete
    public int softDeleteFolder(Long folderId, Long userId) {


        Folder folder = folderMapper.findById(folderId);
        if (folder == null) {
            throw new RuntimeException("file does not exist");
        }


        List<FilePermission> permission = filePermissionMapper.findListByFolderIdAndUserId(folderId, userId);
        if (permission != null) {
            for (FilePermission per : permission) {
                System.out.println("Permission type: " + per.getPermission());
                System.out.println("Is ADMIN? " + (per.getPermission() == PermissionType.ADMIN));
                System.out.println("Permission class: " + per.getPermission().getClass());
            }
        }
        System.out.println("permission checked completed");

        try {
            List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);
            System.out.println(subFolders.size());
            List<Long> folderIds = new ArrayList<>();
            if (!subFolders.isEmpty()) {
                folderIds.addAll(subFolders.stream()
                        .map(Folder::getId)
                        .toList());
            }

            List<File> allFiles = fileMapper.findFilesByFolderIds(folderIds, folderId);

            // soft delete all the files
            if (allFiles != null && !allFiles.isEmpty()) {
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
            throw new RuntimeException("fail soft deleting: " + e.getMessage());
        }
    }

    // restore
    public int restoreFolder(Long folderId, Long userId) {
        Folder folder = folderMapper.findDeletedById(folderId);
        if (folder == null) {
            throw new RuntimeException("folder does not exist");
        }


        if (!folder.isDeleted()) {
            throw new RuntimeException("has not been deleted, please delete it first");
        }

        List<FilePermission> permission = filePermissionMapper.findListByFolderIdAndUserId(folderId, userId);
        if (permission != null) {
            for (FilePermission per : permission) {
                System.out.println("Permission type: " + per.getPermission());
                System.out.println("Is ADMIN? " + (per.getPermission() == PermissionType.ADMIN));
                System.out.println("Permission class: " + per.getPermission().getClass());
            }
        }
        System.out.println("complete checking");

        try {
            // restore subfolder
            List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);
            List<File> allFiles = fileMapper.findFilesByFolderIds(
                    subFolders.stream()
                            .map(Folder::getId)
                            .collect(Collectors.toList()),
                    folderId
            );

            // restore all files
            for (File file : allFiles) {
                fileService.restoreFile(file.getId(), userId);
            }

            // restore all subfolders
            for (Folder subFolder : subFolders) {
                subFolder.setDeleted(false);
                subFolder.setUpdatedAt(Instant.now());
                folderMapper.updateFolderDeleteStatus(subFolder);
            }

            folder.setDeleted(false);
            folder.setUpdatedAt(Instant.now());
            return folderMapper.updateFolderDeleteStatus(folder);

        } catch (Exception e) {
            throw new RuntimeException("fail restoring: " + e.getMessage());
        }
    }

    // directly delete
    public int deleteFolder(Long folderId, Long userId) throws IOException {
        Folder folder = folderMapper.findDeletedById(folderId);
        if (folder == null) {
            throw new RuntimeException("folder does not exist");
        }


        List<FilePermission> permission = filePermissionMapper.findListByFolderIdAndUserId(folderId, userId);
        if (permission != null) {
            for (FilePermission per : permission) {
                System.out.println("Permission type: " + per.getPermission());
                System.out.println("Is ADMIN? " + (per.getPermission() == PermissionType.ADMIN));
                System.out.println("Permission class: " + per.getPermission().getClass());
            }
        }
        System.out.println("permission checked completed");

        try {
            List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);
            subFolders.add(folderMapper.getFolderByFolderId(folderId));
            List<File> allFiles = fileMapper.findFilesByFolderIds(
                    subFolders.stream()
                            .map(Folder::getId)
                            .collect(Collectors.toList()),
                    folderId
            );

            // only delete the file that has been soft-deleted
            for (File file : allFiles) {
                    fileService.deleteFile(file.getId(), userId);
            }

            // only delete the folder that has been soft-deleted
            Collections.reverse(subFolders);  // from the deep layer
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
                throw new RuntimeException("folder has been deleted, please delete it first");
            }
        } catch (Exception e) {
            throw new RuntimeException("fail deleting folder: " + e.getMessage());
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

        // 5. construct temp catalog
        Path tempDir = Files.createTempDirectory("download_");
        Path zipPath = tempDir.resolve(folder.getName() + ".zip");

        // 6. construct zip
        try (FileSystem zipFs = FileSystems.newFileSystem(
                URI.create("jar:" + zipPath.toUri()),
                Map.of("create", "true"))) {


            for (File file : allFiles) {
                FileVersion latestVersion = fileVersionMapper.getLatestVersion(file.getId());
                if (latestVersion != null) {
                    Path sourcePath = Paths.get(latestVersion.getStoragePath());
                    Path pathInZip = zipFs.getPath("/" + file.getName());
                    Files.copy(sourcePath, pathInZip, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // deal with sub folder
            for (Folder subFolder : subFolders) {
                processSubFolder(subFolder, zipFs, "");
            }
        }

        Resource resource = new FileSystemResource(zipPath.toFile());

        // return download information
        return new DownloadFileInfo(
                folder.getName(),
                "application/zip",
                resource
        );
    }

    private void processSubFolder(Folder folder, FileSystem zipFs, String parentPath) throws IOException {
        // construct zip path
        String currentPath = parentPath + "/" + folder.getName();
        Files.createDirectories(zipFs.getPath(currentPath));

        List<File> files = fileMapper.getFilesByFolderId(folder.getId());


        for (File file : files) {
            FileVersion latestVersion = fileVersionMapper.getLatestVersion(file.getId());
            if (latestVersion != null) {
                Path sourcePath = Paths.get(latestVersion.getStoragePath());
                Path pathInZip = zipFs.getPath(currentPath + "/" + file.getName());
                Files.copy(sourcePath, pathInZip, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }


    public void uploadFolder(MultipartFile[] files, String[] relativePaths, Long userId, Long parentFolderId) throws IOException, SQLException, NotFoundException {

        if (parentFolderId == null) {
            Long rootId = initRootFolderForUser(userId);
            parentFolderId = rootId;
        }

        Folder parentFolder = folderMapper.findById(parentFolderId);
        if (parentFolder == null) {
            throw new NotFoundException("Parent folder not found");
        }



        Folder rootFolder = folderMapper.findRootFolderByUserId(userId);
        if (rootFolder != null && !rootFolder.getId().equals(parentFolderId)) {
            FilePermission permission = filePermissionMapper.findByFolderIdAndUserId(parentFolderId, userId);
            System.out.println("permission：" + permission);
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
                throw new RuntimeException("folder does not exist: " + folderPath);
            }


            fileService.uploadFile(file, userId, targetFolderId);
        }
    }

    private String getFolderPath(String relativePath) {
        int lastSeparator = relativePath.lastIndexOf('/');
        String folderPath = lastSeparator > 0 ? relativePath.substring(0, lastSeparator) : "";
        return folderPath;
    }
    private void createFolderStructure(String folderPath, Map<String, Long> pathToFolderIdMap, Long userId, Long parentFolderId)
            throws SQLException, NotFoundException, AccessDeniedException {

        String[] folders = folderPath.split("/");

        StringBuilder currentPath = new StringBuilder();
        Long currentParentId = parentFolderId;

        for (String folder : folders) {
            //currentPath
            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            }
            currentPath.append(folder);

            String currentPathStr = currentPath.toString();
            System.out.println("currentPathStr: "+currentPathStr);
            if (!pathToFolderIdMap.containsKey(currentPathStr)) {
                // construct folder
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
        return folderMapper.getDeletedFoldersByUserIdAndFolderId(userId,parentId);
    }
}
