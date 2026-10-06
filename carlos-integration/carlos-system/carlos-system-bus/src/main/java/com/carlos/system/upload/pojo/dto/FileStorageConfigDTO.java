package com.carlos.system.upload.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件存储配置 DTO
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
public class FileStorageConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String configName;

    private String storageType;

    private String endpoint;

    private String accessKey;

    private String secretKey;

    private String bucketName;

    private String region;

    private Boolean pathStyleAccess;

    private String localBasePath;

    private String publicEndpoint;

    private Boolean isDefault;

    private Integer configStatus;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
