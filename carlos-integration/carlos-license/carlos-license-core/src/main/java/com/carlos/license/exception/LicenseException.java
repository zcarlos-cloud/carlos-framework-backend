package com.carlos.license.exception;


import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * 缓存异常父类
 *
 * @author carlos
 * @date 2020-09-20 00:06:26
 */
public class LicenseException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public LicenseException() {
        super("snowflake exception!");
    }

    public LicenseException(String message) {
        super(message);
    }

    public LicenseException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public LicenseException(ErrorCode errorCode) {
        super(errorCode);
    }

    public LicenseException(Throwable cause) {
        super(cause);
    }

    public LicenseException(String message, Throwable cause) {
        super(message, cause);
    }
}
