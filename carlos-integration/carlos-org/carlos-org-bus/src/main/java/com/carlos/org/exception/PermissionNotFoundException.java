package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 权限不存在异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class PermissionNotFoundException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public PermissionNotFoundException(Object id) {
        super("权限不存在：" + id);
    }

}
