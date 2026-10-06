package com.carlos.boot.translation.field;

import com.carlos.boot.translation.annotation.TransEnum;
import com.carlos.boot.translation.core.TranslationBatch;
import com.carlos.boot.translation.core.TranslationData;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumInfo;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

/**
 * 枚举字段
 */
public class EnumField extends TranslatableField {
    private final TransEnum annotation;
    private final Field targetField;

    public EnumField(Field field, TransEnum annotation) {
        super(field, field.getName());
        this.annotation = annotation;

        if (StringUtils.hasText(annotation.target())) {
            this.targetField = ReflectionUtils.findField(
                field.getDeclaringClass(), annotation.target());
            if (this.targetField != null) {
                this.targetField.setAccessible(true);
            }
        } else {
            this.targetField = field;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void collect(Object obj, TranslationBatch batch) throws Exception {
        Object value = field.get(obj);
        if (value != null) {
            batch.addEnumValue(
                (Class<? extends BaseEnum<?>>) annotation.enumClass(),
                value);
        }
    }

    @Override
    public void fill(Object obj, TranslationData data) throws Exception {
        if (targetField == null) {
            return;
        }
        Object value = field.get(obj);
        if (value == null) {
            return;
        }

        EnumInfo enumVO = data.getEnum(annotation.enumClass(), value);
        if (enumVO == null) {
            return;
        }

        Object output = extractValue(enumVO, annotation.type());
        targetField.set(obj, output);
    }

    private Object extractValue(EnumInfo vo, TransEnum.OutputType type) {
        switch (type) {
            case FULL:
                return vo;
            case CODE:
                return vo.getCode();
            case DESC:
                return vo.getDesc();
            default:
                return vo.getDesc();
        }
    }
}
