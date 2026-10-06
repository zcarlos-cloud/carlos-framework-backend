package com.carlos.boot.web.validator;

import com.carlos.boot.web.validator.constraint.EnumValid;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.util.EnumUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 枚举字段校验器实现，通过 {@link EnumUtil#getByCode} 判断值是否为有效枚举编码。
 *
 * @author Carlos
 * @since 3.0.0
 */
public class EnumValidator implements ConstraintValidator<EnumValid, Object> {

    private Class<? extends BaseEnum<?>> enumEntity;

    @Override
    public void initialize(final EnumValid parameters) {
        enumEntity = parameters.type();
    }

    @Override
    public boolean isValid(final Object value, final ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        }
        return EnumUtil.getByCode(enumEntity, value) != null;
    }
}
