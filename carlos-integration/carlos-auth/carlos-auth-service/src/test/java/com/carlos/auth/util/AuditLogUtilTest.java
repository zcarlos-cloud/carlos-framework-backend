package com.carlos.auth.util;

import com.carlos.audit.api.pojo.enums.AuditLogBizChannelEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * AuditLogUtil 单元测试
 *
 * <p>字段补全：基础参数（应用名/主机名）与客户端上下文（端口/追踪ID/设备指纹/业务渠道/客户端时间）。</p>
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
class AuditLogUtilTest {

    @Mock
    private IpLocationUtil ipLocationUtil;

    @Mock
    private Environment environment;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuditLogUtil auditLogUtil;

    @BeforeEach
    void setUp() {
        // 默认桩：未显式 stub 的请求头返回 null、地理位置查询返回 null
        lenient().when(request.getHeader(any(String.class))).thenReturn(null);
        lenient().when(ipLocationUtil.getLocation(any(String.class))).thenReturn(null);
    }

    @Test
    @DisplayName("基础参数：填充应用名与主机名")
    void should_fill_app_name_and_host_name() {
        when(environment.getProperty("spring.application.name")).thenReturn("carlos-auth");

        ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();

        assertEquals("carlos-auth", param.getAppName());
        assertNotNull(param.getHostName());
        assertNotNull(param.getServerIp());
        assertNotNull(param.getServerTime());
        assertNotNull(param.getEventTime());
        assertNotNull(param.getEventDate());
        assertEquals(Integer.valueOf(1), param.getLogSchemaVersion());
    }

    @Test
    @DisplayName("客户端上下文：填充端口/追踪ID/设备指纹/客户端时间")
    void should_fill_client_context() {
        when(ipLocationUtil.getClientIp(any())).thenReturn("192.168.1.1");
        when(ipLocationUtil.getLocation("192.168.1.1")).thenReturn("中国 浙江 杭州");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        when(request.getRemotePort()).thenReturn(51824);
        when(request.getHeader("X-Trace-Id")).thenReturn("trace-abc");
        when(request.getHeader("X-Device-Fingerprint")).thenReturn("fp-123");
        when(request.getHeader("X-Client-Time")).thenReturn("1727000000123");

        ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();
        auditLogUtil.populateIpAndLocation(param, request);

        assertEquals("192.168.1.1", param.getClientIp());
        assertEquals(Integer.valueOf(51824), param.getClientPort());
        assertEquals("trace-abc", param.getTraceId());
        assertEquals("fp-123", param.getDeviceFingerprint());
        assertEquals("中国", param.getLocationCountry());
        assertEquals("浙江", param.getLocationProvince());
        assertEquals("杭州", param.getLocationCity());
        // 桌面浏览器 UA → WEB
        assertEquals(AuditLogBizChannelEnum.WEB, param.getBizChannel());
        // epoch 毫秒 → 客户端时间
        assertNotNull(param.getClientTime());
        assertEquals(1727000000123L, param.getClientTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    @Test
    @DisplayName("业务渠道：优先 X-Biz-Channel 头，忽略非法值后回退 UA 推断")
    void should_resolve_biz_channel() {
        when(ipLocationUtil.getClientIp(any())).thenReturn("10.0.0.1");
        lenient().when(ipLocationUtil.getLocation("10.0.0.1")).thenReturn(null);

        // 合法头
        ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();
        when(request.getHeader("X-Biz-Channel")).thenReturn("APP");
        auditLogUtil.populateIpAndLocation(param, request);
        assertEquals(AuditLogBizChannelEnum.APP, param.getBizChannel());

        // 非法头回退 UA：移动端 → H5
        ApiAuditLogMainParam param2 = auditLogUtil.buildBaseParam();
        when(request.getHeader("X-Biz-Channel")).thenReturn("NOT_A_CHANNEL");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0)");
        auditLogUtil.populateIpAndLocation(param2, request);
        assertEquals(AuditLogBizChannelEnum.H5, param2.getBizChannel());

        // 微信 → 小程序
        ApiAuditLogMainParam param3 = auditLogUtil.buildBaseParam();
        when(request.getHeader("X-Biz-Channel")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (MicroMessenger/8.0.40)");
        auditLogUtil.populateIpAndLocation(param3, request);
        assertEquals(AuditLogBizChannelEnum.MINI_PROGRAM, param3.getBizChannel());
    }

    @Test
    @DisplayName("客户端时间：支持 ISO-8601，非法值留空")
    void should_parse_client_time() {
        when(ipLocationUtil.getClientIp(any())).thenReturn("10.0.0.1");
        lenient().when(ipLocationUtil.getLocation("10.0.0.1")).thenReturn(null);

        ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();
        when(request.getHeader("X-Client-Time")).thenReturn("2026-09-24T10:15:30");
        auditLogUtil.populateIpAndLocation(param, request);
        assertEquals(LocalDateTime.of(2026, 9, 24, 10, 15, 30), param.getClientTime());

        ApiAuditLogMainParam param2 = auditLogUtil.buildBaseParam();
        when(request.getHeader("X-Client-Time")).thenReturn("not-a-time");
        auditLogUtil.populateIpAndLocation(param2, request);
        assertNull(param2.getClientTime());
    }

    @Test
    @DisplayName("请求为空时跳过客户端上下文填充")
    void should_skip_when_request_null() {
        ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();
        auditLogUtil.populateIpAndLocation(param, null);
        assertNull(param.getClientIp());
        assertNull(param.getClientPort());
    }
}
