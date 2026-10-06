package com.carlos.workflow.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 流程图跳转线 AO。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowChartSkipAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前节点编码
     */
    private String nowNodeCode;

    /**
     * 下一节点编码
     */
    private String nextNodeCode;

    /**
     * 跳转名称
     */
    private String skipName;

    /**
     * 跳转类型（PASS审批通过 REJECT退回）
     */
    private String skipType;

    /**
     * 跳转条件
     */
    private String skipCondition;

    /**
     * 办理状态（0未办理 1待办理 2已办理）
     */
    private Integer status;

}
