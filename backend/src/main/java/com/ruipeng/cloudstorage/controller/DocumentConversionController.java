package com.ruipeng.cloudstorage.controller;

import com.ruipeng.cloudstorage.service.DocumentConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for document conversion operations.
 * Handles DOCX to HTML and HTML to DOCX conversions.
 */
@RestController
public class DocumentConversionController {
    private static final Logger log = LoggerFactory.getLogger(DocumentConversionController.class);

    private final DocumentConversionService conversionService;

    /**
     * Constructor with dependency injection.
     *
     * @param conversionService the document conversion service
     */
    public DocumentConversionController(DocumentConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * Converts a DOCX file to HTML format.
     *
     * @param ownerId the owner user ID
     * @param fileId the file ID
     * @return HTML content and file information
     */
    @GetMapping("/convertDocxToHtml")
    public ResponseEntity<?> convertDocxToHtml(
            @RequestParam("ownerId") long ownerId,
            @RequestParam("fileId") long fileId) {
        logConvertDocxToHtml(fileId, ownerId);

        try {
            Map<String, Object> response = conversionService.convertDocxToHtmlAsMap(fileId, ownerId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to convert DOCX to HTML: fileId={}", fileId, e);
            return buildErrorResponse("Failed to convert DOCX to HTML: " + e.getMessage());
        }
    }

    /**
     * Converts a DOCX file to HTML using Mammoth library.
     *
     * @param ownerId the owner user ID
     * @param fileId the file ID
     * @return HTML content and file information
     */
    @GetMapping("/convertDocxToHtmlMammoth")
    public ResponseEntity<?> convertDocxToHtmlMammoth(
            @RequestParam("ownerId") long ownerId,
            @RequestParam("fileId") long fileId) {
        logConvertDocxToHtmlMammoth(fileId, ownerId);

        try {
            Map<String, Object> response = conversionService.convertDocxToHtmlMammothAsMap(
                    fileId, ownerId
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to convert DOCX to HTML (Mammoth): fileId={}", fileId, e);
            return buildDocumentErrorResponse(e);
        }
    }

    /**
     * Converts HTML content to DOCX format.
     *
     * @param requestBody the request containing HTML content and metadata
     * @return conversion result with file information
     */
    @PostMapping("/convertHtmlToDocx")
    public ResponseEntity<?> convertHtmlToDocx(@RequestBody Map<String, String> requestBody) {
        logConvertHtmlToDocx(requestBody.get("fileName"));

        if (!validateHtmlToDocxRequest(requestBody)) {
            return ResponseEntity.badRequest()
                    .body("HTML content and file name are required");
        }

        try {
            Map<String, Object> response = conversionService.convertHtmlToDocxAsMap(requestBody);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to convert HTML to DOCX", e);
            return buildErrorResponse("Failed to convert HTML to DOCX: " + e.getMessage());
        }
    }

    // ============ Private Helper Methods ============

    /**
     * Validates HTML to DOCX conversion request.
     */
    private boolean validateHtmlToDocxRequest(Map<String, String> requestBody) {
        String htmlContent = requestBody.get("htmlContent");
        String fileName = requestBody.get("fileName");
        return htmlContent != null && fileName != null;
    }

    /**
     * Builds error response.
     */
    private ResponseEntity<?> buildErrorResponse(String errorMessage) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorMessage);
    }

    /**
     * Builds document processing error response.
     */
    private ResponseEntity<?> buildDocumentErrorResponse(Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("htmlContent", "<p>Error processing document: " + e.getMessage() + "</p>");
        errorResponse.put("error", true);
        return ResponseEntity.ok(errorResponse);
    }

    // ============ Logging Methods ============

    private void logConvertDocxToHtml(long fileId, long ownerId) {
        log.info("Converting DOCX to HTML: fileId={}, ownerId={}", fileId, ownerId);
    }

    private void logConvertDocxToHtmlMammoth(long fileId, long ownerId) {
        log.info("Converting DOCX to HTML (Mammoth): fileId={}, ownerId={}", fileId, ownerId);
    }

    private void logConvertHtmlToDocx(String fileName) {
        log.info("Converting HTML to DOCX: fileName={}", fileName);
    }
}