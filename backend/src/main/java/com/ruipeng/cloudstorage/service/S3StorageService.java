package com.ruipeng.cloudstorage.service;


import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

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
