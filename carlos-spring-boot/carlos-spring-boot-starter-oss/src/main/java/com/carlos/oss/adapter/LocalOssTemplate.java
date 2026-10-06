package com.carlos.oss.adapter;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.oss.config.OssProperties;
import com.carlos.oss.core.AbstractOssTemplate;
import com.carlos.oss.exception.OssException;
import com.carlos.oss.model.OssFile;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 本地文件系统存储实现
 * 基于本地文件系统实现 OssTemplate 接口，支持在本地磁盘管理文件
 *
 * @author carlos
 * @date 2026-05-30
 */
@Slf4j
public class LocalOssTemplate extends AbstractOssTemplate {

    public LocalOssTemplate(OssProperties properties) {
        super(properties);
    }

    /**
     * 获取本地存储基础路径
     */
    private Path getBasePath() {
        String localBasePath = properties.getLocalBasePath();
        if (StrUtil.isBlank(localBasePath)) {
            throw new OssException("Local base path is not configured");
        }
        return Paths.get(localBasePath);
    }

    /**
     * 获取桶对应的目录路径
     */
    private Path getBucketPath(String bucketName) {
        return getBasePath().resolve(bucketName);
    }

    /**
     * 获取对象对应的文件路径
     * <p>使用 Path.normalize() 后校验结果是否仍在 bucket 目录内，防止路径穿越。</p>
     */
    private Path getObjectPath(String bucketName, String objectName) {
        Path bucketPath = getBucketPath(bucketName).toAbsolutePath().normalize();
        String normalized = normalizeObjectName(objectName);
        Path resolved = bucketPath.resolve(normalized).toAbsolutePath().normalize();
        if (!resolved.startsWith(bucketPath)) {
            throw new OssException("Object path escapes bucket directory: " + objectName);
        }
        return resolved;
    }

    @Override
    public void createBucket(String bucketName) {
        Path path = getBucketPath(bucketName);
        try {
            Files.createDirectories(path);
            log.info("Created local bucket directory: {}", path);
        } catch (IOException e) {
            throw new OssException("Failed to create bucket directory: " + path, e);
        }
    }

