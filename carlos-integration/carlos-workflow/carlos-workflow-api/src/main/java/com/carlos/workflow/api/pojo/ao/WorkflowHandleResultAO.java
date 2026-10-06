package com.carlos.workflow.api.pojo.ao;

import com.carlos.workflow.api.enums.WorkflowActionEnum;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 审批动作执行结果 AO。
 * </p>
 * <p>
 * 流程流转类动作（PASS/REJECT/FREE_JUMP）会带出流转后的流程实例；转办、委托、加签、减签
 * 只调整办理人，不改变当前节点，{@code instance} 为 null。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
@Builder
public class WorkflowHandleResultAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 本次执行的审批动作
     */
    private WorkflowActionEnum action;

    /**
     * 被操作的任务id
     */
    private Long taskId;

    /**
     * 引擎是否报告操作成功
     */
    private Boolean operated;

    /**
     * 流转后的流程实例，办理人调整类动作为 null
     */
    private WorkflowInstanceAO instance;

}
