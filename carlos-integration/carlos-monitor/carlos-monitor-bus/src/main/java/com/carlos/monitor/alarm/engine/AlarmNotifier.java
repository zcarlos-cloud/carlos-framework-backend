package com.carlos.monitor.alarm.engine;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.response.Result;
import com.carlos.message.api.MessageSendApi;
import com.carlos.message.pojo.param.MessageSendParam;
import com.carlos.monitor.alarm.manager.impl.MonitorAlarmRuleManagerImpl;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRule;
import com.carlos.monitor.alarm.pojo.vo.NotifyTargetsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 告警通知器
 * 组装文案模板（实例名/指标/阈值/当前值/级别/时间）→ carlos-message Feign 发送；
 * 渠道按 rule.notifyChannels（SITE/MAIL/DINGTALK），message 服务不支持的渠道降级为 SITE 并记日志；
 * 失败重试 3 次后由调用方记 notify_status=FAILED；通知失败不影响状态机
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@Component
public class AlarmNotifier {

    /**
     * 告警消息模板编码（需在 carlos-message 模板管理登记；未登记时发送失败记 FAILED）
     */
    private static final String TEMPLATE_CODE = "MONITOR_ALARM";

    /**
     * message 服务支持的渠道（其余降级 SITE）
     */
    private static final List<String> SUPPORTED_CHANNELS = List.of("SITE", "MAIL", "DINGTALK");

    private static final int MAX_RETRY = 3;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectProvider<MessageSendApi> messageSendApiProvider;

    public AlarmNotifier(ObjectProvider<MessageSendApi> messageSendApiProvider) {
        this.messageSendApiProvider = messageSendApiProvider;
    }

    /**
     * 组装告警文案（触发）
     */
    public String buildContent(MonitorAlarmRule rule, String instanceName,
                               BigDecimal currentValue, LocalDateTime triggerTime) {
        return String.format("[%s] 实例「%s」指标 %s 当前值 %s，触发规则「%s」（%s %s，级别 %s），时间 %s",
            rule.getAlarmLevel(), instanceName, rule.getMetricName(),
            currentValue == null ? "-" : currentValue.stripTrailingZeros().toPlainString(),
            rule.getRuleName(), rule.getConditionOp(),
            rule.getThresholdValue().stripTrailingZeros().toPlainString(),
            rule.getAlarmLevel(), triggerTime.format(TIME_FMT));
    }

    /**
     * 组装恢复文案
     */
    public String buildRecoverContent(MonitorAlarmRule rule, String instanceName, LocalDateTime recoverTime) {
        return String.format("[%s] 实例「%s」指标 %s 已恢复正常，规则「%s」，恢复时间 %s",
            rule.getAlarmLevel(), instanceName, rule.getMetricName(), rule.getRuleName(),
            recoverTime.format(TIME_FMT));
    }

    /**
     * 发送通知（重试 3 次）
     *
     * @param rule         规则
     * @param content      文案
     * @param instanceName 实例名
     * @return true=发送成功
     */
    public boolean notify(MonitorAlarmRule rule, String content, String instanceName) {
        MessageSendApi messageSendApi = messageSendApiProvider.getIfAvailable();
        if (messageSendApi == null) {
            log.warn("消息服务不可用，告警通知降级为仅记日志: ruleId={}, instance={}", rule.getId(), instanceName);
            return false;
        }
        MessageSendParam param = buildParam(rule, content, instanceName);
        if (param.getReceivers().isEmpty()) {
            log.warn("告警规则未配置通知目标，跳过发送: ruleId={}", rule.getId());
            return false;
        }
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            try {
                Result<String> result = messageSendApi.sendAsync(param);
                if (result != null && result.isSuccess()) {
                    return true;
                }
                log.warn("告警通知发送失败（{}/{}）: ruleId={}, msg={}",
                    attempt, MAX_RETRY, rule.getId(), result == null ? "null" : result.getMsg());
            } catch (Exception e) {
                log.warn("告警通知发送异常（{}/{}）: ruleId={}, error:{}",
                    attempt, MAX_RETRY, rule.getId(), e.getMessage());
            }
        }
        return false;
    }

    private MessageSendParam buildParam(MonitorAlarmRule rule, String content, String instanceName) {
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("ruleName", rule.getRuleName());
        templateParams.put("alarmLevel", rule.getAlarmLevel());
        templateParams.put("instanceName", instanceName);
        templateParams.put("metricName", rule.getMetricName());
        templateParams.put("content", content);

        MessageSendParam param = new MessageSendParam();
        param.setTemplateCode(TEMPLATE_CODE);
        param.setTemplateParams(templateParams);
        param.setReceivers(buildReceivers(rule));
        param.setChannelCodes(buildChannels(rule));
        param.setSenderId("monitor");
        param.setSenderName("监控中心");
        param.setSenderSystem("carlos-monitor");
        param.setPriority(switch (StrUtil.nullToDefault(rule.getAlarmLevel(), "WARN")) {
            case "CRITICAL" -> 1;
            case "WARN" -> 2;
            default -> 3;
        });
        return param;
    }

    /**
     * 通知目标：userIds（type=1 用户）+ roleIds（type=3 角色）
     */
    private List<MessageSendParam.ReceiverParam> buildReceivers(MonitorAlarmRule rule) {
        NotifyTargetsVO targets = MonitorAlarmRuleManagerImpl.parseNotifyTargets(rule.getNotifyTargets());
        List<MessageSendParam.ReceiverParam> receivers = new ArrayList<>();
        if (CollUtil.isNotEmpty(targets.getUserIds())) {
            targets.getUserIds().forEach(userId -> receivers.add(receiver(String.valueOf(userId), 1)));
        }
        if (CollUtil.isNotEmpty(targets.getRoleIds())) {
            targets.getRoleIds().forEach(roleId -> receivers.add(receiver(String.valueOf(roleId), 3)));
        }
        return receivers;
    }

    private MessageSendParam.ReceiverParam receiver(String receiverId, int receiverType) {
        MessageSendParam.ReceiverParam receiver = new MessageSendParam.ReceiverParam();
        receiver.setReceiverId(receiverId);
        receiver.setReceiverType(receiverType);
        return receiver;
    }

    /**
     * 渠道映射：message 服务不支持的渠道降级为 SITE 并记日志
     */
    private List<String> buildChannels(MonitorAlarmRule rule) {
        List<String> channels = new ArrayList<>();
        for (String channel : StrUtil.nullToDefault(rule.getNotifyChannels(), "SITE").split(",")) {
            String trimmed = channel.trim().toUpperCase();
            if (SUPPORTED_CHANNELS.contains(trimmed)) {
                channels.add(trimmed);
            } else {
                log.warn("通知渠道 {} 不受支持，降级为 SITE, ruleId={}", trimmed, rule.getId());
            }
        }
        if (channels.isEmpty()) {
            channels.add("SITE");
        }
        return channels;
    }
}
