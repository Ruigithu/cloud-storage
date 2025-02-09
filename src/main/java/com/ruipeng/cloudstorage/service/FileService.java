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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.aspectj.util.FileUtil.isZipFile;

@Service
public class FileService {
    private final File file;
    private final ShareMapper shareMapper;
    private FileMapper fileMapper;
    private FileVersionMapper fileVersionMapper;
    private FilePermissionMapper filePermissionMapper;

    @Value("${file.storage.path}")
    private String baseStoragePath;

    public FileService(FileMapper fileMapper, FileVersionMapper fileVersionMapper, FilePermissionMapper filePermissionMapper, File file, ShareMapper shareMapper) {
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.filePermissionMapper = filePermissionMapper;
        this.file = file;
        this.shareMapper = shareMapper;
    }

    public File uploadFile(MultipartFile file, Long ownerId, Long folderId) throws IOException {
        System.out.println("==============上传文件===============");
        System.out.println();
        if (folderId == null && fileMapper.folderExists(folderId).isEmpty()) {
            throw new RuntimeException("目标文件夹不存在");
        }

        String originalFilename = file.getOriginalFilename();


        String subPath = "";
        String fileName = originalFilename;

        // 如果originalFilename包含路径分隔符，则需要正确处理
        if (originalFilename != null && (originalFilename.contains("/") || originalFilename.contains("\\"))) {
            Path fullPath = Paths.get(originalFilename);
            // 只获取最后一级的文件名
            fileName = fullPath.getFileName().toString();
            // 获取除了文件名外的路径部分
            if (fullPath.getParent() != null) {
                subPath = fullPath.getParent().toString();
            }
        }

        // 获取文件名和扩展名
        String fileExtension = "";
        String nameWithoutExtension = fileName;

        // 分离文件名和扩展名
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex > 0) {
            nameWithoutExtension = fileName.substring(0, lastDotIndex);
            fileExtension = fileName.substring(lastDotIndex);
        }
        // 1. 在 files 表中创建文件记录
        File newFile = new File();
        newFile.setName(fileName); // 保存原始文件名（包含扩展名）
        System.out.println("在表中创建文件记录，此时的folderId是：" + folderId);
        newFile.setFolderId(folderId);
        newFile.setOwnerId(ownerId);
        newFile.setMimeType(file.getContentType());
        newFile.setSize(file.getSize());
        fileMapper.insertFile(newFile);

        System.out.println("newFile的Id是" + newFile.getId());

        // 2. 计算新版本号
        int versionNumber = 1;
        // fileVersionMapper.getVersionNumber(newFile.getId())+1;

        // 构建新的文件名（包含版本号）
        String newFileName = String.format("%s_v%d%s", nameWithoutExtension, versionNumber, fileExtension);

        // 构建目录路径时确保不会重复
        Path directoryPath;
        if (subPath.isEmpty()) {
            directoryPath = Paths.get(baseStoragePath, ownerId.toString(), Long.toString(folderId));
        } else {
            directoryPath = Paths.get(baseStoragePath, ownerId.toString(), Long.toString(folderId), subPath);
        }

        System.out.println("directoryPath:" + directoryPath);
        Path filePath = directoryPath.resolve(newFileName);

        // 确保所有父目录都被创建
        try {
            System.out.println("创建目录: " + directoryPath);
            Files.createDirectories(directoryPath);
            System.out.println("目录是否存在: " + Files.exists(directoryPath));
        } catch (IOException e) {
            throw new IOException("Failed to create directories: " + directoryPath, e);
        }

        // 存储文件
        try {
            System.out.println("正在存储文件: " + filePath);
            file.transferTo(filePath.toFile());
            System.out.println("文件存储成功: " + filePath);
        } catch (IOException e) {
            System.err.println("Failed to store file: " + e.getMessage());
            throw new IOException("Failed to store file at: " + filePath, e);
        }

        // 在 file_versions 表中记录文件存储路径
        FileVersion version = new FileVersion();
        version.setFileId(newFile.getId());
        version.setVersionNumber(versionNumber);
        version.setStoragePath(filePath.toString());
        version.setSize(file.getSize());
        version.setCreatedBy(ownerId);
        fileVersionMapper.insertVersion(version);
        System.out.println("执行到这里了吗马马马");

