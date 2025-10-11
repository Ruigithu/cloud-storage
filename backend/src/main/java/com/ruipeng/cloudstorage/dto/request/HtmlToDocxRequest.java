package com.ruipeng.cloudstorage.dto.request;

public class HtmlToDocxRequest {
    private String htmlContent;
    private String fileName;
    private Long ownerId;
    private Long fileId;

    public HtmlToDocxRequest(String htmlContent, String fileName, Long ownerId, Long fileId) {
        this.htmlContent = htmlContent;
        this.fileName = fileName;
        this.ownerId = ownerId;
        this.fileId = fileId;
    }

    public HtmlToDocxRequest() {
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
}
