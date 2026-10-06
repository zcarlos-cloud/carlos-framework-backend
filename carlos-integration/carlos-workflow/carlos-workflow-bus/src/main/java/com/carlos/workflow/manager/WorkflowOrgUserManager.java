package com.carlos.workflow.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.workflow.dto.WorkflowCandidateDTO;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * 组织机构（{@code carlos-org}）用户查询封装：审批中心展示办理人姓名、提供可选办理人时要用的数据。
 * </p>
 * <p>
 * <b>为什么不查 {@code flow_user}：</b>工作流侧只保存「这个任务归谁办」这种关系
 * （{@code processed_by} 存用户 id），姓名、部门等主数据归组织机构中心。
 * 本接口是该远程依赖的唯一出口，调用方不要各自写 Feign 调用——否则一次列表渲染会散出多次远程往返。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
public interface WorkflowOrgUserManager {

    /**
     * 批量把用户标识解析成姓名
     * <p>
     * 一次远程调用取回全部用户，<b>严禁逐行调用</b>；解析不到的 id（用户被删除、传入了非数字的
     * 表达式字面量）不会出现在结果里，不是错误。
     * </p>
     *
     * @param userIds 用户标识集合（{@code flow_user.processed_by} / {@code flow_his_task.approver} 的取值）
     * @return 用户标识 → 姓名；一个都解析不到时返回空 Map
     */
    Map<String, String> mapNames(Collection<String> userIds);

    /**
     * 分页查询候选办理人
     *
     * @param deptId     部门id，可选；非空时含子部门
     * @param includeIds 限定用户集合，可选；<b>给出空集合表示「无可选人」</b>，此时不发起远程调用
     * @param excludeIds 排除用户集合，可选
     * @param current    页码
     * @param size       页大小
     * @return 候选办理人分页结果
     */
    Paging<WorkflowCandidateDTO> getCandidatePage(Long deptId, Collection<String> includeIds,
                                                  Collection<String> excludeIds, int current, int size);

}
