package com.carlos.oss.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * OSS 客户端信息
 * 用于前端初始化 OSS 客户端
 * </p>
 *
 * @author carlos
 * @date 2026-03-06
 */
@Data
@Builder
@Schema(description = "OSS 客户端信息")
public class OssClientInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "连接地址")
    private String endpoint;

    @Schema(description = "公网访问地址")
    private String publicEndpoint;

    @Schema(description = "默认桶名")
    private String defaultBucket;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "是否使用路径样式访问")
    private Boolean pathStyleAccess;

    @Schema(description = "OSS 类型")
    private String type;
}
