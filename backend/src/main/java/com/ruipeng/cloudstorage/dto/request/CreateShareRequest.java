package com.ruipeng.cloudstorage.dto.request;

public class CreateShareRequest {
    private Long fileId;
    private Long userId;
    private String accessType;
    private String expiresAt;

    public CreateShareRequest(Long fileId, Long userId, String accessType, String expiresAt) {
        this.fileId = fileId;
        this.userId = userId;
        this.accessType = accessType;
        this.expiresAt = expiresAt;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}
