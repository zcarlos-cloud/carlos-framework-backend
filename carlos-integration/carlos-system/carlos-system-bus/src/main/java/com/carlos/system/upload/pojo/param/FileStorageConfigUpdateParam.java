package com.carlos.system.upload.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件存储配置更新参数
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
@Schema(description = "文件存储配置更新参数")
public class FileStorageConfigUpdateParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空")
    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotBlank(message = "配置名称不能为空")
    @Size(max = 100, message = "配置名称长度不能超过100")
    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String configName;

    @NotBlank(message = "存储类型不能为空")
    @Size(max = 20, message = "存储类型长度不能超过20")
    @Schema(description = "存储类型 S3/LOCAL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String storageType;

    @Size(max = 255, message = "S3端点地址长度不能超过255")
    @Schema(description = "S3端点地址")
    private String endpoint;

    @Size(max = 500, message = "访问密钥长度不能超过500")
    @Schema(description = "访问密钥")
    private String accessKey;

    @Size(max = 500, message = "秘密密钥长度不能超过500")
    @Schema(description = "秘密密钥")
    private String secretKey;

    @Size(max = 100, message = "默认Bucket长度不能超过100")
    @Schema(description = "默认Bucket")
    private String bucketName;

    @Size(max = 50, message = "S3区域长度不能超过50")
    @Schema(description = "S3区域")
    private String region;

    @Schema(description = "路径样式访问")
    private Boolean pathStyleAccess;

    @Size(max = 255, message = "本地存储基础路径长度不能超过255")
    @Schema(description = "本地存储基础路径")
    private String localBasePath;

    @Size(max = 255, message = "公共访问端点长度不能超过255")
    @Schema(description = "公共访问端点")
    private String publicEndpoint;

    @Schema(description = "状态 1启用 0禁用")
    private Integer configStatus;

    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;
}
