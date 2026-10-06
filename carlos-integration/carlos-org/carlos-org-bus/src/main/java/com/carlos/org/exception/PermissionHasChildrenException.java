package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 权限存在子节点异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class PermissionHasChildrenException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public PermissionHasChildrenException(String permId) {
        super("权限存在子节点，无法删除：" + permId);
    }

}
