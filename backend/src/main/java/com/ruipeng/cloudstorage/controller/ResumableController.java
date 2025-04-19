package com.ruipeng.cloudstorage.controller;

import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.PartSummary;
import com.ruipeng.cloudstorage.entity.CompleteUploadRequest;
import com.ruipeng.cloudstorage.entity.User;
import com.ruipeng.cloudstorage.service.FileS3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/resumable")
public class ResumableController {
    private final FileS3Service fileS3Service;
    private final User user;

    @Autowired
    public ResumableController(FileS3Service fileS3Service, User user) {
        this.fileS3Service = fileS3Service;
        this.user = user;
    }


    @PostMapping("/init")
    public ResponseEntity<?> initiateUpload(
            @RequestParam("ownerId") Long ownerId,
            @RequestParam("folderId") Long folderId,
            @RequestParam("fileName") String fileName,
            @RequestParam("mimeType") String mimeType,
            @RequestParam("fileSize") Long fileSize) {
        System.out.println(ownerId+"get");
        try {
            Map<String, Object> result = fileS3Service.initiateResumableUpload(ownerId, folderId, fileName, mimeType, fileSize);
            return ResponseEntity.ok(result);  // 返回JSON对象
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error initiating upload: " + e.getMessage());
        }
    }


    @PostMapping("/part")
    public ResponseEntity<PartETag> uploadPart(
            @RequestParam("fileId") Long fileId,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber,
            @RequestPart("part") MultipartFile part) throws Exception {
        byte[] partData = part.getBytes();
        PartETag partETag = fileS3Service.uploadPart(fileId, uploadId, partNumber, partData);
        return ResponseEntity.ok(partETag);
    }


    @GetMapping("/parts")
    public ResponseEntity<List<PartSummary>> listParts(
            @RequestParam("fileId") Long fileId,
            @RequestParam("uploadId") String uploadId) {
        List<PartSummary> parts = fileS3Service.listUploadedParts(fileId, uploadId);
        return ResponseEntity.ok(parts);
    }


    @PostMapping("/complete")
    public ResponseEntity<String> completeUpload(@RequestBody CompleteUploadRequest request) {
        try {
            List<PartETag> awsPartETags = request.getPartETags().stream()
                    .map(dto -> new PartETag(dto.getPartNumber(), dto.geteTag()))
                    .collect(Collectors.toList());

            fileS3Service.completeResumableUpload(request.getFileId(), request.getUploadId(), awsPartETags);
            return ResponseEntity.ok("Upload completed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error completing upload: " + e.getMessage());
        }
    }


    @PostMapping("/abort")
    public ResponseEntity<String> abortUpload(
            @RequestParam("fileId") Long fileId,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("folderId") Long folderId,
            @RequestParam("userId") Long userId) {
        try {
            fileS3Service.abortResumableUpload(fileId, uploadId);
            return ResponseEntity.ok("Upload aborted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error aborting upload: " + e.getMessage());
        }
    }
}
