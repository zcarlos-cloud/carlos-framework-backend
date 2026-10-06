package com.carlos.system.upload.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件存储配置 VO
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
@Schema(description = "文件存储配置")
public class FileStorageConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "存储类型")
    private String storageType;

    @Schema(description = "S3端点地址")
    private String endpoint;

    @Schema(description = "访问密钥")
    private String accessKey;

    @Schema(description = "秘密密钥（脱敏）")
    private String secretKey;

    @Schema(description = "默认Bucket")
    private String bucketName;

    @Schema(description = "S3区域")
    private String region;

    @Schema(description = "路径样式访问")
    private Boolean pathStyleAccess;

    @Schema(description = "本地存储基础路径")
    private String localBasePath;

    @Schema(description = "公共访问端点")
    private String publicEndpoint;

    @Schema(description = "是否默认配置")
    private Boolean isDefault;

    @Schema(description = "状态 1启用 0禁用")
    private Integer configStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private Long createBy;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    private Long updateBy;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
