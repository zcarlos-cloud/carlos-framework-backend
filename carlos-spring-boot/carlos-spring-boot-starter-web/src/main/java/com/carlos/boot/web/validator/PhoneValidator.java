package com.carlos.boot.web.validator;

import com.carlos.boot.web.validator.constraint.Phone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * 自定义手机号码验证注解实现类。
 *
 * <p>验证中国大陆 11 位手机号码格式。</p>
 *
 * @author carlos
 * @date 2020/4/15 16:38
 */
public class PhoneValidator implements ConstraintValidator<Phone, String> {

    private static final String REG_EX = "^1[3,4,5,6,7,8,9]\\d{9}$";
    private static final Pattern PATTERN = Pattern.compile(REG_EX);

    @Override
    public void initialize(final Phone parameters) {
    }

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext constraintValidatorContext) {
        if (value == null) {
            return true;
        }
        return PATTERN.matcher(value).matches();
    }
}
