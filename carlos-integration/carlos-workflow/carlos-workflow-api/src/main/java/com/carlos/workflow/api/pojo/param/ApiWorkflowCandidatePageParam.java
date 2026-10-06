package com.carlos.workflow.api.pojo.param;

import com.carlos.core.param.ParamPage;
import com.carlos.workflow.api.enums.WorkflowActionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 审批交互动作的候选办理人分页参数。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "候选办理人分页参数")
public class ApiWorkflowCandidatePageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    /**
     * 审批动作，决定候选人的口径：减签只列出当前任务已有的办理人，其余动作排除这些人
     */
    @Schema(description = "审批动作（REDUCTION_SIGNATURE 只列已有办理人，其余排除已有办理人）")
    private WorkflowActionEnum action;

    /**
     * 部门id，可选过滤
     */
    @Schema(description = "部门id，可选过滤（含子部门）")
    private Long deptId;

}
