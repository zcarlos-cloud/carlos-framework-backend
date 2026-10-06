package com.carlos.system.upload.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 文件上传记录 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2022-2-7 15:22:31
 */
@Data
@Accessors(chain = true)
public class UploadRecordDTO {

    /**
     * 主键
     */
    private Long id;
    /**
     * 文件分组
     */
    private String groupId;
    /**
     * 文件库名称
     */
    private String repositoryName;
    /**
     * 文件库地址
     */
    private String repositoryUrl;
    /**
     * 源文件名
     */
    private String originalName;
    /**
     * 文件用途
     */
    private String uses;
    /**
     * 上传状态 成功 失败
     */
    private String uploadStatus;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * MIME类型
     */
    private String contentType;
    /**
     * 文件扩展名
     */
    private String fileExtension;
    /**
     * 存储类型 MINIO/OSS/LOCAL
     */
    private String storageType;
    /**
     * 上传人
     */
    private Long createBy;
    /**
     * 上传时间
     */
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    private Long updateBy;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 文件链接
     */
    private String url;
}
