package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 部门不存在异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class DepartmentNotFoundException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public DepartmentNotFoundException() {
        super("部门不存在");
    }

    public DepartmentNotFoundException(Object deptId) {
        super("部门不存在：" + deptId);
    }

}
