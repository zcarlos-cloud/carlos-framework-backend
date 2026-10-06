package com.carlos.workflow.api;

import com.carlos.core.response.Result;
import com.carlos.workflow.ServiceNameConstant;
import com.carlos.workflow.api.fallback.ApiWorkflowTaskFallbackFactory;
import com.carlos.workflow.api.pojo.ao.WorkflowHisTaskAO;
import com.carlos.workflow.api.pojo.ao.WorkflowTaskAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <p>
 * 工作流待办任务 Feign 接口：只保留服务间调用真正需要的「单个任务 / 单条历史任务」读取。
 * </p>
 * <p>
 * 命名空间分界：本接口落在 {@code /api/workflow/**}，网关**不路由** {@code /api/**}，
 * 因此这组端点只供其他业务服务通过 Feign 编程式调用。待办 / 已办分页、候选办理人与
 * 统一审批动作属于管理端交互，落在 {@code /workflow/task/**}，
 * 由 {@code com.carlos.workflow.controller.WorkflowTaskController} 提供，
 * 不再出现在本接口上——两套命名空间不得互相混入。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "workflowTask",
    path = "/api/workflow", fallbackFactory = ApiWorkflowTaskFallbackFactory.class)
public interface ApiWorkflowTask {

    /**
     * 审批页加载：任务详情 + 审批表单 + 表单数据
     *
     * @param taskId 待办任务id
     * @return 待办任务详情
     */
    @GetMapping("/task/{taskId}")
    Result<WorkflowTaskAO> load(@PathVariable("taskId") Long taskId);

    /**
     * 已办任务详情
     *
     * @param hisTaskId 历史任务id
     * @return 历史任务详情
     */
    @GetMapping("/his-task/{hisTaskId}")
    Result<WorkflowHisTaskAO> loadHistory(@PathVariable("hisTaskId") Long hisTaskId);

}
