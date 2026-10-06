package com.carlos.system.news.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.news.manager.SysNewsManager;
import com.carlos.system.news.manager.SysNewsReadManager;
import com.carlos.system.news.pojo.dto.SysNewsDTO;
import com.carlos.system.news.pojo.entity.SysNewsRead;
import com.carlos.system.news.pojo.enums.SysNewsScopeEnum;
import com.carlos.system.news.pojo.enums.SysNewsStatusEnum;
import com.carlos.system.news.pojo.enums.SysNewsTypeEnum;
import com.carlos.system.news.pojo.param.SysNewsMyPageParam;
import com.carlos.system.news.convert.SysNewsConvert;
import com.carlos.system.news.pojo.vo.SysNewsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 公告/站内通知 业务
 * </p>
 * <p>管理端 CRUD + 发布/下线；用户端「我的公告」（按发布范围过滤）+ 已读/未读数</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysNewsService {

    private final SysNewsManager newsManager;

    private final SysNewsReadManager newsReadManager;

    /**
     * 新增公告（草稿状态）
     *
     * @param dto          公告数据
     * @param scopeRoleIds 指定角色的角色标识列表（publishScope=2 时必填）
     * @return true-成功
     */
    public boolean addNews(SysNewsDTO dto, List<String> scopeRoleIds) {
        validateScope(dto.getPublishScope(), scopeRoleIds);
        dto.setId(null);
        dto.setStatus(SysNewsStatusEnum.DRAFT);
        dto.setPublishTime(null);
        dto.setIsTop(dto.getIsTop() != null && dto.getIsTop());
        dto.setScopeRoleIds(serializeScopeRoleIds(dto.getPublishScope(), scopeRoleIds));
        dto.setTenantId(currentTenantId());
        boolean success = newsManager.add(dto);
        if (success) {
            log.info("新增公告成功：id={}, title={}", dto.getId(), dto.getTitle());
        }
        return success;
    }

    /**
     * 修改公告（已发布的公告须先下线）
     *
     * @param dto          公告数据
     * @param scopeRoleIds 指定角色的角色标识列表
     * @return true-成功
     */
    public boolean updateNews(SysNewsDTO dto, List<String> scopeRoleIds) {
        SysNewsDTO existing = getExisting(dto.getId());
        if (SysNewsStatusEnum.PUBLISHED == existing.getStatus()) {
            throw SystemErrorCode.SYS_NEWS_STATUS_NOT_ALLOWED.exception();
        }
        validateScope(dto.getPublishScope(), scopeRoleIds);
        // 状态与发布时间由发布/下线动作管理，编辑不可改
        dto.setStatus(existing.getStatus());
        dto.setPublishTime(existing.getPublishTime());
        dto.setScopeRoleIds(serializeScopeRoleIds(dto.getPublishScope(), scopeRoleIds));
        boolean success = newsManager.modify(dto);
        if (success) {
            log.info("修改公告成功：id={}", dto.getId());
        }
        return success;
    }

    /**
     * 删除公告（已发布的公告须先下线）
     *
     * @param ids 公告ID集合
     * @return true-成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNews(Set<Long> ids) {
        for (Long id : ids) {
            SysNewsDTO existing = getExisting(id);
            if (SysNewsStatusEnum.PUBLISHED == existing.getStatus()) {
                throw SystemErrorCode.SYS_NEWS_STATUS_NOT_ALLOWED.exception();
            }
        }
        boolean success = newsManager.delete(ids);
        if (success) {
            // 级联物理清理已读记录（sys_news_read 无逻辑删除字段），避免孤儿数据
            newsReadManager.deleteByNewsIds(ids);
            log.info("删除公告成功：ids={}", ids);
        }
        return success;
    }

    /**
     * 发布公告（草稿/已下线 → 已发布，写入发布时间）
     *
     * @param id 公告ID
     * @return true-成功
     */
    public boolean publish(Long id) {
        SysNewsDTO existing = getExisting(id);
        if (SysNewsStatusEnum.PUBLISHED == existing.getStatus()) {
            throw SystemErrorCode.SYS_NEWS_ALREADY_PUBLISHED.exception();
        }
        if (isExpired(existing)) {
            throw SystemErrorCode.SYS_NEWS_EXPIRED.exception();
        }
        SysNewsDTO dto = new SysNewsDTO()
            .setId(id)
            .setStatus(SysNewsStatusEnum.PUBLISHED)
            .setPublishTime(LocalDateTime.now());
        boolean success = newsManager.modify(dto);
        if (success) {
            log.info("发布公告成功：id={}", id);
        }
        return success;
    }

    /**
     * 下线公告（已发布 → 已下线）
     *
     * @param id 公告ID
     * @return true-成功
     */
    public boolean offline(Long id) {
        SysNewsDTO existing = getExisting(id);
        if (SysNewsStatusEnum.PUBLISHED != existing.getStatus()) {
            throw SystemErrorCode.SYS_NEWS_NOT_PUBLISHED.exception();
        }
        SysNewsDTO dto = new SysNewsDTO()
            .setId(id)
            .setStatus(SysNewsStatusEnum.OFFLINE);
        boolean success = newsManager.modify(dto);
        if (success) {
            log.info("下线公告成功：id={}", id);
        }
        return success;
    }

    /**
     * 公告详情（管理端，不存在抛 SYS_NEWS_NOT_FOUND）
     *
     * <p>不做发布状态/可见性校验：供管理端编辑回显使用。
     * 用户端请走 {@link #myDetail(Long)}（带可见性校验）。</p>
     *
     * @param id 公告ID
     * @return 公告数据
     */
    public SysNewsDTO getDetail(Serializable id) {
        return getExisting(id);
    }

    /**
     * 我的公告详情（用户端：已发布 + 未过期 + 发布范围匹配，否则按不存在处理，避免泄露草稿存在性）
     *
     * @param id 公告ID
     * @return 公告数据
     */
    public SysNewsDTO myDetail(Long id) {
        SysNewsDTO existing = getExisting(id);
        UserContext user = currentUser();
        if (SysNewsStatusEnum.PUBLISHED != existing.getStatus() || isExpired(existing) || !visibleToUser(existing, user)) {
            throw SystemErrorCode.SYS_NEWS_NOT_FOUND.exception();
        }
        return existing;
    }

    /**
     * 我的公告分页（用户端：已发布 + 未过期 + 发布范围匹配，附已读标记）
     *
     * @param param 分页参数
     * @return 分页结果
     */
    public IPage<SysNewsVO> myPage(SysNewsMyPageParam param) {
        UserContext user = currentUser();
        List<SysNewsDTO> visible = listVisible(user);
        if (param.getNewsType() != null) {
            SysNewsTypeEnum type = SysNewsConvert.INSTANCE.parseType(param.getNewsType());
            visible = visible.stream().filter(n -> n.getNewsType() == type).collect(Collectors.toList());
        }

        List<Long> visibleIds = visible.stream().map(SysNewsDTO::getId).collect(Collectors.toList());
        Set<Long> readIds = newsReadManager.getReadNewsIds(user.getUserId(), visibleIds);
        if (Boolean.TRUE.equals(param.getOnlyUnread())) {
            visible = visible.stream().filter(n -> !readIds.contains(n.getId())).collect(Collectors.toList());
        }

        // 可见性需按角色在内存过滤，公告为低频低量数据，内存分页可接受
        int from = Math.min((param.getCurrent() - 1) * param.getSize(), visible.size());
        int to = Math.min(from + param.getSize(), visible.size());
        List<SysNewsVO> records = visible.subList(from, to).stream()
            .map(dto -> {
                SysNewsVO vo = SysNewsConvert.INSTANCE.toVO(dto);
                vo.setIsRead(readIds.contains(dto.getId()));
                return vo;
            })
            .collect(Collectors.toList());

        Page<SysNewsVO> page = new Page<>(param.getCurrent(), param.getSize(), visible.size());
        page.setRecords(records);
        return page;
    }

    /**
     * 标记已读（幂等）
     *
     * @param id 公告ID
     */
    public void read(Long id) {
        SysNewsDTO existing = getExisting(id);
        if (SysNewsStatusEnum.PUBLISHED != existing.getStatus()) {
            throw SystemErrorCode.SYS_NEWS_NOT_PUBLISHED.exception();
        }
        UserContext user = currentUser();
        // 越权标记按不存在处理，与 myDetail 口径一致
        if (!visibleToUser(existing, user)) {
            throw SystemErrorCode.SYS_NEWS_NOT_FOUND.exception();
        }
        SysNewsRead read = new SysNewsRead()
            .setNewsId(id)
            .setUserId(user.getUserId())
            .setReadTime(LocalDateTime.now())
            .setTenantId(user.getTenantId() == null ? 0L : user.getTenantId());
        newsReadManager.markRead(read);
    }

    /**
     * 未读公告数（用户端顶栏角标）
     *
     * @return 未读数量
     */
    public long unreadCount() {
        UserContext user = currentUser();
        List<SysNewsDTO> visible = listVisible(user);
        if (visible.isEmpty()) {
            return 0;
        }
        List<Long> visibleIds = visible.stream().map(SysNewsDTO::getId).collect(Collectors.toList());
        Set<Long> readIds = newsReadManager.getReadNewsIds(user.getUserId(), visibleIds);
        return visibleIds.stream().filter(id -> !readIds.contains(id)).count();
    }

    /**
     * 当前用户可见的已发布公告（全员 + 角色匹配；超管可见全部）
     */
    private List<SysNewsDTO> listVisible(UserContext user) {
        List<SysNewsDTO> candidates = newsManager.listPublishedActive();
        return candidates.stream()
            .filter(n -> visibleToUser(n, user))
            .collect(Collectors.toList());
    }

    private boolean visibleToUser(SysNewsDTO news, UserContext user) {
        if (news.getPublishScope() != SysNewsScopeEnum.ROLE) {
            return true;
        }
        if (user.isSuperAdmin()) {
            return true;
        }
        Set<String> scopeRoleCodes = parseScopeRoleIds(news.getScopeRoleIds());
        return CollUtil.isNotEmpty(user.getRoleCodes()) && scopeRoleCodes.stream().anyMatch(user.getRoleCodes()::contains);
    }

    private void validateScope(SysNewsScopeEnum publishScope, List<String> scopeRoleIds) {
        if (publishScope == null) {
            throw SystemErrorCode.SYS_NEWS_SCOPE_ROLES_EMPTY.exception();
        }
        if (SysNewsScopeEnum.ROLE == publishScope && CollUtil.isEmpty(scopeRoleIds)) {
            throw SystemErrorCode.SYS_NEWS_SCOPE_ROLES_EMPTY.exception();
        }
    }

    /**
     * 发布范围序列化：全员时清空角色列表，指定角色时序列化为 JSON 数组
     */
    private String serializeScopeRoleIds(SysNewsScopeEnum publishScope, List<String> scopeRoleIds) {
        if (SysNewsScopeEnum.ROLE != publishScope) {
            return null;
        }
        return JSONUtil.toJsonStr(scopeRoleIds);
    }

    /**
     * 解析公告的角色范围
     *
     * <p>字段名与列名里的 "Ids" 是历史命名。判定基准是当前登录人 X-User-Roles 头里的角色<b>标识</b>，
     * 而该头承载的是角色编码（org_role.role_code），因此这里按不透明字符串原样解析，不做数值转换 ——
     * 数值转换（{@code Long.valueOf}）正是此前「范围解析失败」告警与角色公告永不匹配的成因。</p>
     */
    private Set<String> parseScopeRoleIds(String scopeRoleIds) {
        if (scopeRoleIds == null || scopeRoleIds.isBlank()) {
            return Set.of();
        }
        try {
            return JSONUtil.parseArray(scopeRoleIds).stream()
                .map(Object::toString)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        } catch (Exception e) {
            log.warn("公告角色范围解析失败: {}", scopeRoleIds);
            return Set.of();
        }
    }

    private boolean isExpired(SysNewsDTO news) {
        return news.getExpireTime() != null && !news.getExpireTime().isAfter(LocalDateTime.now());
    }

    private SysNewsDTO getExisting(Serializable id) {
        SysNewsDTO existing = newsManager.getDtoById(id);
        if (existing == null) {
            throw SystemErrorCode.SYS_NEWS_NOT_FOUND.exception();
        }
        return existing;
    }

    private UserContext currentUser() {
        UserContext user = SecurityUtils.getCurrentUser();
        if (user == null || user.getUserId() == null) {
            throw SystemErrorCode.SYS_SYSTEM_ERROR.exception("未获取到登录用户");
        }
        return user;
    }

    private Long currentTenantId() {
        UserContext user = SecurityUtils.getCurrentUser();
        return user != null && user.getTenantId() != null ? user.getTenantId() : 0L;
    }

}
