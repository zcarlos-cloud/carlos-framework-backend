package com.carlos.oss.adapter;

import cn.hutool.core.io.IoUtil;
import com.carlos.oss.config.OssProperties;
import com.carlos.oss.core.AbstractOssTemplate;
import com.carlos.oss.exception.OssException;
import com.carlos.oss.model.MultipartUploadInfo;
import com.carlos.oss.model.OssFile;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * S3 协议统一实现
 * 支持所有兼容 S3 协议的对象存储服务：
 * - AWS S3
 * - MinIO
 * - 阿里云 OSS
 * - 腾讯云 COS
 * - 华为云 OBS
 * - 七牛云 Kodo
 * - 京东云 OSS
 * - 其他兼容 S3 协议的存储服务
 *
 * @author carlos
 * @date 2026-02-01
 */
@Slf4j
public class S3OssTemplate extends AbstractOssTemplate {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public S3OssTemplate(OssProperties properties, S3Client s3Client, S3Presigner s3Presigner) {
        super(properties);
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @Override
    public void createBucket(String bucketName) {
        try {
            if (!bucketExists(bucketName)) {
                s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
                log.info("Created bucket: {}", bucketName);
            }
        } catch (Exception e) {
            throw new OssException("Failed to create bucket: " + bucketName, e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) {
        try {
            s3Client.deleteBucket(DeleteBucketRequest.builder()
                .bucket(bucketName)
                .build());
            log.info("Deleted bucket: {}", bucketName);
        } catch (Exception e) {
            throw new OssException("Failed to delete bucket: " + bucketName, e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucketName)
                .build());
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        } catch (Exception e) {
            throw new OssException("Failed to check bucket existence: " + bucketName, e);
        }
    }

    @Override
    public List<String> listBuckets() {
        try {
            return s3Client.listBuckets().buckets().stream()
                .map(Bucket::name)
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new OssException("Failed to list buckets", e);
        }
    }

    @Override
    public OssFile putObject(String bucketName, String objectName, InputStream inputStream, String contentType) {
        try {
            ensureBucketExists(bucketName);
            objectName = normalizeObjectName(objectName);

            // 读取流内容
            byte[] bytes = IoUtil.readBytes(inputStream);
            long size = bytes.length;

            // 上传文件
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .contentType(contentType)
                .contentLength(size)
                .build();

            PutObjectResponse response = s3Client.putObject(request,
                RequestBody.fromBytes(bytes));

            return OssFile.builder()
                .fileName(objectName)
                .filePath(objectName)
                .url(getObjectUrl(bucketName, objectName))
                .size(size)
                .contentType(contentType)
                .hash(response.eTag())
                .bucketName(bucketName)
                .build();
        } catch (Exception e) {
            throw new OssException("Failed to upload object: " + objectName, e);
        }
    }

    @Override
    public OssFile putObject(String bucketName, String objectName, byte[] bytes, String contentType) {
        return putObject(bucketName, objectName, new ByteArrayInputStream(bytes), contentType);
    }

    @Override
    public InputStream getObject(String bucketName, String objectName) {
        try {
            objectName = normalizeObjectName(objectName);
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .build();
            return s3Client.getObject(request);
        } catch (Exception e) {
            throw new OssException("Failed to get object: " + objectName, e);
        }
    }

    @Override
    public OssFile getObjectInfo(String bucketName, String objectName) {
        try {
            objectName = normalizeObjectName(objectName);
            HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .build();

            HeadObjectResponse response = s3Client.headObject(request);

            return OssFile.builder()
                .fileName(objectName)
                .filePath(objectName)
                .url(getObjectUrl(bucketName, objectName))
                .size(response.contentLength())
                .contentType(response.contentType())
                .hash(response.eTag())
                .bucketName(bucketName)
                .lastModified(response.lastModified() != null
                    ? LocalDateTime.ofInstant(response.lastModified(), ZoneId.systemDefault())
                    : null)
                .build();
        } catch (Exception e) {
            throw new OssException("Failed to get object info: " + objectName, e);
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectName) {
        try {
            objectName = normalizeObjectName(objectName);
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .build());
            log.info("Deleted object: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            throw new OssException("Failed to delete object: " + objectName, e);
        }
    }

    @Override
    public void deleteObjects(String bucketName, List<String> objectNames) {
        try {
            List<ObjectIdentifier> keys = objectNames.stream()
                .map(this::normalizeObjectName)
                .map(name -> ObjectIdentifier.builder().key(name).build())
                .collect(Collectors.toList());

            Delete delete = Delete.builder()
                .objects(keys)
                .build();

            s3Client.deleteObjects(DeleteObjectsRequest.builder()
                .bucket(bucketName)
                .delete(delete)
                .build());

            log.info("Deleted {} objects from bucket: {}", objectNames.size(), bucketName);
        } catch (Exception e) {
            throw new OssException("Failed to delete objects", e);
        }
    }

    @Override
    public void copyObject(String sourceBucket, String sourceObject, String targetBucket, String targetObject) {
        try {
            sourceObject = normalizeObjectName(sourceObject);
            targetObject = normalizeObjectName(targetObject);

            CopyObjectRequest request = CopyObjectRequest.builder()
                .sourceBucket(sourceBucket)
                .sourceKey(sourceObject)
                .destinationBucket(targetBucket)
                .destinationKey(targetObject)
                .build();

            s3Client.copyObject(request);
            log.info("Copied object from {}/{} to {}/{}", sourceBucket, sourceObject, targetBucket, targetObject);
        } catch (Exception e) {
            throw new OssException("Failed to copy object", e);
        }
    }

    @Override
    public List<OssFile> listObjects(String bucketName, String prefix) {
        try {
            List<OssFile> files = new ArrayList<>();

            ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);

            for (S3Object s3Object : response.contents()) {
                files.add(OssFile.builder()
                    .fileName(s3Object.key())
                    .filePath(s3Object.key())
                    .url(getObjectUrl(bucketName, s3Object.key()))
                    .size(s3Object.size())
                    .hash(s3Object.eTag())
                    .bucketName(bucketName)
                    .lastModified(s3Object.lastModified() != null
                        ? LocalDateTime.ofInstant(s3Object.lastModified(), ZoneId.systemDefault())
                        : null)
                    .build());
            }
            return files;
        } catch (Exception e) {
            throw new OssException("Failed to list objects with prefix: " + prefix, e);
        }
    }

    @Override
    public String getObjectUrl(String bucketName, String objectName) {
        objectName = normalizeObjectName(objectName);
        return buildUrl(bucketName, objectName);
    }

    @Override
    public String getPresignedUrl(String bucketName, String objectName, int expires) {
        try {
            objectName = normalizeObjectName(objectName);

            // P1-003/P2-012 修复：限制预签名URL最大过期时间，防止生成永久有效链接
            if (expires <= 0) {
                throw new OssException("Presigned URL expires must be positive");
            }
            if (expires > properties.getPresignedUrlMaxExpires()) {
                throw new OssException("Presigned URL expires exceeds maximum allowed: " + properties.getPresignedUrlMaxExpires());
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expires))
                .getObjectRequest(getObjectRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            throw new OssException("Failed to generate presigned URL for: " + objectName, e);
        }
    }

    @Override
    public boolean objectExists(String bucketName, String objectName) {
        try {
            objectName = normalizeObjectName(objectName);
            s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void setBucketPolicy(String bucketName, String policy) {
        try {
            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(policy)
                .build());
            log.info("Set bucket policy for bucket: {}", bucketName);
        } catch (Exception e) {
            throw new OssException("Failed to set bucket policy: " + bucketName, e);
        }
    }

    @Override
    public void setBucketPublic(String bucketName) {
        try {
            // S3 公共读策略：允许任何人读取桶中的对象
            String publicPolicy = "{"
                + "\"Version\": \"2012-10-17\","
                + "\"Statement\": [{"
                + "\"Sid\": \"PublicReadGetObject\","
                + "\"Effect\": \"Allow\","
                + "\"Principal\": \"*\","
                + "\"Action\": \"s3:GetObject\","
                + "\"Resource\": \"arn:aws:s3:::" + bucketName + "/*\""
                + "}]"
                + "}";

            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(publicPolicy)
                .build());
            log.info("Set bucket public: {}", bucketName);
        } catch (Exception e) {
            throw new OssException("Failed to set bucket public: " + bucketName, e);
        }
    }

    @Override
    public void setBucketPrivate(String bucketName) {
        try {
            // 删除桶策略，使桶变为私有
            s3Client.deleteBucketPolicy(DeleteBucketPolicyRequest.builder()
                .bucket(bucketName)
                .build());
            log.info("Set bucket private: {}", bucketName);
        } catch (Exception e) {
            throw new OssException("Failed to set bucket private: " + bucketName, e);
        }
    }

    @Override
    public String getBucketPolicy(String bucketName) {
        try {
            GetBucketPolicyResponse response = s3Client.getBucketPolicy(GetBucketPolicyRequest.builder()
                .bucket(bucketName)
                .build());
            return response.policy();
        } catch (Exception e) {
            throw new OssException("Failed to get bucket policy: " + bucketName, e);
        }
    }

    // ==================== 分片上传 ====================

    @Override
    public OssFile uploadLarge(String bucketName, String objectName, InputStream inputStream,
                                String contentType, long fileSize) {
        long threshold = properties.getMultipartThreshold();
        if (fileSize <= threshold) {
            // 小文件直接用普通上传
            return putObject(bucketName, objectName, inputStream, contentType);
        }
        // 大文件走分片上传
        return multipartUpload(bucketName, objectName, inputStream, contentType, fileSize);
    }

    /**
     * 分片上传实现
     */
    private OssFile multipartUpload(String bucketName, String objectName, InputStream inputStream,
                                     String contentType, long fileSize) {
        ensureBucketExists(bucketName);
        objectName = normalizeObjectName(objectName);

        long partSize = properties.getMultipartPartSize();
        if (partSize < 5 * 1024 * 1024) {
            // S3 规范：除最后一片外，每片不得小于 5MB
            partSize = 5 * 1024 * 1024;
        }

        // 计算分片数
        int partCount = (int) Math.ceil((double) fileSize / partSize);

        // 创建分片上传
        String uploadId = null;
        List<CompletedPart> completedParts = new ArrayList<>();

        try {
            CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .contentType(contentType)
                    .build());
            uploadId = createResponse.uploadId();
            log.info("分片上传已创建: bucket={}, key={}, uploadId={}, fileSize={}, partCount={}, partSize={}",
                bucketName, objectName, uploadId, fileSize, partCount, partSize);

            // 逐片上传
            byte[] buffer = new byte[(int) partSize];
            for (int i = 0; i < partCount; i++) {
                int partNumber = i + 1;
                int bytesRead;
                try {
                    bytesRead = readFully(inputStream, buffer,
                        i == partCount - 1 ? (int) (fileSize - (long) i * partSize) : (int) partSize);
                } catch (IOException e) {
                    abortMultipartUpload(bucketName, objectName, uploadId);
                    throw new OssException("Failed to read input stream for part " + partNumber, e);
                }

                if (bytesRead <= 0) {
                    break;
                }

                byte[] partData = new byte[bytesRead];
                System.arraycopy(buffer, 0, partData, 0, bytesRead);

                UploadPartResponse partResponse = s3Client.uploadPart(
                    UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(objectName)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength((long) bytesRead)
                        .build(),
                    RequestBody.fromBytes(partData));

                completedParts.add(CompletedPart.builder()
                    .partNumber(partNumber)
                    .eTag(partResponse.eTag())
                    .build());

                log.debug("分片上传进度: key={}, part={}/{}, size={}",
                    objectName, partNumber, partCount, bytesRead);
            }

            // 完成分片上传
            CompleteMultipartUploadResponse completeResponse = s3Client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build())
                    .build());

