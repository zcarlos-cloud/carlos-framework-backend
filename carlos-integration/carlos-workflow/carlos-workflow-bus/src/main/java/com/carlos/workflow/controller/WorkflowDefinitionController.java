package com.carlos.workflow.controller;

import com.carlos.boot.web.annotation.NoWrap;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.workflow.api.pojo.ao.WorkflowDefinitionAO;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionBasicParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionCopyParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionIdsParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionPageParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionSaveParam;
import com.carlos.workflow.convert.WorkflowAoConvert;
import com.carlos.workflow.convert.WorkflowPagingConvert;
import com.carlos.workflow.dto.WorkflowDefinitionExportDTO;
import com.carlos.workflow.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * <p>
 * 流程定义管理端接口：<b>给前端用的</b>，经网关暴露，统一前缀 {@code /workflow/definition/**}。
 * </p>
 * <p>
 * 与 Feign 契约（{@code /api/workflow/definition/**}）是<b>两套命名空间，别混</b>：
 * 网关只路由 {@code /<服务>/**}，解析后原样透传业务路径，而 {@code /api/**} 根本不在网关路由表内
 * （只供服务间 Feign 调用）。把管理端接口挂到 {@code /api/**} 上等于让内部命名空间对外可见，
 * 因此本类<b>不 implements 任何 {@code com.carlos.workflow.api} 下的接口</b>，
 * 服务间契约只保留 {@code ApiWorkflowDefinition#listPublished}。
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
@RequestMapping("/workflow/definition")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "carlos.workflow", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Tag(name = "工作流-流程定义")
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService workflowDefinitionService;

    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_SAVE",
        businessType = BusinessType.INSERT,
        operation = "'保存流程定义: ' + #param.definitionId",
        targetType = "WF_DEFINITION",
        targetId = "#param.definitionId"
    )
    @PreAuthorize("hasPermission('workflow:definition', 'create')")
    @PostMapping
    @Operation(summary = "保存流程定义 JSON（可选同时发布）")
    public Result<WorkflowDefinitionAO> save(@Validated @RequestBody ApiWorkflowDefinitionSaveParam param) {
        return Result.success(WorkflowAoConvert.INSTANCE.toDefinitionAO(workflowDefinitionService.save(param)));
    }

    /**
     * 按基础信息创建 / 更新流程定义
     * <p>
     * 「先填基本信息、再进设计器画节点」的第一步：只写流程定义主记录，<b>不创建节点</b>。
     * {@code definitionId} 为空走新建、非空走更新，这是与前端约定的同一入口两种语义，
     * 因此这里按 {@code definitionId} 是否为空分派；两条分支的前置校验（必填项、模型取值、
     * 定义是否存在）都在 Service 层。
     * </p>
     *
     * @param param 基础信息参数
     * @return Result&lt;WorkflowDefinitionAO&gt;
     */
    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_SAVE_BASIC",
        businessType = BusinessType.INSERT,
        operation = "'创建或更新流程定义: ' + #param.flowCode",
        targetType = "WF_DEFINITION",
        targetId = "#param.definitionId"
    )
    @PreAuthorize("hasPermission('workflow:definition', 'create')")
    @PostMapping("/basic")
    @Operation(summary = "按基础信息创建或更新流程定义（不创建节点）")
    public Result<WorkflowDefinitionAO> saveBasic(@Validated @RequestBody ApiWorkflowDefinitionBasicParam param) {
        if (param != null && param.getDefinitionId() != null) {
            return Result.success(
                WorkflowAoConvert.INSTANCE.toDefinitionAO(workflowDefinitionService.updateBasic(param)));
        }
        return Result.success(
            WorkflowAoConvert.INSTANCE.toDefinitionAO(workflowDefinitionService.createBasic(param)));
    }

    @PreAuthorize("hasPermission('workflow:definition', 'view')")
    @GetMapping("/page")
    @Operation(summary = "分页查询流程定义")
    public Result<Paging<WorkflowDefinitionAO>> page(@Validated @ModelAttribute ApiWorkflowDefinitionPageParam param) {
        return Result.success(WorkflowPagingConvert.convert(workflowDefinitionService.page(param),
            WorkflowAoConvert.INSTANCE::toDefinitionAOList));
    }

    @PreAuthorize("hasPermission('workflow:definition', 'view')")
    @GetMapping("/{id}")
    @Operation(summary = "流程定义详情")
    public Result<WorkflowDefinitionAO> getById(@PathVariable("id") Long id) {
        return Result.success(WorkflowAoConvert.INSTANCE.toDefinitionAO(workflowDefinitionService.detail(id)));
    }

    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_PUBLISH",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'发布流程定义: ' + #param.ids",
        targetType = "WF_DEFINITION",
        targetId = "#param.ids"
    )
    @PreAuthorize("hasPermission('workflow:definition', 'publish')")
    @PostMapping("/publish")
    @Operation(summary = "发布流程定义")
    public Result<Boolean> publish(@Validated @RequestBody ApiWorkflowDefinitionIdsParam param) {
        return Result.success(workflowDefinitionService.publish(param == null ? null : param.getIds()));
    }

    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_UNPUBLISH",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'撤销发布流程定义: ' + #param.ids",
        targetType = "WF_DEFINITION",
        targetId = "#param.ids"
    )
    @PreAuthorize("hasPermission('workflow:definition', 'unpublish')")
    @PostMapping("/unpublish")
    @Operation(summary = "撤销发布流程定义")
    public Result<Boolean> unpublish(@Validated @RequestBody ApiWorkflowDefinitionIdsParam param) {
        return Result.success(workflowDefinitionService.unpublish(param == null ? null : param.getIds()));
    }

    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_COPY",
        businessType = BusinessType.INSERT,
        operation = "'复制流程定义: ' + #param.id",
        targetType = "WF_DEFINITION",
        targetId = "#param.id"
    )
    @PreAuthorize("hasPermission('workflow:definition', 'copy')")
    @PostMapping("/copy")
    @Operation(summary = "复制流程定义")
    public Result<Long> copy(@Validated @RequestBody ApiWorkflowDefinitionCopyParam param) {
        return Result.success(workflowDefinitionService.copy(param == null ? null : param.getId()));
    }

    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除流程定义: ' + #param.ids",
        targetType = "WF_DEFINITION",
        targetId = "#param.ids"
    )
    @PreAuthorize("hasPermission('workflow:definition', 'delete')")
    @DeleteMapping
    @Operation(summary = "删除流程定义")
    public Result<Boolean> remove(@Validated @RequestBody ApiWorkflowDefinitionIdsParam param) {
        return Result.success(workflowDefinitionService.remove(param == null ? null : param.getIds()));
    }

    // 入参是 MultipartFile：显式关掉请求参数落库，避免把整个文件字节序列化进审计库
    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_IMPORT",
        businessType = BusinessType.IMPORT,
        operation = "'导入流程定义: ' + #file.originalFilename",
        targetType = "WF_DEFINITION",
        saveRequestParams = false
    )
    @PreAuthorize("hasPermission('workflow:definition', 'import')")
    @PostMapping("/import")
    @Operation(summary = "导入流程定义 JSON 文件")
    public Result<WorkflowDefinitionAO> importDefinition(@RequestParam("file") MultipartFile file) {
        return Result.success(
            WorkflowAoConvert.INSTANCE.toDefinitionAO(workflowDefinitionService.importDefinition(file)));
    }

    /**
     * 导出流程定义 JSON 文件（<b>下载，不是 JSON 信封</b>）
     * <p>
     * ⚠️ 本方法必须带 {@link NoWrap}：{@code ResponseWrapAdvice#beforeBodyWrite} 对<b>任何</b>
     * 非 {@code Result} 的返回值都会包一层 {@code Result.success(body)}，{@code ResponseEntity}
     * 的字节数组会被塞进 {@code data} 字段，下载下来的文件就不是流程定义 JSON 了。
     * 标注 {@code @NoWrap} 后 advice 在 {@code supports()} 阶段整个跳过，字节原样写出。
     * </p>
     * <p>
     * 文件名按「流程编码_版本.json」给出：同一次导出多次下载不会互相覆盖，也能一眼看出是哪个版本。
     * </p>
     *
     * @param id 流程定义id
     * @return 直接下载的 JSON 文件
     */
    // 导出属「数据外带」，虽走 GET 仍要留痕；返回的是文件流，故不保存响应数据
    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_EXPORT",
        businessType = BusinessType.EXPORT,
        operation = "'导出流程定义: ' + #id",
        targetType = "WF_DEFINITION",
        targetId = "#id",
        riskLevel = 30,
        saveResponseData = false
    )
    @NoWrap
    @PreAuthorize("hasPermission('workflow:definition', 'export')")
    @GetMapping("/{id}/export")
    @Operation(summary = "导出流程定义 JSON 文件（直接下载，不经统一响应包装）")
    public ResponseEntity<byte[]> exportDefinition(@PathVariable("id") Long id) {
        WorkflowDefinitionExportDTO export = workflowDefinitionService.exportDefinition(id);
        byte[] body = export.getContent().getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + export.getFlowCode() + "_" + export.getVersion() + ".json\"");
        return ResponseEntity.ok()
            .headers(headers)
            // 带 charset：流程编码 / 版本号是 ASCII，但正文里的流程名称是中文，不带 charset
            // 会让部分客户端按 ISO-8859-1 解码，下载下来是乱码
            .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
            .body(body);
    }

    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_ACTIVATE",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'激活流程定义: ' + #id",
        targetType = "WF_DEFINITION",
        targetId = "#id"
    )
    @PreAuthorize("hasPermission('workflow:definition', 'activate')")
    @PostMapping("/{id}/activate")
    @Operation(summary = "激活流程定义")
    public Result<Boolean> activate(@PathVariable("id") Long id) {
        return Result.success(workflowDefinitionService.activate(id));
    }

    @Log(
        title = "流程定义",
        logType = "WF_DEFINITION_SUSPEND",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'挂起流程定义: ' + #id",
        targetType = "WF_DEFINITION",
        targetId = "#id"
    )
    @PreAuthorize("hasPermission('workflow:definition', 'suspend')")
    @PostMapping("/{id}/suspend")
    @Operation(summary = "挂起流程定义")
    public Result<Boolean> suspend(@PathVariable("id") Long id) {
        return Result.success(workflowDefinitionService.suspend(id));
    }

}
