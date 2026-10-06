package com.carlos.oss.exception;

import com.carlos.core.exception.BusinessException;

/**
 * OSS 异常
 *
 * @author carlos
 * @date 2026-02-01
 */
public class OssException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public OssException(String message) {
        super(message);
    }

    public OssException(String message, Throwable cause) {
        super(message, cause);
    }

    public OssException(Throwable cause) {
        super(cause);
    }
}
