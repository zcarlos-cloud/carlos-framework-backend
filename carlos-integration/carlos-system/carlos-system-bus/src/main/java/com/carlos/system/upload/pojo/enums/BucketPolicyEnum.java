package com.carlos.system.upload.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.response.CommonErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Bucket访问策略枚举
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Getter
@AllArgsConstructor
@AppEnum(code = "BucketPolicy")
public enum BucketPolicyEnum implements BaseEnum<String> {

    /**
     * 私有
     */
    PRIVATE("PRIVATE", "私有"),

    /**
     * 公共读
     */
    PUBLIC("PUBLIC", "公共读");

    @EnumValue
    private final String code;

    private final String desc;

    public static BucketPolicyEnum ofCode(final String code) {
        final BucketPolicyEnum[] values = BucketPolicyEnum.values();
        for (final BucketPolicyEnum value : values) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        throw CommonErrorCode.NOT_FOUND.exception("Can't find enum of code " + code);
    }
}
