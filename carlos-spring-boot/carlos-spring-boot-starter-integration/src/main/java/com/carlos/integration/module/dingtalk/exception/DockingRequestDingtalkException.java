package com.carlos.integration.module.dingtalk.exception;

import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 钉钉对接请求异常
 * </p>
 *
 * @author Carlos
 * @date 2022/2/23 18:12
 */
public class DockingRequestDingtalkException extends DockingDingtalkException {

    private static final long serialVersionUID = -2303357122330162359L;

    public DockingRequestDingtalkException() {
        super("Docking dingtalk request exception!");
    }

    public DockingRequestDingtalkException(String message) {
        super(message);
    }

    public DockingRequestDingtalkException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DockingRequestDingtalkException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DockingRequestDingtalkException(Throwable cause) {
        super(cause);
    }

    public DockingRequestDingtalkException(String message, Throwable cause) {
        super(message, cause);
    }

}
