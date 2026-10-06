package com.carlos.workflow.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 审批交互动作的候选办理人 AO，用于加签 / 转办 / 委托 / 减签时选择人员。
 * </p>
 * <p>
 * 数据来源是组织机构中心（{@code carlos-org}），不是工作流自身的 {@code flow_user} 表：
 * {@code flow_user} 只记录「已经是谁的待办」，不含姓名、部门等展示信息。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowCandidateAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 登录账号
     */
    private String code;

    /**
     * 姓名（昵称）
     */
    private String name;

    /**
     * 归属部门id
     */
    private Long deptId;

    /**
     * 归属部门名称
     */
    private String deptName;

}
