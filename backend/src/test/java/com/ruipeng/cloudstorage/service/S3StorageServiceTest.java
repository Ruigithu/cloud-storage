package com.ruipeng.cloudstorage.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("S3StorageService Unit Test")
class S3StorageServiceTest {

    @Mock
    private AmazonS3 s3Client;

    private S3StorageService s3StorageService;

    private static final String BUCKET_NAME = "test-bucket";
    private static final String TEST_KEY = "test/file.txt";

    @BeforeEach
    void setUp() {
        s3StorageService = new S3StorageService("us-east-1", BUCKET_NAME);
        ReflectionTestUtils.setField(s3StorageService, "s3Client", s3Client);
    }

    // ============ uploadFile Tests ============

    @Test
    @DisplayName("upload file successfully uploads to s3")
    void uploadFile_success() throws IOException {
        MultipartFile mockFile = getTestMockMultipartFile();
        String keyName = "1/10/test.txt";

        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
        when(s3Client.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                .thenReturn(new PutObjectResult());

        s3StorageService.uploadFile(mockFile, keyName);

        ArgumentCaptor<String> bucketCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);

        verify(s3Client).putObject(bucketCaptor.capture(), keyCaptor.capture(),
                any(InputStream.class), metadataCaptor.capture());

        assertThat(bucketCaptor.getValue()).isEqualTo(BUCKET_NAME);
        assertThat(keyCaptor.getValue()).isEqualTo(keyName);
        assertThat(metadataCaptor.getValue().getContentLength()).isEqualTo(mockFile.getSize());
        assertThat(metadataCaptor.getValue().getContentType()).isEqualTo(mockFile.getContentType());
    }

