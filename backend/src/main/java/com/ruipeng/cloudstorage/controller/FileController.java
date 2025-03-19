package com.ruipeng.cloudstorage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruipeng.cloudstorage.config.file.FileStorageConfig;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.service.FileService;
import com.ruipeng.cloudstorage.service.FolderService;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
public class FileController {
    private final FilePermission filePermission;
    private final User user;
    private final FileVersionMapper fileVersionMapper;
    private FileService fileService;
    private File file;
    private FileStorageConfig storage;
    private FolderService folderService;



    public FileController(FileService fileService, File file , FileStorageConfig storage, FilePermission filePermission, User user, FileVersionMapper fileVersionMapper, FolderService folderService) {
        this.fileService = fileService;
        this.file = file;
        this.storage = storage;
        this.filePermission = filePermission;
        this.user = user;
        this.fileVersionMapper = fileVersionMapper;
        this.folderService = folderService;
    }
    @GetMapping("/getRootFiles")
    public List<File> getRootFiles(@RequestParam Long ownerId) throws SQLException {
        Long rootFolderId = folderService.getRootFolderId(ownerId);
        return fileService.getFiles( ownerId,rootFolderId);
    }

    @PostMapping("/uploadNewFile")
    public ResponseEntity<?> uploadNewFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerId") Long ownerId,
            @RequestParam("folderId") Long folderId) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file to upload");
            }

            File newVersion = fileService.uploadNewVersion(file, ownerId, folderId);

            return ResponseEntity.ok(newVersion);
        } catch (IOException e) {

            return ResponseEntity.internalServerError().body("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }


    @GetMapping("/getAllFiles")
    public ResponseEntity<List<File>> getAllFiles(@RequestParam long folderId,@RequestParam long ownerId) {
        if (folderId==-1){
            folderId=0;
        }
        List<File> files = fileService.getFiles(ownerId,folderId);

        return ResponseEntity.ok().body(files);
    }

    @GetMapping("/getFileByUserIdAndFileId")
    public ResponseEntity<?> getFileByUserIdAndFileId(@RequestParam long ownerId, @RequestParam long fileId) {
        return fileService.getFileByUserIdAndFileId(ownerId,fileId);
    }


    @PostMapping("/upload")
    public ResponseEntity<File> uploadFile(@RequestParam("file") MultipartFile uploadFile,@RequestParam("userId")long ownerId,@RequestParam("folderId")long folderId) throws IOException {
        if (folderId==-1){
            folderId=0;
        }
        File uploadedFile = fileService.uploadFile(uploadFile, ownerId, folderId);

        return ResponseEntity.ok().body(uploadedFile);
    }
    @DeleteMapping("/deleteFile")
    public ResponseEntity<?> deleteFile(@RequestParam long fileId,@RequestParam long userId) throws IOException {

        int i = fileService.deleteFile(fileId, userId);
        if (i>0){
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/softDeleteFile")
    public ResponseEntity<?> softDeleteFile(@RequestParam long fileId,@RequestParam long userId) throws IOException {
        int i = fileService.softDeleteFile(fileId, userId);
        if (i>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/getAllDeletedFiles")
    public ResponseEntity<List<File>> getAllDeletedFiles(@RequestParam long ownerId,@RequestParam long folderId) {

        if (folderId==-1){
            folderId=0;
        }
        List<File> files = fileService.getAllDeletedFiles(ownerId,folderId);

        return ResponseEntity.ok().body(files);
    }

    @PostMapping("/restoreFile")
    public ResponseEntity<?>restoreFile(@RequestParam long fileId,@RequestParam long ownerId) {
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
            throw new RuntimeException("fail downloading file " + e.getMessage());
        }
    }

    @PostMapping("/convert-doc")
    public ResponseEntity<?> convertDoc(@RequestParam("file") MultipartFile file) {
        try {
            //  Apache POI
            XWPFDocument document;
            if (file.getOriginalFilename().endsWith(".docx")) {
                document = new XWPFDocument(file.getInputStream());
            } else {
                HWPFDocument doc = new HWPFDocument(file.getInputStream());
                return ResponseEntity.ok(doc.getDocumentText());
            }


            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }

            return ResponseEntity.ok(text.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("文档转换失败: " + e.getMessage());
        }
    }
}

