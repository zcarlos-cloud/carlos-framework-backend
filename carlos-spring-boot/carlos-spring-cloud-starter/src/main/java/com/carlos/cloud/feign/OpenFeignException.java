package com.carlos.cloud.feign;


import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * openfeign异常父类
 * </p>
 *
 * @author carlos
 * @date 2022/6/16 13:41
 */
public class OpenFeignException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public OpenFeignException() {
        super("openfeign调用异常！");
    }

    public OpenFeignException(final String message) {
        super(message);
    }

    public OpenFeignException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }

    public OpenFeignException(final String errorCode, final String message) {
        super(errorCode, message);
    }

    public OpenFeignException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }


    public OpenFeignException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public OpenFeignException(final Throwable cause) {
        super(cause);
    }

    public OpenFeignException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
