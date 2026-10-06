package com.carlos.oss.config;

import com.carlos.oss.enums.OssType;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OSS 配置属性
 * 基于 S3 协议，统一配置方式
 *
 * @author carlos
 * @date 2026-02-01
 */
@Data
@ConfigurationProperties(prefix = "carlos.oss")
public class OssProperties {

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * OSS 平台类型（用于选择默认 endpoint）
     */
    private OssType type = OssType.MINIO;

    /**
     * S3 访问端点（必填）
     * 示例：
     * - AWS S3: https://s3.amazonaws.com 或 https://s3.{region}.amazonaws.com
     * - MinIO: http://localhost:9000
     * - 阿里云 OSS: https://{bucket}.oss-{region}.aliyuncs.com
     * - 腾讯云 COS: https://{bucket}.cos.{region}.myqcloud.com
     * - 华为云 OBS: https://obs.{region}.myhuaweicloud.com
     */
    private String endpoint;

    /**
     * 公网访问端点（用于生成外部访问URL，可选）
     */
    private String publicEndpoint;

    /**
     * AWS Access Key ID / Access Key
     */
    @ToString.Exclude
    private String accessKey;

    /**
     * AWS Secret Access Key / Secret Key
     */
    @ToString.Exclude
    private String secretKey;

    /**
     * 区域（Region）
     * 示例：
     * - AWS: us-east-1, us-west-2, ap-northeast-1
     * - 阿里云: cn-hangzhou, cn-beijing, cn-shanghai
     * - 腾讯云: ap-guangzhou, ap-beijing, ap-shanghai
     * - 华为云: cn-north-4, cn-south-1
     */
    private String region = "us-east-1";

    /**
     * 默认存储桶名称
     */
    private String bucketName = "default";

    /**
     * 是否使用路径样式访问（Path Style Access）
     * true: http://endpoint/bucket/key
     * false: http://bucket.endpoint/key
     *
     * MinIO 通常使用 true
     * AWS S3 推荐使用 false（虚拟主机样式）
     */
    private boolean pathStyleAccess = true;

    /**
     * 是否自动创建桶
     */
    private boolean autoCreateBucket = true;

    /**
     * 预签名URL默认过期时间（秒）
     */
    private int presignedUrlExpires = 3600;

    /**
     * 预签名URL最大允许过期时间（秒，默认 7 天）
     */
    private int presignedUrlMaxExpires = 7 * 24 * 60 * 60;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeout = 10000;

    /**
     * 读取超时时间（毫秒）
     */
    private int socketTimeout = 60000;

    /**
     * 最大连接数
     */
    private int maxConnections = 50;

    /**
     * 是否使用 HTTPS
     */
    private boolean useHttps = false;

    /**
     * 分片上传每片大小（字节），默认 5MB
     */
    private long multipartPartSize = 5 * 1024 * 1024;

    /**
     * 分片上传阈值（字节），文件超过此大小自动使用分片上传，默认 5MB
     */
    private long multipartThreshold = 5 * 1024 * 1024;

    /**
     * 本地存储基础路径（仅当 type=LOCAL 时生效）
     */
    private String localBasePath;

    /**
     * 获取实际使用的 endpoint
     */
    public String getActualEndpoint() {
        if (endpoint != null && !endpoint.isEmpty()) {
            return endpoint;
        }
        if (type != null && type.getDefaultEndpoint() != null) {
            return type.getDefaultEndpoint();
        }
        throw new IllegalStateException("Endpoint is not configured and no default endpoint available for type: " + type);
    }
}
