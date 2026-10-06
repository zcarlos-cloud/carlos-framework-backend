package com.carlos.workflow.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.workflow.api.pojo.param.ApiWorkflowTaskPageParam;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
import org.dromara.warm.flow.orm.entity.FlowTask;

import java.util.Collection;
import java.util.List;

/**
 * <p>
 * 待办 / 已办任务查询封装接口。
 * </p>
 * <p>
 * 数据全部来自引擎的 {@code flow_*} 表，用 MyBatis-Plus 条件构造器表达，本层不出现任何 SQL；
 * 引擎自带的 {@code TaskService} 只有「按 id 取任务」这类单条查询，列表查询由本层补充。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public interface WorkflowTaskManager extends BaseService<FlowTask> {

    /**
     * 待办分页：只返回中间节点上、且办理人标识命中给定集合的任务
     *
     * @param param      分页与过滤参数（nodeName 模糊、flowStatus 等值）
     * @param handlerIds 当前用户的办理人标识集合（来自 {@code PermissionHandler#permissions()}）
     * @return Paging&lt;WorkflowTaskDTO&gt;；handlerIds 为空时返回空分页
     */
    Paging<WorkflowTaskDTO> getTodoPage(ApiWorkflowTaskPageParam param, Collection<String> handlerIds);

    /**
     * 已办分页：只返回指定办理人办理过的历史任务，且每个流程实例只保留<b>最近办理的那一条</b>
     *
     * @param param      分页与过滤参数（nodeName 模糊、flowStatus 等值）
     * @param approverId 当前用户的办理人唯一标识
     * @return Paging&lt;WorkflowHisTaskDTO&gt;；approverId 为空时返回空分页
     */
    Paging<WorkflowHisTaskDTO> getDonePage(ApiWorkflowTaskPageParam param, String approverId);

    /**
     * 某待办任务当前的办理人标识（{@code flow_user} 全部协作类型）
     * <p>
     * 减签只能减这些人，加签 / 转办 / 委托则要排除这些人，因此两端共用本方法。
     * </p>
     *
     * @param taskId 待办任务id
     * @return 去重后的办理人标识列表
     */
    List<String> listTaskHandlerIds(Long taskId);

}
