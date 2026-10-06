package com.carlos.workflow.api;

import com.carlos.core.response.Result;
import com.carlos.workflow.ServiceNameConstant;
import com.carlos.workflow.api.fallback.ApiWorkflowInstanceFallbackFactory;
import com.carlos.workflow.api.pojo.ao.WorkflowChartAO;
import com.carlos.workflow.api.pojo.ao.WorkflowInstanceAO;
import com.carlos.workflow.api.pojo.param.ApiWorkflowStartParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>
 * 工作流流程实例 Feign 接口：只保留服务间调用真正需要的「发起流程」与「流程图查询」。
 * </p>
 * <p>
 * 命名空间分界：本接口落在 {@code /api/workflow/instance/**}，网关**不路由** {@code /api/**}，
 * 因此这组端点只供其他业务服务通过 Feign 编程式调用。审批记录、可选跳转节点与实例级动作
 * （撤回 / 终止 / 挂起 / 激活）属于管理端交互，落在 {@code /workflow/instance/**}，
 * 由 {@code com.carlos.workflow.controller.WorkflowInstanceController} 提供，
 * 不再出现在本接口上——两套命名空间不得互相混入。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "workflowInstance",
    path = "/api/workflow/instance", fallbackFactory = ApiWorkflowInstanceFallbackFactory.class)
public interface ApiWorkflowInstance {

    /**
     * 发起流程
     *
     * @param param 发起参数
     * @return 流程实例
     */
    @PostMapping("/start")
    Result<WorkflowInstanceAO> start(@RequestBody ApiWorkflowStartParam param);

    /**
     * 流程图查询，含节点办理状态着色
     *
     * @param instanceId 流程实例id
     * @return 流程图
     */
    @GetMapping("/{instanceId}/chart")
    Result<WorkflowChartAO> chart(@PathVariable("instanceId") Long instanceId);

}
