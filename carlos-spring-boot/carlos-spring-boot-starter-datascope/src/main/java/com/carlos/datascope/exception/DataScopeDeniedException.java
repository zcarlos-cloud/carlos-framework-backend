package com.carlos.datascope.exception;

import com.carlos.core.exception.BusinessException;

/**
 * 数据权限拒绝异常
 * <p>
 * 当用户没有数据权限时抛出
 *
 * @author Carlos
 * @version 2.0
 */
public class DataScopeDeniedException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public DataScopeDeniedException(String message) {
        super("DATA_SCOPE_DENIED", message);
    }

    public DataScopeDeniedException(String code, String message) {
        super(code, message);
    }

    public DataScopeDeniedException(String message, Throwable cause) {
        super("DATA_SCOPE_DENIED", message, 400, cause);
    }
}
