package com.carlos.monitor.alarm.service.impl;

import com.carlos.core.pagination.Paging;
import com.carlos.monitor.alarm.manager.MonitorAlarmRecordManager;
import com.carlos.monitor.alarm.manager.MonitorAlarmRuleManager;
import com.carlos.monitor.alarm.manager.MonitorAlarmSilenceManager;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmSilence;
import com.carlos.monitor.alarm.pojo.param.AlarmRecordPageParam;
import com.carlos.monitor.alarm.pojo.param.AlarmSilenceCreateParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRecordVO;
import com.carlos.monitor.alarm.service.AlarmRecordService;
import com.carlos.monitor.enums.MonitorErrorCode;
import com.carlos.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * <p>
 * 告警记录 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmRecordServiceImpl implements AlarmRecordService {

    private final MonitorAlarmRecordManager alarmRecordManager;

    private final MonitorAlarmSilenceManager alarmSilenceManager;

    private final MonitorAlarmRuleManager alarmRuleManager;

    @Override
    public Paging<AlarmRecordVO> getPage(AlarmRecordPageParam param) {
        return alarmRecordManager.getPage(param);
    }

    @Override
    public void confirm(Serializable id, String handleRemark) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = alarmRecordManager.confirm(id, userId == null ? 0L : userId, handleRemark);
        if (!success) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND
                .exception("告警记录不存在或已被处理（仅触发中的记录可确认）");
        }
        log.info("确认告警, id:{}, operator:{}", id, userId);
    }

    @Override
    public Long addSilence(AlarmSilenceCreateParam param) {
        if (alarmRuleManager.getRuleById(param.getRuleId()) == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("规则不存在或已删除");
        }
        if (!param.getSilenceEnd().isAfter(param.getSilenceStart())) {
            throw MonitorErrorCode.MONITOR_INSTANCE_TYPE_UNSUPPORTED
                .exception("静默结束时间必须晚于开始时间");
        }
        MonitorAlarmSilence silence = new MonitorAlarmSilence();
        silence.setRuleId(param.getRuleId());
        silence.setInstanceId(param.getInstanceId());
        silence.setSilenceStart(param.getSilenceStart());
        silence.setSilenceEnd(param.getSilenceEnd());
        silence.setReason(param.getReason());
        alarmSilenceManager.add(silence);
        log.info("新增告警静默, id:{}, ruleId:{}, instanceId:{}", silence.getId(), param.getRuleId(), param.getInstanceId());
        return silence.getId();
    }

    @Override
    public boolean deleteSilence(Serializable id) {
        MonitorAlarmSilence existing = alarmSilenceManager.getById(id);
        if (existing == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("静默记录不存在或已删除");
        }
        return alarmSilenceManager.delete(id);
    }
}
