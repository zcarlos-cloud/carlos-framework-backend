package com.carlos.redis.serialize;

import lombok.Getter;

/**
 * 序列化类型枚举
 *
 * @author carlos
 * @date 2026-03-14
 */
@Getter
public enum SerializerType {

    /**
     * Jackson JSON 序列化 - 默认，可读性好
     */
    JACKSON("jackson", "Jackson JSON 序列化"),

    /**
     * Fastjson JSON 序列化 - 性能较好
     */
    FASTJSON("fastjson", "Fastjson JSON 序列化"),

    /**
     * Kryo 二进制序列化 - 高性能
     */
    KRYO("kryo", "Kryo 二进制序列化");

    private final String code;
    private final String description;

    SerializerType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据 code 获取枚举
     *
     * @param code 序列化类型代码
     * @return 枚举类型，默认 JACKSON
     */
    public static SerializerType fromCode(String code) {
        for (SerializerType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return JACKSON;
    }
}
