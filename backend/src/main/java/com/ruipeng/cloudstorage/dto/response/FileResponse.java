package com.ruipeng.cloudstorage.dto.response;


public class FileResponse {
    private Long fileId;
    private String fileName;
    private String mimeType;
    private Long size;
    private Long versionId;
    private boolean isWordDocument;
    private String content;

    public FileResponse() {}

    public FileResponse(Long fileId, String fileName, String mimeType, Long size,
                        Long versionId, boolean isWordDocument, String content) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.size = size;
        this.versionId = versionId;
        this.isWordDocument = isWordDocument;
        this.content = content;
    }

    public static FileResponseBuilder builder() {
        return new FileResponseBuilder();
    }

    // Getters and setters
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public boolean isWordDocument() { return isWordDocument; }
    public void setWordDocument(boolean wordDocument) { isWordDocument = wordDocument; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
