package com.carlos.workflow.api.pojo.param;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 流程实例级动作参数：撤回（revoke）与终止（terminate）共用。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class ApiWorkflowInstanceActionParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程实例id
     */
    @NotNull(message = "流程实例id不能为空")
    private Long instanceId;

    /**
     * 审批意见（撤回/终止原因），写进历史任务
     */
    @Size(max = 500, message = "审批意见长度不能超过500个字符")
    private String message;

}
