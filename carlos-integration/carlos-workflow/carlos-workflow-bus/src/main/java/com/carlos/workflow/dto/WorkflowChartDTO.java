package com.carlos.workflow.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 流程图 DTO，节点与跳转线自带办理状态，配合 {@code chartStatusColor} 完成着色。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class WorkflowChartDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程实例id
     */
    private Long instanceId;

    /**
     * 流程定义id
     */
    private Long definitionId;

    /**
     * 流程编码
     */
    private String flowCode;

    /**
     * 流程名称
     */
    private String flowName;

    /**
     * 设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型）
     */
    private String modelValue;

    /**
     * 流程图三原色，顺序为 已办理 / 待办理 / 未办理，元素形如 "56,161,105"
     */
    private List<String> chartStatusColor;

    /**
     * 流程节点
     */
    private List<WorkflowChartNodeDTO> nodes;

    /**
     * 引擎原始流程图 JSON（含节点办理状态），供需要直接对接设计器前端的场景使用
     */
    private String sourceJson;

}
