package com.carlos.json.jackson.exception;


import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * 组件异常父类
 *
 * @author carlos
 * @date 2020-09-20 00:06:26
 */
public class JacksonException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public JacksonException() {
        super("Jackson组件异常！");
    }

    public JacksonException(String message) {
        super(message);
    }


    public JacksonException(ErrorCode errorCode) {
        super(errorCode);
    }

    public JacksonException(Throwable cause) {
        super(cause);
    }

    public JacksonException(String message, Throwable cause) {
        super(message, cause);
    }
}
