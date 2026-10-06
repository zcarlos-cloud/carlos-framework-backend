package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 用户操作异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class UserOperationException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public UserOperationException() {
        super("用户操作失败");
    }

    public UserOperationException(String message) {
        super(message);
    }

}
