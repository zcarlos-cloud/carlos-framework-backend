package com.carlos.workflow.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionPageParam;
import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import org.dromara.warm.flow.orm.entity.FlowDefinition;

import java.util.List;

/**
 * <p>
 * 流程定义 查询封装接口。
 * </p>
 * <p>
 * 数据查询全部走 MyBatis-Plus 条件构造器（引擎实体即表映射对象），本层不出现任何 SQL；
 * 发布 / 撤销 / 复制 / 删除这类带引擎语义的动作由 Service 调 Warm-Flow 的 {@code DefService}，
 * 不在本层实现。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public interface WorkflowDefinitionManager extends BaseService<FlowDefinition> {

    /**
     * 分页查询流程定义
     *
     * @param param 分页与过滤参数
     * @return Paging&lt;WorkflowDefinitionDTO&gt;
     */
    Paging<WorkflowDefinitionDTO> getPage(ApiWorkflowDefinitionPageParam param);

    /**
     * 按主键查询流程定义
     *
     * @param definitionId 流程定义id
     * @return WorkflowDefinitionDTO，不存在时为 null
     */
    WorkflowDefinitionDTO getDtoById(Long definitionId);

    /**
     * 按流程编码查询该编码下的全部流程定义（含历史版本）
     *
     * @param flowCode 流程编码
     * @return List&lt;WorkflowDefinitionDTO&gt;
     */
    List<WorkflowDefinitionDTO> listByFlowCode(String flowCode);

}
