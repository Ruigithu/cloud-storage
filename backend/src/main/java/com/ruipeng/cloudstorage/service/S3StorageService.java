package com.ruipeng.cloudstorage.service;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
public class S3StorageService {
    private final AmazonS3 s3Client;
    private final String bucketName;

    public S3StorageService(
            @Value("${cloud.aws.credentials.access-key}") String accessKey,
            @Value("${cloud.aws.credentials.secret-key}") String secretKey,
            @Value("${cloud.aws.region.static}") String region,
            @Value("${cloud.aws.s3.bucket}") String bucketName) {

        this.bucketName = bucketName;
        this.s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(accessKey, secretKey)))
                .withRegion(region)
                .build();
    }


    public String uploadFile(MultipartFile file, String keyName) throws IOException {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(bucketName, keyName, inputStream, metadata);
        }

        return keyName;
    }
    public InitiateMultipartUploadResult initiateMultipartUpload(String keyName){
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, keyName);
        return s3Client.initiateMultipartUpload(request);
    }

    //upload  file part
    public UploadPartResult uploadPart (String keyName, String uploadId, int partNumber, byte[] partData, long partSize){
        UploadPartRequest uploadPartRequest = new UploadPartRequest()
                .withBucketName(bucketName)
                .withKey(keyName)
                .withPartNumber(partNumber)
                .withUploadId(uploadId)
                .withInputStream(new ByteArrayInputStream(partData))
                .withPartSize(partSize);
        return s3Client.uploadPart(uploadPartRequest);
    }
    //query uploaded part
    public List<PartSummary> listParts(String keyName, String uploadId){
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, keyName, uploadId);
        PartListing partListing = s3Client.listParts(listPartsRequest);
        return partListing.getParts();
    }


    public void completeMultipartUpload(String keyName, String uploadId, List<PartETag> partETags) {
        try {
            CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(
                    bucketName, keyName, uploadId, partETags);
            s3Client.completeMultipartUpload(completeRequest);
        } catch (Exception e) {
            System.err.println(" Error completing multipart upload: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void abortMultipartUpload(String keyName, String uploadId) throws IOException {
        AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(bucketName, keyName, uploadId);
        s3Client.abortMultipartUpload(abortRequest);

    }


    public byte[] downloadFile(String keyName) throws IOException {
        S3Object s3Object = s3Client.getObject(bucketName, keyName);
        try (S3ObjectInputStream inputStream = s3Object.getObjectContent()) {
            return IOUtils.toByteArray(inputStream);
        }
    }

    public void deleteFile(String keyName) {
        s3Client.deleteObject(bucketName, keyName);
    }

    public boolean doesFileExist(String keyName) {
        return s3Client.doesObjectExist(bucketName, keyName);
    }

    public String getFileUrl(String keyName, int expirationInMinutes) {
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * expirationInMinutes;
        expiration.setTime(expTimeMillis);

        return s3Client.generatePresignedUrl(bucketName, keyName, expiration).toString();
    }
}
