package com.ruipeng.cloudstorage.entity;

import org.springframework.core.io.Resource;

public class DownloadFileInfo {
    private String fileName;
    private String mimeType;
    private Resource resource;

    // 构造函数、getter和setter
    public DownloadFileInfo(String fileName, String mimeType, Resource resource) {
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.resource = resource;
    }

    // getters and setters

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}