package com.carlos.sms.exception;

import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 短信异常
 * </p>
 *
 * @author Carlos
 * @date 2024/3/11 15:15
 */
public class SmsNotSupportException extends SmsException {

    private static final long serialVersionUID = -2303357122330162359L;

    public SmsNotSupportException() {
        super("短信功能暂不支持!");
    }

    public SmsNotSupportException(String message) {
        super(message);
    }

    public SmsNotSupportException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SmsNotSupportException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SmsNotSupportException(Throwable cause) {
        super(cause);
    }

    public SmsNotSupportException(String message, Throwable cause) {
        super(message, cause);
    }
}
