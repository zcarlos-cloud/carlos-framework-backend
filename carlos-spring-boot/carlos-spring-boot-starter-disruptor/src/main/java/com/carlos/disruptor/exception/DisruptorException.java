package com.carlos.disruptor.exception;

import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * Disruptor 异常类
 *
 * @author Carlos
 * @date 2026-03-14
 */
public class DisruptorException extends ComponentException {

    private static final long serialVersionUID = 1L;

    public DisruptorException() {
        super("Disruptor Exception");
    }

    public DisruptorException(String message) {
        super(message);
    }

    public DisruptorException(String message, Throwable cause) {
        super(message, cause);
    }

    public DisruptorException(Throwable cause) {
        super(cause);
    }

    public DisruptorException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DisruptorException(ErrorCode errorCode) {
        super(errorCode);
    }
}
