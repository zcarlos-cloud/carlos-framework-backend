package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 用户状态异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class InvalidUserStateException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public InvalidUserStateException() {
        super("用户状态异常");
    }

    public InvalidUserStateException(String message) {
        super(message);
    }

}
