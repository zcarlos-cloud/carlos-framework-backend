package com.carlos.workflow.manager.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUser;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import com.carlos.org.api.pojo.ao.OrgUserCandidateAO;
import com.carlos.workflow.dto.WorkflowCandidateDTO;
import com.carlos.workflow.exception.WorkflowException;
import com.carlos.workflow.manager.WorkflowOrgUserManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * 组织机构用户查询封装实现：走 {@link ApiOrgUser} 的 Feign 契约，失败时按「能不能降级」分别处理。
 * </p>
 * <h2>降级口径</h2>
 * <ul>
 *   <li><b>姓名解析</b>（{@link #mapNames}）：org 不可用时返回空 Map，让列表页仍然可用
 *       （姓名列为空，但任务、节点、流程名称这些本地字段不受影响）；</li>
 *   <li><b>候选人清单</b>（{@link #getCandidatePage}）：org 不可用时直接抛错。
 *       这是常见错误做法——返回空列表会让前端显示「没有可选办理人」，
 *       使用者会以为流程配置有问题，而真实原因是远程调用失败。</li>
 * </ul>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowOrgUserManagerImpl implements WorkflowOrgUserManager {

    /**
     * org 用户契约：用 {@code ObjectProvider} 注入而不是直接注入
     * <p>
     * Feign 客户端 Bean 由宿主装配（{@code @EnableFeignClients}），本模块不能假定它一定在容器里——
     * 只读模式、单测上下文都可能没有。用 {@code @ConditionalOnBean} 也不行：组件扫描先于自动配置，
     * 条件会在 Feign 客户端注册之前求值，结果是生产环境里这个 Bean 反而不存在。
     * </p>
     */
    private final ObjectProvider<ApiOrgUser> apiOrgUserProvider;

    @Override
    public Map<String, String> mapNames(Collection<String> userIds) {
        List<Long> ids = toUserIds(userIds);
        if (ids.isEmpty()) {
            return Map.of();
        }
        ApiOrgUser apiOrgUser = apiOrgUserProvider.getIfAvailable();
        if (apiOrgUser == null) {
            log.error("未装配 org 用户契约（ApiOrgUser），无法解析办理人姓名: ids:{}", ids.size());
            return Map.of();
        }
        Result<List<OrgUserAO>> result = apiOrgUser.listUsersByIds(ids);
        if (!isSuccess(result)) {
            log.error("批量解析办理人姓名失败，本次列表的姓名列为空: ids:{}", ids.size());
            return Map.of();
        }
        Map<String, String> names = new LinkedHashMap<>();
        for (OrgUserAO user : result.getData()) {
            if (user == null || user.getId() == null) {
                continue;
            }
            names.put(String.valueOf(user.getId()), displayName(user));
        }
        return names;
    }

    @Override
    public Paging<WorkflowCandidateDTO> getCandidatePage(Long deptId, Collection<String> includeIds,
                                                         Collection<String> excludeIds, int current, int size) {
        // 「给出但为空」的 include：语义是「限定为一个空集合」，即无可选人，不去打扰 org 服务
        if (includeIds != null && includeIds.isEmpty()) {
            return emptyPaging();
        }
        if (includeIds != null && toUserIds(includeIds).isEmpty()) {
            // 给了一批标识，但没有一个能当用户 id 用（例如还没解析的表达式字面量）
            return emptyPaging();
        }
        ApiOrgUser apiOrgUser = apiOrgUserProvider.getIfAvailable();
        if (apiOrgUser == null) {
            throw new WorkflowException(CommonErrorCode.THIRD_PARTY_ERROR, "获取候选办理人失败：未装配组织机构服务契约");
        }
        List<Long> include = toUserIds(includeIds);
        List<Long> exclude = toUserIds(excludeIds);
        Result<Paging<OrgUserCandidateAO>> result = apiOrgUser.pageCandidates(deptId,
            include.isEmpty() ? null : include, exclude.isEmpty() ? null : exclude, current, size);
        if (!isSuccess(result) || result.getData() == null) {
            throw new WorkflowException(CommonErrorCode.THIRD_PARTY_ERROR, "获取候选办理人失败：组织机构服务不可用");
        }
        Paging<OrgUserCandidateAO> source = result.getData();
        Paging<WorkflowCandidateDTO> target = new Paging<>();
        target.setTotal(source.getTotal());
        target.setCurrent(source.getCurrent());
        target.setSize(source.getSize());
        target.setPages(source.getPages());
        List<WorkflowCandidateDTO> records = new ArrayList<>();
        if (source.getRecords() != null) {
            for (OrgUserCandidateAO user : source.getRecords()) {
                records.add(toCandidateDTO(user));
            }
        }
        target.setRecords(records);
        return target;
    }

    /**
     * 标识集合转用户 id：跳过 null、空白、非数字（{@code flow_user.processed_by} 里可能残留没被
     * 表达式求值替换掉的字面量），去重并保持顺序
     *
     * @param userIds 标识集合
     * @return 用户 id 列表
     */
    private List<Long> toUserIds(Collection<String> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return List.of();
        }
        return userIds.stream()
            .filter(StrUtil::isNotBlank)
            .map(String::trim)
            .map(value -> Convert.toLong(value, null))
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private String displayName(OrgUserAO user) {
        return StrUtil.blankToDefault(user.getNickname(), user.getAccount());
    }

    private WorkflowCandidateDTO toCandidateDTO(OrgUserCandidateAO user) {
        WorkflowCandidateDTO dto = new WorkflowCandidateDTO();
        dto.setUserId(user.getUserId());
        dto.setCode(user.getCode());
        dto.setName(StrUtil.blankToDefault(user.getName(), user.getCode()));
        dto.setDeptId(user.getDeptId());
        dto.setDeptName(user.getDeptName());
        return dto;
    }

    private boolean isSuccess(Result<?> result) {
        return result != null && Boolean.TRUE.equals(result.getSuccess()) && result.getData() != null;
    }

    private Paging<WorkflowCandidateDTO> emptyPaging() {
        Paging<WorkflowCandidateDTO> paging = new Paging<>();
        paging.setTotal(0);
        paging.setCurrent(1);
        paging.setSize(10);
        paging.setPages(0);
        paging.setRecords(List.of());
        return paging;
    }

}
