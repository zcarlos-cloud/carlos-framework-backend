package com.carlos.apm.skywalking.exception;


import com.carlos.apm.exception.ApmException;
import com.carlos.core.response.ErrorCode;

/**
 * 组件异常父类
 *
 * @author carlos
 * @date 2020-09-20 00:06:26
 */
public class SkywalkingException extends ApmException {

    private static final long serialVersionUID = -2303357122330162359L;

    public SkywalkingException() {
        super("Apm组件异常！");
    }

    public SkywalkingException(String message) {
        super(message);
    }


    public SkywalkingException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SkywalkingException(Throwable cause) {
        super(cause);
    }

    public SkywalkingException(String message, Throwable cause) {
        super(message, cause);
    }
}
