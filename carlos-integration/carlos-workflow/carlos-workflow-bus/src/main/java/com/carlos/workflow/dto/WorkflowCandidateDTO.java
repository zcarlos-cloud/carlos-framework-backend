package com.carlos.workflow.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 候选办理人 DTO，由组织机构中心（{@code carlos-org}）提供，工作流侧不保存任何用户主数据。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowCandidateDTO implements Serializable {

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
