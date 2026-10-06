package com.carlos.encrypt.exception;


import com.carlos.core.exception.ComponentException;

/**
 * <p>
 * 加密异常父类
 * </p>
 *
 * @author Carlos
 * @date 2023/3/22 14:15
 */
public class EncryptException extends ComponentException {

    private static final long serialVersionUID = -2303357122330162359L;

    public EncryptException() {
        super("encrypt exception!");
    }

    public EncryptException(String message) {
        super(message);
    }

    public EncryptException(Throwable cause) {
        super(cause);
    }

    public EncryptException(String message, Throwable cause) {
        super(message, cause);
    }
}
