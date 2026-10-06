package com.carlos.system.upload.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储类型枚举（用于文件存储配置管理）
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "StorageType")
public enum StorageTypeEnum implements BaseEnum<String> {

    /**
     * S3对象存储（兼容MinIO/阿里云OSS/腾讯云COS/AWS S3等）
     */
    S3("S3", "S3对象存储"),

    /**
     * 本地文件系统
     */
    LOCAL("LOCAL", "本地文件系统");

    @EnumValue
    private final String code;

    private final String desc;

    public static StorageTypeEnum ofCode(final String code) {
        final StorageTypeEnum[] values = StorageTypeEnum.values();
        for (final StorageTypeEnum value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
