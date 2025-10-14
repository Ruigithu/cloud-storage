package com.ruipeng.cloudstorage.service;

import com.ruipeng.cloudstorage.dto.request.HtmlToDocxRequest;
import com.ruipeng.cloudstorage.dto.response.DocumentConversionResponse;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.FilePermission;
import com.ruipeng.cloudstorage.entity.FileVersion;
import com.ruipeng.cloudstorage.entity.PermissionType;
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
        fileVersion.setId(1L);
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
