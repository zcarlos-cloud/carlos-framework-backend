package com.carlos.json.exception;

import com.carlos.core.exception.ComponentException;

/**
 * <p>
 * JSON 解析异常
 * </p>
 *
 * @author carlos
 * @date 2025/01/15
 */
public class JsonParseException extends ComponentException {

    private static final long serialVersionUID = 1L;

    public JsonParseException() {
        super("JSON 解析异常");
    }

    public JsonParseException(String message) {
        super(message);
    }

    public JsonParseException(Throwable cause) {
        super(cause);
    }

    public JsonParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
