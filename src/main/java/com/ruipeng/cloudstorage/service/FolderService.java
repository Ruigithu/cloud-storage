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

    public static final Long ROOT_FOLDER_ID = 1L;

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

    private void initRootFolderIfNotExists(Long userId) throws SQLException {
        System.out.println("check结果"+folderMapper.existsById(ROOT_FOLDER_ID,userId));
        // First check if root folder exists using a transaction to prevent race conditions
        if (!folderMapper.existsById(ROOT_FOLDER_ID,userId)) {

            synchronized (this) {  // Double-checked locking pattern
                if (!folderMapper.existsById(ROOT_FOLDER_ID,userId)) {
                    // Create root folder first
                    Folder rootFolder = new Folder();
                    rootFolder.setId(ROOT_FOLDER_ID);
                    rootFolder.setOwnerId(userId);
                    rootFolder.setName("Root");
                    rootFolder.setParentId(null);

                    // Set up the ltree path
                    PGobject ltreePath = new PGobject();
                    ltreePath.setType("ltree");
                    ltreePath.setValue(String.valueOf(0L));
                    rootFolder.setPath(ltreePath);

                    rootFolder.setCreatedAt(Instant.now());
                    rootFolder.setUpdatedAt(Instant.now());
                    rootFolder.setDeleted(false);

                    // Insert the folder first
                    int insertResult = folderMapper.insert(rootFolder);
                    System.out.println("成功创建根目录");

                    // Only create permission if folder was successfully created
                    if (insertResult > 0) {
                        try {
                            // Create permission record AFTER folder exists
                            FilePermission permission = new FilePermission();
                            permission.setFolderId(ROOT_FOLDER_ID);
                            permission.setUserId(userId);
                            permission.setPermission(PermissionType.ADMIN);
                            permission.setCreatedAt(Instant.now());
                            permission.setCreatedBy(userId);
                            filePermissionMapper.insert(permission);
                        } catch (Exception e) {
                            // If permission creation fails, we should clean up the folder
                            folderMapper.deleteFolder(ROOT_FOLDER_ID);
                            throw new SQLException("Failed to create root folder permissions: " + e.getMessage());
                        }
                    } else {
                        throw new SQLException("Failed to create root folder");
                    }
                }
            }
        }
        // 这里的查询似乎没有使用返回值，可以考虑移除
        // folderMapper.findById(ROOT_FOLDER_ID);
    }

    public Folder createFolder(String name, Long parentId, Long userId) throws NotFoundException, SQLException {
        System.out.println();
        System.out.println("====================创建文件夹=======================");
        // 确保根目录存在
        if (parentId == 1L || ROOT_FOLDER_ID.equals(parentId)) {
            System.out.println("这里创建根目录了吗");
            initRootFolderIfNotExists(userId);
            parentId = ROOT_FOLDER_ID;
        }
        System.out.println("执行到1");

        Folder parentFolder = folderMapper.findById(parentId);
        if (parentFolder == null) {
            throw new NotFoundException("Parent folder not found");
        }
        System.out.println("执行到2");
        // 验证权限
        if (!ROOT_FOLDER_ID.equals(parentId)) {
            FilePermission permission = filePermissionMapper.findByFolderIdAndUserId(parentId, userId);
            System.out.println("permission是："+permission);
            if (permission == null || permission.getPermission() == PermissionType.READ) {
                System.out.println("no permission");
            }
        }
        System.out.println("执行到3");
        PGobject ltreePath = new PGobject();
        ltreePath.setType("ltree");
        ltreePath.setValue("undefined.path");

        // 创建新文件夹
        Folder folder = new Folder();
        folder.setName(name);
        folder.setParentId(parentId);
        folder.setOwnerId(userId);
        folder.setCreatedAt(Instant.now());
        folder.setUpdatedAt(Instant.now());

        // 临时将 path 设为 null 或其他占位符，后续会更新
        folder.setPath(ltreePath);  // 或者使用空字符串或其他占位符



        folder.setDeleted(false);
        long folderId = folderMapper.insert(folder);
        System.out.println("文件夹id是："+folderId);

        System.out.println("执行到4");
        // 设置初始路径为PGobject
        String pathValue;
        if (ROOT_FOLDER_ID.equals(parentId)) {
            pathValue = folder.getId().toString();
        } else {
            pathValue = parentFolder.getPath() + "." + folderId;
        }
        System.out.println("执行到5");

        try {
            ltreePath.setValue(pathValue);
            folder.setPath(ltreePath);  // 假设Folder类的path字段已经改为PGobject类型
        } catch (SQLException e) {
            throw new RuntimeException("Error setting ltree path value", e);
        }
        // 更新文件夹路径
        ltreePath.setValue(pathValue);  // 设置新的路径值

        // 插入文件夹记录
        folderMapper.updatePath(folder.getId(),ltreePath,Instant.now());
        System.out.println("执行到6");
        // 创建权限
        FilePermission permission = new FilePermission();
        permission.setFolderId(folder.getId());
        permission.setUserId(userId);
        permission.setPermission(PermissionType.ADMIN);
        permission.setCreatedAt(Instant.now());
        permission.setCreatedBy(userId);
        System.out.println(
                "folderId:"+permission.getFolderId()+
                "userId:"+permission.getUserId()+
                "permission:"+permission.getPermission()+
                        "createdAt:"+permission.getCreatedAt()+
                        "createdBy:"+permission.getCreatedBy()
        );

            filePermissionMapper.insert(permission);

        System.out.println();
        System.out.println("======================创建文件夹完成========================");

        return folder;
    }

    public List<Folder> getFolders(long userId, long parentId) throws SQLException {
        if (parentId == 1L || ROOT_FOLDER_ID.equals(parentId)) {
            initRootFolderIfNotExists(userId);
            parentId = ROOT_FOLDER_ID;
        }
        return folderMapper.getFoldersByUserIdAndFolderId(userId,parentId);
    }
    //软删除
