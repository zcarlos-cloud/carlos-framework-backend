package com.carlos.boot.web.converter;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import com.carlos.json.util.ConvertUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;

import java.util.Date;

/**
 * Jackson Date 序列化与反序列化组件。
 *
 * <p>使用 @JsonComponent 自动注册到 ObjectMapper，无需手动配置。</p>
 *
 * @author carlos
 * @date 2020/4/14 14:26
 */
@JacksonComponent
public class DateJsonComponent {

    /**
     * Date 序列化器，默认格式为 yyyy-MM-dd HH:mm:ss，
     * 支持通过 @JsonFormat 注解自定义格式。
     */
    public static class JsonDateSerializer extends ValueSerializer<Date> {

        private final String pattern;

        public JsonDateSerializer() {
            this.pattern = DatePattern.NORM_DATETIME_PATTERN;
        }

        private JsonDateSerializer(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public void serialize(final Date date, final JsonGenerator jsonGenerator,
                              final SerializationContext serializationContext) {
            if (date != null) {
                jsonGenerator.writeString(DateUtil.format(date, pattern));
            }
        }

        @Override
        public ValueSerializer<?> createContextual(final SerializationContext prov,
                                                   final BeanProperty property) {
            if (property != null) {
                JsonFormat annotation = property.getAnnotation(JsonFormat.class);
                if (annotation != null && !annotation.pattern().isEmpty()) {
                    return new JsonDateSerializer(annotation.pattern());
                }
            }
            return this;
        }
    }

    /**
     * Date 反序列化器，支持字符串和长整型时间戳两种格式。
     */
    public static class JsonDateDeserializer extends ValueDeserializer<Date> {

        @Override
        public Date deserialize(final JsonParser jp, final DeserializationContext ctxt) {
            final String date = jp.getText();
            if (NumberUtil.isLong(date)) {
                return new Date(Long.parseLong(date));
            }
            return ConvertUtil.string2Date(date);
        }
    }
}
