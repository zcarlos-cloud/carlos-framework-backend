package com.carlos.boot.translation.field;

import com.carlos.boot.translation.annotation.TransRegion;
import com.carlos.boot.translation.core.TranslationBatch;
import com.carlos.boot.translation.core.TranslationData;
import com.carlos.core.base.RegionInfo;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * 区域字段
 */
public class RegionField extends TranslatableField {
    private final TransRegion annotation;
    private final Field sourceField;
    private final Field targetField;

    public RegionField(Field field, TransRegion annotation) {
        super(field, field.getName());
        this.annotation = annotation;

        if (StringUtils.hasText(annotation.source())) {
            this.sourceField = ReflectionUtils.findField(
                field.getDeclaringClass(), annotation.source());
            if (this.sourceField != null) {
                this.sourceField.setAccessible(true);
            }
        } else {
            this.sourceField = field;
        }

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
        if (sourceField == null) {
            return;
        }
        String code = (String) sourceField.get(obj);
        batch.addRegionCode(code);
    }

    @Override
    public void fill(Object obj, TranslationData data) throws Exception {
        if (sourceField == null || targetField == null) {
            return;
        }
        String code = (String) sourceField.get(obj);
        if (code == null) {
            return;
        }

        RegionInfo region = data.getRegion(code);
        if (region == null) {
            return;
        }

        Object value = extractValue(region, annotation.type(),
            annotation.separator(), annotation.limit());
        targetField.set(obj, value);
    }

    private Object extractValue(RegionInfo region, TransRegion.OutputType type,
                                String separator, int limit) {
        switch (type) {
            case FULL:
                return region;
            case NAME:
                return region.getName();
            case FULLNAME:
                List<String> fullName = region.getFullName();
                if (fullName == null || fullName.isEmpty()) {
                    return region.getName();
                }
                int end = Math.min(limit, fullName.size());
                return String.join(separator, fullName.subList(0, end));
            default:
                return region.getName();
        }
    }
}
