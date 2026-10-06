package com.carlos.auth.session.service;

import com.carlos.auth.oauth2.repository.RedisOAuth2AuthorizationService;
import com.carlos.auth.session.manager.OnlineUserManager;
import com.carlos.auth.session.pojo.dto.OnlineSessionDTO;
import com.carlos.auth.session.pojo.param.OnlineUserKickParam;
import com.carlos.auth.session.pojo.param.OnlineUserPageParam;
import com.carlos.auth.session.pojo.vo.OnlineUserVO;
import com.carlos.auth.util.AuditLogUtil;
import com.carlos.auth.audit.AuditLogPublisher;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.pagination.Paging;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OnlineUserService 单元测试
 *
 * <p>S4 在线用户：分页过滤/排序、踢下线（会话级 + 用户级 + 合并去重）、零命中抛异常。</p>
 *
 * <p>注：踢下线时的网关内省缓存清理已收口进 {@code RedisOAuth2AuthorizationService.remove()}，
 * 本类不再断言缓存删除（java-reviewer S4 HIGH#1）。</p>
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
class OnlineUserServiceTest {

    @Mock
    private OnlineUserManager onlineUserManager;

    @Mock
    private RedisOAuth2AuthorizationService authorizationService;

    @Mock
    private AuditLogPublisher auditLogPublisher;

    @Mock
    private AuditLogUtil auditLogUtil;

    @InjectMocks
    private OnlineUserService onlineUserService;

    private OnlineSessionDTO session(String sessionId, String username, LocalDateTime loginTime) {
        return new OnlineSessionDTO()
            .setSessionId(sessionId)
            .setUsername(username)
            .setLoginTime(loginTime);
    }

    @Test
    @DisplayName("分页：用户名关键字下推并按登录时间倒序")
    void should_filter_and_sort_when_page() {
        LocalDateTime now = LocalDateTime.now();
        when(onlineUserManager.listOnlineSessions("adm")).thenReturn(List.of(
            session("s1", "admin", now.minusHours(2)),
            session("s2", "admin", now)));

        OnlineUserPageParam param = new OnlineUserPageParam();
        param.setCurrent(1);
        param.setSize(10);
        param.setUsername("adm");
        Paging<OnlineUserVO> page = onlineUserService.page(param);

        assertEquals(2, page.getTotal());
        // 登录时间倒序：s2 在前
        assertEquals("s2", page.getRecords().get(0).getSessionId());
        assertEquals("s1", page.getRecords().get(1).getSessionId());
    }

    @Test
    @DisplayName("分页：无关键字时全量枚举并内存分页")
    void should_paginate_in_memory() {
        LocalDateTime now = LocalDateTime.now();
        when(onlineUserManager.listOnlineSessions(isNull())).thenReturn(List.of(
            session("s1", "a", now),
            session("s2", "a", now.minusMinutes(1)),
            session("s3", "a", now.minusMinutes(2))));

        OnlineUserPageParam param = new OnlineUserPageParam();
        param.setCurrent(2);
        param.setSize(2);
        Paging<OnlineUserVO> page = onlineUserService.page(param);

        assertEquals(3, page.getTotal());
        assertEquals(1, page.getRecords().size());
        assertEquals("s3", page.getRecords().get(0).getSessionId());
    }

    @Test
    @DisplayName("踢下线：会话ID与用户名都为空时抛异常")
    void should_throw_when_kick_param_empty() {
        assertThrows(BusinessException.class, () -> onlineUserService.kick(new OnlineUserKickParam()));
    }

    @Test
    @DisplayName("踢下线：会话存在时删除授权记录")
    void should_remove_authorization_when_kick_session() {
        OAuth2Authorization authorization = mock(OAuth2Authorization.class);
        when(authorizationService.findById("s1")).thenReturn(authorization);

        int kicked = onlineUserService.kick(new OnlineUserKickParam().setSessionIds(List.of("s1")));

        assertEquals(1, kicked);
        verify(authorizationService).remove(authorization);
    }

    @Test
    @DisplayName("踢下线：零命中（会话均已过期）时抛会话不存在异常")
    void should_throw_when_no_session_kicked() {
        when(authorizationService.findById(anyString())).thenReturn(null);

        assertThrows(BusinessException.class,
            () -> onlineUserService.kick(new OnlineUserKickParam().setSessionIds(List.of("ghost"))));
        verify(authorizationService, never()).remove(any());
    }

    @Test
    @DisplayName("踢下线：按用户名踢掉全部真实会话")
    void should_kick_all_sessions_when_username_given() {
        when(onlineUserManager.listSessionIds("admin")).thenReturn(Set.of("s1", "s2"));
        when(authorizationService.findById(anyString())).thenReturn(mock(OAuth2Authorization.class));

        int kicked = onlineUserService.kick(new OnlineUserKickParam().setUsername("admin"));

        assertEquals(2, kicked);
        verify(authorizationService, times(2)).remove(any());
    }

    @Test
    @DisplayName("踢下线：会话ID与用户名同传时合并去重")
    void should_dedup_when_both_session_ids_and_username() {
        when(onlineUserManager.listSessionIds("admin")).thenReturn(Set.of("s1", "s2"));
        when(authorizationService.findById(anyString())).thenReturn(mock(OAuth2Authorization.class));

        int kicked = onlineUserService.kick(new OnlineUserKickParam()
            .setSessionIds(List.of("s1", "s3"))
            .setUsername("admin"));

        // s1 在两侧各出现一次，去重后实际踢 s1/s2/s3 三个
        assertEquals(3, kicked);
        verify(authorizationService, times(3)).remove(any());
    }

}
