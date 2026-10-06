package com.carlos.workflow.api.pojo.param;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 流程定义基础信息参数，管理端「先填基本信息、再进设计器画节点」的两步建流程使用。
 * </p>
 * <p>
 * {@code definitionId} 为空表示新建，否则表示更新（非空时定义必须已存在）。
 * 与 {@link ApiWorkflowDefinitionSaveParam} 的区别：后者收的是设计器产出的 {@code defJson}
 * （含节点与跳转线），本参数只承载 {@code flow_definition} 主记录上的字段，<b>不创建节点</b>。
 * </p>
 * <p>
 * {@code modelValue} 只在新建时可缺省（缺省 {@code CLASSICS}）；更新时必须与库中一致，
 * 否则请求被拒绝，见 {@code WorkflowDefinitionService#updateBasic}。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class ApiWorkflowDefinitionBasicParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义id，为空表示新建流程定义
     */
    private Long definitionId;

    /**
     * 流程编码，新建时必填（更新时留空表示保持原值）
     */
    @Size(max = 40, message = "流程编码长度不能超过40个字符")
    private String flowCode;

    /**
     * 流程名称，新建时必填（更新时留空表示保持原值）
     */
    @Size(max = 100, message = "流程名称长度不能超过100个字符")
    private String flowName;

    /**
     * 设计器模型（CLASSICS经典模型 MIMIC仿钉钉模型），缺省 CLASSICS，保存后不可修改
     */
    @Size(max = 40, message = "设计器模型长度不能超过40个字符")
    private String modelValue;

    /**
     * 流程类别
     */
    @Size(max = 100, message = "流程类别长度不能超过100个字符")
    private String category;

}
