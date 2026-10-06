package com.carlos.datascope.exception;


import com.carlos.core.exception.ComponentException;

/**
 * <p>
 * 数据权限异常父类
 * </p>
 *
 * @author Carlos
 * @date 2022/11/23 11:33
 */
public class DataScopeException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public DataScopeException() {
        super("data scope exception!");
    }

    public DataScopeException(String message) {
        super(message);
    }

    public DataScopeException(Throwable cause) {
        super(cause);
    }

    public DataScopeException(String message, Throwable cause) {
        super(message, cause);
    }
}