        // 赋予用户 admin 权限
        FilePermission permission = new FilePermission();
        permission.setFileId(newFile.getId());
        permission.setPermission(PermissionType.ADMIN);
        permission.setCreatedBy(ownerId);
        permission.setCreatedAt(Instant.now());
        permission.setUserId(ownerId);
        permission.setFolderId(folderId);

        try {
            System.out.println("fileId是："+permission.getFileId());
            FilePermission existingPermission = filePermissionMapper.findByFileIdAndUserId(newFile.getId(), ownerId);
            if (existingPermission==null) {
                System.out.println("这里到底有没有啊："+permission.getFileId());
                filePermissionMapper.insert(permission);
                System.out.println("fileId是："+permission.getFileId());
            } else {
                existingPermission.setPermission(PermissionType.ADMIN);
                filePermissionMapper.update(existingPermission);
            }
        } catch (Exception e) {
            System.err.println("Insert failed: " + e.getMessage());
            e.printStackTrace();
        }

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
            File file = fileMapper.getDeletedFileById(fileId);
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

            //检查是否被share


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
        //删除share表记录
            if (!shareMapper.findByFileIdAndCreatedBy(fileId,userId).isEmpty()){
                shareMapper.deleteByFileIdAndCreatedBy(fileId,userId);
            }
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
        deletedFoldersAndFiles.addAll(fileMapper.getDeletedFileByUserId(ownerId));
        return deletedFoldersAndFiles;
    }

    public ResponseEntity<?> getFileByUserIdAndFileId(long ownerId, long fileId) {

        try {
            // 1. 验证文件权限
            FileVersion version = fileVersionMapper.getVersionByFileId(fileId);
            if (version == null) {
                return ResponseEntity.notFound().build();
            }

            // 2. 获取文件路径并验证文件存在
            Path filePath = Paths.get(version.getStoragePath());
            System.out.println("读取文件 获取文件路径： "+filePath);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 3. 获取文件类型
            String contentType = Files.probeContentType(filePath);
            System.out.println("文件类型："+contentType);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // 4. 读取文件

            byte[] fileContent = Files.readAllBytes(filePath);
            System.out.println("读取的文件大小是"+fileContent.length);


            File file= fileMapper.getFileByUserIdAndFileId(ownerId, fileId);

            if (isWordDocument(contentType, version.getStoragePath())) {
                System.out.println("进入elseif语句，文件路径是 "+version.getStoragePath());
                // Word文档处理
                try {
                    String convertedText;
                    if (version.getStoragePath().endsWith(".docx")) {
                        System.out.println("开始处理docx文件");
                        // 处理docx文件
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
                        // 处理doc文件
                        HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(fileContent));
                        convertedText = document.getDocumentText();
                        System.out.println("converted text: " + convertedText);
                        document.close();
                    }
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(convertedText);
                } catch (Exception e) {

                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("文档转换失败: " + e.getMessage());
                }
            } else if (contentType.startsWith("text/") || contentType.equals("application/json")) {
                // 文本文件处理
                String textContent = new String(fileContent, StandardCharsets.UTF_8);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(textContent);
            } else {
                String fileName = Paths.get(version.getStoragePath()).getFileName().toString();
                // 二进制文件处理
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
    // 判断是否是Word文档的辅助方法
    private boolean isWordDocument(String contentType, String filePath) {

        return contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || // docx
                contentType.equals("application/msword") || // doc
                filePath.endsWith(".docx") ||
                filePath.endsWith(".doc");
    }
    private boolean isZipFile(byte[] content) {
        System.out.println(
                "进入zip文件处理"
        );
        // 更健壮的ZIP文件检查
        return content.length > 4 &&
                content[0] == 0x50 &&
                content[1] == 0x4B &&
                content[2] == 0x03 &&
                content[3] == 0x04;
    }

    private ResponseEntity<?> handleZipFile(byte[] content, long ownerId, long fileId) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content);
             ZipInputStream zis = new ZipInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            ZipEntry entry;
            String fileName = fileMapper.getFileByUserIdAndFileId(ownerId, fileId).getName();

            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals(fileName)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }

                    String fileContent = baos.toString(StandardCharsets.UTF_8);
                    ObjectMapper mapper = new ObjectMapper();
                    String jsonContent = mapper.writeValueAsString(fileContent);

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(jsonContent);
                }
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing ZIP file: " + e.getMessage());
        }
    }

}
