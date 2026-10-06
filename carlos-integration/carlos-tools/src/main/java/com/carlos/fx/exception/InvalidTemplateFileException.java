package com.carlos.fx.exception;

/**
 * <p>
 * 无效模板文件异常
 * </p>
 *
 * @author Carlos
 * @date 2020/9/7 17:28
 */
public class InvalidTemplateFileException extends GeneratorException {

    public InvalidTemplateFileException() {
        super("无效的模板文件！");
    }

    public InvalidTemplateFileException(final String message) {
        super(message);
    }

    public InvalidTemplateFileException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidTemplateFileException(Throwable cause) {
        super(cause);
    }

    protected InvalidTemplateFileException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
