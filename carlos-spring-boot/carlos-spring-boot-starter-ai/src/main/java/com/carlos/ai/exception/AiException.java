package com.carlos.ai.exception;

import com.carlos.ai.enums.AiErrorCode;
import com.carlos.core.exception.BusinessException;

/**
 * <p>
 * AI 模块业务异常
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
public class AiException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiException(AiErrorCode errorCode) {
        super(errorCode);
    }

    public AiException(AiErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
