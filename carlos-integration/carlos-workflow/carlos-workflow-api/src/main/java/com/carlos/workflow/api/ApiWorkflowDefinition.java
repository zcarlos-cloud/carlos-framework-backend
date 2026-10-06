package com.carlos.workflow.api;

import com.carlos.core.response.Result;
import com.carlos.workflow.ServiceNameConstant;
import com.carlos.workflow.api.fallback.ApiWorkflowDefinitionFallbackFactory;
import com.carlos.workflow.api.pojo.ao.WorkflowDefinitionAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * <p>
 * 工作流流程定义 Feign 接口：只保留服务间调用真正需要的「已发布定义清单」。
 * </p>
 * <p>
 * 命名空间分界：本接口落在 {@code /api/workflow/definition/**}，网关**不路由** {@code /api/**}，
 * 因此这组端点只供其他业务服务通过 Feign 编程式调用。管理端（前端经网关）的
 * 保存 / 分页 / 详情 / 发布 / 撤销 / 复制 / 删除落在 {@code /workflow/definition/**}，
 * 由 {@code com.carlos.workflow.controller.WorkflowDefinitionController} 提供，
 * 不再出现在本接口上——两套命名空间不得互相混入。
 * </p>
 * <p>
 * 设计器自身的 {@code /warm-flow/**} 数据接口保持引擎原样，不在本接口内包装。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "workflowDefinition",
    path = "/api/workflow/definition", fallbackFactory = ApiWorkflowDefinitionFallbackFactory.class)
public interface ApiWorkflowDefinition {

    /**
     * 已发布的流程定义列表，供发起流程时选择
     *
     * @return 流程定义列表
     */
    @GetMapping("/published")
    Result<List<WorkflowDefinitionAO>> listPublished();

}
