package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.service.FileService;
import com.ruipeng.cloudstorage.util.SecurityUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class FileController {
    private FileService fileService;
    private File file;

    public FileController(FileService fileService, File file) {
        this.fileService = fileService;
        this.file = file;
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("fileUpload")MultipartFile uploadFile, Model model) throws IOException {
        System.out.println("开始上传文件");
        if (uploadFile.isEmpty() || uploadFile==null){
            return "redirect:/home";
        }
        file.setFilename(uploadFile.getOriginalFilename());
        System.out.println(uploadFile.getOriginalFilename());
        file.setContenttype(uploadFile.getContentType());
        file.setFilesize(uploadFile.getSize());
        file.setFiledata(uploadFile.getBytes());
        file.setUserid(SecurityUtil.getCurrentUserId());

        int i = fileService.uploadFile(file);

        if (i==1){
            System.out.println(i+" file uploaded");
            model.addAttribute("uploadMessage","1 file uploaded");
        }else {
            model.addAttribute("uploadError","something wrong with the uploaded file");
        }
        return "redirect:/home";
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
