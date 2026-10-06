package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 角色不存在异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class RoleNotFoundException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public RoleNotFoundException(Object id) {
        super("角色不存在：" + id);
    }

}
