package com.carlos.sms.exception;

import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 短信异常
 * </p>
 *
 * @author Carlos
 * @date 2024/3/11 15:15
 */
public class SmsException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public SmsException() {
        super("短信组件异常!");
    }

    public SmsException(String message) {
        super(message);
    }

    public SmsException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SmsException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SmsException(Throwable cause) {
        super(cause);
    }

    public SmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
