package com.ruipeng.cloudstorage.dto.response;

import com.ruipeng.cloudstorage.entity.PermissionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ShareInfoResponse {
    private UUID id;
    private PermissionType type;
    private String fileName;
    private String filePath;
    private Long fileId;
    private String shareLink;
    private OffsetDateTime expiresAt;
    private boolean active;

    public ShareInfoResponse() {
    }

    public ShareInfoResponse(UUID id, PermissionType type, String fileName, String filePath, Long fileId, String shareLink, OffsetDateTime expiresAt, boolean active) {
        this.id = id;
        this.type = type;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileId = fileId;
        this.shareLink = shareLink;
        this.expiresAt = expiresAt;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PermissionType getType() {
        return type;
    }

    public void setType(PermissionType type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
