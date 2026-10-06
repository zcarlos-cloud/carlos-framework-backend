package com.carlos.audit.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审计日志存储类型枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@AppEnum(code = "AuditLogStorageType")
@Getter
@AllArgsConstructor
public enum AuditLogStorageTypeEnum implements BaseEnum {

    /**
     * 阿里云OSS
     */
    OSS(1, "阿里云OSS", "Aliyun OSS"),

    /**
     * AWS S3
     */
    S3(2, "AWS S3", "AWS S3"),

    /**
     * MinIO
     */
    MINIO(3, "MinIO", "MinIO"),

    /**
     * GridFS
     */
    GRIDFS(4, "GridFS", "GridFS"),

    /**
     * 本地存储
     */
    LOCAL(5, "本地存储", "Local"),

    /**
     * 数据库
     */
    DB(6, "数据库", "Database");

    @EnumValue
    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
