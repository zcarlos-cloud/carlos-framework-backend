package com.carlos.workflow.api.pojo.param;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 流程定义批量操作参数（发布 / 撤销发布 / 删除），管理端列表页批量按钮使用。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class ApiWorkflowDefinitionIdsParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程定义 id 集合
     */
    @NotEmpty(message = "流程定义id集合不能为空")
    private List<Long> ids;

}
