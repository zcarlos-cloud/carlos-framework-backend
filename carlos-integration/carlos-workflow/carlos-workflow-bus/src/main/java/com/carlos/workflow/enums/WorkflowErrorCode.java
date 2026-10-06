package com.carlos.workflow.enums;

import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.ErrorCode;
import com.carlos.workflow.exception.WorkflowException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 工作流组件错误码，模块编码 07。
 * </p>
 * <p>
 * 流程定义、流程实例、任务不存在与操作不允许四个业务码已由
 * {@link com.carlos.core.response.CommonErrorCode} 占用（20701~20704），本枚举不再重复定义，
 * 只补充客户端参数错误与引擎执行异常。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Getter
@RequiredArgsConstructor
public enum WorkflowErrorCode implements ErrorCode {

    // ==================== 客户端错误 (1-07-xx) ====================
    WORKFLOW_PARAM_INVALID("10701", "工作流请求参数无效", 400),
    WORKFLOW_ACTION_UNSUPPORTED("10702", "不支持的审批动作", 400),
    WORKFLOW_DEFINITION_JSON_INVALID("10703", "流程定义 JSON 无效", 400),
    WORKFLOW_TARGET_HANDLER_REQUIRED("10704", "目标办理人不能为空", 400),
    WORKFLOW_ASSIGNEE_EXPRESSION_INVALID("10705", "流程定义的办理人表达式无效", 400),

    // ==================== 系统错误 (5-07-xx) ====================
    WORKFLOW_ENGINE_ERROR("50701", "工作流引擎执行异常", 500),
    WORKFLOW_ASSIGNEE_UNRESOLVED("50702", "节点配置了办理人但未解析到任何人", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    /**
     * 创建业务异常（使用默认消息，保留 httpStatus）
     *
     * @return BusinessException
     */
    public BusinessException exception() {
        return new BusinessException(this);
    }

    /**
     * 创建业务异常（自定义消息，保留 httpStatus）
     *
     * @param message 自定义消息
     * @return BusinessException
     */
    public BusinessException exception(String message) {
        return new BusinessException(this, message);
    }

    /**
     * 创建组件异常（使用默认消息，保留 httpStatus）
     *
     * @return WorkflowException
     */
    public WorkflowException workflowException() {
        return new WorkflowException(this);
    }

    /**
     * 创建组件异常（自定义消息，保留 httpStatus）
     *
     * @param message 自定义消息
     * @return WorkflowException
     */
    public WorkflowException workflowException(String message) {
        return new WorkflowException(this, message);
    }

}
