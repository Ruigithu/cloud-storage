package com.ruipeng.cloudstorage.entity;


import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class FilePermission {
    private Long id;
    private Long fileId;
    private Long folderId;
    private Long userId;
    private PermissionType permission;
    private Instant createdAt;
    private Long createdBy;

    public FilePermission(Long id, Long fileId, Long userId, Long folderId, PermissionType permission, Instant createdAt, Long createdBy) {
        this.id = id;
        this.fileId = fileId;
        this.userId = userId;
        this.folderId = folderId;
        this.permission = permission;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public FilePermission() {
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

    public Long getFolderId() {
        return folderId;
    }

    public void setFolderId(Long folderId) {
        this.folderId = folderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PermissionType getPermission() {
        return permission;
    }

    public void setPermission(PermissionType permission) {
        this.permission = permission;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}

