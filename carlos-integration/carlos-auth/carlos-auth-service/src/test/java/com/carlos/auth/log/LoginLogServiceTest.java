package com.carlos.auth.log;

import com.carlos.auth.log.pojo.param.LoginLogPageParam;
import com.carlos.auth.log.pojo.vo.LoginLogDetailVO;
import com.carlos.auth.log.pojo.vo.LoginLogVO;
import com.carlos.audit.api.ApiAuditLogQuery;
import com.carlos.audit.api.pojo.ao.AuditLogMainAO;
import com.carlos.audit.api.pojo.param.ApiAuditLogQueryParam;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LoginLogService 单元测试
 *
 * <p>S4 登录日志：固定 logType=USER_LOGIN 透传审计 Feign、字段裁剪映射、查询失败抛业务异常。</p>
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
class LoginLogServiceTest {

    @Mock
    private ApiAuditLogQuery apiAuditLogQuery;

    @InjectMocks
    private LoginLogService loginLogService;

    @Test
    @DisplayName("分页：固定 USER_LOGIN 类型透传查询条件")
    void should_pass_fixed_log_type_when_page() {
        Paging<AuditLogMainAO> paging = new Paging<AuditLogMainAO>()
            .setTotal(0)
            .setCurrent(1)
            .setSize(10)
            .setRecords(List.of());
        when(apiAuditLogQuery.pageQuery(any())).thenReturn(Result.success(paging));

        LoginLogPageParam param = new LoginLogPageParam();
        param.setCurrent(1);
        param.setSize(10);
        param.setUsername("adm");
        param.setLogStatus("SUCCESS");
        param.setIp("10.0.0.1");
        param.setAuthType("PASSWORD");
        loginLogService.page(param);

        ArgumentCaptor<ApiAuditLogQueryParam> captor = ArgumentCaptor.forClass(ApiAuditLogQueryParam.class);
        verify(apiAuditLogQuery).pageQuery(captor.capture());
        assertEquals("USER_LOGIN", captor.getValue().getLogType());
        assertEquals("adm", captor.getValue().getPrincipalName());
        assertEquals("SUCCESS", captor.getValue().getLogStatus());
        assertEquals("10.0.0.1", captor.getValue().getClientIp());
        assertEquals("PASSWORD", captor.getValue().getAuthType());
    }

    @Test
    @DisplayName("分页：审计字段裁剪映射为登录日志VO")
    void should_map_audit_fields_to_vo() {
        AuditLogMainAO ao = new AuditLogMainAO();
        ao.setId(1L);
        ao.setPrincipalName("admin");
        ao.setLogStatus("SUCCESS");
        ao.setEventTime(LocalDateTime.of(2026, 9, 23, 10, 0));
        ao.setClientIp("10.0.0.1");
        ao.setLocationCountry("中国");
        ao.setLocationProvince("浙江");
        ao.setLocationCity("杭州");
        ao.setAuthType("PASSWORD");
        ao.setDurationMs(120);
        Paging<AuditLogMainAO> paging = new Paging<AuditLogMainAO>()
            .setTotal(1)
            .setCurrent(1)
            .setSize(10)
            .setRecords(List.of(ao));
        when(apiAuditLogQuery.pageQuery(any())).thenReturn(Result.success(paging));

        LoginLogPageParam param = new LoginLogPageParam();
        param.setCurrent(1);
        param.setSize(10);
        Paging<LoginLogVO> page = loginLogService.page(param);

        assertEquals(1, page.getTotal());
        LoginLogVO vo = page.getRecords().get(0);
        assertEquals("admin", vo.getUsername());
        assertEquals("SUCCESS", vo.getLogStatus());
        assertEquals("10.0.0.1", vo.getIp());
        assertEquals("中国 浙江 杭州", vo.getLocation());
        assertEquals("PASSWORD", vo.getAuthType());
        assertEquals(LocalDateTime.of(2026, 9, 23, 10, 0), vo.getLoginTime());
    }

    @Test
    @DisplayName("分页：审计查询失败时抛业务异常")
    void should_throw_when_audit_query_failed() {
        when(apiAuditLogQuery.pageQuery(any())).thenReturn(Result.error("ClickHouse 不可用"));

        LoginLogPageParam param = new LoginLogPageParam();
        param.setCurrent(1);
        param.setSize(10);
        assertThrows(BusinessException.class, () -> loginLogService.page(param));
    }

    @Test
    @DisplayName("详情：映射审计扩展字段为详情VO")
    void should_map_audit_fields_to_detail_vo() {
        AuditLogMainAO ao = new AuditLogMainAO();
        ao.setId(9L);
        ao.setPrincipalName("admin");
        ao.setPrincipalId("1001");
        ao.setPrincipalType("USER");
        ao.setLogStatus("FAIL");
        ao.setClientIp("10.0.0.1");
        ao.setClientPort(51824);
        ao.setServerIp("10.0.0.9");
        ao.setAuthType("SMS");
        ao.setAuthProvider("LOCAL");
        ao.setBizChannel("WEB");
        ao.setRiskLevel(20);
        ao.setResultCode("20201");
        ao.setDeviceFingerprint("fp-1");
        ao.setTraceId("trace-1");
        ao.setAppName("carlos-auth");
        ao.setDeptName("研发部");
        ao.setTenantId("0");
        when(apiAuditLogQuery.getById(9L)).thenReturn(Result.success(ao));

        LoginLogDetailVO detail = loginLogService.getById(9L);

        assertEquals("admin", detail.getUsername());
        assertEquals("1001", detail.getPrincipalId());
        assertEquals("USER", detail.getPrincipalType());
        assertEquals("10.0.0.1", detail.getIp());
        assertEquals(Integer.valueOf(51824), detail.getClientPort());
        assertEquals("SMS", detail.getAuthType());
        assertEquals("LOCAL", detail.getAuthProvider());
        assertEquals("WEB", detail.getBizChannel());
        assertEquals(Integer.valueOf(20), detail.getRiskLevel());
        assertEquals("20201", detail.getResultCode());
        assertEquals("trace-1", detail.getTraceId());
    }

    @Test
    @DisplayName("详情：日志不存在时抛业务异常")
    void should_throw_when_login_log_not_found() {
        when(apiAuditLogQuery.getById(404L)).thenReturn(Result.success(null));

        assertThrows(BusinessException.class, () -> loginLogService.getById(404L));
    }

}
