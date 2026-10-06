package com.carlos.workflow.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.workflow.api.ApiWorkflowDefinition;
import com.carlos.workflow.api.pojo.ao.WorkflowDefinitionAO;
import com.carlos.workflow.convert.WorkflowAoConvert;
import com.carlos.workflow.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 流程定义 Feign 契约实现（服务间调用，{@code /api/workflow/definition/**}）。
 * </p>
 * <p>
 * 网关**不路由** {@code /api/**}，因此这里的端点只面向其他业务服务的 Feign 调用，
 * 不面向前端。管理端（前端经网关）的端点在同包外另有一套，见
 * {@code com.carlos.workflow.controller.WorkflowDefinitionController}（{@code /workflow/definition/**}）。
 * </p>
 * <p>
 * 设计器自身的 {@code /warm-flow/**} 数据接口保持引擎原样，不在本类内包装。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@RestController
@RequestMapping("/api/workflow/definition")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "carlos.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Tag(name = "工作流-流程定义（服务间）")
public class ApiWorkflowDefinitionImpl implements ApiWorkflowDefinition {

    private final WorkflowDefinitionService workflowDefinitionService;

    @Override
    @GetMapping("/published")
    @Operation(summary = "已发布流程定义列表")
    public Result<List<WorkflowDefinitionAO>> listPublished() {
        return Result.success(
            WorkflowAoConvert.INSTANCE.toDefinitionAOList(workflowDefinitionService.listPublished()));
    }

}
