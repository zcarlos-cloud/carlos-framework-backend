package com.carlos.oss.utils;

import cn.hutool.core.io.file.FileNameUtil;
import com.carlos.oss.config.OssProperties;
import com.carlos.oss.core.OssTemplate;
import com.carlos.oss.exception.OssException;
import com.carlos.oss.model.OssFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

/**
 * <p>
 * OSS 工具类
 * 提供静态方法快速操作对象存储
 * </p>
 *
 * @author carlos
 * @date 2026-03-06
 */
public class OssUtil {

    private static volatile OssTemplate ossTemplate;

    private static volatile String defaultBucketName;

    /**
     * 初始化 OssUtil 的静态字段
     * 由 OssAutoConfiguration 在应用启动时调用
     * 支持运行时重复初始化以切换存储配置
     *
     * @param template     OssTemplate 实例
     * @param properties   OssProperties 实例
     */
    public static synchronized void init(OssTemplate template, OssProperties properties) {
        OssUtil.ossTemplate = template;
        OssUtil.defaultBucketName = properties.getBucketName();
    }

    /**
     * 判断 OssTemplate 是否已完成初始化
     *
     * @return true 已初始化
     */
    public static boolean isInitialized() {
        return ossTemplate != null;
    }

    /**
     * 获取 OSS 模板
     *
     * @return OssTemplate
     */
    public static OssTemplate getTemplate() {
        if (ossTemplate == null) {
            throw new OssException("OssTemplate is not initialized, please check OSS configuration");
        }
        return ossTemplate;
    }

    /**
     * 设置默认桶名称
     *
     * @param bucketName 桶名称
     */
    public static void setDefaultBucketName(String bucketName) {
        OssUtil.defaultBucketName = bucketName;
    }

    /**
     * 获取默认桶名称
     *
     * @return 默认桶名称
     */
    public static String getDefaultBucket() {
        return defaultBucketName;
    }

    // region----------------------  桶操作 start  ------------------------

    /**
     * 创建存储桶
     *
     * @param bucketName 桶名称
     */
    public static void createBucket(String bucketName) {
        getTemplate().createBucket(bucketName);
    }

    /**
     * 删除存储桶
     *
     * @param bucketName 桶名称
     */
    public static void deleteBucket(String bucketName) {
        getTemplate().deleteBucket(bucketName);
    }

    /**
     * 判断存储桶是否存在
     *
     * @param bucketName 桶名称
     * @return 是否存在
     */
    public static boolean bucketExists(String bucketName) {
        return getTemplate().bucketExists(bucketName);
    }

    /**
     * 判断默认存储桶是否存在
     *
     * @return 是否存在
     */
    public static boolean bucketExists() {
        return getTemplate().bucketExists(getDefaultBucket());
    }

    /**
     * 列出所有存储桶
     *
     * @return 桶名称列表
     */
    public static List<String> listBuckets() {
        return getTemplate().listBuckets();
    }

    /**
     * 创建默认存储桶
     */
    public static void createBucket() {
        createBucket(getDefaultBucket());
    }

    /**
     * 创建存储桶并设置为公共读（如果配置允许）
     *
     * @param bucketName 桶名称
     */
    public static void makeBucket(String bucketName) {
        if (!bucketExists(bucketName)) {
            createBucket(bucketName);
        }
    }

    /**
     * 创建默认存储桶（如果不存在）
     */
    public static void makeBucket() {
        makeBucket(getDefaultBucket());
    }

    /**
     * 删除默认存储桶
     */
    public static void deleteBucket() {
        deleteBucket(getDefaultBucket());
    }

    /**
     * 设置存储桶为公共读
     *
     * @param bucketName 桶名称
     */
    public static void setBucketPublic(String bucketName) {
        getTemplate().setBucketPublic(bucketName);
    }

    /**
     * 设置默认存储桶为公共读
     */
    public static void setBucketPublic() {
        setBucketPublic(getDefaultBucket());
    }

    /**
     * 设置存储桶为私有
     *
     * @param bucketName 桶名称
     */
    public static void setBucketPrivate(String bucketName) {
        getTemplate().setBucketPrivate(bucketName);
    }

    /**
     * 设置默认存储桶为私有
     */
    public static void setBucketPrivate() {
        setBucketPrivate(getDefaultBucket());
    }

    /**
     * 设置存储桶策略
     *
     * @param bucketName 桶名称
     * @param policy     策略 JSON 字符串
     */
    public static void setBucketPolicy(String bucketName, String policy) {
        getTemplate().setBucketPolicy(bucketName, policy);
    }

    /**
     * 设置默认存储桶策略
     *
     * @param policy 策略 JSON 字符串
     */
    public static void setBucketPolicy(String policy) {
        setBucketPolicy(getDefaultBucket(), policy);
    }

