package com.carlos.fx.exception;

/**
 * <p>
 * 连接异常
 * </p>
 *
 * @author Carlos
 * @date 2021/9/27 10:45
 */
public class ConnectionException extends GeneratorException {

    public ConnectionException() {
        super("连接异常！");
    }

    public ConnectionException(final String message) {
        super(message);
    }

    public ConnectionException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ConnectionException(Throwable cause) {
        super(cause);
    }
}
