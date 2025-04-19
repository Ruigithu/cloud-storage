package com.ruipeng.cloudstorage.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Data
@Component
public class FileVersion {
    private Long id;
    private Long fileId;
    private int versionNumber;
    private String storagePath;
    private Long size;
    private Long createdBy;
    private Instant createdAt;
    private String comment;
    private String uploadId;

    public FileVersion(Long id, Long fileId, int versionNumber, String storagePath, Long size, Long createdBy, Instant createdAt,String comment, String uploadId) {
        this.id = id;
        this.fileId = fileId;
        this.versionNumber = versionNumber;
        this.storagePath = storagePath;
        this.size = size;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.comment = comment;
        this.uploadId = uploadId;
    }

    public FileVersion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }
}

