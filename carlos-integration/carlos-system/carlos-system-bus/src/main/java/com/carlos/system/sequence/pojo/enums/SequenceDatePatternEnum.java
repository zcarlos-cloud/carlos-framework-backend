package com.carlos.system.sequence.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 序列号日期段格式枚举（白名单）
 * </p>
 *
 * <p>日期段由 DateTimeFormatter 按 pattern 渲染，若允许任意 pattern 则等于向配置里注入格式串，
 * 故只开放下列固定格式；编码本身即 pattern 原文（如 {@code yyyyMMdd}）。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@AppEnum(code = "SequenceDatePattern")
@Getter
@AllArgsConstructor
public enum SequenceDatePatternEnum implements BaseEnum<String> {

    /**
     * 按年：yyyy
     */
    YEAR("yyyy", "按年 yyyy"),
    /**
     * 按月：yyyyMM
     */
    MONTH("yyyyMM", "按月 yyyyMM"),
    /**
     * 按日：yyyyMMdd
     */
    DAY("yyyyMMdd", "按日 yyyyMMdd"),
    /**
     * 按小时：yyyyMMddHH
     */
    HOUR("yyyyMMddHH", "按小时 yyyyMMddHH"),
    /**
     * 按秒：yyyyMMddHHmmss
     */
    SECOND("yyyyMMddHHmmss", "按秒 yyyyMMddHHmmss");


    private final String code;

    private final String desc;


    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    /**
     * 判断 pattern 是否为受支持的日期段格式（空白视为「不含日期段」，同样合法）
     *
     * @param pattern 日期段格式
     * @return 是否支持
     */
    public static boolean isSupported(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return true;
        }
        for (SequenceDatePatternEnum value : values()) {
            if (value.code.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按 pattern 解析日期段格式
     *
     * @param pattern 日期段格式
     * @return 枚举，无匹配返回 null
     */
    public static SequenceDatePatternEnum of(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return null;
        }
        for (SequenceDatePatternEnum value : values()) {
            if (value.code.equals(pattern)) {
                return value;
            }
        }
        return null;
    }
}
