package com.carlos.monitor.config;

import com.carlos.core.enums.BaseEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * String → BaseEnum 通用转换（按字典 code 匹配，兼容枚举 name）。
 *
 * <p>GET 请求 @RequestParam/路径变量绑定枚举时不走 Jackson（@JsonCreator 不生效），
 * Spring 默认按 name() 转换会导致小写字典码（如 env=dev）绑定失败，注册本工厂后统一按 code 解析。</p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BaseEnumConverterFactory implements ConverterFactory<String, BaseEnum> {

    @Override
    public <T extends BaseEnum> Converter<String, T> getConverter(Class<T> targetType) {
        return new StringToBaseEnumConverter<>(targetType);
    }

    private static final class StringToBaseEnumConverter<T extends BaseEnum> implements Converter<String, T> {

        private final Class<T> targetType;

        private StringToBaseEnumConverter(Class<T> targetType) {
            this.targetType = targetType;
        }

        @Override
        public T convert(String source) {
            if (source.isBlank()) {
                return null;
            }
            for (T value : targetType.getEnumConstants()) {
                Object code = value.getCode();
                if (code != null && code.toString().equals(source)) {
                    return value;
                }
            }
            for (T value : targetType.getEnumConstants()) {
                if (value instanceof Enum<?> e && e.name().equalsIgnoreCase(source)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("无法解析的枚举值: " + source + " (" + targetType.getSimpleName() + ")");
        }
    }
}
