package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 用户不存在异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class UserNotFoundException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException() {
        super("用户不存在");
    }

    public UserNotFoundException(Object userId) {
        super("用户不存在：" + userId);
    }

}
