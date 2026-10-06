package com.carlos.workflow.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.workflow.api.ApiWorkflowInstance;
import com.carlos.workflow.api.pojo.ao.WorkflowChartAO;
import com.carlos.workflow.api.pojo.ao.WorkflowInstanceAO;
import com.carlos.workflow.api.pojo.param.ApiWorkflowStartParam;
import com.carlos.workflow.convert.WorkflowAoConvert;
import com.carlos.workflow.service.WorkflowInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 流程实例 Feign 契约实现（服务间调用，{@code /api/workflow/instance/**}）：发起流程、流程图查询。
 * </p>
 * <p>
 * 网关**不路由** {@code /api/**}，因此这里的端点只面向其他业务服务的 Feign 调用，
 * 不面向前端。管理端（前端经网关）的审批记录、可选跳转节点与实例级动作在同包外另有一套，见
 * {@code com.carlos.workflow.controller.WorkflowInstanceController}（{@code /workflow/instance/**}）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@RestController
@RequestMapping("/api/workflow/instance")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "carlos.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Tag(name = "工作流-流程实例（服务间）")
public class ApiWorkflowInstanceImpl implements ApiWorkflowInstance {

    private final WorkflowInstanceService workflowInstanceService;

    // 服务间发起流程（/api/** 不经网关，但发起动作同样属于要留痕的业务操作）
    @Log(
        title = "流程实例",
        logType = "WF_INSTANCE_START",
        businessType = BusinessType.INSERT,
        operation = "'发起流程: ' + #param.flowCode + ', 业务=' + #param.businessId",
        targetType = "WF_INSTANCE",
        targetId = "#param.businessId"
    )
    @Override
    @PostMapping("/start")
    @Operation(summary = "发起流程")
    public Result<WorkflowInstanceAO> start(@RequestBody ApiWorkflowStartParam param) {
        return Result.success(WorkflowAoConvert.INSTANCE.toInstanceAO(workflowInstanceService.start(param)));
    }

    @Override
    @GetMapping("/{instanceId}/chart")
    @Operation(summary = "流程图查询（含节点办理状态着色）")
    public Result<WorkflowChartAO> chart(@PathVariable("instanceId") Long instanceId) {
        return Result.success(WorkflowAoConvert.INSTANCE.toChartAO(workflowInstanceService.chart(instanceId)));
    }

}
