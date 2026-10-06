package com.carlos.workflow.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.workflow.api.enums.WorkflowActionEnum;
import com.carlos.workflow.api.pojo.param.ApiWorkflowCandidatePageParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowHandleParam;
import com.carlos.workflow.api.pojo.param.ApiWorkflowTaskPageParam;
import com.carlos.workflow.convert.WorkflowDtoConvert;
import com.carlos.workflow.dto.WorkflowCandidateDTO;
import com.carlos.workflow.dto.WorkflowHandleResultDTO;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
import com.carlos.workflow.enums.WorkflowErrorCode;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowOrgUserManager;
import com.carlos.workflow.manager.WorkflowTaskManager;
import lombok.RequiredArgsConstructor;
import org.dromara.warm.flow.core.dto.FlowDto;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.dromara.warm.flow.core.entity.HisTask;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.entity.Task;
import org.dromara.warm.flow.core.enums.SkipType;
import org.dromara.warm.flow.core.exception.FlowException;
import org.dromara.warm.flow.core.handler.PermissionHandler;
import org.dromara.warm.flow.core.service.HisTaskService;
import org.dromara.warm.flow.core.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 待办任务服务，把 app 侧请求翻译成 Warm-Flow 的 {@code TaskService} 调用，不实现引擎自身的流转逻辑。
 * </p>
 * <p>
 * 引擎异常映射规则：数据存在性在调用前显式校验（缺失即返回 20701/20702/20703），
 * 调用过程中引擎抛出的 {@code FlowException} 统一视为「该任务当前不允许此操作」（20704）。
 * </p>
 * <p>
 * <b>依赖注入说明：</b>本类由 {@code WorkflowAutoConfiguration} 以 {@code @Bean} 注册
 * （构造器由该配置类给出，config 包不在本批次改动范围内），因此列表查询所需的
 * {@code Manager} 与 {@code PermissionHandler} 用字段注入补充；新增 Bean 一律标注
 * {@code @Component} 由组件扫描装配。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@RequiredArgsConstructor
public class WorkflowTaskService {

    private final TaskService taskService;

    private final HisTaskService hisTaskService;

    /**
     * 待办 / 已办列表查询（含批量补齐流程名称、业务id、办理人姓名）
     * <p>
     * 非必需依赖：本类由 {@code WorkflowAutoConfiguration} 注册，而该配置的单元测试用
     * {@code ApplicationContextRunner} 只装配自动配置本身（不扫描组件），此时 Manager 不在容器里。
     * 缺失时相关端点会以 50701 明确报错，而不是让上下文启动不了。
     * </p>
     */
    @Autowired(required = false)
    private WorkflowTaskManager taskManager;

    /**
     * 候选办理人：数据源是组织机构中心，不在工作流侧保存用户主数据
     */
    @Autowired(required = false)
    private WorkflowOrgUserManager orgUserManager;

    /**
     * 办理人权限处理器：提供当前用户的办理人标识集合
     * <p>
     * 宿主可能没有注册（未开 Feign、或提供自己的实现后未生效），因此不是必需依赖；
     * 缺失时依赖它的端点会明确报错，而不是静默返回空列表。
     * </p>
     */
    @Autowired(required = false)
    private PermissionHandler permissionHandler;

