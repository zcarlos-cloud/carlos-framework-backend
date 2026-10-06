package com.carlos.json.jackson.filter;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

/**
 * <p>
 * 空字符串属性过滤器
 * </p>
 *
 * <p>用于在序列化时跳过值为空字符串的字段</p>
 *
 * @author carlos
 * @date 2025/01/15
 */
public class EmptyStringPropertyFilter extends SimpleBeanPropertyFilter {

    public static final String FILTER_NAME = "emptyStringFilter";

    @Override
    public void serializeAsProperty(Object pojo, JsonGenerator jgen, SerializationContext context, PropertyWriter writer)
        throws Exception {
        if (writer instanceof BeanPropertyWriter beanWriter) {
            Object value = beanWriter.getMember().getValue(pojo);
            if (value instanceof String str && str.isEmpty()) {
                // 跳过空字符串字段
                return;
            }
        }
        super.serializeAsProperty(pojo, jgen, context, writer);
    }
}
