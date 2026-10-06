package com.carlos.workflow.exception;

import com.carlos.core.exception.ComponentException;
import com.carlos.core.response.ErrorCode;

/**
 * <p>
 * 工作流组件异常，用于包装 Warm-Flow 引擎抛出的 {@code FlowException} 并携带框架错误码。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public class WorkflowException extends ComponentException {

    private static final long serialVersionUID = 1L;

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(ErrorCode errorCode) {
        super(errorCode);
    }

    public WorkflowException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
