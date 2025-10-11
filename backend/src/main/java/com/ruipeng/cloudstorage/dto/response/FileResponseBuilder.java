package com.ruipeng.cloudstorage.dto.response;

/**
 * Builder class for FileResponse.
 */
public class FileResponseBuilder {
    private Long fileId;
    private String fileName;
    private String mimeType;
    private Long size;
    private Long versionId;
    private boolean isWordDocument;
    private String content;

    public FileResponseBuilder fileId(Long fileId) {
        this.fileId = fileId;
        return this;
    }

    public FileResponseBuilder fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public FileResponseBuilder mimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public FileResponseBuilder size(Long size) {
        this.size = size;
        return this;
    }

    public FileResponseBuilder versionId(Long versionId) {
        this.versionId = versionId;
        return this;
    }

    public FileResponseBuilder isWordDocument(boolean isWordDocument) {
        this.isWordDocument = isWordDocument;
        return this;
    }

    public FileResponseBuilder content(String content) {
        this.content = content;
        return this;
    }

    public FileResponse build() {
        return new FileResponse(fileId, fileName, mimeType, size,
                versionId, isWordDocument, content);
    }
}
