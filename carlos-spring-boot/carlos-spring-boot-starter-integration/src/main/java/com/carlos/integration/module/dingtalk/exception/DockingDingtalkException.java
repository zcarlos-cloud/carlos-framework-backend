package com.carlos.integration.module.dingtalk.exception;

import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 钉钉对接模块异常
 * </p>
 *
 * @author Carlos
 * @date 2022/2/23 18:12
 */
public class DockingDingtalkException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public DockingDingtalkException() {
        super("Docking Dingtalk component exception!");
    }

    public DockingDingtalkException(String message) {
        super(message);
    }

    public DockingDingtalkException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DockingDingtalkException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DockingDingtalkException(Throwable cause) {
        super(cause);
    }

    public DockingDingtalkException(String message, Throwable cause) {
        super(message, cause);
    }

}
