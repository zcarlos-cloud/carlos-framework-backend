package com.carlos.boot.web.converter;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * Jackson Long 序列化组件。
 *
 * <p>将 {@link Long} 统一序列化为 JSON 字符串，避免雪花 ID（19 位）在 JavaScript 中
 * 因超出 {@code Number.MAX_SAFE_INTEGER}（9007199254740991，16 位）而丢失精度。</p>
 *
 * <p><strong>仅处理包装类型 {@link Long}，不处理原始类型 {@code long}。</strong>
 * 原因：MyBatis-Plus 的 {@code IPage#getTotal()} 等分页计数字段为原始 {@code long}，
 * 若一并转为字符串，前端分页器将收到 {@code "0"} 而计算异常；框架自带的
 * {@code Paging} 使用 {@code int}，同样不受影响。雪花 ID 一律为 {@link Long} 包装类型，
 * 因此只注册 {@link Long} 即可精准覆盖，且零误伤。</p>
 *
 * <p>反序列化方向无需额外配置：Jackson 默认会将 JSON 字符串强制转换为 {@link Long}，
 * 因此 {@code @RequestBody} 接收的 Param/PO 无需改动。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@JacksonComponent
public class LongJsonComponent {

    /**
     * Long 序列化器，输出为 JSON 字符串。
     */
    public static class JsonLongSerializer extends ValueSerializer<Long> {

        @Override
        public void serialize(final Long value, final JsonGenerator jsonGenerator,
                              final SerializationContext serializationContext) {
            if (value == null) {
                jsonGenerator.writeNull();
                return;
            }

            jsonGenerator.writeString(value.toString());
        }
    }
}