            log.info("分片上传完成: bucket={}, key={}, uploadId={}, etag={}",
                bucketName, objectName, uploadId, completeResponse.eTag());

            return OssFile.builder()
                .fileName(objectName)
                .filePath(objectName)
                .url(getObjectUrl(bucketName, objectName))
                .size(fileSize)
                .contentType(contentType)
                .hash(completeResponse.eTag())
                .bucketName(bucketName)
                .build();

        } catch (OssException e) {
            throw e;
        } catch (Exception e) {
            // 出错时尽量清理
            if (uploadId != null) {
                try {
                    abortMultipartUpload(bucketName, objectName, uploadId);
                } catch (Exception ignored) {
                    // cleanup best-effort
                }
            }
            throw new OssException("Failed to multipart upload: " + objectName, e);
        } finally {
            // 防止流未关闭
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 从 InputStream 中读取指定字节数
     */
    private int readFully(InputStream in, byte[] buffer, int bytesToRead) throws IOException {
        int totalRead = 0;
        while (totalRead < bytesToRead) {
            int bytesRead = in.read(buffer, totalRead, bytesToRead - totalRead);
            if (bytesRead == -1) {
                break;
            }
            totalRead += bytesRead;
        }
        return totalRead;
    }

    @Override
    public void abortMultipartUpload(String bucketName, String objectName, String uploadId) {
        try {
            objectName = normalizeObjectName(objectName);
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(objectName)
                .uploadId(uploadId)
                .build());
            log.info("分片上传已中止: bucket={}, key={}, uploadId={}", bucketName, objectName, uploadId);
        } catch (Exception e) {
            throw new OssException("Failed to abort multipart upload: " + objectName, e);
        }
    }

    @Override
    public List<MultipartUploadInfo> listMultipartUploads(String bucketName) {
        try {
            ListMultipartUploadsResponse response = s3Client.listMultipartUploads(
                ListMultipartUploadsRequest.builder()
                    .bucket(bucketName)
                    .build());
            return response.uploads().stream()
                .map(u -> MultipartUploadInfo.builder()
                    .bucketName(bucketName)
                    .objectName(u.key())
                    .uploadId(u.uploadId())
                    .initiated(u.initiated() != null
                        ? LocalDateTime.ofInstant(u.initiated(), ZoneId.systemDefault())
                        : null)
                    .build())
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new OssException("Failed to list multipart uploads: " + bucketName, e);
        }
    }

}
