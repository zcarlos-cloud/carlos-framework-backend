package com.carlos.fx.codege.enums;

/**
 * <p>
 * 指令枚举
 * </p>
 *
 * @author Carlos
 * @date 2020/8/18 12:09
 * @since 3.0
 */
public enum CodeDirectEnum {
    /**
     * 表示需要循环生成文件
     */
    FOR("[for]", "循环指令"),
    BASE("[base]", "文件基础名称"),

    ;

    /**
     * 指令值
     */
    private final String value;

    /**
     * 描述
     */
    private final String describe;

    CodeDirectEnum(String value, String describe) {
        this.value = value;
        this.describe = describe;
    }

    public String getDescribe() {
        return describe;
    }

    public String getValue() {
        return value;
    }
}
