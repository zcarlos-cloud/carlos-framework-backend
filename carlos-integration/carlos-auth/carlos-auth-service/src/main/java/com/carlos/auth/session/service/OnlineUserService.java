package com.carlos.auth.session.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.auth.api.enums.AuthErrorCode;
import com.carlos.auth.oauth2.repository.RedisOAuth2AuthorizationService;
import com.carlos.auth.session.manager.OnlineUserManager;
import com.carlos.auth.session.pojo.dto.OnlineSessionDTO;
import com.carlos.auth.session.pojo.param.OnlineUserKickParam;
import com.carlos.auth.session.pojo.param.OnlineUserPageParam;
import com.carlos.auth.session.pojo.vo.OnlineUserVO;
import com.carlos.auth.util.AuditLogUtil;
import com.carlos.auth.audit.AuditLogPublisher;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.core.pagination.Paging;
import com.carlos.security.auth.UserContext;
import com.carlos.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 在线用户 业务
 * </p>
 *
 * <p>在线用户来自 Redis 授权存储的内存聚合（低频管理操作，量级为在线会话数，可接受）；
 * 踢下线 = 删除授权记录（{@code remove()} 内已同步清网关内省缓存与会话元数据）。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineUserService {

    private final OnlineUserManager onlineUserManager;

    private final RedisOAuth2AuthorizationService authorizationService;

    private final AuditLogPublisher auditLogPublisher;

    private final AuditLogUtil auditLogUtil;

    /**
     * 在线用户分页（用户名模糊过滤 + 登录时间倒序，内存分页）
     *
     * @param param 分页参数
     * @return 分页结果
     */
    public Paging<OnlineUserVO> page(OnlineUserPageParam param) {
        // 关键字先下推到 Redis 索引扫描（glob 子串匹配），减少全量枚举的往返
        List<OnlineSessionDTO> sessions = onlineUserManager.listOnlineSessions(param.getUsername());
        if (StrUtil.isNotBlank(param.getUsername())) {
            String keyword = param.getUsername().trim();
            sessions = sessions.stream()
                .filter(s -> s.getUsername() != null && s.getUsername().contains(keyword))
                .collect(Collectors.toList());
        }
        // 拷贝后排序：不原地修改 manager 返回的列表（不可变性原则）
        List<OnlineSessionDTO> sorted = new ArrayList<>(sessions);
        sorted.sort(Comparator.comparing(OnlineSessionDTO::getLoginTime,
            Comparator.nullsLast(Comparator.reverseOrder())));

        int from = Math.min((param.getCurrent() - 1) * param.getSize(), sorted.size());
        int to = Math.min(from + param.getSize(), sorted.size());
        List<OnlineUserVO> records = sorted.subList(from, to).stream()
            .map(this::toVO)
            .collect(Collectors.toList());

        return new Paging<OnlineUserVO>()
            .setTotal(sorted.size())
            .setCurrent(param.getCurrent())
            .setSize(param.getSize())
            .setRecords(records);
    }

    /**
     * 踢下线（按会话批量 / 按用户踢全部会话；两者同传时合并去重）
     *
     * @param param 踢下线参数
     * @return 实际踢掉的会话数
     */
    public int kick(OnlineUserKickParam param) {
        boolean noSessionIds = CollUtil.isEmpty(param.getSessionIds());
        if (noSessionIds && StrUtil.isBlank(param.getUsername())) {
            throw AuthErrorCode.AUTH_KICK_PARAM_INVALID.exception();
        }

        Set<String> sessionIds = new LinkedHashSet<>();
        if (!noSessionIds) {
            sessionIds.addAll(param.getSessionIds());
        }
        if (StrUtil.isNotBlank(param.getUsername())) {
            Set<Object> userSessions = onlineUserManager.listSessionIds(param.getUsername());
            if (userSessions != null) {
                userSessions.forEach(id -> sessionIds.add(String.valueOf(id)));
            }
        }

        int kicked = 0;
        for (String sessionId : sessionIds) {
            if (kickSession(sessionId)) {
                kicked++;
            }
        }
        if (kicked == 0) {
            throw AuthErrorCode.AUTH_ONLINE_SESSION_NOT_FOUND.exception();
        }
        log.info("踢下线完成：sessionIds={}, username={}, kicked={}", param.getSessionIds(), param.getUsername(), kicked);
        recordKickAudit(param, kicked);
        return kicked;
    }

    /**
     * 踢掉单个会话：删授权记录（内省即刻失败；remove 内同步清网关内省缓存与会话元数据）
     *
     * @return true-会话存在并已删除
     */
    private boolean kickSession(String sessionId) {
        OAuth2Authorization authorization = authorizationService.findById(sessionId);
        if (authorization == null) {
            log.debug("踢下线跳过不存在的会话: sessionId={}", sessionId);
            return false;
        }
        authorizationService.remove(authorization);
        return true;
    }

    /**
     * 踢下线管理操作审计（异步落 ClickHouse，失败不影响踢下线结果）
     */
    private void recordKickAudit(OnlineUserKickParam param, int kicked) {
        try {
            ApiAuditLogMainParam auditParam = auditLogUtil.buildBaseParam();
            auditParam.setCategory(AuditLogCategoryEnum.SECURITY);
            auditParam.setLogType("SESSION_KICK");
            auditParam.setOperation("踢用户下线");
            auditParam.setPrincipalType(AuditLogPrincipalTypeEnum.USER);
            UserContext operator = SecurityUtils.getCurrentUser();
            if (operator != null) {
                auditParam.setPrincipalId(String.valueOf(operator.getUserId()));
                auditParam.setPrincipalName(operator.getUsername());
            }
            auditParam.setTargetType("USER_SESSION");
            auditParam.setTargetId(StrUtil.isNotBlank(param.getUsername())
                ? param.getUsername() : String.join(",", sessionIdsPreview(param)));
            auditParam.setLogStatus(auditLogUtil.resolveStatus("SUCCESS"));
            auditParam.setResultMessage("踢下线 " + kicked + " 个会话");
            auditLogUtil.populateIpAndLocation(auditParam);
            auditLogPublisher.publish(auditParam);
        } catch (Exception e) {
            log.warn("踢下线审计记录失败", e);
        }
    }

    private List<String> sessionIdsPreview(OnlineUserKickParam param) {
        List<String> ids = param.getSessionIds() == null ? List.of() : param.getSessionIds();
        return ids.size() > 5 ? ids.subList(0, 5) : ids;
    }

    private OnlineUserVO toVO(OnlineSessionDTO dto) {
        return new OnlineUserVO()
            .setSessionId(dto.getSessionId())
            .setUsername(dto.getUsername())
            .setUserId(dto.getUserId())
            .setClientId(dto.getClientId())
            .setGrantType(dto.getGrantType())
            .setIp(dto.getIp())
            .setUserAgent(dto.getUserAgent())
            .setLoginTime(dto.getLoginTime())
            .setExpireTime(dto.getExpireTime());
    }

}