    /**
     * 获取存储桶策略
     *
     * @param bucketName 桶名称
     * @return 策略 JSON 字符串
     */
    public static String getBucketPolicy(String bucketName) {
        return getTemplate().getBucketPolicy(bucketName);
    }

    /**
     * 获取默认存储桶策略
     *
     * @return 策略 JSON 字符串
     */
    public static String getBucketPolicy() {
        return getTemplate().getBucketPolicy(getDefaultBucket());
    }

    // endregion----------------------  桶操作 end  ------------------------

    // region----------------------  上传操作 start  ------------------------

    /**
     * 上传文件
     *
     * @param bucketName  桶名称
     * @param objectName  对象名称（文件路径）
     * @param inputStream 文件流
     * @param contentType 文件类型
     * @return 文件信息
     */
    public static OssFile putObject(String bucketName, String objectName, InputStream inputStream, String contentType) {
        return getTemplate().putObject(bucketName, objectName, inputStream, contentType);
    }

    /**
     * 上传文件（使用默认桶）
     *
     * @param objectName  对象名称（文件路径）
     * @param inputStream 文件流
     * @param contentType 文件类型
     * @return 文件信息
     */
    public static OssFile putObject(String objectName, InputStream inputStream, String contentType) {
        return getTemplate().putObject(objectName, inputStream, contentType);
    }

    /**
     * 上传文件（无 contentType，使用默认 application/octet-stream）
     *
     * @param bucketName  桶名称
     * @param objectName  对象名称（文件路径）
     * @param inputStream 文件流
     * @return 文件信息
     */
    public static OssFile putObject(String bucketName, String objectName, InputStream inputStream) {
        return getTemplate().putObject(bucketName, objectName, inputStream, "application/octet-stream");
    }

    /**
     * 上传文件（使用默认桶，无 contentType）
     *
     * @param objectName  对象名称（文件路径）
     * @param inputStream 文件流
     * @return 文件信息
     */
    public static OssFile putObject(String objectName, InputStream inputStream) {
        return getTemplate().putObject(objectName, inputStream, "application/octet-stream");
    }

    /**
     * 上传文件（字节数组）
     *
     * @param bucketName  桶名称
     * @param objectName  对象名称
     * @param bytes       文件字节数组
     * @param contentType 文件类型
     * @return 文件信息
     */
    public static OssFile putObject(String bucketName, String objectName, byte[] bytes, String contentType) {
        return getTemplate().putObject(bucketName, objectName, bytes, contentType);
    }

    /**
     * 上传文件（字节数组，无 contentType，使用默认 application/octet-stream）
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param bytes      文件字节数组
     * @return 文件信息
     */
    public static OssFile putObject(String bucketName, String objectName, byte[] bytes) {
        return getTemplate().putObject(bucketName, objectName, bytes, "application/octet-stream");
    }

    /**
     * 上传文件（MultipartFile）
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param file       文件
     * @return 文件信息
     */
    public static OssFile putObject(String bucketName, String objectName, MultipartFile file) {
        try {
            return getTemplate().putObject(bucketName, objectName, file.getInputStream(), file.getContentType());
        } catch (Exception e) {
            throw new OssException("Failed to upload file: " + FileNameUtil.getName(file.getOriginalFilename()), e);
        }
    }

    /**
     * 上传文件（MultipartFile，使用默认桶）
     *
     * @param objectName 对象名称
     * @param file       文件
     * @return 文件信息
     */
    public static OssFile putObject(String objectName, MultipartFile file) {
        try {
            return getTemplate().putObject(objectName, file.getInputStream(), file.getContentType());
        } catch (Exception e) {
            throw new OssException("Failed to upload file: " + FileNameUtil.getName(file.getOriginalFilename()), e);
        }
    }

