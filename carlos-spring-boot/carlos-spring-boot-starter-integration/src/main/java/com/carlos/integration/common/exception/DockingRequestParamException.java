package com.carlos.integration.common.exception;

import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 对接请求参数异常
 * </p>
 *
 * @author Carlos
 * @date 2022/2/23 18:12
 */
public class DockingRequestParamException extends DockingException {

    private static final long serialVersionUID = -2303357122330162359L;

    public DockingRequestParamException() {
        super("Docking request param exception!");
    }

    public DockingRequestParamException(String message) {
        super(message);
    }

    public DockingRequestParamException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DockingRequestParamException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DockingRequestParamException(Throwable cause) {
        super(cause);
    }

    public DockingRequestParamException(String message, Throwable cause) {
        super(message, cause);
    }
}
