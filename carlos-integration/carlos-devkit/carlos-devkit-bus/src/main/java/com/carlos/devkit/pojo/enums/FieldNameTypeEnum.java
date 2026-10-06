package com.carlos.devkit.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import com.carlos.core.response.CommonErrorCode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * <p>
 * 数据库字段 → Java 属性命名方式枚举
 * </p>
 * <p>
 * 语义与 carlos-tools 原 {@code FieldNameTypeEnum} 一致，仅补上字典能力。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@AppEnum(code = "DevkitFieldNameType")
@Getter
@AllArgsConstructor
public enum FieldNameTypeEnum implements BaseEnum<String> {

    /**
     * 没有前缀名，全小写（create_time -> createTime）
     */
    NOT_PREFIX_AND_UNDERLINE("NOT_PREFIX_AND_UNDERLINE", "不含前缀且下划线小写", "No Prefix, Underline", EnumColor.MUTED),

    /**
     * 没有前缀名，小驼峰命名（createTime 原样保留）
     */
    NOT_PREFIX_AND_CAMEL("NOT_PREFIX_AND_CAMEL", "不含前缀且小驼峰", "No Prefix, Camel", EnumColor.PRIMARY),

    /**
     * 前缀名，下划线隔开全小写（fd_create_time -> createTime）
     */
    PREFIX_AND_UNDERLINE("PREFIX_AND_UNDERLINE", "含前缀且下划线小写", "With Prefix, Underline", EnumColor.MUTED),

    /**
     * 前缀名，小驼峰命名（fd_createTime -> createTime）
     */
    PREFIX_AND_CAMEL("PREFIX_AND_CAMEL", "含前缀且小驼峰", "With Prefix, Camel", EnumColor.MUTED),

    /**
     * 没有前缀且全部大写（CREATE_TIME -> createTime）
     */
    ALL_UPPER("ALL_UPPER", "全部大写", "All Upper", EnumColor.MUTED);

    @JsonValue
    @EnumValue
    private final String code;

    private final String desc;

    private final String descEn;

    private final EnumColor color;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /**
     * 按码值或常量名解析（大小写不敏感）
     *
     * @param code 码值或常量名
     * @return 命名方式枚举
     */
    @JsonCreator
    public static FieldNameTypeEnum ofCode(final String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equalsIgnoreCase(code) || item.name().equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> CommonErrorCode.NOT_FOUND.exception("不支持的字段命名方式: " + code));
    }
}
