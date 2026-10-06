package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 密码相关异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class PasswordException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public PasswordException() {
        super("密码错误");
    }

    public PasswordException(String message) {
        super(message);
    }

}
