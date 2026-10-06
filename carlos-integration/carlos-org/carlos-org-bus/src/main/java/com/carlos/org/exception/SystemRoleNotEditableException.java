package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 系统角色不可编辑异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class SystemRoleNotEditableException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public SystemRoleNotEditableException(String roleId) {
        super("系统角色不可编辑：" + roleId);
    }

}
