package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 角色编码已存在异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class RoleCodeExistsException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public RoleCodeExistsException(String roleCode) {
        super("角色编码已存在：" + roleCode);
    }

}
