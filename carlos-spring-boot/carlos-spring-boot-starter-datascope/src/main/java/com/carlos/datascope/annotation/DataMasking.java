package com.carlos.datascope.annotation;

import java.lang.annotation.*;

/**
 * 数据脱敏注解
 * <p>
 * 用于对敏感数据进行脱敏处理，支持多种脱敏类型
 *
 * <p>使用示例：</p>
 * <pre>
 * &#64;GetMapping("/users")
 * &#64;DataScope(dimension = ScopeDimension.CURRENT_DEPT)
 * &#64;DataMasking(fields = {
 *     &#64;DataMasking.Field(name = "phone", type = DataMasking.Type.PHONE),
 *     &#64;DataMasking.Field(name = "idCard", type = DataMasking.Type.ID_CARD),
 *     &#64;DataMasking.Field(name = "email", type = DataMasking.Type.EMAIL)
 * })
 * public List&lt;User&gt; listUsers() { ... }
 * </pre>
 *
 * @author Carlos
 * @version 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataMasking {

    /**
     * 脱敏字段配置数组
     */
    Field[] fields();

    /**
     * 是否启用脱敏
     */
    boolean enabled() default true;

    /**
     * 脱敏字段配置
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Field {
        /**
         * 字段名，支持嵌套属性，如：user.phone
         */
        String name();

        /**
         * 脱敏类型
         */
        Type type();

        /**
         * 自定义正则表达式（当 type = CUSTOM 时使用）
         */
        String pattern() default "";

        /**
         * 保留前缀字符数（-1表示使用类型默认值）
         */
        int keepPrefix() default -1;

        /**
         * 保留后缀字符数（-1表示使用类型默认值）
         */
        int keepSuffix() default -1;
    }

    /**
     * 脱敏类型枚举
     */
    enum Type {
        /**
         * 手机号：138****8888
         */
        PHONE,

        /**
         * 身份证号：110101********1234
         */
        ID_CARD,

        /**
         * 邮箱：t***@qq.com
         */
        EMAIL,

        /**
         * 银行卡：6222 **** **** 8888
         */
        BANK_CARD,

        /**
         * 姓名：张**
         */
        NAME,

        /**
         * 地址：北京市********
         */
        ADDRESS,

        /**
         * 自定义（使用 pattern 正则）
         */
        CUSTOM
    }
}
