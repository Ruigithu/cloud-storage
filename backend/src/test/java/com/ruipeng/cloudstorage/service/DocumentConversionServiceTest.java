package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.dto.request.HtmlToDocxRequest;
import com.ruipeng.cloudstorage.dto.response.DocumentConversionResponse;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.FilePermission;
import com.ruipeng.cloudstorage.entity.FileVersion;
import com.ruipeng.cloudstorage.entity.PermissionType;
import com.ruipeng.cloudstorage.exception.FileProcessingException;
import com.ruipeng.cloudstorage.exception.InsufficientPermissionException;
import com.ruipeng.cloudstorage.exception.ResourceNotFoundException;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentConversionService Unit Test")
class DocumentConversionServiceTest {

    @Mock private FileMapper fileMapper;
    @Mock private FileVersionMapper fileVersionMapper;
    @Mock private FilePermissionMapper filePermissionMapper;
    @Mock private S3StorageService s3StorageService;

    @InjectMocks
    private DocumentConversionService documentConversionService;

    @Test
    @DisplayName("convert docx to html successfully")
    void convertDocxToHtml_success() throws IOException {
        File testFile = getTestMockFile();
        FileVersion testFileVersion = getTestMockFileVersion();
        FilePermission testFilePermission = getTestMockPermission();
        byte[] testDocxContent = getTestMockFileContent();

        Long testFileId = 1L;
        Long testOwnerId = 1L;


        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(testFile);
        when(fileVersionMapper.getLatestVersion(anyLong())).thenReturn(testFileVersion);
        when(s3StorageService.downloadFile(anyString())).thenReturn(testDocxContent);

        DocumentConversionResponse response = documentConversionService.convertDocxToHtml(testFileId, testOwnerId);

        assertThat(response).isNotNull();
        assertThat(response.getFileId()).isEqualTo(testFileId);
        assertThat(response.getMimeType()).isEqualTo(testFile.getMimeType());
        assertThat(response.getHtmlContent()).isEqualTo("<p>Hello, world!</p>");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
        verify(fileVersionMapper).getLatestVersion(anyLong());
        verify(s3StorageService).downloadFile(anyString());

    }

