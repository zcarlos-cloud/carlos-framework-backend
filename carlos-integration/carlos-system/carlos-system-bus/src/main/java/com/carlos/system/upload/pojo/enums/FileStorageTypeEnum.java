package com.carlos.system.upload.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件存储类型枚举
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "FileStorageType")
public enum FileStorageTypeEnum implements BaseEnum<String> {

    /**
     * S3对象存储（兼容MinIO/阿里云OSS/腾讯云COS/AWS S3等）
     */
    S3("S3", "S3对象存储"),

    /**
     * MinIO存储
     */
    MINIO("MINIO", "MinIO存储"),

    /**
     * 阿里云OSS
     */
    OSS("OSS", "阿里云OSS"),

    /**
     * 本地存储
     */
    LOCAL("LOCAL", "本地存储");

    @EnumValue
    private final String code;

    private final String desc;

    public static FileStorageTypeEnum ofCode(final String code) {
        final FileStorageTypeEnum[] values = FileStorageTypeEnum.values();
        for (final FileStorageTypeEnum value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
