package com.carlos.workflow.api.pojo.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 流程定义保存参数，管理端使用。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class ApiWorkflowDefinitionSaveParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义id，为空表示新增流程定义
     */
    private Long definitionId;

    /**
     * 流程定义 JSON，结构为 Warm-Flow 设计器产出的 DefJson
     */
    @NotBlank(message = "流程定义JSON不能为空")
    private String defJson;

    /**
     * 是否只保存节点与跳转（不影响流程定义表主记录），仅在更新已有定义时生效
     */
    private boolean onlyNodeSkip = true;

    /**
     * 保存后是否立即发布
     */
    private boolean publish;

}
