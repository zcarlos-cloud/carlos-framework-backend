package com.carlos.boot.translation.field;

import com.carlos.boot.translation.annotation.TransDept;
import com.carlos.boot.translation.core.TranslationBatch;
import com.carlos.boot.translation.core.TranslationData;
import com.carlos.core.base.DepartmentInfo;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 部门字段
 */
public class DeptField extends TranslatableField {
    private final TransDept annotation;
    private final Field sourceField;
    private final Field targetField;

    public DeptField(Field field, TransDept annotation) {
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
        Serializable id = (Serializable) sourceField.get(obj);
        batch.addDeptId(id);
    }

    @Override
    public void fill(Object obj, TranslationData data) throws Exception {
        if (sourceField == null || targetField == null) {
            return;
        }
        Serializable id = (Serializable) sourceField.get(obj);
        if (id == null) {
            return;
        }

        DepartmentInfo dept = data.getDept(id);
        if (dept == null) {
            return;
        }

        Object value = extractValue(dept, annotation.type(),
            annotation.separator(), annotation.limit());
        targetField.set(obj, value);
    }

    private Object extractValue(DepartmentInfo dept, TransDept.OutputType type,
                                String separator, int limit) {
        switch (type) {
            case FULL:
                return dept;
            case NAME:
                return dept.getName();
            case FULLNAME:
                List<String> fullName = dept.getFullName();
                if (fullName == null || fullName.isEmpty()) {
                    return dept.getName();
                }
                int end = Math.min(limit, fullName.size());
                return String.join(separator, fullName.subList(0, end));
            default:
                return dept.getName();
        }
    }
}
