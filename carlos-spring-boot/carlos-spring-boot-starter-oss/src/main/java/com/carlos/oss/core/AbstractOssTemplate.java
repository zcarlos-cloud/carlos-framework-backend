package com.carlos.oss.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.carlos.oss.config.OssProperties;
import com.carlos.oss.exception.OssException;
import com.carlos.oss.model.OssFile;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * OSS 模板抽象类
 * 提供通用方法实现和默认行为
 *
 * @author carlos
 * @date 2026-02-01
 */
@Slf4j
public abstract class AbstractOssTemplate implements OssTemplate {

    protected final OssProperties properties;

    protected AbstractOssTemplate(OssProperties properties) {
        this.properties = properties;
    }

    /**
     * 获取默认桶名称
     */
    protected String getDefaultBucket() {
        String bucketName = properties.getBucketName();
        if (StrUtil.isBlank(bucketName)) {
            throw new OssException("Default bucket name is not configured");
        }
        return bucketName;
    }

    /**
     * 确保桶存在
     */
    protected void ensureBucketExists(String bucketName) {
        if (properties.isAutoCreateBucket() && !bucketExists(bucketName)) {
            log.info("Bucket [{}] does not exist, creating...", bucketName);
            createBucket(bucketName);
        }
    }

    @Override
    public OssFile putObject(String objectName, InputStream inputStream, String contentType) {
        return putObject(getDefaultBucket(), objectName, inputStream, contentType);
    }

    @Override
    public InputStream getObject(String objectName) {
        return getObject(getDefaultBucket(), objectName);
    }

    @Override
    public void deleteObject(String objectName) {
        deleteObject(getDefaultBucket(), objectName);
    }

    @Override
    public List<OssFile> listObjects(String prefix) {
        return listObjects(getDefaultBucket(), prefix);
    }

    @Override
    public String getObjectUrl(String objectName) {
        return getObjectUrl(getDefaultBucket(), objectName);
    }

    /**
     * 构建完整的访问URL
     */
    protected String buildUrl(String bucketName, String objectName) {
        String endpoint = StrUtil.isNotBlank(properties.getPublicEndpoint())
            ? properties.getPublicEndpoint()
            : properties.getEndpoint();

        if (StrUtil.isBlank(endpoint)) {
            throw new OssException("Endpoint is not configured");
        }

        // 确保endpoint以/结尾
        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }

        // 确保objectName不以/开头
        if (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }

        return endpoint + bucketName + "/" + objectName;
    }

    /**
     * 规范化对象名称
     * <p>移除开头斜杠并拒绝路径穿越片段（..），防止通过对象名访问基础目录之外文件。</p>
     */
    protected String normalizeObjectName(String objectName) {
        if (StrUtil.isBlank(objectName)) {
            throw new OssException("Object name cannot be blank");
        }
        // 移除开头的斜杠
        while (objectName.startsWith("/")) {
            objectName = objectName.substring(1);
        }
        // 拒绝路径穿越
        if (objectName.contains("..")) {
            throw new OssException("Object name contains path traversal characters: " + objectName);
        }
        return objectName;
    }

    @Override
    public OssFile putObjectFromUrl(String objectName, String url) {
        return putObjectFromUrl(getDefaultBucket(), objectName, url);
    }

    @Override
    public OssFile putObjectFromUrl(String bucketName, String objectName, String url) {
        try {
            byte[] bytes = HttpUtil.downloadBytes(url);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
            String contentType = HttpUtil.getMimeType(url);
            return putObject(bucketName, objectName, inputStream, contentType);
        } catch (Exception e) {
            throw new OssException("Failed to download file from URL: " + url, e);
        }
    }

    @Override
    public void setBucketPolicy(String bucketName, String policy) {
        throw new UnsupportedOperationException("setBucketPolicy is not supported by this OSS provider");
    }

    @Override
    public void setBucketPublic(String bucketName) {
        throw new UnsupportedOperationException("setBucketPublic is not supported by this OSS provider");
    }

    @Override
    public void setBucketPrivate(String bucketName) {
        throw new UnsupportedOperationException("setBucketPrivate is not supported by this OSS provider");
    }

    @Override
    public String getBucketPolicy(String bucketName) {
        throw new UnsupportedOperationException("getBucketPolicy is not supported by this OSS provider");
    }
}
