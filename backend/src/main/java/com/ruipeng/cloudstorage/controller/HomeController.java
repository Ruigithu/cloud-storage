package com.ruipeng.cloudstorage.controller;


import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.UserInfo;
import com.ruipeng.cloudstorage.service.FileService;
import com.ruipeng.cloudstorage.util.SecurityUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class HomeController {
    private FileService fileService;


    public HomeController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/home")
    public ResponseEntity<?> home() {
        long currentUserId = SecurityUtil.getCurrentUserId();

        List<File> files = fileService.getFiles(currentUserId,0);
        System.out.println("file amount" + files.size());


        return ResponseEntity.ok().body(Map.of("message", "Successfully accessed home"));
    }

}
