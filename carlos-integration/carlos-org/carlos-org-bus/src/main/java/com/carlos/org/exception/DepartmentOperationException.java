package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 部门操作异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class DepartmentOperationException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public DepartmentOperationException() {
        super("部门操作失败");
    }

    public DepartmentOperationException(String message) {
        super(message);
    }

}
