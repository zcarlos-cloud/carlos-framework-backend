package com.carlos.workflow.api.pojo.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 流程定义复制参数，管理端列表页「复制」按钮使用。
 * </p>
 * <p>
 * 与 {@link ApiWorkflowDefinitionIdsParam} 分成两个类型，是因为复制是单定义操作且需要回传新定义 id，
 * 批量复制的语义（多份新定义 id）尚未定契约。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class ApiWorkflowDefinitionCopyParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 被复制的流程定义 id
     */
    @NotNull(message = "流程定义id不能为空")
    private Long id;

}