    /**
     * 审批页加载：任务详情 + 审批表单 + 表单数据
     *
     * @param taskId 待办任务id
     * @return WorkflowTaskDTO
     */
    public WorkflowTaskDTO load(Long taskId) {
        if (taskId == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("任务ID不能为空");
        }
        Task task = taskService.getById(taskId);
        if (task == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_TASK_NOT_FOUND);
        }
        return WorkflowDtoConvert.toTaskDTO(task, loadFormData(taskId));
    }

    /**
     * 已办任务详情：历史任务 + 审批表单 + 表单数据
     *
     * @param hisTaskId 历史任务id
     * @return WorkflowHisTaskDTO
     */
    public WorkflowHisTaskDTO loadHistory(Long hisTaskId) {
        if (hisTaskId == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("历史任务ID不能为空");
        }
        HisTask hisTask = hisTaskService.getById(hisTaskId);
        if (hisTask == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_TASK_NOT_FOUND);
        }
        FlowDto flowDto;
        try {
            flowDto = taskService.hisLoad(hisTaskId, FlowParams.build());
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        return WorkflowDtoConvert.toHisTaskDTO(hisTask, flowDto);
    }

    /**
     * 统一审批动作，按 {@link WorkflowActionEnum} 分派
     * <p>
     * 一次流转会写多张表（{@code flow_task} / {@code flow_his_task} / {@code flow_user} /
     * {@code flow_instance}），而引擎自身没有任何 {@code @Transactional}；中途失败会留下
     * 「任务已转历史、待办未生成」这类脏数据，因此本方法（以及其它写方法）必须开启事务。
     * </p>
     *
     * @param param 审批参数
     * @return WorkflowHandleResultDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkflowHandleResultDTO handle(ApiWorkflowHandleParam param) {
        if (param == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("审批参数不能为空");
        }
        if (param.getTaskId() == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("任务ID不能为空");
        }
        WorkflowActionEnum action = param.getAction();
        if (action == null) {
            throw WorkflowErrorCode.WORKFLOW_ACTION_UNSUPPORTED.workflowException();
        }
        if (action.handlerAdjust() && CollUtil.isEmpty(param.getTargetHandlers())) {
            throw WorkflowErrorCode.WORKFLOW_TARGET_HANDLER_REQUIRED.workflowException();
        }
        FlowParams flowParams = toFlowParams(param, action);
        if (action.flowSkip()) {
            return skip(param.getTaskId(), action, flowParams);
        }
        if (action.handlerAdjust()) {
            return adjustHandlers(param.getTaskId(), action, flowParams);
        }
        if (action.rejectLast()) {
            return rejectLast(param.getTaskId(), action, flowParams);
        }
        if (action.takeBack()) {
            return takeBack(param.getTaskId(), action, flowParams);
        }
        throw WorkflowErrorCode.WORKFLOW_ACTION_UNSUPPORTED.workflowException();
    }

    /**
     * 待办分页
     * <p>
     * 当前用户的办理人标识集合取自 {@link PermissionHandler#permissions()}——它与节点侧
     * {@code flow_user.processed_by} 是同一套词汇（见 {@code OrgPermissionHandler} 类注释），
     * 因此这里不额外拼角色 / 部门标识，避免「恰好等于某个用户 id」而误放行。
     * </p>
     *
     * @param param 分页与过滤参数
     * @return Paging&lt;WorkflowTaskDTO&gt;
     */
    public Paging<WorkflowTaskDTO> todoPage(ApiWorkflowTaskPageParam param) {
        return requireTaskManager().getTodoPage(param, currentPermissions());
    }

    /**
     * 已办分页：只查自己的，且每个流程实例只保留最近办理的那一条
     *
     * @param param 分页与过滤参数
     * @return Paging&lt;WorkflowHisTaskDTO&gt;
     */
    public Paging<WorkflowHisTaskDTO> donePage(ApiWorkflowTaskPageParam param) {
        return requireTaskManager().getDonePage(param, currentHandler());
    }

    /**
     * 审批交互动作的候选办理人
     * <p>
     * 减签只能减「当前任务已有的办理人」，加签 / 转办 / 委托则只能选「还没参与的人」，
     * 两种口径共用同一个远程查询，只是把任务现有办理人放在 include 还是 exclude。
     * </p>
     *
     * @param taskId 待办任务id
     * @param param  分页与过滤参数
     * @return Paging&lt;WorkflowCandidateDTO&gt;
     */
    public Paging<WorkflowCandidateDTO> candidates(Long taskId, ApiWorkflowCandidatePageParam param) {
        if (taskId == null) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("任务ID不能为空");
        }
        if (taskService.getById(taskId) == null) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_TASK_NOT_FOUND);
        }
        ApiWorkflowCandidatePageParam query = param == null ? new ApiWorkflowCandidatePageParam() : param;
        List<String> handlers = requireTaskManager().listTaskHandlerIds(taskId);
        boolean reduction = query.getAction() == WorkflowActionEnum.REDUCTION_SIGNATURE;
        return requireOrgUserManager().getCandidatePage(query.getDeptId(),
            reduction ? handlers : null,
            reduction ? null : handlers,
            query.getCurrent(), query.getSize());
    }

    private WorkflowHandleResultDTO skip(Long taskId, WorkflowActionEnum action, FlowParams flowParams) {
        if (action == WorkflowActionEnum.FREE_JUMP && StrUtil.isBlank(flowParams.getNodeCode())) {
            throw WorkflowErrorCode.WORKFLOW_PARAM_INVALID.workflowException("自由跳转必须指定目标节点编码");
        }
        flowParams.skipType(action == WorkflowActionEnum.REJECT ? SkipType.REJECT.getKey() : SkipType.PASS.getKey());
        Instance instance;
        try {
            instance = taskService.skip(taskId, flowParams);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        return WorkflowHandleResultDTO.builder()
            .action(action)
            .taskId(taskId)
            .operated(true)
            .instance(WorkflowDtoConvert.toInstanceDTO(instance))
            .build();
    }

    private WorkflowHandleResultDTO adjustHandlers(Long taskId, WorkflowActionEnum action, FlowParams flowParams) {
        boolean operated;
        try {
            operated = switch (action) {
                case TRANSFER -> taskService.transfer(taskId, flowParams);
                case DEPUTE -> taskService.depute(taskId, flowParams);
                case ADD_SIGNATURE -> taskService.addSignature(taskId, flowParams);
                case REDUCTION_SIGNATURE -> taskService.reductionSignature(taskId, flowParams);
                default -> throw WorkflowErrorCode.WORKFLOW_ACTION_UNSUPPORTED.workflowException();
            };
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        return WorkflowHandleResultDTO.builder()
            .action(action)
            .taskId(taskId)
            .operated(operated)
            .build();
    }

    /**
     * 退回上一节点：引擎按「当前节点是某条历史任务的 target_node_code」回溯，找到最近的那条并跳回去
     *
     * @param taskId     待办任务id
     * @param action     审批动作
     * @param flowParams 流程参数
     * @return 流转结果
     */
    private WorkflowHandleResultDTO rejectLast(Long taskId, WorkflowActionEnum action, FlowParams flowParams) {
        Instance instance;
        try {
            instance = taskService.rejectLast(taskId, flowParams);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        return WorkflowHandleResultDTO.builder()
            .action(action)
            .taskId(taskId)
            .operated(true)
            .instance(WorkflowDtoConvert.toInstanceDTO(instance))
            .build();
    }

    /**
     * 拿回：把「我上一次办理完的任务」重新变成我的待办
     * <p>
     * 引擎要求 {@code handler} 与最近一条已通过的历史任务审批人一致（它按这个条件定位要拿回的任务），
     * 因此这里必须把当前用户的办理人标识交给引擎，而不是交给调用方随意指定。
     * </p>
     *
     * @param taskId     待办任务id
     * @param action     审批动作
     * @param flowParams 流程参数
     * @return 流转结果
     */
    private WorkflowHandleResultDTO takeBack(Long taskId, WorkflowActionEnum action, FlowParams flowParams) {
        String handler = currentHandler();
        if (StrUtil.isBlank(handler)) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("无法确定当前用户，不能执行拿回");
        }
        flowParams.handler(handler);
        Instance instance;
        try {
            instance = taskService.taskBack(taskId, flowParams);
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
        return WorkflowHandleResultDTO.builder()
            .action(action)
            .taskId(taskId)
            .operated(true)
            .instance(WorkflowDtoConvert.toInstanceDTO(instance))
            .build();
    }

    /**
     * 当前用户的办理人唯一标识（与节点侧 {@code flow_user.processed_by} 同一套词汇）
     *
     * @return 用户 id；处理器未装配时返回 null
     */
    private String currentHandler() {
        return permissionHandler == null ? null : permissionHandler.getHandler();
    }

    /**
     * 当前用户可办理的任务的办理人标识集合
     *
     * @return 标识列表
     */
    private List<String> currentPermissions() {
        if (permissionHandler == null) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException(
                "未装配 PermissionHandler，无法确定当前用户的办理人标识");
        }
        List<String> permissions = permissionHandler.permissions();
        return permissions == null ? List.of() : permissions;
    }

    private WorkflowTaskManager requireTaskManager() {
        if (taskManager == null) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("待办任务查询组件未装配");
        }
        return taskManager;
    }

    private WorkflowOrgUserManager requireOrgUserManager() {
        if (orgUserManager == null) {
            throw WorkflowErrorCode.WORKFLOW_ENGINE_ERROR.workflowException("组织机构查询组件未装配");
        }
        return orgUserManager;
    }

    private FlowDto loadFormData(Long taskId) {
        try {
            return taskService.load(taskId, FlowParams.build());
        } catch (FlowException e) {
            throw new WorkflowException(CommonErrorCode.WORKFLOW_OPERATION_NOT_ALLOWED, e.getMessage());
        }
    }

    private FlowParams toFlowParams(ApiWorkflowHandleParam param, WorkflowActionEnum action) {
        Map<String, Object> variable = param.getVariable() == null ? new HashMap<>() : param.getVariable();
        FlowParams flowParams = FlowParams.build()
            .handler(StrUtil.isNotBlank(param.getHandler()) ? param.getHandler() : currentHandler())
            .message(param.getMessage())
            .variable(variable)
            .nodeCode(param.getNodeCode());
        // 显式给出当前用户的办理人标识集合：不传时引擎也会回落到 PermissionHandler，
        // 但显式传入可以让「谁有权办」这件事在本层可读、可测
        List<String> permissions = permissionHandler == null ? List.of() : permissionHandler.permissions();
        if (CollUtil.isNotEmpty(permissions)) {
            flowParams.permissionFlag(permissions);
        }
        if (action == WorkflowActionEnum.REDUCTION_SIGNATURE) {
            flowParams.reductionHandlers(param.getTargetHandlers());
        } else if (action.handlerAdjust()) {
            flowParams.addHandlers(param.getTargetHandlers());
        }
        return flowParams;
    }

}
