package com.carlos.workflow.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstanceActionParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowInstancePageParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowStartParam;
import com.carlos.workflow.convert.WorkflowDtoConvert;
import com.carlos.workflow.dto.WorkflowChartDTO;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowInstanceDTO;
import com.carlos.workflow.dto.WorkflowNodeDTO;
import com.carlos.workflow.enums.WorkflowErrorCode;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowInstanceManager;
import com.carlos.workflow.manager.WorkflowOrgUserManager;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.FlowEngine;
import org.dromara.warm.flow.core.dto.DefJson;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.Definition;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Node;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.service.ChartService;
import org.dromara.warm.flow.core.service.DefService;
import org.dromara.warm.flow.core.service.HisTaskService;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.NodeService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 流程实例服务：「我发起的」分页、发起流程、流程图、审批记录、可选跳转节点，
 * 以及实例级动作（撤回 / 终止 / 挂起 / 激活 / 拿回）。
 * </p>
 * <p>
 * 发起前先按流程编码查一次已发布定义，把引擎内部的「流程定义不存在」断言提前为确定的 20701；
 * 调用过程中引擎抛出的 {@code FlowException} 统一映射为 20704。
 * </p>
 * <p>
 * <b>为什么所有实例级动作都先查一次实例：</b>引擎的 {@code revoke} / {@code terminationByInsId} /
 * {@code active} / {@code unActive} 都是「先解引用再判空」（例如 {@code revoke} 会先取
 * {@code instance.getVariableMap()}），实例不存在时抛的是 {@code NullPointerException} 而不是
 * {@code FlowException}，既翻译不成 20702，也没有任何可读信息。因此本层把存在性校验提前。
 * </p>
 * <p>
 * <b>依赖注入说明：</b>本类由 {@code WorkflowAutoConfiguration} 以 {@code @Bean} 注册
 * （构造器由该配置类给出，config 包不在本批次改动范围内），新增依赖用字段注入补充。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class WorkflowInstanceService {

    private final InsService instanceService;

    private final DefService definitionService;

    private final ChartService chartService;

    /**
     * 实例级流转（撤回 / 终止）
     * <p>
     * 与其它新增依赖一样是可选注入：本类由 {@code WorkflowAutoConfiguration} 注册，而针对该配置的
     * 单元测试只装配自动配置本身（{@code ApplicationContextRunner}，不扫描组件），
     * 必需注入会让那些上下文直接启动失败。
     * </p>
     */
    @Autowired(required = false)
    private TaskService taskService;

    /**
     * 审批记录（某实例的全部历史任务）
     */
    @Autowired(required = false)
    private HisTaskService hisTaskService;

    /**
     * 节点结构（可选跳转节点的计算依据）
     */
    @Autowired(required = false)
    private NodeService nodeService;

    /**
     * 名称批量解析（审批记录里的审批人 / 协作人）
     */
    @Autowired(required = false)
    private WorkflowOrgUserManager orgUserManager;

    /**
     * 「我发起的」分页查询（{@code flow_instance} + {@code flow_definition} 的字段补齐）
     * <p>
     * 与其它 Manager 依赖一样是可选注入：本类由 {@code WorkflowAutoConfiguration} 注册，
     * 而针对该配置的单元测试只装配自动配置本身（{@code ApplicationContextRunner}，不扫描组件）。
     * </p>
     */
    @Autowired(required = false)
    private WorkflowInstanceManager instanceManager;

    /**
     * 当前登录用户，用于拿办理人唯一标识
     */
    @Autowired(required = false)
    private PermissionHandler permissionHandler;

    /**
     * 发起流程
     * <p>
     * 发起同样会写多张表（实例、首个待办任务、任务办理人、历史记录），引擎自身没有事务，
     * 因此与其它写方法一样开启事务。
     * </p>
     *
     * @param param 发起参数
     * @return WorkflowInstanceDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowInstanceDTO start(ApiWorkflowStartParam param) {
        if (param == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("发起参数不能为空");
        }
        if (StrUtil.isBlank(param.getFlowCode())) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程编码不能为空");
        }
        if (StrUtil.isBlank(param.getBusinessId())) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("业务ID不能为空");
        }
        Definition definition = definitionService.getPublishByFlowCode(param.getFlowCode());
        if (definition == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_DEFINITION_NOT_FOUND,
                "流程定义不存在或未发布：" + param.getFlowCode());
        }
        Map<String, Object> variable = param.getVariable() == null ? new HashMap<>() : param.getVariable();
        FlowParams flowParams = FlowParams.build()
            .flowCode(param.getFlowCode())
            .handler(param.getHandler())
            .message(param.getMessage())
            .variable(variable)
            .nextHandler(param.getNextHandlers() == null ? null : param.getNextHandlers().toArray(String[]::new));
        Instance instance;
        try {
            instance = instanceService.start(param.getBusinessId(), flowParams);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        return WorkflowDtoConvert.toInstanceDTO(instance);
    }

    /**
     * 「我发起的」分页：{@code flow_instance.create_by} 等于当前登录用户的实例
     * <p>
     * 发起人由服务端从 {@link PermissionHandler#getHandler()} 取，<b>不接受前端传入</b>：
     * 这是个「我的」列表，把用户标识开放给入参就等于允许越权查看他人的申请。
     * </p>
     * <p>
     * 过滤与展示字段补齐（流程名称来自 {@code flow_definition}）全部落在 Manager 层，
     * 本层只负责把当前用户交给它。
     * </p>
     *
     * @param param 分页与过滤参数
     * @return Paging&lt;WorkflowInstanceDTO&gt;
     */
    public Paging<WorkflowInstanceDTO> myPage(ApiWorkflowInstancePageParam param) {
        return requireInstanceManager().getMyPage(param, currentHandler());
    }

    /**
     * 流程图查询，节点自带办理状态，配合三原色完成着色。
     * <p>
     * 流程实例上的 {@code defJson} 由引擎在每次流转时写入并维护节点状态，这里只做解析与转换；
     * 引擎没有为 {@code JsonConvert} 注册 Bean，其自带设计器同样直接使用 {@code FlowEngine.jsonConvert}。
     * </p>
     *
     * @param instanceId 流程实例id
     * @return WorkflowChartDTO
     */
    public WorkflowChartDTO chart(Long instanceId) {
        Instance instance = requireInstance(instanceId);
        String defJsonStr = instance.getDefJson();
        if (StrUtil.isBlank(defJsonStr)) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("流程实例缺少流程图元数据");
        }
        DefJson defJson;
        try {
            defJson = FlowEngine.jsonConvert.strToBean(defJsonStr, DefJson.class);
        } catch (FlowException e) {
            throw new WorkflowException(WorkflowErrorCode.WORKFLOW_ENGINE_ERROR, e.getMessage());
        }
        WorkflowChartDTO dto = new WorkflowChartDTO();
        dto.setInstanceId(instance.getId());
        dto.setDefinitionId(instance.getDefinitionId());
        dto.setFlowCode(defJson.getFlowCode());
        dto.setFlowName(StrUtil.blankToDefault(defJson.getFlowName(), instance.getFlowName()));
        dto.setModelValue(defJson.getModelValue());
        dto.setChartStatusColor(chartService.getChartRgb(defJson.getModelValue()));
        dto.setNodes(WorkflowDtoConvert.toChartNodeDTOList(defJson.getNodeList()));
        dto.setSourceJson(defJsonStr);
        return dto;
    }

    /**
     * 审批记录：某流程实例的全部历史任务（不分页）
     *
     * <p>用引擎的链式查询 {@code hisTaskService.orderById().desc().list(entity)} 表达，
     * 不需要自定义 SQL；{@code approver} / {@code collaborator} 是用户 id，这里批量换成姓名
     * （一次远程调用，不按行回查）。</p>
     *
     * @param instanceId 流程实例id
     * @return List&lt;WorkflowHisTaskDTO&gt;，按历史任务id倒序（最近办理的在前）
     */
    public List<WorkflowHisTaskDTO> listHisTask(Long instanceId) {
        Instance instance = requireInstance(instanceId);
        List<HisTask> hisTasks = requireHisTaskService().orderById().desc()
            .list(new FlowHisTask().setInstanceId(instance.getId()));
        List<WorkflowHisTaskDTO> dtoList = WorkflowDtoConvert.toHisTaskDTOList(hisTasks);
        if (dtoList.isEmpty()) {
            return dtoList;
        }

        Set<String> userIds = new LinkedHashSet<>();
        for (WorkflowHisTaskDTO dto : dtoList) {
            if (StrUtil.isNotBlank(dto.getApprover())) {
                userIds.add(dto.getApprover().trim());
            }
            if (StrUtil.isNotBlank(dto.getCollaborator())) {
                for (String id : dto.getCollaborator().split(",")) {
                    if (StrUtil.isNotBlank(id)) {
                        userIds.add(id.trim());
                    }
                }
            }
        }
        Map<String, String> names = requireOrgUserManager().mapNames(userIds);
        for (WorkflowHisTaskDTO dto : dtoList) {
            dto.setApprover(resolve(dto.getApprover(), names));
            dto.setCollaborator(resolve(dto.getCollaborator(), names));
        }
        return dtoList;
    }

    /**
     * 可选跳转节点：自由跳转与「驳回到指定节点」共用一个候选清单
     *
     * <h2>为什么不照抄参考实现</h2>
     * <p>
     * 参考实现（hh-vue）取的是「开始节点的全部后继」{@code suffixNodeList(startNode.getId())}，
     * 列出的是整张图上的所有节点，包含当前节点、以及<b>已经走过且不可能退回</b>的节点，
     * 语义是错的。引擎 1.8.9 其实给得出更合适的两种能力：
     * </p>
     * <ul>
     *   <li>{@code NodeService#previousNodeList(definitionId, nodeCode)} —— 当前节点的全部<b>前置</b>
     *       节点（沿 PASS 跳转线反向可达），这才是「能退回到哪些节点」的结构依据；</li>
     *   <li>{@code HisTaskService#getByInsId(instanceId)} —— 本实例<b>实际走过</b>的节点，
     *       用来把结构上可达、但本实例没走到的节点（并行分支、未命中的互斥分支）剔掉。</li>
     * </ul>
     * <p>
     * 因此本实现的候选集合 = 「（前置节点 ∩ 已走过节点）」∪「当前节点的全部后继节点」，
     * 再排除开始节点、当前节点与网关节点（引擎在 {@code anyNodeCode} 命中网关时会抛
     * {@code TAR_NOT_GATEWAY}，{@code prefixOrSuffixNodes} 本身也已过滤网关）。
     * </p>
     * <p>
     * <b>与「严格正确的可退回集合」的偏差：</b>
     * </p>
     * <ol>
     *   <li>它是一份<b>并集</b>：对「驳回到指定节点」而言，后半段（当前节点的后继）是前向节点，
     *       引擎虽不拦（{@code anyNodeCode} 优先于 {@code skipType}），但属于「任意退回选择后置节点」，
     *       该约束在参考项目里也只是注释，引擎未强制。前端应只在自由跳转时允许选择后半段；</li>
     *   <li>对「自由跳转」而言它是<b>更严格的子集</b>：引擎实际允许 {@code anyNodeCode} 指向定义中
     *       任意非网关节点，包括与本节点无直接路径关系的节点（例如另一条并行分支上的节点），
     *       本实现不会列出这些；宁可少给选项，也不给出引擎会拒绝的选项；</li>
     *   <li>开始节点被排除（{@code FIRST_FORBID_BACK}：引擎禁止退回开始节点）。</li>
     * </ol>
     *
     * @param instanceId 流程实例id
     * @return List&lt;WorkflowNodeDTO&gt;
     */
    public List<WorkflowNodeDTO> listAnyNode(Long instanceId) {
        Instance instance = requireInstance(instanceId);
        NodeService nodes = requireNodeService();
        Node startNode = nodes.getStartNode(instance.getDefinitionId());
        if (nodes.getByDefIdAndNodeCode(instance.getDefinitionId(), instance.getNodeCode()) == null) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("流程实例的当前节点在定义中已不存在");
        }

        Set<String> visited = visitedNodeCodes(instance);
        List<Node> candidates = new ArrayList<>();
        for (Node node : nodes.previousNodeList(instance.getDefinitionId(), instance.getNodeCode())) {
            if (visited.contains(node.getNodeCode())) {
                candidates.add(node);
            }
        }
        for (Node node : nodes.suffixNodeList(instance.getDefinitionId(), instance.getNodeCode())) {
            if (!containsNodeCode(candidates, node.getNodeCode())) {
                candidates.add(node);
            }
        }

        List<Node> result = new ArrayList<>();
        for (Node node : candidates) {
            if (isSelectable(node, startNode, instance.getNodeCode())) {
                result.add(node);
            }
        }
        return WorkflowDtoConvert.toNodeDTOList(result);
    }

    /**
     * 撤回流程：发起人把自己的申请撤回，流程状态置为「撤销」
     *
     * <p>引擎会校验 {@code handler} 就是实例的发起人（{@code createBy}），因此这里显式把当前
     * 用户交给引擎，避免调用方伪造。</p>
     *
     * @param param 实例级动作参数
     * @return WorkflowInstanceDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowInstanceDTO revoke(ApiWorkflowInstanceActionParam param) {
        Instance instance = requireInstance(param == null ? null : param.getInstanceId());
        FlowParams flowParams = FlowParams.build()
            .handler(currentHandler())
            .message(param == null ? null : param.getMessage());
        Instance revoked;
        try {
            revoked = requireTaskService().revoke(instance.getId(), flowParams);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        return WorkflowDtoConvert.toInstanceDTO(revoked == null ? instance : revoked);
    }

    /**
     * 终止流程：提前结束，所有待办转历史
     *
     * @param param 实例级动作参数
     * @return WorkflowInstanceDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowInstanceDTO terminate(ApiWorkflowInstanceActionParam param) {
        Instance instance = requireInstance(param == null ? null : param.getInstanceId());
        FlowParams flowParams = FlowParams.build()
            .handler(currentHandler())
            .message(param == null ? null : param.getMessage());
        Instance terminated;
        try {
            terminated = requireTaskService().terminationByInsId(instance.getId(), flowParams);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        if (terminated == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_INSTANCE_NOT_FOUND);
        }
        return WorkflowDtoConvert.toInstanceDTO(terminated);
    }

    /**
     * 拿回：发起人把「我上一次已经办结的那一步」重新变回自己的待办
     * <p>
     * 与 {@link WorkflowTaskService} 里基于任务 id 的拿回相比，
     * 这里是<b>按流程实例</b>拿回（引擎的 {@code taskBackByInsId}），入口不同、语义一致。
     * </p>
     * <p>
     * <b>为什么必须显式传 {@code handler} 与 {@code permissionFlag}：</b>引擎的
     * {@code TaskServiceImpl#taskBack} 按 {@code approver.equals(flowParams.getHandler())}
     * 定位「我办过的那一条」历史任务（并把它作为退回目标节点），拿不到当前用户时直接抛
     * {@code NOT_FOUNT_HANDLED_TASK}；之后 {@code skip} 还会用 {@code permissionFlag} 做一次
     * 办理人交集校验。两者都由服务端从登录上下文取，不接受调用方指定。
     * </p>
     *
     * @param param 实例级动作参数（instanceId 为流程实例id，message 为拿回原因）
     * @return WorkflowInstanceDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowInstanceDTO takeBack(ApiWorkflowInstanceActionParam param) {
        Instance instance = requireInstance(param == null ? null : param.getInstanceId());
        FlowParams flowParams = FlowParams.build()
            .handler(currentHandler())
            .message(param == null ? null : param.getMessage());
        List<String> permissions = currentPermissions();
        if (CollUtil.isNotEmpty(permissions)) {
            flowParams.permissionFlag(permissions);
        }
        Instance takenBack;
        try {
            takenBack = requireTaskService().taskBackByInsId(instance.getId(), flowParams);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        if (takenBack == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_INSTANCE_NOT_FOUND);
        }
        return WorkflowDtoConvert.toInstanceDTO(takenBack);
    }

    /**
     * 挂起流程实例：挂起后该实例无法继续流转
     *
     * @param instanceId 流程实例id
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean suspend(Long instanceId) {
        requireInstance(instanceId);
        try {
            return instanceService.unActive(instanceId);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
    }

    /**
     * 激活流程实例
     *
     * @param instanceId 流程实例id
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean activate(Long instanceId) {
        requireInstance(instanceId);
        try {
            return instanceService.active(instanceId);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
    }

    /**
     * 校验实例存在并返回
     *
     * @param instanceId 流程实例id
     * @return 流程实例
     */
    private Instance requireInstance(Long instanceId) {
        if (instanceId == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("流程实例ID不能为空");
        }
        Instance instance = instanceService.getById(instanceId);
        if (instance == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_INSTANCE_NOT_FOUND);
        }
        return instance;
    }

    /**
     * 本实例已走过的节点编码：历史任务的办理节点 + 它们跳转到的目标节点 + 当前节点
     *
     * <p>历史任务的 {@code target_node_code} 可能是逗号分隔的多个节点（并行网关），需拆开。</p>
     *
     * @param instance 流程实例
     * @return 节点编码集合
     */
    private Set<String> visitedNodeCodes(Instance instance) {
        Set<String> visited = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(instance.getNodeCode())) {
            visited.add(instance.getNodeCode());
        }
        List<HisTask> hisTasks = requireHisTaskService().getByInsId(instance.getId());
        if (hisTasks == null) {
            return visited;
        }
        for (HisTask hisTask : hisTasks) {
            if (StrUtil.isNotBlank(hisTask.getNodeCode())) {
                visited.add(hisTask.getNodeCode());
            }
            if (StrUtil.isNotBlank(hisTask.getTargetNodeCode())) {
                for (String targetCode : hisTask.getTargetNodeCode().split(",")) {
                    if (StrUtil.isNotBlank(targetCode)) {
                        visited.add(targetCode.trim());
                    }
                }
            }
        }
        return visited;
    }

    /**
     * 候选节点是否可以选择：排除开始节点、当前节点与网关节点
     *
     * @param node        候选节点
     * @param startNode   流程开始节点
     * @param nowNodeCode 当前节点编码
     * @return true 表示可以选
     */
    private boolean isSelectable(Node node, Node startNode, String nowNodeCode) {
        if (node == null || StrUtil.isBlank(node.getNodeCode())) {
            return false;
        }
        if (node.getNodeCode().equals(nowNodeCode)) {
            return false;
        }
        if (startNode != null && node.getNodeCode().equals(startNode.getNodeCode())) {
            return false;
        }
        return !NodeType.isGateWay(node.getNodeType());
    }

    private boolean containsNodeCode(List<Node> nodes, String nodeCode) {
        for (Node node : nodes) {
            if (node.getNodeCode().equals(nodeCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把用户标识串换成姓名串（解析不到时保留原标识）
     *
     * @param ids   用户标识串（逗号分隔）
     * @param names 用户标识 → 姓名
     * @return 姓名串
     */
    private String resolve(String ids, Map<String, String> names) {
        if (StrUtil.isBlank(ids)) {
            return ids;
        }
        List<String> resolved = new ArrayList<>();
        for (String id : ids.split(",")) {
            if (StrUtil.isBlank(id)) {
                continue;
            }
            String trimmed = id.trim();
            resolved.add(names.getOrDefault(trimmed, trimmed));
        }
        return String.join(",", resolved);
    }

    private String currentHandler() {
        return permissionHandler == null ? null : permissionHandler.getHandler();
    }

    /**
     * 当前用户可办理的标识集合（与节点侧 {@code flow_user.processed_by} 同一套词汇）
     * <p>
     * 处理器未装配时返回空列表，而不是抛错：{@code permissionFlag} 在引擎侧本就允许缺省
     * （{@code FlowParams#getPermissionFlag} 为空时会回落到 {@code PermissionHandler}），
     * 这里不额外制造一个失败点。
     * </p>
     *
     * @return 标识列表
     */
    private List<String> currentPermissions() {
        if (permissionHandler == null) {
            return List.of();
        }
        List<String> permissions = permissionHandler.permissions();
        return permissions == null ? List.of() : permissions;
    }

    private WorkflowInstanceManager requireInstanceManager() {
        if (instanceManager == null) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("流程实例查询组件未装配");
        }
        return instanceManager;
    }

    private TaskService requireTaskService() {
        if (taskService == null) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("引擎待办任务服务未装配");
        }
        return taskService;
    }

    private HisTaskService requireHisTaskService() {
        if (hisTaskService == null) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("引擎历史任务服务未装配");
        }
        return hisTaskService;
    }

    private NodeService requireNodeService() {
        if (nodeService == null) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("引擎流程节点服务未装配");
        }
        return nodeService;
    }

    private WorkflowOrgUserManager requireOrgUserManager() {
        if (orgUserManager == null) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("组织机构查询组件未装配");
        }
        return orgUserManager;
    }

}
