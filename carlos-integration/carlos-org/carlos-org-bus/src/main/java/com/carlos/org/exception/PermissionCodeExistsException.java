package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 权限编码已存在异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class PermissionCodeExistsException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public PermissionCodeExistsException(String permCode) {
        super("权限编码已存在：" + permCode);
    }

}
