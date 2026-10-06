package com.carlos.workflow.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.workflow.convert.WorkflowDtoConvert;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionBasicParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionPageParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowDefinitionSaveParam;
import com.carlos.workflow.dto.WorkflowDefinitionDTO;
import com.carlos.workflow.dto.WorkflowDefinitionExportDTO;
import com.carlos.workflow.enums.WorkflowErrorCode;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.handler.WorkflowAssigneeValidator;
import com.carlos.workflow.manager.WorkflowDefinitionManager;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.enums.ModelEnum;
import org.dromara.warm.flow.core.enums.PublishStatus;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * 流程定义服务，面向业务侧提供「可选流程清单」「保存/发布定义」，以及管理端列表页的
 * 分页 / 详情 / 发布 / 撤销发布 / 复制 / 删除 / 导入 / 导出 / 挂起 / 激活能力。
 * </p>
 * <p>
 * 数据读取（分页、按 id、按编码）走 {@link WorkflowDefinitionManager} 的 MyBatis-Plus 条件构造器，
 * 不在本层编写 SQL；发布 / 撤销 / 复制 / 删除这类带引擎语义的动作交给 Warm-Flow 的
 * {@code DefService}，本层只负责前置校验与引擎异常到框架错误码的翻译。
 * </p>
 * <p>
 * 查询已发布定义时直接以引擎实体作为 MyBatis-Plus 查询条件；
 * 引擎侧尚无「查询已发布定义列表」的专用方法，其 {@code DefService#list} 即为此用途。
 * </p>
 * <p>
 * 保存前会对办理人表达式做一次校验，但<b>必须清楚它的边界</b>：设计器 SPA 直接调引擎自带的
 * {@code POST /warm-flow/save-json}，不经过本服务；本校验只是对调用 {@code POST /api/workflow/definition}
 * 的一方（含服务间 Feign 调用）做前置提示。真正的控制点是运行期的
 * {@link com.carlos.workflow.handler.WorkflowAssigneeFailFastListener}。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class WorkflowDefinitionService {

    private final DefService definitionService;

    private final WorkflowDefinitionManager definitionManager;

    /**
     * 已发布的流程定义列表，供发起流程时选择
     *
     * @return List&lt;WorkflowDefinitionDTO&gt;
     */
    public List<WorkflowDefinitionDTO> listPublished() {
        Definition query = new FlowDefinition().setIsPublish(PublishStatus.PUBLISHED.getKey());
        return WorkflowDtoConvert.toDefinitionDTOList(definitionService.list(query));
    }

    /**
     * 分页查询流程定义（管理端列表页）
     * <p>
     * 过滤条件与排序全部在 Manager 层用 MyBatis-Plus 条件构造器表达：{@code flowName} 模糊匹配，
     * {@code flowCode} / {@code category} / {@code isPublish} 等值匹配，固定按创建时间倒序。
     * </p>
     *
     * @param param 分页与过滤参数
     * @return Paging&lt;WorkflowDefinitionDTO&gt;
     */
    public Paging<WorkflowDefinitionDTO> page(ApiWorkflowDefinitionPageParam param) {
        return definitionManager.getPage(param);
    }

    /**
     * 流程定义详情
     *
     * @param definitionId 流程定义id
     * @return WorkflowDefinitionDTO
     */
    public WorkflowDefinitionDTO detail(Long definitionId) {
        if (definitionId == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程定义id不能为空");
        }
        WorkflowDefinitionDTO dto = definitionManager.getDtoById(definitionId);
        if (dto == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_DEFINITION_NOT_FOUND);
        }
        return dto;
    }

    /**
     * 发布流程定义（批量）
     * <p>
     * 引擎的 {@code DefService#publish} 会校验流程是否有节点、并把同编码下的旧版本置为「已失效」，
     * 因此这里逐条发布而不是批量改状态；批量里任意一条失败都要整体回滚，否则会留下
     * 「一半已发布、一半还是草稿」的中间态。
     * </p>
     *
     * @param definitionIds 流程定义id集合
     * @return 是否全部发布成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean publish(List<Long> definitionIds) {
        for (Long definitionId : requireExisting(definitionIds)) {
            publish(definitionId);
        }
        return true;
    }

    /**
     * 撤销发布流程定义（批量）
     * <p>
     * 引擎的 {@code DefService#unPublish} 在定义已产生流程实例时会拒绝，此时统一翻译成
     * {@code 20704 当前操作不允许}。
     * </p>
     *
     * @param definitionIds 流程定义id集合
     * @return 是否全部撤销成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean unpublish(List<Long> definitionIds) {
        for (Long definitionId : requireExisting(definitionIds)) {
            boolean unpublished;
            try {
                unpublished = definitionService.unPublish(definitionId);
            } catch (FlowException e) {
                throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
            }
            if (!unpublished) {
                throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, "流程定义撤销发布失败");
            }
        }
        return true;
    }

    /**
     * 复制流程定义
     * <p>
     * 复制由引擎完成（版本号递增、节点与跳转线一并复制），但引擎的 {@code DefService#copyDef}
     * 只返回布尔值，拿不到新定义 id。这里以「复制前后同流程编码的定义 id 差集」定位新定义，
     * 不依赖主键单调性，也就不依赖雪花 ID 的时钟特性。
     * </p>
     *
     * @param definitionId 被复制的流程定义id
     * @return 新流程定义id
     */
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long definitionId) {
        WorkflowDefinitionDTO source = detail(definitionId);
        Set<Long> existingIds = new HashSet<>(
            definitionManager.listByFlowCode(source.getFlowCode()).stream()
                .map(WorkflowDefinitionDTO::getDefinitionId)
                .toList());

        boolean copied;
        try {
            copied = definitionService.copyDef(definitionId);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        if (!copied) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, "流程定义复制失败");
        }

        return definitionManager.listByFlowCode(source.getFlowCode()).stream()
            .map(WorkflowDefinitionDTO::getDefinitionId)
            .filter(id -> !existingIds.contains(id))
            .max(Long::compareTo)
            .orElseThrow(() -> new WorkflowException(CommonErrorCode.WORKFLOW_DEFINITION_NOT_FOUND,
                "流程定义复制已提交，但未能定位新定义id"));
    }

    /**
     * 删除流程定义（批量）
     * <p>
     * 引擎的 {@code DefService#removeDef} 会连同节点、跳转线一起删除，并在定义已产生流程实例时拒绝，
     * 此时统一翻译成 {@code 20704 当前操作不允许}。
     * </p>
     *
     * @param definitionIds 流程定义id集合
     * @return 是否全部删除成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(List<Long> definitionIds) {
        List<Long> targets = requireExisting(definitionIds);
        boolean removed;
        try {
            removed = definitionService.removeDef(targets);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        if (!removed) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, "流程定义删除失败");
        }
        return true;
    }

    /**
     * 从上传的 JSON 文件导入流程定义（含节点与跳转线）
     * <p>
     * 与 {@link #save(ApiWorkflowDefinitionSaveParam)} 的差别只在于入口：设计器走 JSON 字符串，
     * 这里走上传文件。两者最终都落到引擎的 {@code DefService#importJson}，因此版本号递增、
     * 节点与跳转线一并写入的语义完全一致。
     * </p>
     * <p>
     * <b>两级错误码的分工</b>：文件为空、或内容根本不是能解析成 {@code DefJson} 的 JSON
     * （用引擎自己的 {@code JsonConvert} 判定，不另写一套 JSON 校验）属于调用方给错了东西，
     * 报 {@code 10701}；能解析但引擎认为结构不合格（缺流程编码、没有节点等）才是
     * {@code 10703 流程定义 JSON 无效}。这样前端能区分「传错文件」与「文件内容有问题」。
     * </p>
     *
     * @param file 上传的 JSON 文件
     * @return WorkflowDefinitionDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionDTO importDefinition(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("导入文件不能为空");
        }
        String content;
        try {
            content = IoUtil.readUtf8(file.getInputStream());
        } catch (IOException e) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("导入文件读取失败");
        }
        if (StrUtil.isBlank(content)) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("导入文件内容不能为空");
        }
        requireJsonDefinition(content);

        Definition imported;
        try {
            // 引擎的 importIs 只接受 InputStream，这里把已校验过的内容重新包一个流交给它，
            // 保证「校验的那份」与「导入的那份」逐字一致
            imported = definitionService.importIs(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (FlowException e) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_DEFINITION_JSON_INVALID, e.getMessage());
        }
        if (imported == null) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_DEFINITION_JSON_INVALID);
        }
        Definition latest = imported.getId() == null ? null : definitionService.getById(imported.getId());
        return WorkflowDtoConvert.toDefinitionDTO(latest == null ? imported : latest);
    }

    /**
     * 导出流程定义 JSON（含节点与跳转线）
     * <p>
     * 正文由引擎生成，本层只负责两件事：确认定义存在（引擎的 {@code exportJson} 在定义不存在时
     * 会抛 {@code NullPointerException}，翻译不出可读错误码），以及把 {@code flowCode} /
     * {@code version} 一并带出去给 Controller 拼下载文件名。
     * </p>
     *
     * @param definitionId 流程定义id
     * @return WorkflowDefinitionExportDTO
     */
    public WorkflowDefinitionExportDTO exportDefinition(Long definitionId) {
        WorkflowDefinitionDTO definition = detail(definitionId);
        String content;
        try {
            content = definitionService.exportJson(definitionId);
        } catch (FlowException e) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_ENGINE_ERROR, e.getMessage());
        }
        if (StrUtil.isBlank(content)) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_ENGINE_ERROR, "流程定义导出内容为空");
        }
        WorkflowDefinitionExportDTO export = new WorkflowDefinitionExportDTO();
        export.setDefinitionId(definition.getDefinitionId());
        export.setFlowCode(definition.getFlowCode());
        export.setVersion(definition.getVersion());
        export.setContent(content);
        return export;
    }

    /**
     * 激活流程定义：激活后，该定义下的流程实例才能继续流转
     * <p>
     * 引擎对「已经是激活状态」的定义会拒绝（抛 {@code FlowException}），这里统一翻译成
     * {@code 20704 当前操作不允许}，与发布 / 撤销发布 / 删除的口径一致。
     * </p>
     *
     * @param definitionId 流程定义id
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean activate(Long definitionId) {
        requireExisting(definitionId);
        boolean activated;
        try {
            activated = definitionService.active(definitionId);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        if (!activated) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, "流程定义激活失败");
        }
        return true;
    }

    /**
     * 挂起流程定义：挂起后，该定义下的流程实例都无法继续流转
     * <p>
     * 与 {@link #activate(Long)} 对称，引擎对「已经挂起」的定义同样会拒绝，翻译口径一致。
     * </p>
     *
     * @param definitionId 流程定义id
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean suspend(Long definitionId) {
        requireExisting(definitionId);
        boolean suspended;
        try {
            suspended = definitionService.unActive(definitionId);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        if (!suspended) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, "流程定义挂起失败");
        }
        return true;
    }

    /**
     * 校验内容能解析成引擎的 {@code DefJson}
     *
     * @param content 上传文件的内容
     */
    private void requireJsonDefinition(String content) {
        DefJson parsed;
        try {
            parsed = FlowEngine.jsonConvert.strToBean(content, DefJson.class);
        } catch (FlowException e) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("导入文件不是合法的 JSON");
        }
        if (parsed == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("导入文件不是合法的 JSON");
        }
    }

    /**
     * 批量操作的公共前置：入参非空且每个 id 都必须能查到定义
     *
     * @param definitionIds 流程定义id集合
     * @return 校验通过的入参
     */
    private List<Long> requireExisting(List<Long> definitionIds) {
        if (definitionIds == null || definitionIds.isEmpty()) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程定义id不能为空");
        }
        for (Long definitionId : definitionIds) {
            requireExisting(definitionId);
        }
        return definitionIds;
    }

    /**
     * 单个操作的公共前置：id 非空且能查到定义
     *
     * @param definitionId 流程定义id
     */
    private void requireExisting(Long definitionId) {
        if (definitionId == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程定义id不能为空");
        }
        if (definitionManager.getDtoById(definitionId) == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_DEFINITION_NOT_FOUND);
        }
    }

    /**
     * 保存流程定义 JSON，可选同时发布
     * <p>
     * {@code definitionId} 为空表示新增（走引擎的 JSON 导入），否则表示更新（走引擎的设计器保存）；
     * {@code onlyNodeSkip} 只对更新生效，语义与引擎设计器的同名开关一致。
     * </p>
     * <p>
     * ⚠️ 保存前会做一次办理人表达式校验（{@link WorkflowAssigneeValidator}），但它<b>不是硬闸门</b>：
     * 设计器 SPA 走的是引擎自带的 {@code POST /warm-flow/save-json}，不经过本方法；
     * 运行期真正兜住「配了办理人却解析不到人」的是
     * {@link com.carlos.workflow.handler.WorkflowAssigneeFailFastListener}。
     * </p>
     *
     * @param param 保存参数
     * @return WorkflowDefinitionDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionDTO save(ApiWorkflowDefinitionSaveParam param) {
        if (param == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程定义保存参数不能为空");
        }
        if (StrUtil.isBlank(param.getDefJson())) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程定义JSON不能为空");
        }
        DefJson defJson = parse(param.getDefJson());
        WorkflowAssigneeValidator.validate(defJson);
        if (param.getDefinitionId() == null) {
            return create(param);
        }
        return update(param, defJson);
    }

    /**
     * 按基础信息新增流程定义（不创建节点）
     * <p>
     * 走引擎的 {@code DefService#checkAndSave}：引擎补上新 version 后只写 {@code flow_definition}
     * 主记录，节点与跳转线由后续的设计器保存（{@link #save}）写入。因此这里返回的定义「还没有流程图」，
     * 此时发布会被引擎以 20704 拒绝，属预期行为，不是缺陷。
     * </p>
     *
     * @param param 基础信息参数，{@code definitionId} 必须为空
     * @return WorkflowDefinitionDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionDTO createBasic(ApiWorkflowDefinitionBasicParam param) {
        if (param == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程定义基础信息不能为空");
        }
        if (StrUtil.isBlank(param.getFlowCode())) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程编码不能为空");
        }
        if (StrUtil.isBlank(param.getFlowName())) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程名称不能为空");
        }

        Definition definition = new FlowDefinition();
        definition.setFlowCode(StrUtil.trim(param.getFlowCode()));
        definition.setFlowName(StrUtil.trim(param.getFlowName()));
        definition.setModelValue(resolveModelValue(param.getModelValue()));
        definition.setCategory(param.getCategory());

        boolean saved;
        try {
            saved = definitionService.checkAndSave(definition);
        } catch (FlowException e) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_ENGINE_ERROR, e.getMessage());
        }
        if (!saved) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_ENGINE_ERROR, "流程定义保存失败");
        }

        // 引擎在 save 前用 dataFillHandler 补 id，这里才拿得到新定义的主键；回查一次是为了带上
        // 库里的默认值（create_time / is_publish / activity_status），与 save 的新增分支口径一致
        Definition latest = definition.getId() == null ? null : definitionService.getById(definition.getId());
        return WorkflowDtoConvert.toDefinitionDTO(latest == null ? definition : latest);
    }

    /**
     * 按基础信息更新流程定义（不触碰节点与跳转线）
     * <p>
     * 走引擎的 {@code DefService#updateById}，只改 {@code flow_definition} 主记录：
     * {@code flowCode} / {@code flowName} / {@code category} 允许修改，其中前两者留空表示「保持原值」
     * （主记录这两列非空，用空串覆盖没有意义），{@code category} 允许置空。
     * </p>
     * <p>
     * <b>{@code modelValue} 与库中不一致时直接拒绝（10701）</b>：经典模型与仿钉钉模型的节点结构不同，
     * 放开修改会静默毁掉已有节点的布局（引擎设计器自身也把该值当成只读）。
     * </p>
     *
     * @param param 基础信息参数，{@code definitionId} 必须非空且定义存在
     * @return WorkflowDefinitionDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinitionDTO updateBasic(ApiWorkflowDefinitionBasicParam param) {
        if (param == null || param.getDefinitionId() == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程定义id不能为空");
        }
        WorkflowDefinitionDTO existing = definitionManager.getDtoById(param.getDefinitionId());
        if (existing == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_DEFINITION_NOT_FOUND);
        }

        Definition definition = new FlowDefinition();
        definition.setId(param.getDefinitionId());
        definition.setFlowCode(StrUtil.blankToDefault(StrUtil.trim(param.getFlowCode()), existing.getFlowCode()));
        definition.setFlowName(StrUtil.blankToDefault(StrUtil.trim(param.getFlowName()), existing.getFlowName()));
        definition.setCategory(param.getCategory() == null ? existing.getCategory() : param.getCategory());
        definition.setModelValue(requireUnchangedModelValue(param.getModelValue(), existing.getModelValue()));

        boolean updated;
        try {
            updated = definitionService.updateById(definition);
        } catch (FlowException e) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_ENGINE_ERROR, e.getMessage());
        }
        if (!updated) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_DEFINITION_NOT_FOUND);
        }

        Definition latest = definitionService.getById(param.getDefinitionId());
        if (latest == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_DEFINITION_NOT_FOUND);
        }
        return WorkflowDtoConvert.toDefinitionDTO(latest);
    }

    private WorkflowDefinitionDTO create(ApiWorkflowDefinitionSaveParam param) {
        Definition created;
        try {
            created = definitionService.importJson(param.getDefJson());
        } catch (FlowException e) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_DEFINITION_JSON_INVALID, e.getMessage());
        }
        if (param.isPublish()) {
            publish(created.getId());
        }
        Definition latest = definitionService.getById(created.getId());
        return WorkflowDtoConvert.toDefinitionDTO(latest == null ? created : latest);
    }

    private WorkflowDefinitionDTO update(ApiWorkflowDefinitionSaveParam param, DefJson defJson) {
        defJson.setId(param.getDefinitionId());
        try {
            definitionService.saveDef(defJson, param.isOnlyNodeSkip());
        } catch (FlowException e) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_DEFINITION_JSON_INVALID, e.getMessage());
        } catch (Exception e) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_ENGINE_ERROR, e.getMessage());
        }
        if (param.isPublish()) {
            publish(param.getDefinitionId());
        }
        Definition latest = definitionService.getById(param.getDefinitionId());
        if (latest == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_DEFINITION_NOT_FOUND);
        }
        return WorkflowDtoConvert.toDefinitionDTO(latest);
    }

    private DefJson parse(String defJson) {
        DefJson parsed;
        try {
            parsed = FlowEngine.jsonConvert.strToBean(defJson, DefJson.class);
        } catch (FlowException e) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_DEFINITION_JSON_INVALID, e.getMessage());
        }
        if (parsed == null) {
            throw WorkflowErrorCode.WORKFLOW_DEFINITION_JSON_INVALID.workflowException();
        }
        return parsed;
    }

    private void publish(Long definitionId) {
        boolean published;
        try {
            published = definitionService.publish(definitionId);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        if (!published) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, "流程定义发布失败");
        }
    }

    /**
     * 设计器模型取值：留空按 {@code CLASSICS} 处理，非空只接受引擎 {@code ModelEnum} 的两个取值
     * <p>
     * 取值清单直接取自引擎枚举，不另抄一份常量，升级引擎时不会两边走散。
     * </p>
     *
     * @param modelValue 入参中的设计器模型
     * @return 归一化后的模型取值
     */
    private String resolveModelValue(String modelValue) {
        if (StrUtil.isBlank(modelValue)) {
            return ModelEnum.CLASSICS.name();
        }
        String normalized = StrUtil.trim(modelValue);
        for (ModelEnum supported : ModelEnum.values()) {
            if (supported.name().equals(normalized)) {
                return normalized;
            }
        }
        throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("设计器模型只支持 "
            + ModelEnum.CLASSICS.name() + " 或 " + ModelEnum.MIMIC.name() + "，非法取值：" + normalized);
    }

    /**
     * 更新时的设计器模型校验：留空表示保持原值，非空必须与库中一致
     *
     * @param modelValue       入参中的设计器模型
     * @param storedModelValue 库中当前的模型取值
     * @return 写回更新的模型取值
     */
    private String requireUnchangedModelValue(String modelValue, String storedModelValue) {
        if (StrUtil.isBlank(modelValue)) {
            return storedModelValue;
        }
        String normalized = resolveModelValue(modelValue);
        if (!normalized.equals(storedModelValue)) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException(
                "设计器模型保存后不可修改（经典与仿钉钉模型的节点结构不同）：库中为 " + storedModelValue
                    + "，请求为 " + normalized);
        }
        return storedModelValue;
    }

}
