package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class FileService {
    private final File file;
    private FileMapper fileMapper;
    private FileVersionMapper fileVersionMapper;
    private FilePermissionMapper filePermissionMapper;

    @Value("${file.storage.path}")
    private String baseStoragePath;

    public FileService(FileMapper fileMapper, FileVersionMapper fileVersionMapper, FilePermissionMapper filePermissionMapper, File file) {
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.filePermissionMapper = filePermissionMapper;
        this.file = file;
    }

    public File uploadFile(MultipartFile file, Long ownerId, Long folderId) throws IOException {
        System.out.println("==============上传文件===============");
        System.out.println();
        if (folderId == null && fileMapper.folderExists(folderId).isEmpty()) {
            throw new RuntimeException("目标文件夹不存在");
        }

        String originalFilename = file.getOriginalFilename();

// 使用 Paths.get() 提取子目录和文件名
        Path path = Paths.get(originalFilename);
        String subPath = path.getParent() != null ? path.getParent().toString() : "";
        String fileName = path.getFileName().toString();


        // 1. 在 files 表中创建文件记录
        File newFile = new File();
        newFile.setName(fileName);
        System.out.println("在表中创建文件记录，此时的folderId是："+folderId);
        newFile.setFolderId(folderId);
        newFile.setOwnerId(ownerId);
        newFile.setMimeType(file.getContentType());
        newFile.setSize(file.getSize());
        fileMapper.insertFile(newFile);

        System.out.println("newFile的Id是"+newFile.getId());


        // 2. 计算新版本号
        int versionNumber =1 ;
//                fileVersionMapper.getVersionNumber(newFile.getId())+1;

        // 1. 使用 Path 代替 File，更好地处理路径
        Path directoryPath = Paths.get(baseStoragePath, ownerId.toString(), Long.toString(newFile.getFolderId()),subPath);
        System.out.println("directoryPath:"+directoryPath);
        Path filePath = directoryPath.resolve(fileName + "_" + versionNumber);

// 2. 确保所有父目录都被创建
        try {
            System.out.println("创建目录: " + directoryPath);
            Files.createDirectories(directoryPath);
            System.out.println("目录是否存在: " + Files.exists(directoryPath));

        } catch (IOException e) {
            throw new IOException("Failed to create directories: " + directoryPath, e);
        }

// 3. 存储文件
        try {
            System.out.println("正在存储文件: " + filePath);
            file.transferTo(filePath.toFile());
            System.out.println("文件存储成功: " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to store file: " + e.getMessage());
            throw new IOException("Failed to store file at: " + filePath, e);
        }


        // 6. 在 file_versions 表中记录文件存储路径
        FileVersion version = new FileVersion();
        version.setFileId(newFile.getId());
        version.setVersionNumber(versionNumber);
        version.setStoragePath(filePath.toString());
        version.setSize(file.getSize());
        version.setCreatedBy(ownerId);
        fileVersionMapper.insertVersion(version);

        // 7. 赋予用户 admin 权限
        FilePermission permission = new FilePermission();
        permission.setFileId(newFile.getId());
        permission.setPermission(PermissionType.ADMIN);
        permission.setCreatedBy(ownerId);
        permission.setCreatedAt(Instant.now());
        permission.setUserId(ownerId);
        permission.setFolderId(folderId);


        try {
            FilePermission existingPermission = filePermissionMapper.findByFolderIdAndUserId(folderId, ownerId);
            if (existingPermission == null) {
                filePermissionMapper.insert(permission);
            } else {
                // 可以选择更新已存在的权限
                existingPermission.setPermission(PermissionType.ADMIN);
                filePermissionMapper.update(existingPermission);
            }
        } catch (Exception e) {
            System.err.println("Insert failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("6");

        System.out.println();
        System.out.println("==============上传文件完成=================");

        return newFile;
    }

    public List<File> getFiles(long ownerId,long folderId){
        return fileMapper.getFilesByUserIdAndFolderId(ownerId,folderId);
    }


//软删除
    public int softDeleteFile(Long fileId, Long userId) {
        System.out.println("==============开始软删除文件");
        // 1. 检查文件是否存在
        File file = fileMapper.getFileById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }

        // 2. 检查用户权限
//        FilePermission permission = filePermissionMapper.getPermission(fileId, userId);
//        if (permission == null || !permission.getPermission().equals(PermissionType.ADMIN)) {
//            throw new RuntimeException("没有权限删除此文件");
//        }
        FilePermission permission = filePermissionMapper.getPermission(fileId, userId);
        if (permission != null) {
            System.out.println("在FileService里检查Permission type: " + permission.getPermission()); // 打印权限类型
            System.out.println("在FileService里检查Is ADMIN? " + (permission.getPermission() == PermissionType.ADMIN)); // 直接比较结果
            System.out.println("在FileService里检查Equals ADMIN? " + permission.getPermission().equals(PermissionType.ADMIN)); // equals比较结果
            System.out.println("在FileService里检查Permission class: " + permission.getPermission().getClass()); // 打印权限类的类型
            System.out.println("在FileService里检查ADMIN class: " + PermissionType.ADMIN.getClass()); // 打印ADMIN枚举的类型
        }
        System.out.println("权限检查完成");

        // 3. 更新文件状态为已删除，并记录删除时间
        file.setDeleted(true);
        file.setUpdatedAt(Instant.now());

        return fileMapper.updateFileDeleteStatus(file);
    }

    // 恢复软删除的文件
    public int restoreFile(Long fileId, Long userId) {
        File file = fileMapper.getDeletedFileById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }

//        FilePermission permission = filePermissionMapper.getPermission(fileId, userId);
//        if (permission == null || !permission.getPermission().equals(PermissionType.ADMIN)) {
//            throw new RuntimeException("没有权限恢复此文件");
//        }

        FilePermission permission = filePermissionMapper.getPermission(fileId, userId);
        if (permission != null) {
            System.out.println("在FileService里检查Permission type: " + permission.getPermission()); // 打印权限类型
            System.out.println("在FileService里检查Is ADMIN? " + (permission.getPermission() == PermissionType.ADMIN)); // 直接比较结果
            System.out.println("在FileService里检查Equals ADMIN? " + permission.getPermission().equals(PermissionType.ADMIN)); // equals比较结果
            System.out.println("在FileService里检查Permission class: " + permission.getPermission().getClass()); // 打印权限类的类型
            System.out.println("在FileService里检查ADMIN class: " + PermissionType.ADMIN.getClass()); // 打印ADMIN枚举的类型
        }
        System.out.println("权限检查完成");

        if (!file.isDeleted()) {
            throw new RuntimeException("文件未被删除，无需恢复");
        }

        file.setDeleted(false);
        file.setUpdatedAt(Instant.now());

        return fileMapper.updateFileDeleteStatus(file);
    }

    // 定时任务：删除超过30天的软删除文件
    @Scheduled(cron = "0 0 0 * * ?") // 每天凌晨执行
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
            System.out.println("======================开始硬删除文件："+" userId在delete:"+userId+" fileId在delete"+fileId);
            System.out.println();
            // 1. 检查文件是否存在
            File file = fileMapper.getFileById(fileId);
            if (file == null) {
                throw new RuntimeException("文件不存在");
            }

            // 2. 检查用户权限
            FilePermission permission = filePermissionMapper.getPermission(fileId, userId);
            if (permission != null) {
                System.out.println("在FileService里检查Permission type: " + permission.getPermission()); // 打印权限类型
                System.out.println("在FileService里检查Is ADMIN? " + (permission.getPermission() == PermissionType.ADMIN)); // 直接比较结果
                System.out.println("在FileService里检查Equals ADMIN? " + permission.getPermission().equals(PermissionType.ADMIN)); // equals比较结果
                System.out.println("在FileService里检查Permission class: " + permission.getPermission().getClass()); // 打印权限类的类型
                System.out.println("在FileService里检查ADMIN class: " + PermissionType.ADMIN.getClass()); // 打印ADMIN枚举的类型
            }
            System.out.println("权限检查完成");
//        if (permission == null || permission.getPermission().equals( PermissionType.ADMIN) ){
//            throw new RuntimeException("没有权限删除此文件");
//        }

            // 3. 获取所有文件版本
            List<FileVersion> versions = fileVersionMapper.getVersionsByFileId(fileId);

            // 4. 删除物理文件
            for (FileVersion version : versions) {

                System.out.println("版本路径是"+version.getStoragePath());
                Path filePath = Paths.get(version.getStoragePath());
                try {
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    throw new IOException("删除文件失败: " + filePath, e);
                }
            }

            // 5. 删除数据库记录（建议按此顺序删除以维护参照完整性）
            filePermissionMapper.deleteByFileId(fileId);  // 删除权限记录
            fileVersionMapper.deleteByFileId(fileId);     // 删除版本记录
            return fileMapper.deleteFile(fileId);                // 删除文件记录
        }


    public DownloadFileInfo downloadFile(Long fileId) throws IOException {
        // 1. 获取文件信息
        File file = fileMapper.getFileById(fileId);
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }

        // 2. 获取最新版本的文件
        FileVersion latestVersion = fileVersionMapper.getLatestVersion(fileId);
        if (latestVersion == null) {
            throw new RuntimeException("文件版本不存在");
        }

        // 3. 获取文件的物理路径
        Path filePath = Paths.get(latestVersion.getStoragePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("文件不存在于存储系统中");
        }

        // 4. 创建文件资源
        Resource resource = new FileSystemResource(filePath.toFile());

        return new DownloadFileInfo(
                file.getName(),
                file.getMimeType(),
                resource
        );
    }


    public List<File> getAllDeletedFiles( long ownerId,long folderId) {

        List<File> deletedFoldersAndFiles = fileMapper.getDeletedFilesByUserIdAndFolderId(ownerId, folderId);
        deletedFoldersAndFiles.add(fileMapper.getDeletedFileByUserId(ownerId));
        return deletedFoldersAndFiles;
    }
}
