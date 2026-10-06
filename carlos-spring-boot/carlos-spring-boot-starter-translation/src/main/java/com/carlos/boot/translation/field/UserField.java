package com.carlos.boot.translation.field;

import com.carlos.boot.translation.annotation.TransUser;
import com.carlos.boot.translation.core.TranslationBatch;
import com.carlos.boot.translation.core.TranslationData;
import com.carlos.core.base.UserInfo;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.lang.reflect.Field;

/**
 * 用户字段
 */
public class UserField extends TranslatableField {
    private final TransUser annotation;
    private final Field sourceField;
    private final Field targetField;

    public UserField(Field field, TransUser annotation) {
        super(field, field.getName());
        this.annotation = annotation;

        // 确定源字段和目标字段
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
        batch.addUserId(id);
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

        UserInfo user = data.getUser(id);
        if (user == null) {
            return;
        }

        Object value = extractValue(user, annotation.type());
        targetField.set(obj, value);
    }

    private Object extractValue(UserInfo user, TransUser.Type type) {
        return switch (type) {
            case FULL -> user;
            case NAME -> user.getName();
            case NICKNAME -> user.getNickName();
            case REALNAME -> user.getRealName();
            case PHONE -> user.getPhone();
            case EMAIL -> user.getEmail();
            default -> user.getName();
        };
    }
}
