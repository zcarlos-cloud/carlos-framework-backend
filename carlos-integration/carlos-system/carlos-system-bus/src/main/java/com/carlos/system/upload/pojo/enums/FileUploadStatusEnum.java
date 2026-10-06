package com.carlos.system.upload.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件上传状态枚举
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "FileUploadStatus")
public enum FileUploadStatusEnum implements BaseEnum<String> {

    /**
     * 上传成功
     */
    SUCCESS("SUCCESS", "上传成功"),

    /**
     * 上传失败
     */
    FAILED("FAILED", "上传失败"),

    /**
     * 处理中
     */
    PENDING("PENDING", "处理中");

    @EnumValue
    private final String code;

    private final String desc;

    public static FileUploadStatusEnum ofCode(final String code) {
        final FileUploadStatusEnum[] values = FileUploadStatusEnum.values();
        for (final FileUploadStatusEnum value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
