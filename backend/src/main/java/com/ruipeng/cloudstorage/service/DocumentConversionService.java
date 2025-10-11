package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.dto.request.HtmlToDocxRequest;
import com.ruipeng.cloudstorage.dto.response.DocumentConversionResponse;
import com.ruipeng.cloudstorage.entity.FilePermission;
import com.ruipeng.cloudstorage.entity.FileVersion;
import com.ruipeng.cloudstorage.entity.PermissionType;
import com.ruipeng.cloudstorage.exception.FileProcessingException;
import com.ruipeng.cloudstorage.exception.InsufficientPermissionException;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zwobble.mammoth.DocumentConverter;
import org.zwobble.mammoth.Result;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for converting documents between different formats.
 *
 * Currently supports:
 * - DOCX to HTML conversion (using Mammoth)
 * - HTML to DOCX conversion (using Docx4j)
 */
@Service
public class DocumentConversionService {
    private static final Logger log = LoggerFactory.getLogger(DocumentConversionService.class);

    private final FileMapper fileMapper;
    private final FileVersionMapper fileVersionMapper;
    private final FilePermissionMapper filePermissionMapper;
    private final S3StorageService storageService;

    public DocumentConversionService(FileMapper fileMapper,
                                     FileVersionMapper fileVersionMapper,
                                     FilePermissionMapper filePermissionMapper,
                                     S3StorageService storageService) {
        this.fileMapper = fileMapper;
        this.fileVersionMapper = fileVersionMapper;
        this.filePermissionMapper = filePermissionMapper;
        this.storageService = storageService;
    }

    /**
     * Converts a DOCX file to HTML format.
     */
    public DocumentConversionResponse convertDocxToHtml(Long fileId, Long ownerId) {
        validateFileAccess(fileId, ownerId);

        com.ruipeng.cloudstorage.entity.File file = findFileById(fileId);
        FileVersion latestVersion = getLatestVersion(fileId);

        byte[] docxBytes = downloadFile(latestVersion.getStoragePath());
        String htmlContent = convertDocxBytesToHtml(docxBytes);

        log.info("DOCX converted to HTML: fileId={}", fileId);

        return buildConversionResponse(file, htmlContent, fileId);
    }

    /**
     * Converts HTML content to DOCX and saves as new version.
     */
    @Transactional
    public DocumentConversionResponse convertHtmlToDocx(HtmlToDocxRequest request) {
        validateFileUpdateAccess(request.getFileId(), request.getOwnerId());

        com.ruipeng.cloudstorage.entity.File existingFile = findFileById(request.getFileId());

        byte[] docxBytes = convertHtmlToDocxBytes(request.getHtmlContent());
        int newVersionNumber = createNewVersion(existingFile, docxBytes, request.getOwnerId());
        updateFileMetadata(existingFile, request.getFileName(), docxBytes.length);

        log.info("HTML converted to DOCX: fileId={}, version={}",
                request.getFileId(), newVersionNumber);

        return buildDocxConversionResponse(existingFile, newVersionNumber);
    }

    // ============ Private Helper Methods ============

    private void validateFileAccess(Long fileId, Long ownerId) {
        FilePermission permission = filePermissionMapper.findByFileIdAndUserId(fileId, ownerId);
        if (permission == null) {
            throw new InsufficientPermissionException("No permission to access this file");
        }
    }

    private void validateFileUpdateAccess(Long fileId, Long ownerId) {
        FilePermission permission = filePermissionMapper.findByFileIdAndUserId(fileId, ownerId);
        if (permission == null || permission.getPermission() != PermissionType.ADMIN) {
            throw new InsufficientPermissionException("No permission to update this file");
        }
    }

    private com.ruipeng.cloudstorage.entity.File findFileById(Long fileId) {
        com.ruipeng.cloudstorage.entity.File file = fileMapper.getFileById(fileId);
        if (file == null) {
            throw new ResourceNotFoundException("File", fileId);
        }
        return file;
    }

    private FileVersion getLatestVersion(Long fileId) {
        FileVersion version = fileVersionMapper.getLatestVersion(fileId);
        if (version == null) {
            throw new ResourceNotFoundException("File version for file", fileId);
        }
        return version;
    }

    private byte[] downloadFile(String storagePath) {
        try {
            return storageService.downloadFile(storagePath);
        } catch (IOException e) {
            throw new FileProcessingException("Failed to download file", e);
        }
    }

