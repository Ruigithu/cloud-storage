package com.ruipeng.cloudstorage.dto.response;


/**
 * Builder class for DocumentConversionResponse.
 */
public class DocumentConversionResponseBuilder {
    private String htmlContent;
    private String fileName;
    private String mimeType;
    private Long fileId;
    private Long versionId;
    private String fakeDocx;
    private Boolean error;

    public DocumentConversionResponseBuilder htmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
        return this;
    }

    public DocumentConversionResponseBuilder fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public DocumentConversionResponseBuilder mimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public DocumentConversionResponseBuilder fileId(Long fileId) {
        this.fileId = fileId;
        return this;
    }

    public DocumentConversionResponseBuilder versionId(Long versionId) {
        this.versionId = versionId;
        return this;
    }

    public DocumentConversionResponseBuilder fakeDocx(String fakeDocx) {
        this.fakeDocx = fakeDocx;
        return this;
    }

    public DocumentConversionResponseBuilder error(Boolean error) {
        this.error = error;
        return this;
    }

    public DocumentConversionResponse build() {
        return new DocumentConversionResponse(htmlContent, fileName, mimeType,
                fileId, versionId, fakeDocx, error);
    }
}
