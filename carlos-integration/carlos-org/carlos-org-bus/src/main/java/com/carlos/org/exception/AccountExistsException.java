package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 账号已存在异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class AccountExistsException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public AccountExistsException() {
        super("账号已存在");
    }

    public AccountExistsException(String account) {
        super("账号已存在：" + account);
    }

}
