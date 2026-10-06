package com.carlos.fx.exception;

/**
 * <p>
 * 模板文件异常
 * </p>
 *
 * @author Carlos
 * @date 2021/9/27 12:05
 */
public class TemplateException extends GeneratorException {

    public TemplateException() {
        super("模板文件异常！");
    }

    public TemplateException(final String message) {
        super(message);
    }

    public TemplateException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public TemplateException(Throwable cause) {
        super(cause);
    }
}
