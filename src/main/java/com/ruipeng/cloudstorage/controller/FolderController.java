package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.entity.DownloadFileInfo;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.Folder;
import com.ruipeng.cloudstorage.service.FolderService;
import org.apache.ibatis.javassist.NotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.List;

@RestController
public class FolderController {

    private FolderService folderService;

    @Autowired
    public FolderController(FolderService folderService) {
        this.folderService = folderService;
    }

    @PostMapping("/createFolder")
    public ResponseEntity<Folder> createFolder(@RequestParam("name")String name,@RequestParam("parentId")Long parentId,@RequestParam("userId") Long userId) throws SQLException, NotFoundException {
        System.out.println("开始创建新文件夹");
        System.out.println("文件夹名字:"+name+" parentId:"+parentId+" userId:"+userId);
        Folder folder = folderService.createFolder(name, parentId, userId);
        return ResponseEntity.ok(folder);
    }

    @GetMapping("/getAllFolders")
    public ResponseEntity<List<Folder>> getAllFolders(@RequestParam long parentId, @RequestParam long userId) throws SQLException {
        System.out.println("请求所有folders");
        System.out.println("userId是："+userId);
        System.out.println("parentId是："+parentId);

        List<Folder> folders = folderService.getFolders(userId,parentId);
        if (folders!=null){
            for (Folder folder:folders){
                System.out.println("文件夹名字是："+folder.getName());
            }
        }
        return ResponseEntity.ok().body(folders);
    }

    @DeleteMapping  ("/deleteFolder")
    public ResponseEntity<?> deleteFolder(@RequestParam("folderId") long folderId,@RequestParam("userId")long ownerId) throws IOException {
        int i = folderService.deleteFolder(folderId, ownerId);
        if (i>0){
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }


    @DeleteMapping  ("/softDeleteFolder")
    public ResponseEntity<?> softDeleteFolder(@RequestParam("folderId") long folderId,@RequestParam("userId")long ownerId) throws IOException {
        int i = folderService.softDeleteFolder(folderId, ownerId);
        if (i>0){
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }



    @GetMapping("/getAllDeletedFolders")
    public ResponseEntity<List<Folder>> getAllDeletedFolders(@RequestParam long parentId, @RequestParam long userId) throws SQLException {

        List<Folder> folders = folderService.getAllDeletedFolders(userId,parentId);
        if (folders!=null){
            for (Folder folder:folders){
                System.out.println("文件夹名字是："+folder.getName());
            }
        }
        return ResponseEntity.ok().body(folders);
    }

    @PostMapping("/restoreFolder")
    public ResponseEntity<?>restoreFile(@RequestParam long folderId,@RequestParam long ownerId) {
        int i = folderService.restoreFolder(folderId, ownerId);
        if (i>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }


    @GetMapping("/downloadFolder")
    public ResponseEntity<Resource> downloadFolder(@RequestParam("folderId") Long folderId, @RequestParam("userId") Long userId) {
        try {
            DownloadFileInfo downloadInfo = folderService.downloadFolder(folderId, userId);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + URLEncoder.encode(downloadInfo.getFileName() + ".zip", "UTF-8") + "\"")
                    .body(downloadInfo.getResource());
        } catch (Exception e) {
            throw new RuntimeException("文件夹下载失败: " + e.getMessage());
        }
    }

    @PostMapping("/uploadFolder")
    public ResponseEntity<?> uploadFolder(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("paths") String[] relativePaths,
            @RequestParam("folderId") long parentFolderId,
            @RequestParam("userId")long userId) throws IOException {

        if (files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No files uploaded");
        }

        try {
            System.out.println("Received " + files.length + " files.");
            System.out.println("Received " + relativePaths.length + " paths.");
            for (MultipartFile file : files) {
                System.out.println("文件名字是："+file.getOriginalFilename());
            }
            for (String path : relativePaths) {
                System.out.println("文件相对路径是："+path
                );
            }

            folderService.uploadFolder(files, relativePaths, userId, parentFolderId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("文件夹上传失败: " + e.getMessage());
        }
    }

}
