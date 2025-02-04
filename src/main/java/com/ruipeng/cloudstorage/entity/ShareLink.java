package com.ruipeng.cloudstorage.entity;

import lombok.Data;
import java.time.Instant;

@Data
public class ShareLink {
    private Long id;
    private String token;
    private Long fileId;
    private Long folderId;
    private Long createdBy;
    private Instant createdAt;
    private Instant expiresAt;
    private String passwordHash;
    private PermissionType permission;

    public ShareLink(Long id, String token, Long fileId, Long folderId, Long createdBy, Instant createdAt, Instant expiresAt, String passwordHash, PermissionType permission) {
        this.id = id;
        this.token = token;
        this.fileId = fileId;
        this.folderId = folderId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.passwordHash = passwordHash;
        this.permission = permission;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public PermissionType getPermission() {
        return permission;
    }

    public void setPermission(PermissionType permission) {
        this.permission = permission;
    }
}

