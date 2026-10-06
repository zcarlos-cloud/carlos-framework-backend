package com.carlos.system.upload.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件存储配置 实体
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
@Accessors(chain = true)
@TableName("sys_file_storage_config")
@JsonInclude(JsonInclude.Include.ALWAYS)
public class FileStorageConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 配置名称
     */
    @TableField(value = "config_name")
    private String configName;

    /**
     * 存储类型 S3/LOCAL
     */
    @TableField(value = "storage_type")
    private String storageType;

    /**
     * S3端点地址
     */
    @TableField(value = "endpoint")
    private String endpoint;

    /**
     * 访问密钥（加密存储）
     */
    @TableField(value = "access_key")
    private String accessKey;

    /**
     * 秘密密钥（加密存储）
     */
    @TableField(value = "secret_key")
    private String secretKey;

    /**
     * 默认Bucket
     */
    @TableField(value = "bucket_name")
    private String bucketName;

    /**
     * S3区域
     */
    @TableField(value = "region")
    private String region;

    /**
     * 路径样式访问
     */
    @TableField(value = "path_style_access")
    private Boolean pathStyleAccess;

    /**
     * 本地存储基础路径
     */
    @TableField(value = "local_base_path")
    private String localBasePath;

    /**
     * 公共访问端点
     */
    @TableField(value = "public_endpoint")
    private String publicEndpoint;

    /**
     * 是否默认配置
     */
    @TableField(value = "is_default")
    private Boolean isDefault;

    /**
     * 状态 1启用 0禁用
     */
    @TableField(value = "config_status")
    private Integer configStatus;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
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