    @Test
    @DisplayName("upload file throws exception when io error occurs")
    void uploadFile_ioError_throwsException() throws IOException {
        MultipartFile mockFile = getTestMockMultipartFile();
        String keyName = "1/10/test.txt";

        when(mockFile.getInputStream()).thenThrow(new IOException("IO Error"));

        assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, keyName))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file");

        verify(s3Client, never()).putObject(anyString(), anyString(),
                any(InputStream.class), any(ObjectMetadata.class));
    }

    @Test
    @DisplayName("upload file throws exception when s3 upload fails")
    void uploadFile_s3Error_throwsException() throws IOException {
        MultipartFile mockFile = getTestMockMultipartFile();
        String keyName = "1/10/test.txt";

        when(mockFile.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        when(s3Client.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                .thenThrow(new RuntimeException("S3 Error"));

        assertThatThrownBy(() -> s3StorageService.uploadFile(mockFile, keyName))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("S3 Error");
    }

    // ============ downloadFile Tests ============

    @Test
    @DisplayName("download file successfully retrieves file from s3")
    void downloadFile_success() throws IOException {
        String keyName = "1/10/test.txt";
        byte[] expectedContent = "test content".getBytes();
        S3Object s3Object = createMockS3Object(expectedContent);

        when(s3Client.getObject(BUCKET_NAME, keyName)).thenReturn(s3Object);

        byte[] result = s3StorageService.downloadFile(keyName);

        assertThat(result).isEqualTo(expectedContent);
        verify(s3Client).getObject(BUCKET_NAME, keyName);
    }

    @Test
    @DisplayName("download file throws exception when file not found")
    void downloadFile_fileNotFound_throwsException() {
        String keyName = "nonexistent/file.txt";

        when(s3Client.getObject(BUCKET_NAME, keyName))
                .thenThrow(new RuntimeException("File not found"));

        assertThatThrownBy(() -> s3StorageService.downloadFile(keyName))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to download file");

        verify(s3Client).getObject(BUCKET_NAME, keyName);
    }

    @Test
    @DisplayName("download file throws exception when s3 error occurs")
    void downloadFile_s3Error_throwsException() {
        String keyName = "1/10/test.txt";

        when(s3Client.getObject(BUCKET_NAME, keyName))
                .thenThrow(new RuntimeException("S3 service error"));

        assertThatThrownBy(() -> s3StorageService.downloadFile(keyName))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to download file");

        verify(s3Client).getObject(BUCKET_NAME, keyName);
    }

    // ============ deleteFile Tests ============

    @Test
    @DisplayName("delete file successfully deletes when file exists")
    void deleteFile_fileExists_success() {
        String keyName = "1/10/test.txt";

        when(s3Client.doesObjectExist(BUCKET_NAME, keyName)).thenReturn(true);
        doNothing().when(s3Client).deleteObject(BUCKET_NAME, keyName);

        s3StorageService.deleteFile(keyName);

        verify(s3Client).doesObjectExist(BUCKET_NAME, keyName);
        verify(s3Client).deleteObject(BUCKET_NAME, keyName);
    }

    @Test
    @DisplayName("delete file does nothing when file does not exist")
    void deleteFile_fileNotExists_doesNothing() {
        String keyName = "nonexistent/file.txt";

        when(s3Client.doesObjectExist(BUCKET_NAME, keyName)).thenReturn(false);

        s3StorageService.deleteFile(keyName);

        verify(s3Client).doesObjectExist(BUCKET_NAME, keyName);
        verify(s3Client, never()).deleteObject(anyString(), anyString());
    }

    // ============ doesFileExist Tests ============

    @Test
    @DisplayName("does file exist returns true when file exists")
    void doesFileExist_fileExists_returnsTrue() {
        String keyName = "1/10/test.txt";

        when(s3Client.doesObjectExist(BUCKET_NAME, keyName)).thenReturn(true);

        boolean result = s3StorageService.doesFileExist(keyName);

        assertThat(result).isTrue();
        verify(s3Client).doesObjectExist(BUCKET_NAME, keyName);
    }

    @Test
    @DisplayName("does file exist returns false when file does not exist")
    void doesFileExist_fileNotExists_returnsFalse() {
        String keyName = "nonexistent/file.txt";

        when(s3Client.doesObjectExist(BUCKET_NAME, keyName)).thenReturn(false);

        boolean result = s3StorageService.doesFileExist(keyName);

        assertThat(result).isFalse();
        verify(s3Client).doesObjectExist(BUCKET_NAME, keyName);
    }

    // ============ generateStorageKey Tests ============

    @Test
    @DisplayName("generate storage key creates correct key for simple file")
    void generateStorageKey_simpleFile_success() {
        Long ownerId = 1L;
        Long folderId = 10L;
        Long fileId = 100L;
        String fileName = "document.pdf";
        int version = 1;

        String result = s3StorageService.generateStorageKey(
                ownerId, folderId, fileId, fileName, version
        );

        assertThat(result).isEqualTo("1/10/100_v1.pdf");
    }

    @Test
    @DisplayName("generate storage key creates correct key for file with path")
    void generateStorageKey_fileWithPath_success() {
        Long ownerId = 2L;
        Long folderId = 20L;
        Long fileId = 200L;
        String fileName = "folder/subfolder/document.docx";
        int version = 2;

        String result = s3StorageService.generateStorageKey(
                ownerId, folderId, fileId, fileName, version
        );

        assertThat(result).isEqualTo("2/20/folder/subfolder/200_v2.docx");
    }

    @Test
    @DisplayName("generate storage key creates correct key for file without extension")
    void generateStorageKey_fileWithoutExtension_success() {
        Long ownerId = 3L;
        Long folderId = 30L;
        Long fileId = 300L;
        String fileName = "README";
        int version = 1;

        String result = s3StorageService.generateStorageKey(
                ownerId, folderId, fileId, fileName, version
        );

        assertThat(result).isEqualTo("3/30/300_v1");
    }

    @Test
    @DisplayName("generate storage key handles windows style path separator")
    void generateStorageKey_windowsPath_success() {
        Long ownerId = 4L;
        Long folderId = 40L;
        Long fileId = 400L;
        String fileName = "folder\\subfolder\\file.txt";
        int version = 1;

        String result = s3StorageService.generateStorageKey(
                ownerId, folderId, fileId, fileName, version
        );

        assertThat(result).isEqualTo("4/40/folder/subfolder/400_v1.txt");
    }

    // ============ generateVersionedStorageKey Tests ============

    @Test
    @DisplayName("generate versioned storage key updates version number when version exists")
    void generateVersionedStorageKey_versionExists_updatesVersion() {
        String previousPath = "1/10/100_v1.pdf";
        int newVersion = 2;

        String result = s3StorageService.generateVersionedStorageKey(previousPath, newVersion);

        assertThat(result).isEqualTo("1/10/100_v2.pdf");
    }

    @Test
    @DisplayName("generate versioned storage key adds version number when no version exists")
    void generateVersionedStorageKey_noVersionExists_addsVersion() {
        String previousPath = "1/10/100.pdf";
        int newVersion = 2;

        String result = s3StorageService.generateVersionedStorageKey(previousPath, newVersion);

        assertThat(result).isEqualTo("1/10/100_v2.pdf");
    }

    @Test
    @DisplayName("generate versioned storage key handles multiple digit versions")
    void generateVersionedStorageKey_multipleDigitVersion_success() {
        String previousPath = "1/10/100_v9.pdf";
        int newVersion = 10;

        String result = s3StorageService.generateVersionedStorageKey(previousPath, newVersion);

        assertThat(result).isEqualTo("1/10/100_v10.pdf");
    }

    @Test
    @DisplayName("generate versioned storage key handles file with multiple dots in name")
    void generateVersionedStorageKey_multipleDotsInName_success() {
        String previousPath = "1/10/my.document.backup.pdf";
        int newVersion = 2;

        String result = s3StorageService.generateVersionedStorageKey(previousPath, newVersion);

        assertThat(result).isEqualTo("1/10/my.document.backup_v2.pdf");
    }

    // ============ initiateMultipartUpload Tests ============

    @Test
    @DisplayName("initiate multipart upload returns upload result with upload id")
    void initiateMultipartUpload_success() {
        String keyName = "1/10/largefile.mp4";
        String expectedUploadId = "test-upload-id-123";

        InitiateMultipartUploadResult mockResult = new InitiateMultipartUploadResult();
        mockResult.setUploadId(expectedUploadId);

        when(s3Client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class)))
                .thenReturn(mockResult);

        InitiateMultipartUploadResult result = s3StorageService.initiateMultipartUpload(keyName);

        assertThat(result).isNotNull();
        assertThat(result.getUploadId()).isEqualTo(expectedUploadId);

        ArgumentCaptor<InitiateMultipartUploadRequest> requestCaptor =
                ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
        verify(s3Client).initiateMultipartUpload(requestCaptor.capture());

        InitiateMultipartUploadRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getBucketName()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.getKey()).isEqualTo(keyName);
    }

    // ============ uploadPart Tests ============

    @Test
    @DisplayName("upload part successfully uploads part and returns etag")
    void uploadPart_success() {
        String keyName = "1/10/largefile.mp4";
        String uploadId = "upload-123";
        int partNumber = 1;
        byte[] partData = "test part data".getBytes();
        String expectedETag = "etag-abc123";

        UploadPartResult mockResult = new UploadPartResult();
        mockResult.setETag(expectedETag);
        mockResult.setPartNumber(partNumber);

        when(s3Client.uploadPart(any(UploadPartRequest.class))).thenReturn(mockResult);

        PartETag result = s3StorageService.uploadPart(keyName, uploadId, partNumber, partData);

        assertThat(result).isNotNull();
        assertThat(result.getPartNumber()).isEqualTo(partNumber);
        assertThat(result.getETag()).isEqualTo(expectedETag);

        ArgumentCaptor<UploadPartRequest> requestCaptor =
                ArgumentCaptor.forClass(UploadPartRequest.class);
        verify(s3Client).uploadPart(requestCaptor.capture());

        UploadPartRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getBucketName()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.getKey()).isEqualTo(keyName);
        assertThat(capturedRequest.getUploadId()).isEqualTo(uploadId);
        assertThat(capturedRequest.getPartNumber()).isEqualTo(partNumber);
        assertThat(capturedRequest.getPartSize()).isEqualTo(partData.length);
    }

    @Test
    @DisplayName("upload part handles multiple parts correctly")
    void uploadPart_multipleParts_success() {
        String keyName = "1/10/largefile.mp4";
        String uploadId = "upload-123";
        byte[] partData = "test data".getBytes();

        UploadPartResult mockResult1 = new UploadPartResult();
        mockResult1.setETag("etag-1");
        mockResult1.setPartNumber(1);

        UploadPartResult mockResult2 = new UploadPartResult();
        mockResult2.setETag("etag-2");
        mockResult2.setPartNumber(2);

        when(s3Client.uploadPart(any(UploadPartRequest.class)))
                .thenReturn(mockResult1)
                .thenReturn(mockResult2);

        PartETag result1 = s3StorageService.uploadPart(keyName, uploadId, 1, partData);
        PartETag result2 = s3StorageService.uploadPart(keyName, uploadId, 2, partData);

        assertThat(result1.getPartNumber()).isEqualTo(1);
        assertThat(result1.getETag()).isEqualTo("etag-1");
        assertThat(result2.getPartNumber()).isEqualTo(2);
        assertThat(result2.getETag()).isEqualTo("etag-2");

        verify(s3Client, times(2)).uploadPart(any(UploadPartRequest.class));
    }

    // ============ listUploadedParts Tests ============

    @Test
    @DisplayName("list uploaded parts returns list of part summaries")
    void listUploadedParts_success() {
        String keyName = "1/10/largefile.mp4";
        String uploadId = "upload-123";

        PartListing mockListing = new PartListing();
        List<PartSummary> mockParts = Arrays.asList(
                createPartSummary(1, 1000L, "etag-1"),
                createPartSummary(2, 1000L, "etag-2"),
                createPartSummary(3, 500L, "etag-3")
        );
        mockListing.setParts(mockParts);

        when(s3Client.listParts(any(ListPartsRequest.class))).thenReturn(mockListing);

        List<PartSummary> result = s3StorageService.listUploadedParts(keyName, uploadId);

        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPartNumber()).isEqualTo(1);
        assertThat(result.get(1).getPartNumber()).isEqualTo(2);
        assertThat(result.get(2).getPartNumber()).isEqualTo(3);

        ArgumentCaptor<ListPartsRequest> requestCaptor =
                ArgumentCaptor.forClass(ListPartsRequest.class);
        verify(s3Client).listParts(requestCaptor.capture());

        ListPartsRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getBucketName()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.getKey()).isEqualTo(keyName);
        assertThat(capturedRequest.getUploadId()).isEqualTo(uploadId);
    }

    @Test
    @DisplayName("list uploaded parts returns empty list when no parts uploaded")
    void listUploadedParts_noParts_returnsEmptyList() {
        String keyName = "1/10/largefile.mp4";
        String uploadId = "upload-123";

        PartListing mockListing = new PartListing();
        mockListing.setParts(Arrays.asList());

        when(s3Client.listParts(any(ListPartsRequest.class))).thenReturn(mockListing);

        List<PartSummary> result = s3StorageService.listUploadedParts(keyName, uploadId);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(s3Client).listParts(any(ListPartsRequest.class));
    }

    // ============ completeMultipartUpload Tests ============

    @Test
    @DisplayName("complete multipart upload successfully completes upload")
    void completeMultipartUpload_success() {
        String keyName = "1/10/largefile.mp4";
        String uploadId = "upload-123";
        List<PartETag> partETags = Arrays.asList(
                new PartETag(1, "etag-1"),
                new PartETag(2, "etag-2")
        );

        CompleteMultipartUploadResult mockResult = new CompleteMultipartUploadResult();
        mockResult.setKey(keyName);

        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenReturn(mockResult);

        s3StorageService.completeMultipartUpload(keyName, uploadId, partETags);

        ArgumentCaptor<CompleteMultipartUploadRequest> requestCaptor =
                ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
        verify(s3Client).completeMultipartUpload(requestCaptor.capture());

        CompleteMultipartUploadRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getBucketName()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.getKey()).isEqualTo(keyName);
        assertThat(capturedRequest.getUploadId()).isEqualTo(uploadId);
        assertThat(capturedRequest.getPartETags()).hasSize(2);
    }

    @Test
    @DisplayName("complete multipart upload throws exception when s3 fails")
    void completeMultipartUpload_s3Error_throwsException() {
        String keyName = "1/10/largefile.mp4";
        String uploadId = "upload-123";
        List<PartETag> partETags = Arrays.asList(new PartETag(1, "etag-1"));

        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertThatThrownBy(() -> s3StorageService.completeMultipartUpload(
                keyName, uploadId, partETags))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to complete multipart upload");

        verify(s3Client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }

    // ============ abortMultipartUpload Tests ============

    @Test
    @DisplayName("abort multipart upload successfully aborts upload")
    void abortMultipartUpload_success() {
        String keyName = "1/10/largefile.mp4";
        String uploadId = "upload-123";

        doNothing().when(s3Client).abortMultipartUpload(any(AbortMultipartUploadRequest.class));

        s3StorageService.abortMultipartUpload(keyName, uploadId);

        ArgumentCaptor<AbortMultipartUploadRequest> requestCaptor =
                ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
        verify(s3Client).abortMultipartUpload(requestCaptor.capture());

        AbortMultipartUploadRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getBucketName()).isEqualTo(BUCKET_NAME);
        assertThat(capturedRequest.getKey()).isEqualTo(keyName);
        assertThat(capturedRequest.getUploadId()).isEqualTo(uploadId);
    }

    // ============ uploadFileBytes Tests ============

    @Test
    @DisplayName("upload file bytes successfully uploads byte array")
    void uploadFileBytes_success() {
        byte[] fileBytes = "test file content".getBytes();
        String keyName = "1/10/document.pdf";
        String contentType = "application/pdf";

        when(s3Client.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                .thenReturn(new PutObjectResult());

        s3StorageService.uploadFileBytes(fileBytes, keyName, contentType);

        ArgumentCaptor<String> bucketCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);

        verify(s3Client).putObject(bucketCaptor.capture(), keyCaptor.capture(),
                any(InputStream.class), metadataCaptor.capture());

        assertThat(bucketCaptor.getValue()).isEqualTo(BUCKET_NAME);
        assertThat(keyCaptor.getValue()).isEqualTo(keyName);
        assertThat(metadataCaptor.getValue().getContentLength()).isEqualTo(fileBytes.length);
        assertThat(metadataCaptor.getValue().getContentType()).isEqualTo(contentType);
    }

    @Test
    @DisplayName("upload file bytes throws exception when s3 upload fails")
    void uploadFileBytes_s3Error_throwsException() {
        byte[] fileBytes = "test content".getBytes();
        String keyName = "1/10/document.pdf";
        String contentType = "application/pdf";

        when(s3Client.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                .thenThrow(new RuntimeException("S3 error"));

        assertThatThrownBy(() -> s3StorageService.uploadFileBytes(fileBytes, keyName, contentType))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload byte array");

        verify(s3Client).putObject(anyString(), anyString(),
                any(InputStream.class), any(ObjectMetadata.class));
    }

    @Test
    @DisplayName("upload file bytes handles empty byte array")
    void uploadFileBytes_emptyArray_success() {
        byte[] fileBytes = new byte[0];
        String keyName = "1/10/empty.txt";
        String contentType = "text/plain";

        when(s3Client.putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class)))
                .thenReturn(new PutObjectResult());

        s3StorageService.uploadFileBytes(fileBytes, keyName, contentType);

        ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
        verify(s3Client).putObject(anyString(), anyString(),
                any(InputStream.class), metadataCaptor.capture());

        assertThat(metadataCaptor.getValue().getContentLength()).isEqualTo(0);
    }

    // ============ Helper Methods ============

    private MultipartFile getTestMockMultipartFile() {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.getSize()).thenReturn(1000L);
        when(mockFile.getContentType()).thenReturn("text/plain");
        when(mockFile.getOriginalFilename()).thenReturn("test.txt");
        return mockFile;
    }

    private S3Object createMockS3Object(byte[] content) {
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new S3ObjectInputStream(
                new ByteArrayInputStream(content), null));
        return s3Object;
    }

    private PartSummary createPartSummary(int partNumber, long size, String eTag) {
        PartSummary summary = new PartSummary();
        summary.setPartNumber(partNumber);
        summary.setSize(size);
        summary.setETag(eTag);
        return summary;
    }
}