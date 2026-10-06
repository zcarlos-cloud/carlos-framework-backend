package com.carlos.workflow.manager.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.workflow.api.pojo.param.ApiWorkflowTaskPageParam;
import com.carlos.workflow.convert.WorkflowDtoConvert;
import com.carlos.workflow.dto.WorkflowHisTaskDTO;
import com.carlos.workflow.dto.WorkflowTaskDTO;
import com.carlos.workflow.manager.WorkflowOrgUserManager;
import com.carlos.workflow.manager.WorkflowTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.enums.NodeType;
import org.dromara.warm.flow.core.enums.UserType;
import org.dromara.warm.flow.orm.entity.FlowDefinition;
import org.dromara.warm.flow.orm.entity.FlowHisTask;
import org.dromara.warm.flow.orm.entity.FlowInstance;
import org.dromara.warm.flow.orm.entity.FlowTask;
import org.dromara.warm.flow.orm.entity.FlowUser;
import org.dromara.warm.flow.orm.mapper.FlowDefinitionMapper;
import org.dromara.warm.flow.orm.mapper.FlowHisTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowInstanceMapper;
import org.dromara.warm.flow.orm.mapper.FlowTaskMapper;
import org.dromara.warm.flow.orm.mapper.FlowUserMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p>
 * 待办 / 已办任务查询封装实现类。
 * </p>
 *
 * <h2>为什么列表查询要 join，却又「不能 SQL」</h2>
 * <p>
 * 列表要展示的字段散在三张表：{@code flow_task}（节点、状态）、{@code flow_definition}（流程名称）、
 * {@code flow_instance}（业务id、激活状态），另外还要 {@code flow_user} 上的办理人。
 * 参考实现（RuoYi 版）用一条 LEFT JOIN 的 XML 一次取回；本模块禁止手写 SQL，因此改为
 * <b>「一页任务 + 三次批量补齐」</b>：先分页取出本页任务，再按 id 集合批量取流程名称、
 * 实例信息与办理人行。<b>批量是关键</b>——按行回查会退化成 N+1（每次翻页最多 4 次查询，
 * 与页大小无关）。
 * </p>
 *
 * <h2>去重</h2>
 * <p>
 * 一个任务可能有多条 {@code flow_user}（会签、加签、转办后并存多个人），参考实现靠
 * {@code DISTINCT} 去重；本实现先取「命中我的任务 id 集合」（{@code Set} 天然去重），
 * 再按 {@code flow_task.id IN (...)} 分页，同一个任务在待办里只会出现一次。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowTaskManagerImpl extends BaseServiceImpl<FlowTaskMapper, FlowTask>
    implements WorkflowTaskManager {

    /**
     * 办理人姓名分隔符：与前端约定的「姓名串」拼接方式
     */
    private static final String NAME_SEPARATOR = ";";

    /**
     * {@code flow_his_task.collaborator} 的分隔符，与引擎写入时保持一致
     */
    private static final String COLLABORATOR_SEPARATOR = ",";

    private final FlowUserMapper flowUserMapper;

    private final FlowHisTaskMapper flowHisTaskMapper;

    private final FlowDefinitionMapper flowDefinitionMapper;

    private final FlowInstanceMapper flowInstanceMapper;

    private final WorkflowOrgUserManager orgUserManager;

    @Override
    public Paging<WorkflowTaskDTO> getTodoPage(ApiWorkflowTaskPageParam param, Collection<String> handlerIds) {
        if (CollUtil.isEmpty(handlerIds)) {
            // 未登录或没有办理人标识：没有待办可言，也不该去查库
            return MybatisPage.emptyPaging();
        }
        List<Long> taskIds = listTaskIdsByHandlers(handlerIds);
        if (taskIds.isEmpty()) {
            return MybatisPage.emptyPaging();
        }
        ApiWorkflowTaskPageParam query = param == null ? new ApiWorkflowTaskPageParam() : param;

        LambdaQueryWrapper<FlowTask> wrapper = queryWrapper();
        wrapper.eq(FlowTask::getNodeType, NodeType.BETWEEN.getKey())
            .in(FlowTask::getId, taskIds)
            .like(StrUtil.isNotBlank(query.getNodeName()), FlowTask::getNodeName, query.getNodeName())
            .eq(StrUtil.isNotBlank(query.getFlowStatus()), FlowTask::getFlowStatus, query.getFlowStatus());
        // 分页需要稳定排序：create_time 可能同秒，补 id 作次级键
        wrapper.orderByDesc(FlowTask::getCreateTime).orderByDesc(FlowTask::getId);

        PageInfo<FlowTask> page = page(pageInfo(query), wrapper);
        List<WorkflowTaskDTO> records = enrichTaskList(page.getRecords());
        return MybatisPage.convert(page, ignored -> records);
    }

    @Override
    public Paging<WorkflowHisTaskDTO> getDonePage(ApiWorkflowTaskPageParam param, String approverId) {
        if (StrUtil.isBlank(approverId)) {
            return MybatisPage.emptyPaging();
        }
        ApiWorkflowTaskPageParam query = param == null ? new ApiWorkflowTaskPageParam() : param;
        List<Long> latestIds = listLatestHisTaskIds(approverId, query.getNodeName());
        if (latestIds.isEmpty()) {
            return MybatisPage.emptyPaging();
        }

        LambdaQueryWrapper<FlowHisTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(FlowHisTask::getId, latestIds)
            .eq(StrUtil.isNotBlank(query.getFlowStatus()), FlowHisTask::getFlowStatus, query.getFlowStatus())
            .orderByDesc(FlowHisTask::getCreateTime)
            .orderByDesc(FlowHisTask::getId);

        PageInfo<FlowHisTask> hisTaskPage = new PageInfo<>(query.getCurrent(), query.getSize());
        hisTaskPage = flowHisTaskMapper.selectPage(hisTaskPage, wrapper);
        List<WorkflowHisTaskDTO> records = enrichHisTaskList(hisTaskPage.getRecords());
        return MybatisPage.convert(hisTaskPage, ignored -> records);
    }

    @Override
    public List<String> listTaskHandlerIds(Long taskId) {
        if (taskId == null) {
            return List.of();
        }
        LambdaQueryWrapper<FlowUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(FlowUser::getProcessedBy).eq(FlowUser::getAssociated, taskId);
        return flowUserMapper.selectList(wrapper).stream()
            .map(FlowUser::getProcessedBy)
            .filter(StrUtil::isNotBlank)
            .distinct()
            .toList();
    }

    /**
     * 命中给定办理人标识的待办任务 id 集合（去重）
     *
     * <p>不过滤 {@code flow_user.type}：转办（type=2）、委托（type=3）产生的行同样代表
     * 「这个任务现在归谁办」，漏掉它们会让转办后的人看不到待办——引擎自己的
     * {@code checkAuth} 也是把三种类型的办理人合并后做交集校验的。</p>
     *
     * @param handlerIds 办理人标识集合
     * @return 任务 id 列表
     */
    private List<Long> listTaskIdsByHandlers(Collection<String> handlerIds) {
        LambdaQueryWrapper<FlowUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(FlowUser::getAssociated).in(FlowUser::getProcessedBy, handlerIds);
        Set<Long> taskIds = new LinkedHashSet<>();
        for (FlowUser user : flowUserMapper.selectList(wrapper)) {
            if (user.getAssociated() != null) {
                taskIds.add(user.getAssociated());
            }
        }
        return new ArrayList<>(taskIds);
    }

    /**
     * 每个流程实例中「我最近办理的那一条」历史任务 id
     *
     * <p>对应参考实现的 {@code SELECT MAX(id) ... GROUP BY instance_id}：id 由引擎的
     * {@code DataFillHandler} 生成（雪花号），单调递增，因此 {@code MAX(id)} 就是最近办理的一条。
     * <b>不手写 SQL</b>：这里用 MyBatis-Plus 的 {@code select("MAX(id) AS id")} + {@code groupBy}
     * 表达，仍然只查一列。</p>
     *
     * @param approverId 办理人 id
     * @param nodeName   节点名称，可选模糊匹配
     * @return 历史任务 id 列表
     */
    private List<Long> listLatestHisTaskIds(String approverId, String nodeName) {
        QueryWrapper<FlowHisTask> wrapper = new QueryWrapper<>();
        wrapper.select("MAX(id) AS id")
            .eq("approver", approverId)
            .like(StrUtil.isNotBlank(nodeName), "node_name", nodeName)
            .groupBy("instance_id");
        List<Long> ids = new ArrayList<>();
        for (Object value : flowHisTaskMapper.selectObjs(wrapper)) {
            Long id = value == null ? null : Long.valueOf(String.valueOf(value));
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }

    /**
     * 批量补齐待办列表的展示字段：流程名称、业务id、激活状态、办理人姓名
     *
     * @param tasks 本页任务
     * @return List&lt;WorkflowTaskDTO&gt;
     */
    private List<WorkflowTaskDTO> enrichTaskList(List<FlowTask> tasks) {
        List<WorkflowTaskDTO> dtoList = WorkflowDtoConvert.toTaskDTOList(tasks);
        if (dtoList.isEmpty()) {
            return dtoList;
        }
        Map<Long, String> flowNames = mapFlowNames(collDefinitionIds(tasks));
        Map<Long, FlowInstance> instances = mapInstances(collInstanceIds(tasks));
        Map<Long, List<FlowUser>> taskUsers = mapTaskUsers(collTaskIds(tasks));
        Map<String, String> names = resolveNames(taskUsers.values());

        for (WorkflowTaskDTO dto : dtoList) {
            dto.setFlowName(flowNames.get(dto.getDefinitionId()));
            FlowInstance instance = instances.get(dto.getInstanceId());
            if (instance != null) {
                dto.setBusinessId(instance.getBusinessId());
                dto.setActivityStatus(instance.getActivityStatus());
            }
            List<FlowUser> users = taskUsers.getOrDefault(dto.getTaskId(), List.of());
            dto.setApprover(joinNames(users, UserType.APPROVAL.getKey(), names));
            dto.setTransferredBy(joinNames(users, UserType.TRANSFER.getKey(), names));
            dto.setDelegate(joinNames(users, UserType.DEPUTE.getKey(), names));
        }
        return dtoList;
    }

    /**
     * 批量补齐已办列表的展示字段：流程名称、业务id，并把审批人 / 协作人的 id 串换成姓名串
     *
     * @param hisTasks 本页历史任务
     * @return List&lt;WorkflowHisTaskDTO&gt;
     */
    private List<WorkflowHisTaskDTO> enrichHisTaskList(List<FlowHisTask> hisTasks) {
        List<WorkflowHisTaskDTO> dtoList = WorkflowDtoConvert.toHisTaskDTOList(hisTasks);
        if (dtoList.isEmpty()) {
            return dtoList;
        }
        Map<Long, String> flowNames = mapFlowNames(hisTasks.stream()
            .map(FlowHisTask::getDefinitionId).filter(Objects::nonNull).distinct().toList());
        Map<Long, FlowInstance> instances = mapInstances(hisTasks.stream()
            .map(FlowHisTask::getInstanceId).filter(Objects::nonNull).distinct().toList());

        Set<String> approverIds = new LinkedHashSet<>();
        for (FlowHisTask hisTask : hisTasks) {
            approverIds.addAll(collaboratorIds(hisTask));
        }
        Map<String, String> names = orgUserManager.mapNames(approverIds);

        for (WorkflowHisTaskDTO dto : dtoList) {
            dto.setFlowName(flowNames.get(dto.getDefinitionId()));
            FlowInstance instance = instances.get(dto.getInstanceId());
            if (instance != null) {
                dto.setBusinessId(instance.getBusinessId());
            }
            dto.setApprover(displayNames(dto.getApprover(), names));
            dto.setCollaborator(displayNames(dto.getCollaborator(), names));
        }
        return dtoList;
    }

    /**
     * 取历史任务涉及的审批人 / 协作人标识
     *
     * @param hisTask 历史任务
     * @return 标识列表
     */
    private List<String> collaboratorIds(FlowHisTask hisTask) {
        List<String> ids = new ArrayList<>();
        if (StrUtil.isNotBlank(hisTask.getApprover())) {
            ids.add(hisTask.getApprover().trim());
        }
        if (StrUtil.isNotBlank(hisTask.getCollaborator())) {
            for (String id : hisTask.getCollaborator().split(COLLABORATOR_SEPARATOR)) {
                if (StrUtil.isNotBlank(id)) {
                    ids.add(id.trim());
                }
            }
        }
        return ids;
    }

    /**
     * 按协作类型取出办理人姓名串
     *
     * @param users  任务下的 {@code flow_user} 行
     * @param type   协作类型（1 审批 2 转办 3 委托）
     * @param names  用户标识 → 姓名
     * @return 姓名串（分号分隔）；没有该类型的人时返回 null
     */
    private String joinNames(List<FlowUser> users, String type, Map<String, String> names) {
        List<String> resolved = new ArrayList<>();
        for (FlowUser user : users) {
            if (!type.equals(user.getType()) || StrUtil.isBlank(user.getProcessedBy())) {
                continue;
            }
            String id = user.getProcessedBy().trim();
            // org 侧查不到姓名时回退为原始标识：宁可显示一个 id，也不要让列表整列为空
            resolved.add(names.getOrDefault(id, id));
        }
        return resolved.isEmpty() ? null : String.join(NAME_SEPARATOR, resolved);
    }

    /**
     * 把逗号分隔的用户标识串整体换成姓名串（保持同样的分隔符）
     *
     * @param ids   用户标识串
     * @param names 用户标识 → 姓名
     * @return 姓名串；入参为空时返回原值
     */
    private String displayNames(String ids, Map<String, String> names) {
        if (StrUtil.isBlank(ids)) {
            return ids;
        }
        List<String> resolved = new ArrayList<>();
        for (String id : ids.split(COLLABORATOR_SEPARATOR)) {
            if (StrUtil.isBlank(id)) {
                continue;
            }
            String trimmed = id.trim();
            resolved.add(names.getOrDefault(trimmed, trimmed));
        }
        return String.join(COLLABORATOR_SEPARATOR, resolved);
    }

    /**
     * 一次性解析本页涉及的全部用户姓名
     *
     * @param userGroups 任务id → {@code flow_user} 行
     * @return 用户标识 → 姓名
     */
    private Map<String, String> resolveNames(Collection<List<FlowUser>> userGroups) {
        Set<String> ids = new LinkedHashSet<>();
        for (List<FlowUser> users : userGroups) {
            for (FlowUser user : users) {
                if (StrUtil.isNotBlank(user.getProcessedBy())) {
                    ids.add(user.getProcessedBy().trim());
                }
            }
        }
        return orgUserManager.mapNames(ids);
    }

    /**
     * 本页任务下的全部 {@code flow_user} 行，按任务id分组
     *
     * @param taskIds 任务id集合
     * @return 任务id → {@code flow_user} 行
     */
    private Map<Long, List<FlowUser>> mapTaskUsers(List<Long> taskIds) {
        Map<Long, List<FlowUser>> result = new LinkedHashMap<>();
        if (taskIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<FlowUser> wrapper = new LambdaQueryWrapper<>();
        // 只认「任务办理人」的三种协作类型：type=4 的行关联的是流程实例，不是任务
        wrapper.in(FlowUser::getAssociated, taskIds)
            .in(FlowUser::getType, List.of(UserType.APPROVAL.getKey(), UserType.TRANSFER.getKey(),
                UserType.DEPUTE.getKey()));
        for (FlowUser user : flowUserMapper.selectList(wrapper)) {
            result.computeIfAbsent(user.getAssociated(), key -> new ArrayList<>()).add(user);
        }
        return result;
    }

    private Map<Long, String> mapFlowNames(List<Long> definitionIds) {
        Map<Long, String> result = new LinkedHashMap<>();
        if (definitionIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<FlowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(FlowDefinition::getId, FlowDefinition::getFlowName)
            .in(FlowDefinition::getId, definitionIds);
        for (FlowDefinition definition : flowDefinitionMapper.selectList(wrapper)) {
            result.put(definition.getId(), definition.getFlowName());
        }
        return result;
    }

    private Map<Long, FlowInstance> mapInstances(List<Long> instanceIds) {
        Map<Long, FlowInstance> result = new LinkedHashMap<>();
        if (instanceIds.isEmpty()) {
            return result;
        }
        LambdaQueryWrapper<FlowInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(FlowInstance::getId, FlowInstance::getBusinessId, FlowInstance::getActivityStatus)
            .in(FlowInstance::getId, instanceIds);
        for (FlowInstance instance : flowInstanceMapper.selectList(wrapper)) {
            result.put(instance.getId(), instance);
        }
        return result;
    }

    private List<Long> collDefinitionIds(List<FlowTask> tasks) {
        return tasks.stream().map(FlowTask::getDefinitionId).filter(Objects::nonNull).distinct().toList();
    }

    private List<Long> collInstanceIds(List<FlowTask> tasks) {
        return tasks.stream().map(FlowTask::getInstanceId).filter(Objects::nonNull).distinct().toList();
    }

    private List<Long> collTaskIds(List<FlowTask> tasks) {
        return tasks.stream().map(FlowTask::getId).filter(Objects::nonNull).distinct().toList();
    }

}
