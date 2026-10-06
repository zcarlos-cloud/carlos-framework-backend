package com.carlos.workflow.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.workflow.api.ApiWorkflowTask;
import com.carlos.workflow.api.pojo.ao.WorkflowHisTaskAO;
import com.carlos.workflow.api.pojo.ao.WorkflowTaskAO;
import com.carlos.workflow.convert.WorkflowAoConvert;
import com.carlos.workflow.service.WorkflowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 待办任务 Feign 契约实现（服务间调用，{@code /api/workflow/**}）：审批页加载、已办任务详情。
 * </p>
 * <p>
 * 网关**不路由** {@code /api/**}，因此这里的端点只面向其他业务服务的 Feign 调用，
 * 不面向前端。管理端（前端经网关）的待办/已办分页、候选办理人与统一审批动作在同包外另有一套，见
 * {@code com.carlos.workflow.controller.WorkflowTaskController}（{@code /workflow/task/**}）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@RestController
@RequestMapping("/api/workflow")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "carlos.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Tag(name = "工作流-待办任务（服务间）")
public class ApiWorkflowTaskImpl implements ApiWorkflowTask {

    private final WorkflowTaskService workflowTaskService;

    @Override
    @GetMapping("/task/{taskId}")
    @Operation(summary = "审批页加载（任务详情 + 表单 + 表单数据）")
    public Result<WorkflowTaskAO> load(@PathVariable("taskId") Long taskId) {
        return Result.success(WorkflowAoConvert.INSTANCE.toTaskAO(workflowTaskService.load(taskId)));
    }

    @Override
    @GetMapping("/his-task/{hisTaskId}")
    @Operation(summary = "已办任务详情")
    public Result<WorkflowHisTaskAO> loadHistory(@PathVariable("hisTaskId") Long hisTaskId) {
        return Result.success(WorkflowAoConvert.INSTANCE.toHisTaskAO(workflowTaskService.loadHistory(hisTaskId)));
    }

}
