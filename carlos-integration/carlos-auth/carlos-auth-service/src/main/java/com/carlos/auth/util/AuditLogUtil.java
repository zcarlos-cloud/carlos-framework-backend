package com.carlos.auth.util;

import com.carlos.audit.api.pojo.enums.AuditLogBizChannelEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * <p>
 * 审计日志工具类
 * </p>
 *
 * <p>统一封装审计日志参数构建逻辑，消除各服务中的重复代码。</p>
 * <p>carlos-auth 模块不存储审计日志，所有审计数据通过 carlos-audit-api 提供的 Feign 接口发送到 carlos-audit 模块处理。</p>
 *
 * <p>支持的功能：</p>
 * <ul>
 *   <li>基础参数构建（时间、Schema版本、服务器IP、应用名、主机名）</li>
 *   <li>IP与地理位置信息填充</li>
 *   <li>从当前请求上下文自动提取客户端信息（端口、追踪ID、设备指纹、业务渠道、客户端时间）</li>
 * </ul>
 *
 * @author carlos
 * @date 2026-05-29
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogUtil {

    private final IpLocationUtil ipLocationUtil;

    private final Environment environment;

    /**
     * 构建审计日志基础参数
     *
     * <p>自动填充以下通用字段：</p>
     * <ul>
     *   <li>serverTime / eventTime - 当前服务器时间</li>
     *   <li>eventDate - 当前日期</li>
     *   <li>logSchemaVersion - Schema 版本（固定为 1）</li>
     *   <li>serverIp / hostName - 本机服务器 IP 与主机名</li>
     *   <li>appName - 应用名（spring.application.name）</li>
     * </ul>
     *
     * @return 包含基础信息的审计日志参数对象
     */
    public ApiAuditLogMainParam buildBaseParam() {
        ApiAuditLogMainParam param = new ApiAuditLogMainParam();

        LocalDateTime now = LocalDateTime.now();
        param.setServerTime(now);
        param.setEventTime(now);
        param.setEventDate(LocalDate.now());
        param.setLogSchemaVersion(1);

        populateServerInfo(param);
        param.setAppName(environment.getProperty("spring.application.name"));

        return param;
    }

    /**
     * 填充 IP 和地理位置信息（从当前请求上下文自动获取）
     *
     * <p>从 {@link RequestContextHolder} 获取当前 HTTP 请求，提取客户端 IP、User-Agent 和地理位置信息。</p>
     * <p>如果当前线程无请求上下文（如异步任务中），则跳过填充。</p>
     *
     * @param param 审计日志参数对象
     */
    public void populateIpAndLocation(ApiAuditLogMainParam param) {
        HttpServletRequest request = getCurrentRequest();
        populateIpAndLocation(param, request);
    }

    /**
     * 填充客户端上下文信息（使用指定的 HTTP 请求）
     *
     * <p>除 IP / User-Agent / 地理位置外，还采集：</p>
     * <ul>
     *   <li>clientPort - 客户端端口（request.getRemotePort）</li>
     *   <li>traceId - 网关透传的 X-Trace-Id 头</li>
     *   <li>deviceFingerprint - X-Device-Fingerprint / X-Device-Id 头</li>
     *   <li>bizChannel - X-Biz-Channel 头；缺省时按 User-Agent 启发（微信→小程序、移动端→H5、其余→WEB）</li>
     *   <li>clientTime - X-Client-Time 头（epoch 毫秒或 ISO-8601）</li>
     * </ul>
     *
     * @param param   审计日志参数对象
     * @param request HTTP 请求对象，为 null 时跳过填充
     */
    public void populateIpAndLocation(ApiAuditLogMainParam param, HttpServletRequest request) {
        if (request == null) {
            return;
        }

        String clientIp = ipLocationUtil.getClientIp(request);
        param.setClientIp(clientIp);
        param.setUserAgent(request.getHeader("User-Agent"));
        param.setClientPort(request.getRemotePort());
        param.setTraceId(request.getHeader("X-Trace-Id"));
        param.setDeviceFingerprint(resolveDeviceFingerprint(request));
        param.setBizChannel(resolveBizChannel(request));
        param.setClientTime(parseClientTime(request.getHeader("X-Client-Time")));

        try {
            String location = ipLocationUtil.getLocation(clientIp);
            if (location != null && !location.isEmpty()) {
                String[] parts = location.split(" ");
                if (parts.length >= 1) {
                    param.setLocationCountry(parts[0]);
                }
                if (parts.length >= 2) {
                    param.setLocationProvince(parts[1]);
                }
                if (parts.length >= 3) {
                    param.setLocationCity(parts[2]);
                }
            }
        } catch (Exception e) {
            log.debug("获取 IP 地理位置失败: {}", clientIp);
        }
    }

    /**
     * 解析设备指纹：优先 X-Device-Fingerprint，回退 X-Device-Id
     */
    private String resolveDeviceFingerprint(HttpServletRequest request) {
        String fingerprint = request.getHeader("X-Device-Fingerprint");
        if (fingerprint == null || fingerprint.isBlank()) {
            fingerprint = request.getHeader("X-Device-Id");
        }
        return fingerprint;
    }

    /**
     * 解析业务渠道：优先 X-Biz-Channel 头（须为 AuditLogBizChannelEnum 枚举名），
     * 缺省时按 User-Agent 启发式推断
     */
    private AuditLogBizChannelEnum resolveBizChannel(HttpServletRequest request) {
        String header = request.getHeader("X-Biz-Channel");
        if (header != null && !header.isBlank()) {
            try {
                return AuditLogBizChannelEnum.valueOf(header.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.debug("未知的业务渠道头: {}", header);
            }
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        String lower = userAgent.toLowerCase();
        if (lower.contains("micromessenger")) {
            return AuditLogBizChannelEnum.MINI_PROGRAM;
        }
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) {
            return AuditLogBizChannelEnum.H5;
        }
        return AuditLogBizChannelEnum.WEB;
    }

    /**
     * 解析客户端时间（X-Client-Time 头），支持 epoch 毫秒与 ISO-8601 两种格式
     */
    private LocalDateTime parseClientTime(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            long epochMillis = Long.parseLong(header.trim());
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        } catch (NumberFormatException ignored) {
            // 非纯数字，尝试 ISO-8601
        }
        try {
            return LocalDateTime.parse(header.trim());
        } catch (Exception e) {
            log.debug("无法解析客户端时间头: {}", header);
            return null;
        }
    }

    /**
     * 填充服务器信息
     *
     * @param param 审计日志参数对象
     */
    public void populateServerInfo(ApiAuditLogMainParam param) {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            param.setServerIp(localHost.getHostAddress());
            param.setHostName(localHost.getHostName());
        } catch (Exception e) {
            log.debug("获取服务器 IP 失败");
        }
    }

    /**
     * 解析状态字符串为枚举
     *
     * @param status 状态字符串，如 SUCCESS、FAILURE
     * @return 对应的审计日志状态枚举
     */
    public AuditLogStatusEnum resolveStatus(String status) {
        return "SUCCESS".equals(status) ? AuditLogStatusEnum.SUCCESS : AuditLogStatusEnum.FAIL;
    }

    /**
     * 获取当前线程的 HTTP 请求对象
     *
     * @return 当前 HTTP 请求，若不在请求上下文中则返回 null
     */
    private HttpServletRequest getCurrentRequest() {
        return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
            .filter(ServletRequestAttributes.class::isInstance)
            .map(ServletRequestAttributes.class::cast)
            .map(ServletRequestAttributes::getRequest)
            .orElse(null);
    }
}
