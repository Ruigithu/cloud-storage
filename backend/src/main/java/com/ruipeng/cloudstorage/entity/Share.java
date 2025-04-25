package com.ruipeng.cloudstorage.entity;



import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;
@Component
public class Share {

    private UUID id;
    private Long fileId;
    private Long createdBy;
    private PermissionType accessType;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
    private boolean active;

    public Share() {
        this.id = UUID.randomUUID();
        this.createdAt = OffsetDateTime.now();
        this.active = true;
    }

    public Share(UUID id, Long fileId, Long createdBy, PermissionType accessType, OffsetDateTime expiresAt, OffsetDateTime createdAt, boolean active) {
        this.id = id;
        this.fileId = fileId;
        this.createdBy = createdBy;
        this.accessType = accessType;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.active = active;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public PermissionType getAccessType() {
        return accessType;
    }

    public void setAccessType(PermissionType accessType) {
        this.accessType = accessType;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}