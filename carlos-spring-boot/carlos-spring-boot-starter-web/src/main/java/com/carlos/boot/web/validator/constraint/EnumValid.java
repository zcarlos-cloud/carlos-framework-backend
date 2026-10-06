package com.carlos.boot.web.validator.constraint;

import com.carlos.boot.web.validator.EnumValidator;
import com.carlos.core.enums.BaseEnum;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 枚举类型校验注解，用于校验字段值是否为指定枚举的有效编码。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Documented
@Constraint(validatedBy = {EnumValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface EnumValid {

    String message() default "请输入正确的类型值";

    Class<? extends BaseEnum<?>> type();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
