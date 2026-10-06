package com.carlos.core.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 字段级错误详情
 * <p>
 * 用于参数校验失败时返回具体的字段错误信息
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldErrorDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 错误字段名
     */
    private String field;

    /**
     * 错误消息
     */
    private String message;

    /**
     * 被拒绝的值
     */
    private Object rejectedValue;
}
