package com.carlos.fx.exception;

/**
 * <p>
 * 代码生成器异常
 * </p>
 *
 * @author Carlos
 * @date 2021/9/27 10:44
 */
public class GeneratorException extends RuntimeException {

    public GeneratorException() {
        super("代码生成器异常！");
    }

    public GeneratorException(String message) {
        super(message);
    }

    public GeneratorException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeneratorException(Throwable cause) {
        super(cause);
    }

    protected GeneratorException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