    /**
     * 上传本地文件
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param file       本地文件
     * @return 文件信息
     */
    public static OssFile uploadFile(String bucketName, String objectName, File file) {
        try {
            String contentType = Files.probeContentType(file.toPath());
            return getTemplate().putObject(bucketName, objectName, new java.io.FileInputStream(file), contentType);
        } catch (Exception e) {
            throw new OssException("Failed to upload file: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 上传本地文件（使用默认桶）
     *
     * @param objectName 对象名称
     * @param file       本地文件
     * @return 文件信息
     */
    public static OssFile uploadFile(String objectName, File file) {
        return uploadFile(getDefaultBucket(), objectName, file);
    }

    /**
     * 从 URL 上传文件
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param url        文件 URL
     * @return 文件信息
     */
    public static OssFile putObjectFromUrl(String bucketName, String objectName, String url) {
        return getTemplate().putObjectFromUrl(bucketName, objectName, url);
    }

    /**
     * 从 URL 上传文件（使用默认桶）
     *
     * @param objectName 对象名称
     * @param url        文件 URL
     * @return 文件信息
     */
    public static OssFile putObjectFromUrl(String objectName, String url) {
        return getTemplate().putObjectFromUrl(objectName, url);
    }

    // endregion----------------------  上传操作 end  ------------------------

    // region----------------------  下载操作 start  ------------------------

    /**
     * 获取文件流
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 文件流
     */
    public static InputStream getObject(String bucketName, String objectName) {
        return getTemplate().getObject(bucketName, objectName);
    }

    /**
     * 获取文件流（使用默认桶）
     *
     * @param objectName 对象名称
     * @return 文件流
     */
    public static InputStream getObject(String objectName) {
        return getTemplate().getObject(objectName);
    }

    /**
     * 获取文件信息
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 文件信息
     */
    public static OssFile getObjectInfo(String bucketName, String objectName) {
        return getTemplate().getObjectInfo(bucketName, objectName);
    }

    /**
     * 获取文件信息（使用默认桶）
     *
     * @param objectName 对象名称
     * @return 文件信息
     */
    public static OssFile getObjectInfo(String objectName) {
        return getTemplate().getObjectInfo(getDefaultBucket(), objectName);
    }

    // endregion----------------------  下载操作 end  ------------------------

    // region----------------------  删除操作 start  ------------------------

    /**
     * 删除文件
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     */
    public static void deleteObject(String bucketName, String objectName) {
        getTemplate().deleteObject(bucketName, objectName);
    }

    /**
     * 删除文件（使用默认桶）
     *
     * @param objectName 对象名称
     */
    public static void deleteObject(String objectName) {
        getTemplate().deleteObject(objectName);
    }

    /**
     * 批量删除文件
     *
     * @param bucketName  桶名称
     * @param objectNames 对象名称列表
     */
    public static void deleteObjects(String bucketName, List<String> objectNames) {
        getTemplate().deleteObjects(bucketName, objectNames);
    }

    /**
     * 批量删除文件（使用默认桶）
     *
     * @param objectNames 对象名称列表
     */
    public static void deleteObjects(List<String> objectNames) {
        getTemplate().deleteObjects(getDefaultBucket(), objectNames);
    }

    // endregion----------------------  删除操作 end  ------------------------

    // region----------------------  其他操作 start  ------------------------

    /**
     * 复制文件
     *
     * @param sourceBucket 源桶名称
     * @param sourceObject 源对象名称
     * @param targetBucket 目标桶名称
     * @param targetObject 目标对象名称
     */
    public static void copyObject(String sourceBucket, String sourceObject, String targetBucket, String targetObject) {
        getTemplate().copyObject(sourceBucket, sourceObject, targetBucket, targetObject);
    }

    /**
     * 列出文件
     *
     * @param bucketName 桶名称
     * @param prefix     前缀
     * @return 文件列表
     */
    public static List<OssFile> listObjects(String bucketName, String prefix) {
        return getTemplate().listObjects(bucketName, prefix);
    }

    /**
     * 列出文件（使用默认桶）
     *
     * @param prefix 前缀
     * @return 文件列表
     */
    public static List<OssFile> listObjects(String prefix) {
        return getTemplate().listObjects(prefix);
    }

    /**
     * 获取文件访问URL
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 访问URL
     */
    public static String getObjectUrl(String bucketName, String objectName) {
        return getTemplate().getObjectUrl(bucketName, objectName);
    }

    /**
     * 获取文件访问URL（使用默认桶）
     *
     * @param objectName 对象名称
     * @return 访问URL
     */
    public static String getObjectUrl(String objectName) {
        return getTemplate().getObjectUrl(objectName);
    }

    /**
     * 获取预签名URL（临时访问链接）
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param expires    过期时间（秒）
     * @return 预签名URL
     */
    public static String getPresignedUrl(String bucketName, String objectName, int expires) {
        return getTemplate().getPresignedUrl(bucketName, objectName, expires);
    }

    /**
     * 获取预签名URL（临时访问链接，使用默认桶）
     *
     * @param objectName 对象名称
     * @param expires    过期时间（秒）
     * @return 预签名URL
     */
    public static String getPresignedUrl(String objectName, int expires) {
        return getTemplate().getPresignedUrl(getDefaultBucket(), objectName, expires);
    }

    /**
     * 判断文件是否存在
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 是否存在
     */
    public static boolean objectExists(String bucketName, String objectName) {
        return getTemplate().objectExists(bucketName, objectName);
    }

    /**
     * 判断文件是否存在（使用默认桶）
     *
     * @param objectName 对象名称
     * @return 是否存在
     */
    public static boolean objectExists(String objectName) {
        return getTemplate().objectExists(getDefaultBucket(), objectName);
    }

    // endregion----------------------  其他操作 end  ------------------------
}
