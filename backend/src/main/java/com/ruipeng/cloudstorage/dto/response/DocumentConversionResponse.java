package com.ruipeng.cloudstorage.dto.response;


public class DocumentConversionResponse {
    private String htmlContent;
    private String fileName;
    private String mimeType;
    private Long fileId;
    private Long versionId;
    private String fakeDocx;
    private Boolean error;

    public DocumentConversionResponse() {}

    public DocumentConversionResponse(String htmlContent, String fileName, String mimeType,
                                      Long fileId, Long versionId, String fakeDocx, Boolean error) {
        this.htmlContent = htmlContent;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.fileId = fileId;
        this.versionId = versionId;
        this.fakeDocx = fakeDocx;
        this.error = error;
    }

    public static DocumentConversionResponseBuilder builder() {
        return new DocumentConversionResponseBuilder();
    }

    // Getters and setters omitted for brevity
    public String getHtmlContent() { return htmlContent; }
    public void setHtmlContent(String htmlContent) { this.htmlContent = htmlContent; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public String getFakeDocx() { return fakeDocx; }
    public void setFakeDocx(String fakeDocx) { this.fakeDocx = fakeDocx; }

    public Boolean getError() { return error; }
    public void setError(Boolean error) { this.error = error; }
}