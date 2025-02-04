package com.ruipeng.cloudstorage.entity;

import lombok.Data;
import org.postgresql.util.PGobject;

import java.time.Instant;

@Data
public class Folder {
    private Long id;
    private String name;
    private Long parentId;
    private Long ownerId;
    private Instant createdAt;
    private Instant updatedAt;
    private  PGobject path;    // ltree 类型存储为 String
    private boolean isDeleted;

    public Folder(Long id, String name, Long ownerId, Long parentId, Instant createdAt, Instant updatedAt, PGobject path, boolean isDeleted) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.parentId = parentId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.path = path;
        this.isDeleted = isDeleted;
    }

    public Folder(String name, Long parentId, Long ownerId, Instant createdAt, Instant updatedAt, boolean isDeleted) {
        this.name = name;
        this.parentId = parentId;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isDeleted = isDeleted;
    }

    public Folder() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
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

    public PGobject getPath() {
        return path;
    }

    public void setPath(PGobject path) {
        this.path = path;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }
}
