package com.carlos.sms.exception;

import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 短信模板异常
 * </p>
 *
 * @author Carlos
 * @date 2024/3/11 15:15
 */
public class SmsTemplateException extends SmsException {

    private static final long serialVersionUID = -2303357122330162359L;

    public SmsTemplateException() {
        super("短信模板异常!");
    }

    public SmsTemplateException(String message) {
        super(message);
    }

    public SmsTemplateException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SmsTemplateException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SmsTemplateException(Throwable cause) {
        super(cause);
    }

    public SmsTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
