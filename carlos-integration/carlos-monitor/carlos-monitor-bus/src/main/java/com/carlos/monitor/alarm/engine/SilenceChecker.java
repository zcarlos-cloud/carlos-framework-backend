package com.carlos.monitor.alarm.engine;

import com.carlos.monitor.alarm.manager.MonitorAlarmSilenceManager;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmSilence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 告警静默检查器
 * 生效中静默：rule_id 且 (instance_id 为空或等于当前) AND now between start/end
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SilenceChecker {

    private final MonitorAlarmSilenceManager alarmSilenceManager;

    /**
     * 规则+实例是否处于静默期
     *
     * @param ruleId     规则ID
     * @param instanceId 实例ID
     * @return true=静默中
     */
    public boolean isSilenced(Long ruleId, Long instanceId) {
        List<MonitorAlarmSilence> silences = alarmSilenceManager.listEffective(LocalDateTime.now());
        for (MonitorAlarmSilence silence : silences) {
            if (!silence.getRuleId().equals(ruleId)) {
                continue;
            }
            if (silence.getInstanceId() == null || silence.getInstanceId().equals(instanceId)) {
                return true;
            }
        }
        return false;
    }
}
