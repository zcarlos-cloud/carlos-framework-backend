package com.carlos.org.exception;

import com.carlos.core.exception.GlobalException;

/**
 * <p>
 * 部门下有用户异常
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
public class DepartmentHasUsersException extends GlobalException {

    private static final long serialVersionUID = 1L;

    public DepartmentHasUsersException() {
        super("部门下有用户，无法删除");
    }

    public DepartmentHasUsersException(String deptId) {
        super("部门[" + deptId + "]下有用户，无法删除");
    }

}
