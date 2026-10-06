package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 角色已被使用异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class RoleInUseException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public RoleInUseException(String roleId) {
        super("角色已被用户使用，无法删除：" + roleId);
    }

}
