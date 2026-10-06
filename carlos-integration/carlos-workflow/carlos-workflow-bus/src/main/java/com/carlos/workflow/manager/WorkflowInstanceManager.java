package com.carlos.workflow.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstancePageParam;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import org.dromara.warm.flow.orm.entity.FlowInstance;

/**
 * <p>
 * 流程实例查询封装接口。
 * </p>
 * <p>
 * 数据全部来自引擎的 {@code flow_* } 表，用 MyBatis-Plus 条件构造器表达，本层不出现任何 SQL；
 * 引擎自带的 {@code InsService} 只有「按 id 取实例 / 按业务id 取实例」这类单条查询，
 * 「我发起的」列表查询由本层补充。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public interface WorkflowInstanceManager extends BaseService<FlowInstance> {

    /**
     * 「我发起的」分页：只返回 {@code flow_instance.create_by} 等于当前用户的实例
     *
     * @param param    分页与过滤参数（flowName 模糊、flowStatus / businessId 等值）
     * @param createBy 当前用户的用户 id（即实例发起人）
     * @return Paging&lt;WorkflowInstanceDTO&gt;；createBy 为空时返回空分页
     */
    Paging<WorkflowInstanceDTO> getMyPage(ApiWorkflowInstancePageParam param, String createBy);

}