    @Test
    @DisplayName("failed to convert docx to html because of lacking permission")
    void convertDocxToHtml_lackPermission_notSuccess() throws IOException {

        Long testFileId = 1L;
        Long testOwnerId = 1L;

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(null);

        assertThatThrownBy(() -> documentConversionService.convertDocxToHtml(testFileId, testOwnerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No permission to access this file");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper,never()).getFileById(anyLong());
        verify(fileVersionMapper,never()).getLatestVersion(anyLong());
        verify(s3StorageService,never()).downloadFile(anyString());

    }

    @Test
    @DisplayName("failed to convert docx to html because of file not found")
    void convertDocxToHtml_fileNotFound_notSuccess() throws IOException {

        FilePermission testFilePermission = getTestMockPermission();

        Long testFileId = 1L;
        Long testOwnerId = 1L;


        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> documentConversionService.convertDocxToHtml(testFileId, testOwnerId))
        .isInstanceOf(RuntimeException.class)
                .hasMessage("File not found with id: 1");


        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
        verify(fileVersionMapper,never()).getLatestVersion(anyLong());
        verify(s3StorageService,never()).downloadFile(anyString());

    }

    @Test
    @DisplayName("failed to convert docx to html because of file version not found")
    void convertDocxToHtml_fileVersionNotFound_notSuccess() throws IOException {
        FilePermission testFilePermission = getTestMockPermission();
        File testFile = getTestMockFile();

        Long testFileId = 1L;
        Long testOwnerId = 1L;

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(testFile);
        when(fileVersionMapper.getLatestVersion(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> documentConversionService.convertDocxToHtml(testFileId, testOwnerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("File version for file not found with id: 1");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
        verify(fileVersionMapper).getLatestVersion(anyLong());
        verify(s3StorageService,never()).downloadFile(anyString());
    }

    @Test
    @DisplayName("failed to convert docx to html because of file version not found")
    void convertDocxToHtml_storagePathNotFound_notSuccess() throws IOException {
        FilePermission testFilePermission = getTestMockPermission();
        File testFile = getTestMockFile();
        FileVersion testFileVersion = getTestMockFileVersion();

        Long testFileId = 1L;
        Long testOwnerId = 1L;

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(testFile);
        when(fileVersionMapper.getLatestVersion(anyLong())).thenReturn(testFileVersion);
        when(s3StorageService.downloadFile(anyString())).thenThrow(new IOException());

        assertThatThrownBy(() -> documentConversionService.convertDocxToHtml(testFileId, testOwnerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to download file");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
        verify(fileVersionMapper).getLatestVersion(anyLong());
        verify(s3StorageService).downloadFile(anyString());

    }

    @Test
    @DisplayName("convert html to docx successfully")
    void convertHtmlToDocx_success() throws IOException {
        HtmlToDocxRequest htmlToDocxRequest = getHtmlToDocxRequest();
        File testFile = getTestMockFile();
        FileVersion testFileVersion = getTestMockFileVersion();
        FileVersion testNewFileVersion = getTestMockNewFileVersion();

        FilePermission testFilePermission = getTestMockPermission();

        Long testFileId = 1L;


        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(testFile);
        when(fileVersionMapper.getLatestVersionNumber(anyLong())).thenReturn(1);
        when(fileVersionMapper.getLatestVersion(anyLong()))
                .thenReturn(testFileVersion)
                .thenReturn(testNewFileVersion);
        when(s3StorageService.generateVersionedStorageKey(anyString(),anyInt())).thenReturn("path/to/file_v2.docx");


        DocumentConversionResponse response = documentConversionService.convertHtmlToDocx(htmlToDocxRequest);

        assertThat(response).isNotNull();
        assertThat(response.getFileId()).isEqualTo(testFileId);
        assertThat(response.getMimeType()).isEqualTo(testFile.getMimeType());
        assertThat(response.getVersionId()).isEqualTo(testNewFileVersion.getId());


        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
        verify(fileVersionMapper,times(2)).getLatestVersion(anyLong());
    }

    @Test
    @DisplayName("failed to convert html to docx because of lacking admin permission")
    void convertHtmlToDocx_lackAdminPermission_notSuccess() {
        HtmlToDocxRequest htmlToDocxRequest = getHtmlToDocxRequest();
        FilePermission readOnlyPermission = new FilePermission();
        readOnlyPermission.setPermission(PermissionType.READ);

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(readOnlyPermission);

        assertThatThrownBy(() -> documentConversionService.convertHtmlToDocx(htmlToDocxRequest))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessage("No permission to update this file");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper, never()).getFileById(anyLong());
    }

    @Test
    @DisplayName("failed to convert html to docx because of no permission")
    void convertHtmlToDocx_noPermission_notSuccess() {
        HtmlToDocxRequest htmlToDocxRequest = getHtmlToDocxRequest();

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(null);

        assertThatThrownBy(() -> documentConversionService.convertHtmlToDocx(htmlToDocxRequest))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessage("No permission to update this file");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper, never()).getFileById(anyLong());
    }

    @Test
    @DisplayName("failed to convert html to docx because of file not found")
    void convertHtmlToDocx_fileNotFound_notSuccess() {
        HtmlToDocxRequest htmlToDocxRequest = getHtmlToDocxRequest();
        FilePermission testFilePermission = getTestMockPermission();

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(null);

        assertThatThrownBy(() -> documentConversionService.convertHtmlToDocx(htmlToDocxRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("File");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
        verify(s3StorageService, never()).uploadFileBytes(any(byte[].class), anyString(), anyString());
    }

    @Test
    @DisplayName("failed to convert html to docx because of storage upload failure")
    void convertHtmlToDocx_storageUploadFailure_notSuccess() throws Exception {
        HtmlToDocxRequest htmlToDocxRequest = getHtmlToDocxRequest();
        File testFile = getTestMockFile();
        FileVersion testFileVersion = getTestMockFileVersion();
        FilePermission testFilePermission = getTestMockPermission();

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(testFile);
        when(fileVersionMapper.getLatestVersionNumber(anyLong())).thenReturn(1);
        when(fileVersionMapper.getLatestVersion(anyLong())).thenReturn(testFileVersion);
        when(s3StorageService.generateVersionedStorageKey(anyString(), anyInt())).thenReturn("path/to/file_v2.docx");
        doThrow(new RuntimeException("Storage error")).when(s3StorageService).uploadFileBytes(any(byte[].class), anyString(), anyString());

        assertThatThrownBy(() -> documentConversionService.convertHtmlToDocx(htmlToDocxRequest))
                .isInstanceOf(FileProcessingException.class)
                .hasMessage("Failed to upload DOCX to storage");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
        verify(s3StorageService).uploadFileBytes(any(byte[].class), anyString(), anyString());
        verify(fileMapper, never()).updateFile(any(File.class));
    }

    // ============ convertDocxToHtmlAsMap Tests ============

    @Test
    @DisplayName("convert docx to html as map successfully")
    void convertDocxToHtmlAsMap_success() throws IOException {
        File testFile = getTestMockFile();
        FileVersion testFileVersion = getTestMockFileVersion();
        FilePermission testFilePermission = getTestMockPermission();
        byte[] testDocxContent = getTestMockFileContent();

        Long testFileId = 1L;
        Long testOwnerId = 1L;

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(testFile);
        when(fileVersionMapper.getLatestVersion(anyLong())).thenReturn(testFileVersion);
        when(s3StorageService.downloadFile(anyString())).thenReturn(testDocxContent);

        Map<String, Object> result = documentConversionService.convertDocxToHtmlAsMap(testFileId, testOwnerId);

        assertThat(result).isNotNull();
        assertThat(result).containsKeys("htmlContent", "fileName", "mimeType", "fileId");
        assertThat(result.get("fileId")).isEqualTo(testFileId);
        assertThat(result.get("fileName")).isEqualTo(testFile.getName());
        assertThat(result.get("mimeType")).isEqualTo(testFile.getMimeType());
        assertThat(result.get("htmlContent")).isNotNull();

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
        verify(fileVersionMapper).getLatestVersion(anyLong());
        verify(s3StorageService).downloadFile(anyString());
    }

    @Test
    @DisplayName("failed to convert docx to html as map because of lacking permission")
    void convertDocxToHtmlAsMap_lackPermission_notSuccess() {
        Long testFileId = 1L;
        Long testOwnerId = 1L;

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(null);

        assertThatThrownBy(() -> documentConversionService.convertDocxToHtmlAsMap(testFileId, testOwnerId))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessage("No permission to access this file");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
    }

    // ============ convertDocxToHtmlMammothAsMap Tests ============

    @Test
    @DisplayName("convert docx to html mammoth as map successfully")
    void convertDocxToHtmlMammothAsMap_success() throws IOException {
        File testFile = getTestMockFile();
        FileVersion testFileVersion = getTestMockFileVersion();
        FilePermission testFilePermission = getTestMockPermission();
        byte[] testDocxContent = getTestMockFileContent();

        Long testFileId = 1L;
        Long testOwnerId = 1L;

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(testFile);
        when(fileVersionMapper.getLatestVersion(anyLong())).thenReturn(testFileVersion);
        when(s3StorageService.downloadFile(anyString())).thenReturn(testDocxContent);

        Map<String, Object> result = documentConversionService.convertDocxToHtmlMammothAsMap(testFileId, testOwnerId);

        assertThat(result).isNotNull();
        assertThat(result).containsKeys("htmlContent", "fileName", "mimeType", "fileId");
        assertThat(result.get("fileId")).isEqualTo(testFileId);
        assertThat(result.get("fileName")).isEqualTo(testFile.getName());
        assertThat(result.get("error")).isNull();

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
    }

    @Test
    @DisplayName("convert docx to html mammoth as map returns error on exception")
    void convertDocxToHtmlMammothAsMap_exception_returnsError() {
        Long testFileId = 1L;
        Long testOwnerId = 1L;

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenThrow(new RuntimeException("Test exception"));

        Map<String, Object> result = documentConversionService.convertDocxToHtmlMammothAsMap(testFileId, testOwnerId);

        assertThat(result).isNotNull();
        assertThat(result.get("error")).isEqualTo(true);
        assertThat(result.get("htmlContent")).asString().contains("Error processing document");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
    }

    // ============ convertHtmlToDocxAsMap Tests ============

    @Test
    @DisplayName("convert html to docx as map successfully")
    void convertHtmlToDocxAsMap_success() throws Exception {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("htmlContent", "<p>Hello, world!</p>");
        requestBody.put("fileName", "test.docx");
        requestBody.put("ownerId", "1");
        requestBody.put("fileId", "1");

        File testFile = getTestMockFile();
        FileVersion testFileVersion = getTestMockFileVersion();
        FileVersion testNewFileVersion = getTestMockNewFileVersion();
        FilePermission testFilePermission = getTestMockPermission();

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(testFilePermission);
        when(fileMapper.getFileById(anyLong())).thenReturn(testFile);
        when(fileVersionMapper.getLatestVersionNumber(anyLong())).thenReturn(1);
        when(fileVersionMapper.getLatestVersion(anyLong()))
                .thenReturn(testFileVersion)
                .thenReturn(testNewFileVersion);
        when(s3StorageService.generateVersionedStorageKey(anyString(), anyInt())).thenReturn("path/to/file_v2.docx");
        doNothing().when(s3StorageService).uploadFileBytes(any(byte[].class), anyString(), anyString());
        doNothing().when(fileVersionMapper).insertVersion(any(FileVersion.class));
        when(fileMapper.updateFile(any(File.class))).thenReturn(1);

        Map<String, Object> result = documentConversionService.convertHtmlToDocxAsMap(requestBody);

        assertThat(result).isNotNull();
        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("message")).isEqualTo("Word document created successfully");
        assertThat(result.get("fileId")).isEqualTo(1L);
        assertThat(result.get("versionId")).isEqualTo(testNewFileVersion.getId());
        assertThat(result).containsKeys("fileName", "mimeType");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
        verify(fileMapper).getFileById(anyLong());
    }

    @Test
    @DisplayName("failed to convert html to docx as map because of lacking admin permission")
    void convertHtmlToDocxAsMap_lackAdminPermission_notSuccess() {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("htmlContent", "<p>Hello, world!</p>");
        requestBody.put("fileName", "test.docx");
        requestBody.put("ownerId", "1");
        requestBody.put("fileId", "1");

        FilePermission readOnlyPermission = new FilePermission();
        readOnlyPermission.setPermission(PermissionType.READ);

        when(filePermissionMapper.findByFileIdAndUserId(anyLong(), anyLong())).thenReturn(readOnlyPermission);

        assertThatThrownBy(() -> documentConversionService.convertHtmlToDocxAsMap(requestBody))
                .isInstanceOf(InsufficientPermissionException.class)
                .hasMessage("No permission to update this file");

        verify(filePermissionMapper).findByFileIdAndUserId(anyLong(), anyLong());
    }

    // ============ Helper Methods ============

    private FilePermission getTestMockPermission() {
        FilePermission filePermission = new FilePermission();
        filePermission.setPermission(PermissionType.ADMIN);
        return filePermission;
    }

    private File getTestMockFile() {
        File file = new File();
        file.setId(1L);
        file.setName("test.docx");
        file.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        return file;
    }

    private FileVersion getTestMockFileVersion() {
        FileVersion fileVersion = new FileVersion();
        fileVersion.setId(1L);
        fileVersion.setFileId(1L);
        fileVersion.setVersionNumber(1);
        fileVersion.setStoragePath("path/to/file.docx");
        return fileVersion;
    }

    private FileVersion getTestMockNewFileVersion() {
        FileVersion fileVersion = new FileVersion();
        fileVersion.setId(2L);
        fileVersion.setFileId(1L);
        fileVersion.setVersionNumber(2);
        fileVersion.setStoragePath("path/to/file_v2.docx");
        return fileVersion;
    }

    private byte[] getTestMockFileContent() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Hello, world!");
            doc.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HtmlToDocxRequest getHtmlToDocxRequest() {
        HtmlToDocxRequest htmlToDocxRequest = new HtmlToDocxRequest();
        htmlToDocxRequest.setHtmlContent("<p>Hello, world!</p>");
        htmlToDocxRequest.setFileName("test.docx");
        htmlToDocxRequest.setOwnerId(1L);
        htmlToDocxRequest.setFileId(1L);
        return htmlToDocxRequest;
    }
}

