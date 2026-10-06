package com.carlos.workflow.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.workflow.api.pojo.ao.WorkflowCandidateAO;
import com.carlos.workflow.api.pojo.ao.WorkflowHandleResultAO;
import com.carlos.workflow.api.pojo.ao.WorkflowHisTaskAO;
import com.carlos.workflow.api.pojo.ao.WorkflowTaskAO;
import com.carlos.workflow.api.pojo.param.ApiWorkflowCandidatePageParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowHandleParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowTaskPageParam;
import com.carlos.workflow.convert.WorkflowAoConvert;
import com.carlos.workflow.convert.WorkflowPagingConvert;
import com.carlos.workflow.service.WorkflowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 待办任务管理端接口：<b>给前端用的</b>，经网关暴露，统一前缀 {@code /workflow/task/**}。
 * </p>
 * <p>
 * 与 Feign 契约（{@code /api/workflow/**}）是<b>两套命名空间，别混</b>：
 * 网关只路由 {@code /<服务>/**}，解析后原样透传业务路径，而 {@code /api/**} 根本不在网关路由表内
 * （只供服务间 Feign 调用）。把管理端接口挂到 {@code /api/**} 上等于让内部命名空间对外可见，
 * 因此本类<b>不 implements 任何 {@code com.carlos.workflow.api} 下的接口</b>，
 * 服务间契约只保留 {@code ApiWorkflowTask#load} 与 {@code #loadHistory}。
 * </p>
 * <p>
 * 本类只做「收参数 → 调 Service → 包 {@code Result}」，不承载业务逻辑，
 * 也不加 {@code @Transactional}（事务边界在 Service 层）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@RestController
@RequestMapping("/workflow/task")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "carlos.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Tag(name = "工作流-待办任务")
public class WorkflowTaskController {

    private final WorkflowTaskService workflowTaskService;

    @PreAuthorize("hasPermission('workflow:task', 'view')")
    @GetMapping("/todo/page")
    @Operation(summary = "待办分页（当前用户可办理的任务，按任务去重）")
    public Result<Paging<WorkflowTaskAO>> todoPage(@Validated @ModelAttribute ApiWorkflowTaskPageParam param) {
        return Result.success(WorkflowPagingConvert.convert(workflowTaskService.todoPage(param),
            WorkflowAoConvert.INSTANCE::toTaskAOList));
    }

    @PreAuthorize("hasPermission('workflow:task', 'view')")
    @GetMapping("/done/page")
    @Operation(summary = "已办分页（每个流程实例只保留我最近办理的那一条）")
    public Result<Paging<WorkflowHisTaskAO>> donePage(@Validated @ModelAttribute ApiWorkflowTaskPageParam param) {
        return Result.success(WorkflowPagingConvert.convert(workflowTaskService.donePage(param),
            WorkflowAoConvert.INSTANCE::toHisTaskAOList));
    }

    @PreAuthorize("hasPermission('workflow:task', 'view')")
    @GetMapping("/{taskId}/candidates")
    @Operation(summary = "候选办理人（减签只列已有办理人，其余动作排除已有办理人）")
    public Result<Paging<WorkflowCandidateAO>> candidates(@PathVariable("taskId") Long taskId,
                                                          @Validated @ModelAttribute ApiWorkflowCandidatePageParam param) {
        return Result.success(WorkflowPagingConvert.convert(workflowTaskService.candidates(taskId, param),
            WorkflowAoConvert.INSTANCE::toCandidateAOList));
    }

    // 审批是全模块最核心、最需要留痕的操作：动作类型进 operation，任务 id 进 targetId
    @Log(
        title = "待办任务",
        logType = "WF_TASK_HANDLE",
        businessType = BusinessType.UPDATE,
        operation = "'流程审批: 动作=' + #param.action + ', 任务=' + #param.taskId",
        targetType = "WF_TASK",
        targetId = "#param.taskId",
        riskLevel = 50
    )
    @PreAuthorize("hasPermission('workflow:task', 'approve')")
    @PostMapping("/handle")
    @Operation(summary = "统一审批动作")
    public Result<WorkflowHandleResultAO> handle(@Validated @RequestBody ApiWorkflowHandleParam param) {
        return Result.success(WorkflowAoConvert.INSTANCE.toHandleResultAO(workflowTaskService.handle(param)));
    }

}
