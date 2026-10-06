package com.carlos.boot.translation.field;


import com.carlos.boot.translation.annotation.TransNested;
import com.carlos.boot.translation.core.TranslationBatch;
import com.carlos.boot.translation.core.TranslationData;
import com.carlos.boot.translation.core.TranslationMetadata;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * 嵌套对象字段（递归翻译）
 */
public class NestedField extends TranslatableField {
    private final TransNested annotation;

    public NestedField(Field field, TransNested annotation) {
        super(field, field.getName());
        this.annotation = annotation;
    }

    @Override
    public void collect(Object obj, TranslationBatch batch) throws Exception {
        if (!annotation.recursive()) {
            return;
        }

        Object nested = field.get(obj);
        if (nested == null) {
            return;
        }

        if (nested instanceof Collection) {
            for (Object item : (Collection<?>) nested) {
                if (item != null && !isBasicType(item.getClass())) {
                    TranslationMetadata.of(item.getClass()).collect(item, batch);
                }
            }
        } else if (!isBasicType(nested.getClass())) {
            TranslationMetadata.of(nested.getClass()).collect(nested, batch);
        }
    }

    @Override
    public void fill(Object obj, TranslationData data) throws Exception {
        if (!annotation.recursive()) {
            return;
        }

        Object nested = field.get(obj);
        if (nested == null) {
            return;
        }

        if (nested instanceof Collection) {
            for (Object item : (Collection<?>) nested) {
                if (item != null && !isBasicType(item.getClass())) {
                    TranslationMetadata.of(item.getClass()).fill(item, data);
                }
            }
        } else if (!isBasicType(nested.getClass())) {
            TranslationMetadata.of(nested.getClass()).fill(nested, data);
        }
    }

    private boolean isBasicType(Class<?> clazz) {
        return clazz.isPrimitive()
            || clazz.isEnum()
            || clazz.getName().startsWith("java.")
            || clazz.getName().startsWith("javax.")
            || clazz.getName().startsWith("jakarta.");
    }
}
