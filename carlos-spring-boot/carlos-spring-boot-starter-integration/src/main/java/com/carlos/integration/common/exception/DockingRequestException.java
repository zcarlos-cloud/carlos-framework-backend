package com.carlos.integration.common.exception;

import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 对接请求发生异常
 * </p>
 *
 * @author Carlos
 * @date 2022/2/23 18:12
 */
public class DockingRequestException extends DockingException {

    private static final long serialVersionUID = -2303357122330162359L;

    public DockingRequestException() {
        super("Docking request exception!");
    }

    public DockingRequestException(String message) {
        super(message);
    }

    public DockingRequestException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DockingRequestException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DockingRequestException(Throwable cause) {
        super(cause);
    }

    public DockingRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
