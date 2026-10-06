package com.carlos.workflow.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.workflow.api.pojo.ao.WorkflowHisTaskAO;
import com.carlos.workflow.api.pojo.ao.WorkflowInstanceAO;
import com.carlos.workflow.api.pojo.ao.WorkflowNodeAO;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstanceActionParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstancePageParam;
import com.carlos.workflow.convert.WorkflowAoConvert;
import com.carlos.workflow.convert.WorkflowPagingConvert;
import com.carlos.workflow.service.WorkflowInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 流程实例管理端接口：<b>给前端用的</b>，经网关暴露，统一前缀 {@code /workflow/instance/**}。
 * </p>
 * <p>
 * 与 Feign 契约（{@code /api/workflow/instance/**}）是<b>两套命名空间，别混</b>：
 * 网关只路由 {@code /<服务>/**}，解析后原样透传业务路径，而 {@code /api/**} 根本不在网关路由表内
 * （只供服务间 Feign 调用）。把管理端接口挂到 {@code /api/**} 上等于让内部命名空间对外可见，
 * 因此本类<b>不 implements 任何 {@code com.carlos.workflow.api} 下的接口</b>，
 * 服务间契约只保留 {@code ApiWorkflowInstance#start} 与 {@code #chart}。
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
@RequestMapping("/workflow/instance")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "carlos.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Tag(name = "工作流-流程实例")
public class WorkflowInstanceController {

    private final WorkflowInstanceService workflowInstanceService;

    /**
     * 「我发起的」流程实例分页
     * <p>
     * 只返回 {@code flow_instance.create_by} 等于当前登录用户的实例，当前用户由服务端从登录上下文取；
     * {@code flowName} 模糊匹配、{@code flowStatus} / {@code businessId} 等值匹配，固定按创建时间倒序。
     * </p>
     *
     * @param param 分页与过滤参数
     * @return Result&lt;Paging&lt;WorkflowInstanceAO&gt;&gt;
     */
    @PreAuthorize("hasPermission('workflow:instance', 'view')")
    @GetMapping("/page")
    @Operation(summary = "我发起的流程实例分页")
    public Result<Paging<WorkflowInstanceAO>> page(@Validated @ModelAttribute ApiWorkflowInstancePageParam param) {
        return Result.success(WorkflowPagingConvert.convert(workflowInstanceService.myPage(param),
            WorkflowAoConvert.INSTANCE::toInstanceAOList));
    }

    @PreAuthorize("hasPermission('workflow:instance', 'view')")
    @GetMapping("/{instanceId}/his-task")
    @Operation(summary = "审批记录（某实例的全部历史任务）")
    public Result<List<WorkflowHisTaskAO>> listHisTask(@PathVariable("instanceId") Long instanceId) {
        return Result.success(WorkflowAoConvert.INSTANCE.toHisTaskAOList(
            workflowInstanceService.listHisTask(instanceId)));
    }

    @PreAuthorize("hasPermission('workflow:instance', 'view')")
    @GetMapping("/{instanceId}/any-node")
    @Operation(summary = "可选跳转节点（自由跳转 / 驳回到指定节点）")
    public Result<List<WorkflowNodeAO>> listAnyNode(@PathVariable("instanceId") Long instanceId) {
        return Result.success(WorkflowAoConvert.INSTANCE.toNodeAOList(
            workflowInstanceService.listAnyNode(instanceId)));
    }

    @Log(
        title = "流程实例",
        logType = "WF_INSTANCE_REVOKE",
        businessType = BusinessType.FORCE,
        operation = "'撤回流程: ' + #param.instanceId",
        targetType = "WF_INSTANCE",
        targetId = "#param.instanceId"
    )
    @PreAuthorize("hasPermission('workflow:instance', 'revoke')")
    @PostMapping("/revoke")
    @Operation(summary = "撤回流程（发起人撤销申请）")
    public Result<WorkflowInstanceAO> revoke(@Validated @RequestBody ApiWorkflowInstanceActionParam param) {
        return Result.success(WorkflowAoConvert.INSTANCE.toInstanceAO(workflowInstanceService.revoke(param)));
    }

    @Log(
        title = "流程实例",
        logType = "WF_INSTANCE_TERMINATE",
        businessType = BusinessType.FORCE,
        operation = "'终止流程: ' + #param.instanceId",
        targetType = "WF_INSTANCE",
        targetId = "#param.instanceId"
    )
    @PreAuthorize("hasPermission('workflow:instance', 'terminate')")
    @PostMapping("/terminate")
    @Operation(summary = "终止流程")
    public Result<WorkflowInstanceAO> terminate(@Validated @RequestBody ApiWorkflowInstanceActionParam param) {
        return Result.success(WorkflowAoConvert.INSTANCE.toInstanceAO(workflowInstanceService.terminate(param)));
    }

    /**
     * 拿回流程：发起人收回自己上一次已办结的步骤
     * <p>
     * 与审批动作里的「拿回」按任务 id 定位不同，这里按<b>流程实例</b>定位（引擎的
     * {@code taskBackByInsId}），用于「我发起的」列表上的操作。
     * </p>
     *
     * @param param 实例级动作参数
     * @return Result&lt;WorkflowInstanceAO&gt;
     */
    @Log(
        title = "流程实例",
        logType = "WF_INSTANCE_TAKE_BACK",
        businessType = BusinessType.UPDATE,
        operation = "'拿回流程: ' + #param.instanceId",
        targetType = "WF_INSTANCE",
        targetId = "#param.instanceId"
    )
    @PreAuthorize("hasPermission('workflow:instance', 'takeBack')")
    @PostMapping("/take-back")
    @Operation(summary = "拿回流程（按实例收回已办结的步骤）")
    public Result<WorkflowInstanceAO> takeBack(@Validated @RequestBody ApiWorkflowInstanceActionParam param) {
        return Result.success(WorkflowAoConvert.INSTANCE.toInstanceAO(workflowInstanceService.takeBack(param)));
    }

    @Log(
        title = "流程实例",
        logType = "WF_INSTANCE_SUSPEND",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'挂起流程实例: ' + #instanceId",
        targetType = "WF_INSTANCE",
        targetId = "#instanceId"
    )
    @PreAuthorize("hasPermission('workflow:instance', 'suspend')")
    @PostMapping("/{instanceId}/suspend")
    @Operation(summary = "挂起流程实例")
    public Result<Boolean> suspend(@PathVariable("instanceId") Long instanceId) {
        return Result.success(workflowInstanceService.suspend(instanceId));
    }

    @Log(
        title = "流程实例",
        logType = "WF_INSTANCE_ACTIVATE",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'激活流程实例: ' + #instanceId",
        targetType = "WF_INSTANCE",
        targetId = "#instanceId"
    )
    @PreAuthorize("hasPermission('workflow:instance', 'activate')")
    @PostMapping("/{instanceId}/activate")
    @Operation(summary = "激活流程实例")
    public Result<Boolean> activate(@PathVariable("instanceId") Long instanceId) {
        return Result.success(workflowInstanceService.activate(instanceId));
    }

}
