package com.carlos.integration.core.support;

import com.carlos.integration.common.exception.DockingException;

/**
 * <p>
 * RestClient 对接异常
 * </p>
 *
 * @author Carlos
 * @since 1.0.0
 */
public class DockingRestClientException extends DockingException {

    public DockingRestClientException(String message) {
        super(message);
    }

    public DockingRestClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockingRestClientException(Throwable cause) {
        super(cause);
    }
}
