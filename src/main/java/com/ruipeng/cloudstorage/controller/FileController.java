package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.config.file.FileStorageConfig;
import com.ruipeng.cloudstorage.entity.DownloadFileInfo;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.FilePermission;
import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@RestController
public class FileController {
    private final FilePermission filePermission;
    private final User user;
    private FileService fileService;
    private File file;
    private FileStorageConfig storage;





    public FileController(FileService fileService, File file , FileStorageConfig storage, FilePermission filePermission, User user) {
        this.fileService = fileService;
        this.file = file;
        this.storage = storage;
        this.filePermission = filePermission;
        this.user = user;
    }

    @GetMapping("/getAllFiles")
    public ResponseEntity<List<File>> getAllFiles(@RequestParam long folderId,@RequestParam long ownerId) {
        System.out.println("请求所有的files"+"ownerId是："+ownerId+"folderId是："+folderId);
        System.out.println();
        if (folderId==-1){
            folderId=0;
        }
        List<File> files = fileService.getFiles(ownerId,folderId);

        return ResponseEntity.ok().body(files);
    }

    @PostMapping("/upload")
    public ResponseEntity<File> uploadFile(@RequestParam("file") MultipartFile uploadFile,@RequestParam("userId")long ownerId,@RequestParam("folderId")long folderId) throws IOException {
        System.out.println("开始上传文件"+"上传文件folderId是"+folderId);
        System.out.println();
        if (folderId==-1){
            folderId=0;
        }
        File uploadedFile = fileService.uploadFile(uploadFile, ownerId, folderId);

        return ResponseEntity.ok().body(uploadedFile);
    }
    @DeleteMapping("/deleteFile")
    public ResponseEntity<?> deleteFile(@RequestParam long fileId,@RequestParam long userId) throws IOException {
        System.out.println("进入deleteFileController");
        System.out.println("controller fileId:"+fileId);
        System.out.println("controller userId:"+userId);
        int i = fileService.deleteFile(fileId, userId);
        if (i>0){
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/softDeleteFile")
    public ResponseEntity<?> softDeleteFile(@RequestParam long fileId,@RequestParam long userId) throws IOException {
        System.out.println("进入softDeleteFileController");
        int i = fileService.softDeleteFile(fileId, userId);
        if (i>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/getAllDeletedFiles")
    public ResponseEntity<List<File>> getAllDeletedFiles(@RequestParam long ownerId,@RequestParam long folderId) {
        System.out.println("请求所有的files "+"ownerId是："+ownerId+"folderId是："+folderId);
        System.out.println();
        if (folderId==-1){
            folderId=0;
        }
        List<File> files = fileService.getAllDeletedFiles(ownerId,folderId);
        System.out.println("已删除的文件有 "+files.size()+" 个");
        for (File file :files){
            System.out.println("得到的已经删除的文件名字有："+file.getName());
        }

        return ResponseEntity.ok().body(files);
    }

    @PostMapping("/restoreFile")
    public ResponseEntity<?>restoreFile(@RequestParam long fileId,@RequestParam long ownerId) {
        System.out.println("恢复的文件 fileId:"+fileId+"ownerId:"+ownerId);
        int i = fileService.restoreFile(fileId, ownerId);

        if (i>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }




     @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("fileId") Long fileId) {
        try {
            DownloadFileInfo downloadInfo = fileService.downloadFile(fileId);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadInfo.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + URLEncoder.encode(downloadInfo.getFileName(), "UTF-8") + "\"")
                .body(downloadInfo.getResource());
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }


}

