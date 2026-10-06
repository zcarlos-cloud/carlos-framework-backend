package com.carlos.core.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 简单的错误码实现，用于动态包装异常自身携带的错误码
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleErrorCode implements ErrorCode {

    private String code;
    private String message;
    private int httpStatus;

}