// 软删除文件夹
    public int softDeleteFolder(Long folderId, Long userId) {


        // 1. 检查文件夹是否存在
        Folder folder = folderMapper.findById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }

        // 2. 检查是否为根目录
        if (ROOT_FOLDER_ID.equals(folderId)) {
            throw new RuntimeException("不能删除根目录");
        }

        // 3. 检查权限
        List<FilePermission> permission = filePermissionMapper.findListByFolderIdAndUserId(folderId, userId);
        if (permission != null) {
            for (FilePermission per : permission) {
                System.out.println("Permission type: " + per.getPermission());
                System.out.println("Is ADMIN? " + (per.getPermission() == PermissionType.ADMIN));
                System.out.println("Permission class: " + per.getPermission().getClass());
            }
        }
        System.out.println("权限检查完成");

        try {
            // 4. 获取子文件夹
            List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);
            System.out.println(subFolders.size());
             //5. 获取所有文件
            List<Long> folderIds = new ArrayList<>();
            if (!subFolders.isEmpty()) {
                folderIds.addAll(subFolders.stream()
                        .map(Folder::getId)
                        .toList());
            }

            List<File> allFiles = fileMapper.findFilesByFolderIds(folderIds, folderId);

            // 6. 软删除所有文件
            if (allFiles != null && !allFiles.isEmpty()) {
                for (File file : allFiles) {
                    fileService.softDeleteFile(file.getId(), userId);
                }
            }

            // 7. 标记文件夹为已删除
            folder.setDeleted(true);
            System.out.println("执行到这里0");
            folder.setUpdatedAt(Instant.now());
            System.out.println("执行到这里1");

            // 8. 同样标记所有子文件夹为已删除
            for (Folder subFolder : subFolders) {
                subFolder.setDeleted(true);
                subFolder.setUpdatedAt(Instant.now());
                folderMapper.updateFolderDeleteStatus(subFolder);
            }
            System.out.println("执行到这里2");

            return folderMapper.updateFolderDeleteStatus(folder);
        } catch (Exception e) {
            throw new RuntimeException("软删除文件夹失败: " + e.getMessage());
        }
    }

    // 恢复文件夹
    public int restoreFolder(Long folderId, Long userId) {
        Folder folder = folderMapper.findDeletedById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }
        System.out.println("要恢复的文件夹是 "+folder.getId());

        if (!folder.isDeleted()) {
            System.out.println("文件是否删除："+folder.isDeleted());
            throw new RuntimeException("文件夹未被删除，无需恢复");
        }

        List<FilePermission> permission = filePermissionMapper.findListByFolderIdAndUserId(folderId, userId);
        if (permission != null) {
            for (FilePermission per : permission) {
                System.out.println("Permission type: " + per.getPermission());
                System.out.println("Is ADMIN? " + (per.getPermission() == PermissionType.ADMIN));
                System.out.println("Permission class: " + per.getPermission().getClass());
            }
        }
        System.out.println("权限检查完成");

        try {
            // 恢复子文件夹
            List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);
            List<File> allFiles = fileMapper.findFilesByFolderIds(
                    subFolders.stream()
                            .map(Folder::getId)
                            .collect(Collectors.toList()),
                    folderId
            );

            // 恢复所有文件
            for (File file : allFiles) {
                fileService.restoreFile(file.getId(), userId);
            }

            // 恢复所有子文件夹
            for (Folder subFolder : subFolders) {
                subFolder.setDeleted(false);
                subFolder.setUpdatedAt(Instant.now());
                folderMapper.updateFolderDeleteStatus(subFolder);
            }

            // 恢复当前文件夹
            folder.setDeleted(false);
            folder.setUpdatedAt(Instant.now());
            return folderMapper.updateFolderDeleteStatus(folder);

        } catch (Exception e) {
            throw new RuntimeException("恢复文件夹失败: " + e.getMessage());
        }
    }

    // 硬删除文件夹
    public int deleteFolder(Long folderId, Long userId) throws IOException {
        Folder folder = folderMapper.findDeletedById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }

        if (ROOT_FOLDER_ID.equals(folderId)) {
            throw new RuntimeException("不能删除根目录");
        }

        List<FilePermission> permission = filePermissionMapper.findListByFolderIdAndUserId(folderId, userId);
        if (permission != null) {
            for (FilePermission per : permission) {
                System.out.println("Permission type: " + per.getPermission());
                System.out.println("Is ADMIN? " + (per.getPermission() == PermissionType.ADMIN));
                System.out.println("Permission class: " + per.getPermission().getClass());
            }
        }
        System.out.println("权限检查完成");

        try {
            List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);
            subFolders.add(folderMapper.getFolderByFolderId(folderId));
            List<File> allFiles = fileMapper.findFilesByFolderIds(
                    subFolders.stream()
                            .map(Folder::getId)
                            .collect(Collectors.toList()),
                    folderId
            );

            // 只删除已经软删除的文件
            for (File file : allFiles) {
                    fileService.deleteFile(file.getId(), userId);
            }

            // 只删除已经软删除的文件夹
            Collections.reverse(subFolders);  // 从最深层开始删除
            for (Folder subFolder : subFolders) {
                if (subFolder.isDeleted()) {
                    filePermissionMapper.deleteByFolderId(subFolder.getId());
                    folderMapper.deleteFolder(subFolder.getId());
                }
            }

            // 只有当前文件夹被软删除时才进行硬删除
            if (folder.isDeleted()) {
                filePermissionMapper.deleteByFolderId(folderId);
                return folderMapper.deleteFolder(folderId);
            } else {
                throw new RuntimeException("文件夹未被软删除，无法进行硬删除");
            }
        } catch (Exception e) {
            throw new RuntimeException("删除文件夹失败: " + e.getMessage());
        }
    }

    public DownloadFileInfo downloadFolder(Long folderId, Long userId) throws IOException {
        // 1. 检查文件夹是否存在
        Folder folder = folderMapper.findById(folderId);
        if (folder == null) {
            throw new RuntimeException("文件夹不存在");
        }

        // 2. 检查权限
        FilePermission permission = filePermissionMapper.findByFolderIdAndUserId(folderId, userId);
        if (permission == null) {
            throw new RuntimeException("没有权限下载此文件夹");
        }

        // 3. 获取所有子文件夹
        List<Folder> subFolders = folderMapper.findSubFolders(folder.getPath(), folderId);

        // 4. 获取所有文件
        List<File> allFiles = fileMapper.findFilesByFolderIds(
                subFolders.stream()
                        .map(Folder::getId)
                        .collect(Collectors.toList()),
                folderId
        );

        // 5. 创建临时目录用于存放zip文件
        Path tempDir = Files.createTempDirectory("download_");
        Path zipPath = tempDir.resolve(folder.getName() + ".zip");

        // 6. 创建zip文件
        try (FileSystem zipFs = FileSystems.newFileSystem(
                URI.create("jar:" + zipPath.toUri()),
                Map.of("create", "true"))) {

            // 处理当前文件夹中的文件
            for (File file : allFiles) {
                FileVersion latestVersion = fileVersionMapper.getLatestVersion(file.getId());
                if (latestVersion != null) {
                    Path sourcePath = Paths.get(latestVersion.getStoragePath());
                    Path pathInZip = zipFs.getPath("/" + file.getName());
                    Files.copy(sourcePath, pathInZip, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 处理子文件夹
            for (Folder subFolder : subFolders) {
                // 递归处理子文件夹
                processSubFolder(subFolder, zipFs, "");
            }
        }

        // 7. 创建Resource对象
        Resource resource = new FileSystemResource(zipPath.toFile());

        // 8. 返回下载信息
        return new DownloadFileInfo(
                folder.getName(),
                "application/zip",
                resource
        );
    }

    private void processSubFolder(Folder folder, FileSystem zipFs, String parentPath) throws IOException {
        // 创建文件夹在zip中的路径
        String currentPath = parentPath + "/" + folder.getName();
        Files.createDirectories(zipFs.getPath(currentPath));

        // 获取文件夹中的文件
        List<File> files = fileMapper.getFilesByFolderId(folder.getId());

        // 添加文件到zip
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
        if (parentFolderId == 1L || ROOT_FOLDER_ID.equals(parentFolderId)) {
            initRootFolderIfNotExists(userId);
            parentFolderId = ROOT_FOLDER_ID;
        }
        System.out.println("====================上传整个文件夹========================");

        // 1. 验证父文件夹权限
        if (parentFolderId != ROOT_FOLDER_ID) {
            FilePermission permission = filePermissionMapper.findByFolderIdAndUserId(parentFolderId, userId);
            if (permission == null || permission.getPermission() == PermissionType.READ) {
                throw new RuntimeException("没有权限上传到此文件夹");
            }
        }
        for(MultipartFile file : files) {
            System.out.println("需要上传的 " + files.length+" 个 文件名字分别是 "+file.getOriginalFilename());
            System.out.println("=================================================");
        }

        // 2. 创建文件夹结构并记录文件夹ID映射
        Map<String, Long> pathToFolderIdMap = new HashMap<>();
        pathToFolderIdMap.put("", parentFolderId);  // 根路径映射到父文件夹ID

        // 3. 首先创建所有需要的文件夹
        for (String relativePath : relativePaths) {
            String folderPath = getFolderPath(relativePath);
            System.out.println("=========================创建所有需要的文件夹============================");
            System.out.println("FolderService中的folderPath是：" + folderPath);
            if (!folderPath.isEmpty() && !pathToFolderIdMap.containsKey(folderPath)) {
                createFolderStructure(folderPath, pathToFolderIdMap, userId, parentFolderId);
            }
            System.out.println("============================文件夹创建完成====================================");
        }

        // 4. 上传文件
        System.out.println("files.length是："+files.length);
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            System.out.println("上传文件的文件名字 "+ file.getOriginalFilename());
            String relativePath = relativePaths[i];
            String folderPath = getFolderPath(relativePath);
            System.out.println("上传文件的文件路径 " + folderPath);

            // 获取目标文件夹ID
            Long targetFolderId = pathToFolderIdMap.get(folderPath);
            System.out.println("上传的文件夹ID是:" + targetFolderId);
            if (targetFolderId == null) {
                throw new RuntimeException("未找到目标文件夹: " + folderPath);
            }

            // 使用现有的文件上传服务
            System.out.println("===============遍历每个文件并开始上传==================");
            fileService.uploadFile(file, userId, targetFolderId);
            System.out.println("==============遍历每个文件并上传完成==================");
        }
    }

    private String getFolderPath(String relativePath) {
        int lastSeparator = relativePath.lastIndexOf('/');
        //这里返回的是除了文件名字所有的文件路径，可能是Work/Person/Documents
        String folderPath = lastSeparator > 0 ? relativePath.substring(0, lastSeparator) : "";
        return folderPath;
    }
    private void createFolderStructure(String folderPath, Map<String, Long> pathToFolderIdMap, Long userId, Long parentFolderId)
            throws SQLException, NotFoundException {

        String[] folders = folderPath.split("/");
        for (String folder : folders) {
            System.out.println("createFolderStructure里的folder名字是"+folder);
        }
        StringBuilder currentPath = new StringBuilder();
        Long currentParentId = parentFolderId;
        System.out.println("FolderService中的currentParentId是"+ currentParentId);

        for (String folder : folders) {
            //currentPath
            if (!currentPath.isEmpty()) {
                currentPath.append("/");
            }
            currentPath.append(folder);

            String currentPathStr = currentPath.toString();
            System.out.println("currentPathStr是"+currentPathStr);
            if (!pathToFolderIdMap.containsKey(currentPathStr)) {
                // 创建文件夹
                Folder newFolder = createFolder(folder, currentParentId, userId);
                pathToFolderIdMap.put(currentPathStr, newFolder.getId());
                currentParentId = newFolder.getId();
            } else {
                currentParentId = pathToFolderIdMap.get(currentPathStr);
            }
        }
    }

    public List<Folder> getAllDeletedFolders(long userId, long parentId) throws SQLException {
        if (parentId == 1L || ROOT_FOLDER_ID.equals(parentId)) {
            initRootFolderIfNotExists(userId);
            parentId = ROOT_FOLDER_ID;
        }
        return folderMapper.getDeletedFoldersByUserIdAndFolderId(userId,parentId);
    }
}
