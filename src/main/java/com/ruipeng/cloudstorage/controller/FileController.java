package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.service.FileService;
import com.ruipeng.cloudstorage.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class FileController {
    private FileService fileService;
    private File file;

    public FileController(FileService fileService, File file) {
        this.fileService = fileService;
        this.file = file;
    }
    @GetMapping("/getAllFiles")
    public ResponseEntity<List<File>> getAllFiles(@RequestParam int userId) {
        List<File> files = fileService.getFiles(userId);
        return ResponseEntity.ok().body(files);
    }

    @PostMapping("/upload")
    public ResponseEntity<List<File>> uploadFile(@RequestParam("file") MultipartFile uploadFile) throws IOException {
        System.out.println("开始上传文件");
//        if (uploadFile.isEmpty() || uploadFile==null){
//            return "redirect:/home";
//        }
        file.setFilename(uploadFile.getOriginalFilename());
        System.out.println(uploadFile.getOriginalFilename());
        file.setContenttype(uploadFile.getContentType());
        file.setFilesize(uploadFile.getSize());
        file.setFiledata(uploadFile.getBytes());
        file.setUserid(SecurityUtil.getCurrentUserId());

        int i = fileService.uploadFile(file);

        System.out.println("i= "+i);
        return ResponseEntity.ok().body(new ArrayList<File>());
    }

    @GetMapping("/deleteFile/{id}")
    public String deleteFile(@PathVariable int id){
        int i = fileService.deleteById(id);
        if (i==1){
            return "redirect:/home";
        }
        return "redirect:/home";
    }
}
