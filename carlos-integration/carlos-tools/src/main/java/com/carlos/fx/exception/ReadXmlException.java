package com.carlos.fx.exception;

/**
 * <p>
 * 模板文件异常
 * </p>
 *
 * @author Carlos
 * @date 2021/9/27 12:05
 */
public class ReadXmlException extends GeneratorException {

    public ReadXmlException() {
        super("xml文件读取异常！");
    }

    public ReadXmlException(final String message) {
        super(message);
    }

    public ReadXmlException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ReadXmlException(Throwable cause) {
        super(cause);
    }
}
