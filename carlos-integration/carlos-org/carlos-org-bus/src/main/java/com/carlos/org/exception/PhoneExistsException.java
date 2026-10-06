package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 手机号已存在异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class PhoneExistsException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public PhoneExistsException() {
        super("手机号已存在");
    }

    public PhoneExistsException(String phone) {
        super("手机号已存在：" + phone);
    }

}
