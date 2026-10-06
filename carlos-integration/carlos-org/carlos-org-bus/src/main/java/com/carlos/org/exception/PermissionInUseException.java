package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 权限已被使用异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class PermissionInUseException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public PermissionInUseException(String permId) {
        super("权限已被角色使用，无法删除：" + permId);
    }

}
