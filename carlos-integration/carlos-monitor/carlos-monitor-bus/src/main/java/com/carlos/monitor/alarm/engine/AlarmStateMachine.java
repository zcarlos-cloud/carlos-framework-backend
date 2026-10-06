package com.carlos.monitor.alarm.engine;

import com.carlos.monitor.alarm.manager.MonitorAlarmRecordManager;
import com.carlos.monitor.alarm.manager.impl.MonitorAlarmRecordManagerImpl;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRecord;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRule;
import com.carlos.monitor.collect.spi.MetricPoint;
import com.carlos.monitor.config.MonitorProperties;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 告警状态机（FIRING → CONFIRMED → RECOVERED；FIRING 也可直接 RECOVERED）
 * 状态迁移全部走 Manager 层条件更新（WHERE alarm_status = 期望值）防并发覆盖；
 * 通知失败不影响状态机
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmStateMachine {

    private final MonitorAlarmRecordManager alarmRecordManager;

    private final SilenceChecker silenceChecker;

    private final AlarmNotifier alarmNotifier;

    private final MonitorProperties monitorProperties;

    /**
     * 触发：无未恢复记录 → 新建 FIRING（静默则 SILENCED 不通知）；
     * 已有 FIRING/CONFIRMED → 刷 lastTriggerTime 不重复通知（收敛）
     *
     * @param rule     命中规则
     * @param instance 实例 DTO
     * @param point    触发指标点
     */
    public void fire(MonitorAlarmRule rule, MonitorInstanceDTO instance, MetricPoint point) {
        MonitorAlarmRecord unresolved = alarmRecordManager.findUnresolved(rule.getId(), point.instanceId());
        if (unresolved != null) {
            alarmRecordManager.refreshTriggerTime(unresolved.getId());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        MonitorAlarmRecord record = new MonitorAlarmRecord();
        record.setRuleId(rule.getId());
        record.setRuleName(rule.getRuleName());
        record.setInstanceId(point.instanceId());
        record.setInstanceName(instance == null ? String.valueOf(point.instanceId()) : instance.getInstanceName());
        record.setMetricName(rule.getMetricName());
        record.setMetricValue(BigDecimal.valueOf(point.value()));
        record.setThresholdValue(rule.getThresholdValue());
        record.setAlarmLevel(rule.getAlarmLevel());
        record.setAlarmStatus(MonitorAlarmRecordManagerImpl.STATUS_FIRING);
        record.setAlarmContent(alarmNotifier.buildContent(rule, record.getInstanceName(),
            record.getMetricValue(), now));
        record.setFirstTriggerTime(now);
        record.setLastTriggerTime(now);
        record.setNotifyStatus(MonitorAlarmRecordManagerImpl.NOTIFY_PENDING);
        alarmRecordManager.createFiring(record);

        // 静默检查 → 通知
        if (silenceChecker.isSilenced(rule.getId(), point.instanceId())) {
            alarmRecordManager.updateNotifyStatus(record.getId(), MonitorAlarmRecordManagerImpl.NOTIFY_SILENCED);
            return;
        }
        boolean sent = alarmNotifier.notify(rule, record.getAlarmContent(), record.getInstanceName());
        alarmRecordManager.updateNotifyStatus(record.getId(),
            sent ? MonitorAlarmRecordManagerImpl.NOTIFY_SENT : MonitorAlarmRecordManagerImpl.NOTIFY_FAILED);
    }

    /**
     * 恢复：指标恢复正常 → RECOVERED + recoverTime + 恢复通知（recover-notify 可配）
     *
     * @param rule     规则
     * @param instance 实例 DTO
     * @param point    指标点
     */
    public void recover(MonitorAlarmRule rule, MonitorInstanceDTO instance, MetricPoint point) {
        MonitorAlarmRecord unresolved = alarmRecordManager.findUnresolved(rule.getId(), point.instanceId());
        if (unresolved == null) {
            return;
        }
        boolean recovered = alarmRecordManager.recover(unresolved.getId());
        if (!recovered) {
            return;
        }
        if (monitorProperties.getAlarm().isRecoverNotify()
            && !silenceChecker.isSilenced(rule.getId(), point.instanceId())) {
            String content = alarmNotifier.buildRecoverContent(rule, unresolved.getInstanceName(),
                LocalDateTime.now());
            alarmNotifier.notify(rule, content, unresolved.getInstanceName());
        }
    }
}
