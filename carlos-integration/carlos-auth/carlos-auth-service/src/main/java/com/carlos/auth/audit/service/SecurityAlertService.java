package com.carlos.auth.audit.service;

import cn.hutool.core.util.StrUtil;
import com.carlos.auth.audit.AuditLogPublisher;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.auth.audit.convert.SecurityAlertConvert;
import com.carlos.auth.audit.manager.SecurityAlertManager;
import com.carlos.auth.audit.pojo.dto.SecurityAlertDTO;
import com.carlos.auth.audit.pojo.entity.SecurityAlert;
import com.carlos.auth.audit.pojo.enums.AlertSeverity;
import com.carlos.auth.audit.pojo.enums.AlertType;
import com.carlos.auth.config.OAuth2Properties;
import com.carlos.auth.provider.UserInfo;
import com.carlos.auth.util.AuditLogUtil;
import com.carlos.auth.util.DeviceFingerprint;
import com.carlos.auth.util.IpLocationUtil;
import com.carlos.sms.SmsUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 安全告警服务
 * </p>
 *
 * <p>处理安全告警的创建、查询、通知等功能</p>
 *
 * <p>业务逻辑服务层，通过 Manager 层进行数据获取</p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAlertService {

    private final SecurityAlertManager securityAlertManager;
    private final IpLocationUtil ipLocationUtil;
    private final DeviceFingerprint deviceFingerprint;
    private final AuditLogPublisher auditLogPublisher;
    private final AuditLogUtil auditLogUtil;
    private final SecurityAlertConvert securityAlertConvert;
    private final OAuth2Properties oAuth2Properties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 创建告警
     *
     * @param user 用户信息
     * @param alertType 告警类型
     * @param severity 告警级别
     * @param request HTTP请求
     * @return 创建的告警 DTO
     */
    public SecurityAlertDTO createAlert(UserInfo user, AlertType alertType, AlertSeverity severity, HttpServletRequest request) {
        String ip = ipLocationUtil.getClientIp(request);
        String location = ipLocationUtil.getLocation(ip);
        String userAgent = deviceFingerprint.getUserAgent(request);

        // 构建实体
        SecurityAlert alert = new SecurityAlert();
        alert.setUserId(user.getUserId());
        alert.setUsername(user.getUsername());
        alert.setAlertType(alertType.getCode());
        alert.setSeverity(severity.getCode());
        alert.setTitle(alertType.getName());
        alert.setContent(alertType.getDescription() + " (IP: " + ip + ", 位置: " + location + ")");
        alert.setIpAddress(ip);
        alert.setLocation(location);
        alert.setUserAgent(userAgent);
        alert.setHandled(false);

        // 1. 通过 Manager 层保存到本地数据库
        securityAlertManager.save(alert);

        log.warn("Security Alert: {} - {} - User: {} (IP: {}, Location: {})",
            alert.getTitle(), alert.getSeverity(), user.getUsername(), ip, location);

        // 2. 发送通知
        sendNotification(alert);

        // 3. 同时记录到统一审计服务
        recordSecurityAlertAudit(user.getUserId(), user.getUsername(), alertType.getCode(),
            severity.getCode(), alertType.getName(), alert.getContent(), request);

        // 4. 转换为 DTO 返回
        return securityAlertConvert.entityToDto(alert);
    }

    /**
     * 创建异地登录告警
     *
     * @param user 用户信息
     * @param request HTTP请求
     * @return 创建的告警 DTO
     */
    public SecurityAlertDTO createUnusualLocationAlert(UserInfo user, HttpServletRequest request) {
        String ip = ipLocationUtil.getClientIp(request);
        String location = ipLocationUtil.getLocation(ip);

        SecurityAlert alert = buildAlert(user.getUserId(), user.getUsername(),
            AlertType.UNUSUAL_LOCATION, AlertSeverity.HIGH,
            "异地登录告警",
            "用户 " + user.getUsername() + " 从异地登录 (IP: " + ip + ", 位置: " + location + ")，请确认是否为本人操作",
            ip, location, deviceFingerprint.getUserAgent(request));

        return persistAndNotify(alert, request);
    }

    /**
     * 创建新设备登录告警
     *
     * @param user 用户信息
     * @param request HTTP请求
     * @return 创建的告警 DTO
     */
    public SecurityAlertDTO createNewDeviceAlert(UserInfo user, HttpServletRequest request) {
        String ip = ipLocationUtil.getClientIp(request);
        String location = ipLocationUtil.getLocation(ip);
        String deviceDesc = deviceFingerprint.getDeviceDescription(request);

        SecurityAlert alert = buildAlert(user.getUserId(), user.getUsername(),
            AlertType.NEW_DEVICE, AlertSeverity.MEDIUM,
            "新设备登录告警",
            "用户 " + user.getUsername() + " 从新设备登录 (设备: " + deviceDesc + ", IP: " + ip + ", 位置: " + location + ")",
            ip, location, deviceFingerprint.getUserAgent(request));

        return persistAndNotify(alert, request);
    }

    /**
     * 创建暴力破解告警
     *
     * @param identifier 用户名或IP
     * @param request HTTP请求
     * @return 创建的告警 DTO
     */
    public SecurityAlertDTO createBruteForceAlert(String identifier, HttpServletRequest request) {
        String ip = ipLocationUtil.getClientIp(request);
        String location = ipLocationUtil.getLocation(ip);

        SecurityAlert alert = buildAlert(null, identifier,
            AlertType.BRUTE_FORCE, AlertSeverity.CRITICAL,
            "暴力破解告警",
            "检测到账号 " + identifier + " 可能被暴力破解 (IP: " + ip + ", 位置: " + location + ")",
            ip, location, deviceFingerprint.getUserAgent(request));

        return persistAndNotify(alert, request);
    }

    /**
     * 创建非工作时间登录告警
     *
     * @param user 用户信息
     * @param request HTTP请求
     * @return 创建的告警 DTO
     */
    public SecurityAlertDTO createOffHoursLoginAlert(UserInfo user, HttpServletRequest request) {
        String ip = ipLocationUtil.getClientIp(request);
        String location = ipLocationUtil.getLocation(ip);

        SecurityAlert alert = buildAlert(user.getUserId(), user.getUsername(),
            AlertType.OFF_HOURS_LOGIN, AlertSeverity.LOW,
            "非工作时间登录",
            "用户 " + user.getUsername() + " 在非工作时间登录 (IP: " + ip + ", 位置: " + location + ")",
            ip, location, deviceFingerprint.getUserAgent(request));

        return persistAndNotify(alert, request);
    }

    /**
     * 构建告警实体
     */
    private SecurityAlert buildAlert(Long userId, String username, AlertType alertType,
                                     AlertSeverity severity, String title, String content,
                                     String ip, String location, String userAgent) {
        SecurityAlert alert = new SecurityAlert();
        alert.setUserId(userId);
        alert.setUsername(username);
        alert.setAlertType(alertType.getCode());
        alert.setSeverity(severity.getCode());
        alert.setTitle(title);
        alert.setContent(content);
        alert.setIpAddress(ip);
        alert.setLocation(location);
        alert.setUserAgent(userAgent);
        alert.setHandled(false);
        return alert;
    }

    /**
     * 持久化告警并发送通知
     */
    private SecurityAlertDTO persistAndNotify(SecurityAlert alert, HttpServletRequest request) {
        securityAlertManager.save(alert);

        if (AlertSeverity.LOW.getCode().equals(alert.getSeverity())) {
            log.info("Security Alert: {} - {} - User: {} (IP: {}, Location: {})",
                alert.getTitle(), alert.getSeverity(), alert.getUsername(),
                alert.getIpAddress(), alert.getLocation());
        } else {
            log.warn("Security Alert: {} - {} - User: {} (IP: {}, Location: {})",
                alert.getTitle(), alert.getSeverity(), alert.getUsername(),
                alert.getIpAddress(), alert.getLocation());
        }

        sendNotification(alert);
        recordSecurityAlertAudit(alert.getUserId(), alert.getUsername(), alert.getAlertType(),
            alert.getSeverity(), alert.getTitle(), alert.getContent(), request);
        return securityAlertConvert.entityToDto(alert);
    }

    /**
     * 获取用户告警列表
     *
     * @param userId 用户ID
     * @return 告警 DTO 列表
     */
    public List<SecurityAlertDTO> getUserAlerts(Long userId) {
        // 通过 Manager 层查询数据
        List<SecurityAlert> alerts = securityAlertManager.selectByUserId(userId);
        // 转换为 DTO 列表
        return alerts.stream()
            .map(securityAlertConvert::entityToDto)
            .toList();
    }

    /**
     * 获取未处理的告警
     *
     * @return 告警 DTO 列表
     */
    public List<SecurityAlertDTO> getUnhandledAlerts() {
        // 通过 Manager 层查询数据
        List<SecurityAlert> alerts = securityAlertManager.selectUnhandled();
        // 转换为 DTO 列表
        return alerts.stream()
            .map(securityAlertConvert::entityToDto)
            .toList();
    }

    /**
     * 标记告警为已处理
     *
     * @param alertId 告警ID
     * @param remark 备注
     */
    public void handleAlert(Long alertId, String remark) {
        // 通过 Manager 层查询
        SecurityAlert alert = securityAlertManager.getById(alertId);

        if (alert == null) {
            log.warn("Alert not found: {}", alertId);
            return;
        }

        alert.setHandled(true);
        alert.setHandledTime(LocalDateTime.now());
        alert.setRemark(remark);

        // 通过 Manager 层更新
        securityAlertManager.updateById(alert);

        log.info("Alert {} handled by admin with remark: {}", alertId, remark);
    }

    /**
     * 发送告警通知
     */
    private void sendNotification(SecurityAlert alert) {
        // 发送邮件通知
        sendEmailNotification(alert);

        // 高级别告警发送短信
        if (AlertSeverity.HIGH.getCode().equals(alert.getSeverity()) ||
            AlertSeverity.CRITICAL.getCode().equals(alert.getSeverity())) {
            sendSmsNotification(alert);
        }

        // 发送钉钉Webhook通知（企业场景）
        sendDingTalkNotification(alert);
    }

    private void sendEmailNotification(SecurityAlert alert) {
        // 邮件服务未配置，记录日志
        log.info("[告警通知-邮件] 标题: {}, 内容: {}, 级别: {}",
            alert.getTitle(), alert.getContent(), alert.getSeverity());
    }

    private void sendSmsNotification(SecurityAlert alert) {
        String templateCode = oAuth2Properties.getAlertNotification().getSmsTemplateCode();
        if (StrUtil.isBlank(templateCode)) {
            log.warn("SMS alert template not configured, skip SMS notification");
            return;
        }
        try {
            // 从 alert 的 username 推断手机号（当前系统 username 即为手机号）
            String phone = alert.getUsername();
            if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
                log.debug("Cannot send SMS alert, username is not a valid phone: {}", alert.getUsername());
                return;
            }
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            params.put("title", alert.getTitle());
            params.put("content", alert.getContent());
            params.put("severity", alert.getSeverity());
            SmsUtil.sendByTemplateKey(phone, templateCode, params);
            log.info("[告警通知-短信] 已发送: 标题={}, 用户={}", alert.getTitle(), alert.getUsername());
        } catch (Exception e) {
            log.error("Failed to send SMS alert notification", e);
        }
    }

    private void sendDingTalkNotification(SecurityAlert alert) {
        String webhookUrl = oAuth2Properties.getAlertNotification().getDingtalkWebhookUrl();
        if (StrUtil.isBlank(webhookUrl)) {
            log.debug("DingTalk webhook not configured, skip notification");
            return;
        }
        try {
            Map<String, Object> markdown = new HashMap<>();
            markdown.put("title", "安全告警: " + alert.getTitle());
            markdown.put("text", String.format(
                "#### 安全告警: %s\n\n" +
                    "- **告警级别**: %s\n" +
                    "- **用户**: %s\n" +
                    "- **IP**: %s\n" +
                    "- **位置**: %s\n" +
                    "- **时间**: %s\n" +
                    "- **内容**: %s",
                alert.getTitle(), alert.getSeverity(), alert.getUsername(),
                alert.getIpAddress(), alert.getLocation(),
                alert.getCreateTime(), alert.getContent()));

            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "markdown");
            payload.put("markdown", markdown);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            log.info("[告警通知-钉钉] 已发送: 标题={}, 用户={}", alert.getTitle(), alert.getUsername());
        } catch (Exception e) {
            log.error("Failed to send DingTalk alert notification", e);
        }
    }

    /**
     * 异步记录安全告警审计日志
     */
    @Async("virtualTaskExecutor")
    public void recordSecurityAlertAudit(Long userId, String username, String alertType,
                                         String severity, String title, String content,
                                         HttpServletRequest request) {
        try {
            ApiAuditLogMainParam param = buildAuditLogParam(userId, username, alertType,
                severity, title, content, request);
            auditLogPublisher.publish(param);
            log.debug("Security alert audit log sent: user={}, alertType={}", username, alertType);
        } catch (Exception e) {
            log.error("Failed to send security alert audit log", e);
        }
    }

    /**
     * 构建审计日志参数
     */
    private ApiAuditLogMainParam buildAuditLogParam(Long userId, String username, String alertType,
                                                    String severity, String title, String content,
                                                    HttpServletRequest request) {
        ApiAuditLogMainParam param = auditLogUtil.buildBaseParam();

        // 日志分类和类型
        param.setCategory(AuditLogCategoryEnum.SECURITY);
        param.setLogType("SECURITY_ALERT_" + alertType);
        param.setOperation(title);

        // 主体信息
        param.setPrincipalId(userId != null ? String.valueOf(userId) : null);
        param.setPrincipalName(username);
        param.setPrincipalType(AuditLogPrincipalTypeEnum.USER);

        // 状态信息
        param.setLogStatus(AuditLogStatusEnum.SUCCESS);
        param.setResultMessage(content);

        // 风险等级
        param.setRiskLevel(getRiskLevel(severity));

        // IP和位置信息
        auditLogUtil.populateIpAndLocation(param, request);

        return param;
    }

    /**
     * 获取风险等级数值
     */
    private Integer getRiskLevel(String severity) {
        return switch (severity) {
            case "critical" -> 100;
            case "high" -> 75;
            case "medium" -> 50;
            case "low" -> 25;
            default -> 0;
        };
    }
}
