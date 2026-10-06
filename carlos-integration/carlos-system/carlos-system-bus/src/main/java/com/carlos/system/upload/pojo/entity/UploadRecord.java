package com.carlos.system.upload.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 文件上传记录 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2022-2-7 15:22:31
 */
@Data
@Accessors(chain = true)
@TableName("sys_upload_record")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class UploadRecord implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 文件分组
     */
    @TableField(value = "group_id")
    private String groupId;
    /**
     * 文件库名称
     */
    @TableField(value = "repository_name")
    private String repositoryName;
    /**
     * 文件库地址
     */
    @TableField(value = "repository_url")
    private String repositoryUrl;
    /**
     * 源文件名
     */
    @TableField(value = "original_name")
    private String originalName;
    /**
     * 文件用途
     */
    @TableField(value = "uses")
    private String uses;
    /**
     * 上传状态 成功 失败
     */
    @TableField(value = "upload_status")
    private String uploadStatus;
    /**
     * 文件大小（字节）
     */
    @TableField(value = "file_size")
    private Long fileSize;
    /**
     * MIME类型
     */
    @TableField(value = "content_type")
    private String contentType;
    /**
     * 文件扩展名
     */
    @TableField(value = "file_extension")
    private String fileExtension;
    /**
     * 存储类型 MINIO/OSS/LOCAL
     */
    @TableField(value = "storage_type")
    private String storageType;
    /**
     * 上传人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 上传时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    /**
     * 逻辑删除 0=正常 1=已删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Integer isDeleted;
    /**
     * 删除时间
     */
    @TableField(value = "deleted_time")
    private LocalDateTime deletedTime;

}
