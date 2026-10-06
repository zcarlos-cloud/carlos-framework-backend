package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 部门编号已存在异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class DepartmentCodeExistsException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public DepartmentCodeExistsException() {
        super("部门编号已存在");
    }

    public DepartmentCodeExistsException(String deptCode) {
        super("部门编号已存在：" + deptCode);
    }

}
