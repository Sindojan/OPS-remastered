package com.sindoflow.ops.documents;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    private final MinioClient minioClient;
    private final String bucket;

    public DocumentStorageService(MinioClient minioClient,
                                  @Value("${minio.bucket}") String bucket) {
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public void upload(String objectKey, InputStream data, String contentType, long size) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(data, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("Uploaded object: {}", objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO: " + objectKey, e);
        }
    }

    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " + objectKey, e);
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            log.info("Deleted object: {}", objectKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO: " + objectKey, e);
        }
    }

    public String getPresignedUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL for: " + objectKey, e);
        }
    }
}
