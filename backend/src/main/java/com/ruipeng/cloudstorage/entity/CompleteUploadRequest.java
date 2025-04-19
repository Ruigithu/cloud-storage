package com.ruipeng.cloudstorage.entity;

import java.util.List;

public class CompleteUploadRequest {
    private Long fileId;
    private String uploadId;
    private List<PartETagDto> partETags;

    // getters and setters

    public CompleteUploadRequest() {
    }

    public CompleteUploadRequest(Long fileId, String uploadId, List<PartETagDto> partETags) {
        this.fileId = fileId;
        this.uploadId = uploadId;
        this.partETags = partETags;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public String getUploadId() {
        return uploadId;
    }

    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    public List<PartETagDto> getPartETags() {
        return partETags;
    }

    public void setPartETags(List<PartETagDto> partETags) {
        this.partETags = partETags;
    }
}

