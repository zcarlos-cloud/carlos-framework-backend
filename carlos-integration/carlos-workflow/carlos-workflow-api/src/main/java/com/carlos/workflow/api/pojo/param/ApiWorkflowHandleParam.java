package com.carlos.workflow.api.pojo.param;

import com.carlos.workflow.api.enums.WorkflowActionEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 统一审批动作参数，按 {@link WorkflowActionEnum} 分派到引擎的不同方法。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class ApiWorkflowHandleParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 待办任务id
     */
    @NotNull(message = "待办任务id不能为空")
    private Long taskId;

    /**
     * 审批动作
     */
    @NotNull(message = "审批动作不能为空")
    private WorkflowActionEnum action;

    /**
     * 当前办理人唯一标识，缺省时由引擎的 PermissionHandler 提供
     */
    @Size(max = 80, message = "办理人标识长度不能超过80个字符")
    private String handler;

    /**
     * 审批意见
     */
    @Size(max = 500, message = "审批意见长度不能超过500个字符")
    private String message;

    /**
     * 目标节点编码，FREE_JUMP 必填，PASS/REJECT 填了即为任意通过/任意驳回
     */
    @Size(max = 100, message = "目标节点编码长度不能超过100个字符")
    private String nodeCode;

    /**
     * 目标办理人，转办、委托、加签、减签必填
     */
    private List<String> targetHandlers;

    /**
     * 流程变量
     */
    private Map<String, Object> variable;

}
