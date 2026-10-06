package com.carlos.integration.module.dingtalk.exception;

import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 钉钉对接请求参数异常
 * </p>
 *
 * @author Carlos
 * @date 2022/2/23 18:12
 */
public class DockingRequestParamDingtalkException extends DockingDingtalkException {

    private static final long serialVersionUID = -2303357122330162359L;

    public DockingRequestParamDingtalkException() {
        super("Docking dingtalk request param exception!");
    }

    public DockingRequestParamDingtalkException(String message) {
        super(message);
    }

    public DockingRequestParamDingtalkException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DockingRequestParamDingtalkException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DockingRequestParamDingtalkException(Throwable cause) {
        super(cause);
    }

    public DockingRequestParamDingtalkException(String message, Throwable cause) {
        super(message, cause);
    }

}
