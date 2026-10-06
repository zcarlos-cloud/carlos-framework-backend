package com.carlos.oss.enums;

/**
 * OSS 平台类型枚举
 * 所有平台都使用 S3 协议，只需配置不同的 endpoint
 *
 * @author carlos
 * @date 2026-02-01
 */
public enum OssType {

    /**
     * AWS S3
     */
    AWS("aws", "AWS S3", "https://s3.amazonaws.com"),

    /**
     * MinIO
     */
    MINIO("minio", "MinIO", "http://localhost:9000"),

    /**
     * 阿里云 OSS（兼容 S3）
     */
    ALIYUN("aliyun", "阿里云OSS", "https://oss-cn-hangzhou.aliyuncs.com"),

    /**
     * 腾讯云 COS（兼容 S3）
     */
    TENCENT("tencent", "腾讯云COS", "https://cos.ap-guangzhou.myqcloud.com"),

    /**
     * 华为云 OBS（兼容 S3）
     */
    HUAWEI("huawei", "华为云OBS", "https://obs.cn-north-4.myhuaweicloud.com"),

    /**
     * 七牛云 Kodo（兼容 S3）
     */
    QINIU("qiniu", "七牛云Kodo", "https://s3-cn-east-1.qiniucs.com"),

    /**
     * 京东云 OSS（兼容 S3）
     */
    JD("jd", "京东云OSS", "https://s3.cn-north-1.jdcloud-oss.com"),

    /**
     * 自定义（兼容 S3 协议）
     */
    CUSTOM("custom", "自定义S3", null),

    /**
     * 本地文件系统
     */
    LOCAL("local", "本地文件系统", null);

    private final String code;
    private final String description;
    private final String defaultEndpoint;

    OssType(String code, String description, String defaultEndpoint) {
        this.code = code;
        this.description = description;
        this.defaultEndpoint = defaultEndpoint;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }

    public static OssType fromCode(String code) {
        for (OssType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown OSS type: " + code);
    }
}
