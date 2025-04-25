package com.ruipeng.cloudstorage.entity;

import org.springframework.stereotype.Component;

import java.time.Instant;


@Component

public class File {
    private Long id;
    private String name;
    private long folderId;
    private Long ownerId;
    private String mimeType;
    private Long size;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean isDeleted;

    public File(Long id, String name, long folderId, Long ownerId, String mimeType, Long size, Instant createdAt, Instant updatedAt, boolean isDeleted) {
        this.id = id;
        this.name = name;
        this.folderId = folderId;
        this.ownerId = ownerId;
        this.mimeType = mimeType;
        this.size = size;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    public File() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getFolderId() {
        return folderId;
    }

    public void setFolderId(long folderId) {
        this.folderId = folderId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
}