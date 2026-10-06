package com.carlos.integration.common.exception;

import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 对接模块基础异常
 * </p>
 *
 * @author Carlos
 * @date 2022/2/23 18:12
 */
public class DockingException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public DockingException() {
        super("Docking api exception!");
    }

    public DockingException(String message) {
        super(message);
    }

    public DockingException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DockingException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DockingException(Throwable cause) {
        super(cause);
    }

    public DockingException(String message, Throwable cause) {
        super(message, cause);
    }
}
