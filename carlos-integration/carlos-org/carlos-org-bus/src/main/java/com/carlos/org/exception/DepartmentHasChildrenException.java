package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 部门下有子部门异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class DepartmentHasChildrenException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public DepartmentHasChildrenException() {
        super("部门下有子部门，无法删除");
    }

    public DepartmentHasChildrenException(String deptId) {
        super("部门[" + deptId + "]下有子部门，无法删除");
    }

}