    @Override
    public void deleteBucket(String bucketName) {
        Path path = getBucketPath(bucketName);
        try {
            FileUtil.del(path.toFile());
            log.info("Deleted local bucket directory: {}", path);
        } catch (Exception e) {
            throw new OssException("Failed to delete bucket directory: " + path, e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        return Files.isDirectory(getBucketPath(bucketName));
    }

    @Override
    public List<String> listBuckets() {
        Path basePath = getBasePath();
        if (!Files.exists(basePath)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(basePath)) {
            return stream.filter(Files::isDirectory)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new OssException("Failed to list buckets", e);
        }
    }

    @Override
    public OssFile putObject(String bucketName, String objectName, InputStream inputStream, String contentType) {
        ensureBucketExists(bucketName);
        Path objectPath = getObjectPath(bucketName, objectName);
        try {
            Files.createDirectories(objectPath.getParent());
            long size = Files.copy(inputStream, objectPath, StandardCopyOption.REPLACE_EXISTING);
            return OssFile.builder()
                .fileName(objectName)
                .filePath(objectName)
                .url(getObjectUrl(bucketName, objectName))
                .size(size)
                .contentType(contentType)
                .bucketName(bucketName)
                .uploadTime(LocalDateTime.now())
                .build();
        } catch (IOException e) {
            throw new OssException("Failed to write local file: " + objectPath, e);
        }
    }

    @Override
    public OssFile putObject(String bucketName, String objectName, byte[] bytes, String contentType) {
        return putObject(bucketName, objectName, new ByteArrayInputStream(bytes), contentType);
    }

    @Override
    public InputStream getObject(String bucketName, String objectName) {
        Path objectPath = getObjectPath(bucketName, objectName);
        if (!Files.exists(objectPath)) {
            throw new OssException("File not found: " + objectPath);
        }
        try {
            return new FileInputStream(objectPath.toFile());
        } catch (IOException e) {
            throw new OssException("Failed to read local file: " + objectPath, e);
        }
    }

    @Override
    public OssFile getObjectInfo(String bucketName, String objectName) {
        Path objectPath = getObjectPath(bucketName, objectName);
        if (!Files.exists(objectPath)) {
            return null;
        }
        try {
            return OssFile.builder()
                .fileName(objectName)
                .filePath(objectName)
                .url(getObjectUrl(bucketName, objectName))
                .size(Files.size(objectPath))
                .bucketName(bucketName)
                .lastModified(LocalDateTime.ofInstant(Files.getLastModifiedTime(objectPath).toInstant(), java.time.ZoneId.systemDefault()))
                .build();
        } catch (IOException e) {
            throw new OssException("Failed to get file info: " + objectPath, e);
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectName) {
        Path objectPath = getObjectPath(bucketName, objectName);
        try {
            Files.deleteIfExists(objectPath);
            log.info("Deleted local file: {}", objectPath);
        } catch (IOException e) {
            throw new OssException("Failed to delete local file: " + objectPath, e);
        }
    }

    @Override
    public void deleteObjects(String bucketName, List<String> objectNames) {
        for (String objectName : objectNames) {
            deleteObject(bucketName, objectName);
        }
    }

    @Override
    public void copyObject(String sourceBucket, String sourceObject, String targetBucket, String targetObject) {
        Path sourcePath = getObjectPath(sourceBucket, sourceObject);
        Path targetPath = getObjectPath(targetBucket, targetObject);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Copied local file from {} to {}", sourcePath, targetPath);
        } catch (IOException e) {
            throw new OssException("Failed to copy local file", e);
        }
    }

    @Override
    public List<OssFile> listObjects(String bucketName, String prefix) {
        Path bucketPath = getBucketPath(bucketName);
        if (!Files.exists(bucketPath)) {
            return new ArrayList<>();
        }
        String normalizedPrefix = prefix == null ? "" : normalizeObjectName(prefix);
        try (Stream<Path> stream = Files.walk(bucketPath)) {
            return stream.filter(Files::isRegularFile)
                .filter(p -> {
                    String relative = bucketPath.relativize(p).toString().replace('\\', '/');
                    return normalizedPrefix.isEmpty() || relative.startsWith(normalizedPrefix);
                })
                .map(p -> {
                    String relative = bucketPath.relativize(p).toString().replace('\\', '/');
                    OssFile file = new OssFile();
                    file.setFileName(relative);
                    file.setFilePath(relative);
                    try {
                        file.setSize(Files.size(p));
                        file.setLastModified(LocalDateTime.ofInstant(
                            Files.getLastModifiedTime(p).toInstant(), java.time.ZoneId.systemDefault()));
                    } catch (IOException ignored) {
                        file.setSize(0L);
                    }
                    file.setUrl(getObjectUrl(bucketName, relative));
                    file.setBucketName(bucketName);
                    return file;
                })
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new OssException("Failed to list local files", e);
        }
    }

    @Override
    public String getObjectUrl(String bucketName, String objectName) {
        String publicEndpoint = properties.getPublicEndpoint();
        if (StrUtil.isNotBlank(publicEndpoint)) {
            if (!publicEndpoint.endsWith("/")) {
                publicEndpoint += "/";
            }
            return publicEndpoint + bucketName + "/" + normalizeObjectName(objectName);
        }
        // 没有公共端点时，返回本地文件 URI
        return getObjectPath(bucketName, objectName).toUri().toString();
    }

    @Override
    public String getPresignedUrl(String bucketName, String objectName, int expires) {
        // 本地存储直接返回公共URL，不支持签名URL
        return getObjectUrl(bucketName, objectName);
    }

    @Override
    public boolean objectExists(String bucketName, String objectName) {
        return Files.exists(getObjectPath(bucketName, objectName));
    }

    @Override
    public void setBucketPolicy(String bucketName, String policy) {
        log.warn("Bucket policy is not supported by local file storage");
    }

    @Override
    public void setBucketPublic(String bucketName) {
        log.warn("Bucket public access is not supported by local file storage");
    }

    @Override
    public void setBucketPrivate(String bucketName) {
        log.warn("Bucket private access is not supported by local file storage");
    }

    @Override
    public String getBucketPolicy(String bucketName) {
        return "{}";
    }
}
