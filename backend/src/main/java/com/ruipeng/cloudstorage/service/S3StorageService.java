package com.ruipeng.cloudstorage.service;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Service for handling file storage operations.
 * Encapsulates all S3 storage logic.
 */
@Service
public class S3StorageService {
    private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

    private final AmazonS3 s3Client;
    private final String bucketName;

    public S3StorageService(
            @Value("${cloud.aws.region.static}") String region,
            @Value("${AWS_S3_BUCKET}") String bucketName) {

        this.bucketName = bucketName;
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(region)
                .build();
    }
    /**
     * Uploads a file to S3 storage.
     */
    public void uploadFile(MultipartFile file, String keyName) {
        try {
            ObjectMetadata metadata = createMetadata(file);
            uploadToS3(file.getInputStream(), keyName, metadata);
            log.info("File uploaded to S3: {}", keyName);
        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", keyName, e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    /**
     * Downloads a file from S3 storage.
     */
    public byte[] downloadFile(String keyName) throws IOException {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, keyName);
            return readS3Object(s3Object);
        } catch (Exception e) {
            log.error("Failed to download file from S3: {}", keyName, e);
            throw new IOException("Failed to download file", e);
        }
    }

    /**
     * Deletes a file from S3 storage.
     */
    public void deleteFile(String keyName) {
        if (doesFileExist(keyName)) {
            s3Client.deleteObject(bucketName, keyName);
            log.info("File deleted from S3: {}", keyName);
        }
    }

    /**
     * Checks if a file exists in S3 storage.
     */
    public boolean doesFileExist(String keyName) {
        return s3Client.doesObjectExist(bucketName, keyName);
    }

    /**
     * Generates a storage key for a new file.
     */
    public String generateStorageKey(Long ownerId, Long folderId, Long fileId,
                                     String fileName, int version) {
        String extension = extractFileExtension(fileName);
        String subPath = extractSubPath(fileName);

        return String.format("%d/%d/%s%d_v%d%s",
                ownerId, folderId, subPath, fileId, version, extension);
    }

    /**
     * Generates a storage key for a new version based on previous path.
     */
    public String generateVersionedStorageKey(String previousPath, int newVersion) {
        if (previousPath.contains("_v")) {
            return previousPath.replaceAll("_v\\d+\\.", "_v" + newVersion + ".");
        }

        String basePath = previousPath.substring(0, previousPath.lastIndexOf('.'));
        String extension = previousPath.substring(previousPath.lastIndexOf('.'));
        return basePath + "_v" + newVersion + extension;
    }

    /**
     * Initiates a multipart upload for large files.
     */
    public InitiateMultipartUploadResult initiateMultipartUpload(String keyName) {
        InitiateMultipartUploadRequest request =
                new InitiateMultipartUploadRequest(bucketName, keyName);
        return s3Client.initiateMultipartUpload(request);
    }

    /**
     * Uploads a single part in a multipart upload.
     */
    public PartETag uploadPart(String keyName, String uploadId,
                               int partNumber, byte[] partData) {
        UploadPartRequest request = createUploadPartRequest(
                keyName, uploadId, partNumber, partData
        );
        UploadPartResult result = s3Client.uploadPart(request);
        return new PartETag(partNumber, result.getETag());
    }

    /**
     * Lists all uploaded parts for a multipart upload.
     */
    public List<PartSummary> listUploadedParts(String keyName, String uploadId) {
        ListPartsRequest request = new ListPartsRequest(bucketName, keyName, uploadId);
        PartListing partListing = s3Client.listParts(request);
        return partListing.getParts();
    }

    /**
     * Completes a multipart upload.
     */
    public void completeMultipartUpload(String keyName, String uploadId,
                                        List<PartETag> partETags) {
        try {
            CompleteMultipartUploadRequest request =
                    new CompleteMultipartUploadRequest(bucketName, keyName, uploadId, partETags);
            s3Client.completeMultipartUpload(request);
            log.info("Multipart upload completed: {}", keyName);
        } catch (Exception e) {
            log.error("Failed to complete multipart upload: {}", keyName, e);
            throw new RuntimeException("Failed to complete multipart upload", e);
        }
    }

    /**
     * Aborts a multipart upload.
     */
    public void abortMultipartUpload(String keyName, String uploadId) {
        AbortMultipartUploadRequest request =
                new AbortMultipartUploadRequest(bucketName, keyName, uploadId);
        s3Client.abortMultipartUpload(request);
        log.info("Multipart upload aborted: {}", keyName);
    }

    /**
     * Uploads a byte array to S3 storage.
     *
     * @param fileBytes the file content as byte array
     * @param keyName the S3 key name
     * @param contentType the MIME type of the file
     */
    public void uploadFileBytes(byte[] fileBytes, String keyName, String contentType) {
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileBytes.length);
            metadata.setContentType(contentType);

            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
            s3Client.putObject(bucketName, keyName, inputStream, metadata);

            log.info("Byte array uploaded to S3: {} ({} bytes)", keyName, fileBytes.length);
        } catch (Exception e) {
            log.error("Failed to upload byte array to S3: {}", keyName, e);
            throw new RuntimeException("Failed to upload byte array", e);
        }
    }

    // ============ Private Helper Methods ============

    private ObjectMetadata createMetadata(MultipartFile file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        return metadata;
    }

    private void uploadToS3(InputStream inputStream, String keyName,
                            ObjectMetadata metadata) throws IOException {
        try (InputStream stream = inputStream) {
            s3Client.putObject(bucketName, keyName, stream, metadata);
        }
    }

    private byte[] readS3Object(S3Object s3Object) throws IOException {
        try (S3ObjectInputStream inputStream = s3Object.getObjectContent()) {
            return IOUtils.toByteArray(inputStream);
        }
    }

    private String extractFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        return lastDotIndex > 0 ? fileName.substring(lastDotIndex) : "";
    }

    private String extractSubPath(String fileName) {
        if (!fileName.contains("/") && !fileName.contains("\\")) {
            return "";
        }

        Path fullPath = Paths.get(fileName);
        if (fullPath.getParent() != null) {
            return fullPath.getParent().toString().replace("\\", "/") + "/";
        }
        return "";
    }

    private UploadPartRequest createUploadPartRequest(String keyName, String uploadId,
                                                      int partNumber, byte[] partData) {
        return new UploadPartRequest()
                .withBucketName(bucketName)
                .withKey(keyName)
                .withPartNumber(partNumber)
                .withUploadId(uploadId)
                .withInputStream(new ByteArrayInputStream(partData))
                .withPartSize(partData.length);
    }

}