    private String convertDocxBytesToHtml(byte[] docxBytes) {
        try {
            java.io.File tempFile = createTempDocxFile(docxBytes);
            String html = convertDocxFileToHtml(tempFile);
            deleteTempFile(tempFile);
            return html;
        } catch (Exception e) {
            log.error("Failed to convert DOCX to HTML", e);
            throw new FileProcessingException("Failed to convert DOCX to HTML", e);
        }
    }

    private java.io.File createTempDocxFile(byte[] docxBytes) throws IOException {
        java.io.File tempFile = java.io.File.createTempFile("document_", ".docx");
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(docxBytes);
            fos.flush();
        }

        return tempFile;
    }

    private String convertDocxFileToHtml(java.io.File docxFile) throws IOException {
        DocumentConverter converter = new DocumentConverter();
        Result<String> result = converter.convertToHtml(docxFile);
        return result.getValue();
    }

    private void deleteTempFile(java.io.File tempFile) {
        if (tempFile.exists()) {
            tempFile.delete();
        }
    }


    private int createNewVersion(com.ruipeng.cloudstorage.entity.File file,
                                 byte[] docxBytes, Long ownerId) {
        int currentVersion = fileVersionMapper.getLatestVersionNumber(file.getId());
        int newVersionNumber = currentVersion + 1;

        FileVersion latestVersion = getLatestVersion(file.getId());
        String newS3Key = generateNewVersionKey(latestVersion.getStoragePath(), newVersionNumber);

        uploadDocxToStorage(docxBytes, newS3Key);
        saveFileVersion(file.getId(), newS3Key, (long) docxBytes.length, ownerId, newVersionNumber);

        return newVersionNumber;
    }

    private String generateNewVersionKey(String previousPath, int newVersion) {
        return storageService.generateVersionedStorageKey(previousPath, newVersion);
    }


    private void saveFileVersion(Long fileId, String storagePath,
                                 Long size, Long createdBy, int versionNumber) {
        FileVersion version = createNewFileVersion(fileId, storagePath, size, createdBy, versionNumber,fileVersionMapper);
    }

    static FileVersion createNewFileVersion(Long fileId, String storagePath, Long size, Long createdBy, int versionNumber,FileVersionMapper fileVersionMapper) {
        FileVersion version = new FileVersion();
        version.setFileId(fileId);
        version.setVersionNumber(versionNumber);
        version.setStoragePath(storagePath);
        version.setSize(size);
        version.setCreatedBy(createdBy);
        version.setCreatedAt(Instant.now());
        fileVersionMapper.insertVersion(version);
        return version;
    }

    private void updateFileMetadata(com.ruipeng.cloudstorage.entity.File file,
                                    String fileName, long fileSize) {
        String finalName = fileName.endsWith(".docx") ? fileName : fileName + ".docx";

        file.setName(finalName);
        file.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        file.setSize(fileSize);
        file.setUpdatedAt(Instant.now());

        fileMapper.updateFile(file);
    }

    private DocumentConversionResponse buildConversionResponse(
            com.ruipeng.cloudstorage.entity.File file, String htmlContent, Long fileId) {
        return DocumentConversionResponse.builder()
                .htmlContent(htmlContent)
                .fileName(file.getName())
                .mimeType(file.getMimeType())
                .fileId(fileId)
                .build();
    }

    private DocumentConversionResponse buildDocxConversionResponse(
            com.ruipeng.cloudstorage.entity.File file, int versionNumber) {
        FileVersion newVersion = fileVersionMapper.getLatestVersion(file.getId());

        return DocumentConversionResponse.builder()
                .fileName(file.getName())
                .mimeType(file.getMimeType())
                .fileId(file.getId())
                .versionId(newVersion.getId())
                .build();
    }
    /**
     * Converts DOCX to HTML and returns as Map for Controller.
     *
     * @param fileId the file ID
     * @param ownerId the owner user ID
     * @return map containing HTML content and metadata
     */
    public Map<String, Object> convertDocxToHtmlAsMap(Long fileId, Long ownerId) {
        return generateDocumentConversionResponse(fileId, ownerId);
    }

    /**
     * Converts DOCX to HTML using Mammoth and returns as Map.
     *
     * @param fileId the file ID
     * @param ownerId the owner user ID
     * @return map containing HTML content and metadata
     */
    public Map<String, Object> convertDocxToHtmlMammothAsMap(Long fileId, Long ownerId) {
        // Similar implementation with error handling
        try {
            return generateDocumentConversionResponse(fileId, ownerId);
        } catch (Exception e) {
            log.error("Document conversion error", e);

            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("htmlContent", "<p>Error processing document: " + e.getMessage() + "</p>");
            errorMap.put("error", true);

            return errorMap;
        }
    }

    private Map<String, Object> generateDocumentConversionResponse(Long fileId, Long ownerId) {
        DocumentConversionResponse response = convertDocxToHtml(fileId, ownerId);

        Map<String, Object> map = new HashMap<>();
        map.put("htmlContent", response.getHtmlContent());
        map.put("fileName", response.getFileName());
        map.put("mimeType", response.getMimeType());
        map.put("fileId", response.getFileId());

        return map;
    }

    /**
     * Converts HTML to DOCX and returns as Map.
     *
     * @param requestBody map containing htmlContent, fileName, ownerId, fileId
     * @return map containing conversion result
     */
    public Map<String, Object> convertHtmlToDocxAsMap(Map<String, String> requestBody) {
        HtmlToDocxRequest request = new HtmlToDocxRequest();
        request.setHtmlContent(requestBody.get("htmlContent"));
        request.setFileName(requestBody.get("fileName"));
        request.setOwnerId(Long.parseLong(requestBody.get("ownerId")));
        request.setFileId(Long.parseLong(requestBody.get("fileId")));

        DocumentConversionResponse response = convertHtmlToDocx(request);

        Map<String, Object> map = new HashMap<>();
        map.put("success", true);
        map.put("message", "Word document created successfully");
        map.put("fileName", response.getFileName());
        map.put("mimeType", response.getMimeType());
        map.put("fileId", response.getFileId());
        map.put("versionId", response.getVersionId());

        return map;
    }
    /**
     * Converts HTML content to DOCX byte array using Docx4j.
     */
    private byte[] convertHtmlToDocxBytes(String htmlContent) {
        try {
            // Create a new Word document
            org.docx4j.openpackaging.packages.WordprocessingMLPackage wordMLPackage =
                    org.docx4j.openpackaging.packages.WordprocessingMLPackage.createPackage();

            // Clean and prepare HTML
            String cleanedHtml = prepareHtmlForConversion(htmlContent);

            // Convert HTML to DOCX using Docx4j's XHTMLImporter
            org.docx4j.convert.in.xhtml.XHTMLImporterImpl xhtmlImporter =
                    new org.docx4j.convert.in.xhtml.XHTMLImporterImpl(wordMLPackage);

            // Configure the importer
            xhtmlImporter.setHyperlinkStyle("Hyperlink");

            // Convert HTML string to DOCX content
            List<Object> blocks = xhtmlImporter.convert(cleanedHtml, null);

            // Add converted content to document
            wordMLPackage.getMainDocumentPart().getContent().addAll(blocks);

            // Convert to byte array
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            wordMLPackage.save(outputStream);

            byte[] docxBytes = outputStream.toByteArray();
            log.info("Successfully converted HTML to DOCX ({} bytes)", docxBytes.length);

            return docxBytes;

        } catch (Exception e) {
            log.error("Failed to convert HTML to DOCX", e);
            throw new FileProcessingException("Failed to convert HTML to DOCX", e);
        }
    }

    /**
     * Prepares HTML content for conversion by ensuring proper structure.
     */
    private String prepareHtmlForConversion(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return "<html><body><p></p></body></html>";
        }

        // If HTML doesn't have html/body tags, wrap it
        String trimmed = htmlContent.trim();
        if (!trimmed.toLowerCase().startsWith("<html")) {
            if (!trimmed.toLowerCase().startsWith("<body")) {
                htmlContent = "<body>" + htmlContent + "</body>";
            }
            htmlContent = "<html>" + htmlContent + "</html>";
        }

        return htmlContent;
    }

    /**
     * Uploads DOCX byte array to S3 storage.
     */
    private void uploadDocxToStorage(byte[] docxBytes, String s3Key) {
        try {
            storageService.uploadFileBytes(
                    docxBytes,
                    s3Key,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
            log.info("DOCX uploaded to storage: {}", s3Key);
        } catch (Exception e) {
            log.error("Failed to upload DOCX to storage: {}", s3Key, e);
            throw new FileProcessingException("Failed to upload DOCX to storage", e);
        }
    }

}
