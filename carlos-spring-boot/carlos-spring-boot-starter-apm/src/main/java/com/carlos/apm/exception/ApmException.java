package com.carlos.apm.exception;


import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * 组件异常父类
 *
 * @author carlos
 * @date 2020-09-20 00:06:26
 */
public class ApmException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public ApmException() {
        super("Apm组件异常！");
    }

    public ApmException(String message) {
        super(message);
    }


    public ApmException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ApmException(Throwable cause) {
        super(cause);
    }

    public ApmException(String message, Throwable cause) {
        super(message, cause);
    }
}
