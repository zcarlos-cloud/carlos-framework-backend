package com.carlos.oss.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * OSS 文件信息
 *
 * @author carlos
 * @date 2026-02-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OssFile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文件名（原始文件名）
     */
    private String fileName;

    /**
     * 文件路径（存储路径）
     */
    private String filePath;

    /**
     * 文件访问URL
     */
    private String url;

    /**
     * 文件大小（字节）
     */
    private Long size;

    /**
     * 文件内容类型
     */
    private String contentType;

    /**
     * 文件哈希值（MD5/ETag）
     */
    private String hash;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;

    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
}
