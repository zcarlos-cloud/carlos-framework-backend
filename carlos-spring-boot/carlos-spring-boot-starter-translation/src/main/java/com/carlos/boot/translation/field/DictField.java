package com.carlos.boot.translation.field;

import com.carlos.boot.translation.annotation.TransDict;
import com.carlos.boot.translation.core.TranslationBatch;
import com.carlos.boot.translation.core.TranslationData;
import com.carlos.core.base.Dict;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;

/**
 * 字典字段
 */
public class DictField extends TranslatableField {
    private final TransDict annotation;
    private final Field targetField;

    public DictField(Field field, TransDict annotation) {
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
    public void collect(Object obj, TranslationBatch batch) throws Exception {
        String value = (String) field.get(obj);
        if (value != null) {
            batch.addDictCode(annotation.type(), value);
        }
    }

    @Override
    public void fill(Object obj, TranslationData data) throws Exception {
        if (targetField == null) {
            return;
        }
        String value = (String) field.get(obj);
        if (value == null) {
            return;
        }

        Dict dict = data.getDict(annotation.type(), value);
        if (dict == null) {
            return;
        }

        Object output = extractValue(dict, annotation.outputType());
        targetField.set(obj, output);
    }

    private Object extractValue(Dict dict, TransDict.OutputType type) {
        switch (type) {
            case FULL:
                return dict;
            case NAME:
                return dict.getName();
            case CODE:
                return dict.getCode();
            default:
                return dict.getName();
        }
    }
}
