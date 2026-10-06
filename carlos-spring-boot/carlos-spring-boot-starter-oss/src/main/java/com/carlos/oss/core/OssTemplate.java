package com.carlos.oss.core;

import com.carlos.oss.model.MultipartUploadInfo;
import com.carlos.oss.model.OssFile;

import java.io.InputStream;
import java.util.List;

/**
 * OSS 统一操作接口
 * 提供跨云平台的统一文件存储操作
 *
 * @author carlos
 * @date 2026-02-01
 */
public interface OssTemplate {

    /**
     * 创建存储桶
     *
     * @param bucketName 桶名称
     */
    void createBucket(String bucketName);

    /**
     * 删除存储桶
     *
     * @param bucketName 桶名称
     */
    void deleteBucket(String bucketName);

    /**
     * 判断存储桶是否存在
     *
     * @param bucketName 桶名称
     * @return 是否存在
     */
    boolean bucketExists(String bucketName);

    /**
     * 列出所有存储桶
     *
     * @return 桶名称列表
     */
    List<String> listBuckets();

    /**
     * 上传文件
     *
     * @param bucketName  桶名称
     * @param objectName  对象名称（文件路径）
     * @param inputStream 文件流
     * @param contentType 文件类型
     * @return 文件信息
     */
    OssFile putObject(String bucketName, String objectName, InputStream inputStream, String contentType);

    /**
     * 上传文件（使用默认桶）
     *
     * @param objectName  对象名称（文件路径）
     * @param inputStream 文件流
     * @param contentType 文件类型
     * @return 文件信息
     */
    OssFile putObject(String objectName, InputStream inputStream, String contentType);

    /**
     * 上传文件（字节数组）
     *
     * @param bucketName  桶名称
     * @param objectName  对象名称
     * @param bytes       文件字节数组
     * @param contentType 文件类型
     * @return 文件信息
     */
    OssFile putObject(String bucketName, String objectName, byte[] bytes, String contentType);

    /**
     * 获取文件流
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 文件流
     */
    InputStream getObject(String bucketName, String objectName);

    /**
     * 获取文件流（使用默认桶）
     *
     * @param objectName 对象名称
     * @return 文件流
     */
    InputStream getObject(String objectName);

    /**
     * 获取文件信息
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 文件信息
     */
    OssFile getObjectInfo(String bucketName, String objectName);

    /**
     * 删除文件
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     */
    void deleteObject(String bucketName, String objectName);

    /**
     * 删除文件（使用默认桶）
     *
     * @param objectName 对象名称
     */
    void deleteObject(String objectName);

    /**
     * 批量删除文件
     *
     * @param bucketName  桶名称
     * @param objectNames 对象名称列表
     */
    void deleteObjects(String bucketName, List<String> objectNames);

    /**
     * 复制文件
     *
     * @param sourceBucket 源桶名称
     * @param sourceObject 源对象名称
     * @param targetBucket 目标桶名称
     * @param targetObject 目标对象名称
     */
    void copyObject(String sourceBucket, String sourceObject, String targetBucket, String targetObject);

    /**
     * 列出文件
     *
     * @param bucketName 桶名称
     * @param prefix     前缀
     * @return 文件列表
     */
    List<OssFile> listObjects(String bucketName, String prefix);

    /**
     * 列出文件（使用默认桶）
     *
     * @param prefix 前缀
     * @return 文件列表
     */
    List<OssFile> listObjects(String prefix);

    /**
     * 获取文件访问URL
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 访问URL
     */
    String getObjectUrl(String bucketName, String objectName);

    /**
     * 获取文件访问URL（使用默认桶）
     *
     * @param objectName 对象名称
     * @return 访问URL
     */
    String getObjectUrl(String objectName);

    /**
     * 获取预签名URL（临时访问链接）
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param expires    过期时间（秒）
     * @return 预签名URL
     */
    String getPresignedUrl(String bucketName, String objectName, int expires);

    /**
     * 判断文件是否存在
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @return 是否存在
     */
    boolean objectExists(String bucketName, String objectName);

    /**
     * 从 URL 上传文件
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param url        文件 URL
     * @return 文件信息
     */
    OssFile putObjectFromUrl(String bucketName, String objectName, String url);

    /**
     * 从 URL 上传文件（使用默认桶）
     *
     * @param objectName 对象名称
     * @param url        文件 URL
     * @return 文件信息
     */
    OssFile putObjectFromUrl(String objectName, String url);

    /**
     * 设置存储桶访问策略
     *
     * @param bucketName 桶名称
     * @param policy     策略 JSON 字符串
     */
    void setBucketPolicy(String bucketName, String policy);

    // ==================== 分片上传 ====================

    /**
     * 智能上传（根据文件大小自动选择普通上传或分片上传）
     * <p>
     * 文件小于分片阈值时使用普通 putObject，超过阈值时自动切分为分片上传，
     * 避免大文件场景下的 OOM。
     * </p>
     *
     * @param bucketName  桶名称
     * @param objectName  对象名称
     * @param inputStream 文件输入流
     * @param contentType 文件类型
     * @param fileSize    文件大小（字节）
     * @return 文件信息
     */
    default OssFile uploadLarge(String bucketName, String objectName, InputStream inputStream,
                                 String contentType, long fileSize) {
        // 默认委托给普通上传（不支持分片的后端如本地存储）
        return putObject(bucketName, objectName, inputStream, contentType);
    }

    /**
     * 中止分片上传（清理未完成的分片）
     *
     * @param bucketName 桶名称
     * @param objectName 对象名称
     * @param uploadId   上传 ID
     */
    default void abortMultipartUpload(String bucketName, String objectName, String uploadId) {
        throw new UnsupportedOperationException("abortMultipartUpload is not supported by this OSS provider");
    }

    /**
     * 列出桶中未完成的分片上传
     *
     * @param bucketName 桶名称
     * @return 未完成的分片上传信息
     */
    default List<MultipartUploadInfo> listMultipartUploads(String bucketName) {
        throw new UnsupportedOperationException("listMultipartUploads is not supported by this OSS provider");
    }

    // ==================== 桶策略 ====================

    /**
     * 设置存储桶为公共读
     *
     * @param bucketName 桶名称
     */
    void setBucketPublic(String bucketName);

    /**
     * 设置存储桶为私有
     *
     * @param bucketName 桶名称
     */
    void setBucketPrivate(String bucketName);

    /**
     * 获取存储桶访问策略
     *
     * @param bucketName 桶名称
     * @return 策略 JSON 字符串
     */
    String getBucketPolicy(String bucketName);
}
