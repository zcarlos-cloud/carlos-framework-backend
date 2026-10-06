package com.carlos.monitor.alarm.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.monitor.alarm.manager.MonitorAlarmSilenceManager;
import com.carlos.monitor.alarm.mapper.MonitorAlarmSilenceMapper;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmSilence;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 告警静默 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorAlarmSilenceManagerImpl extends BaseServiceImpl<MonitorAlarmSilenceMapper, MonitorAlarmSilence>
    implements MonitorAlarmSilenceManager {

    @Override
    public boolean add(MonitorAlarmSilence silence) {
        boolean success = save(silence);
        if (!success) {
            log.warn("Insert 'MonitorAlarmSilence' data fail, ruleId:{}", silence.getRuleId());
        }
        return success;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            return false;
        }
        return removeById(id);
    }

    @Override
    public List<MonitorAlarmSilence> listEffective(LocalDateTime now) {
        LambdaQueryWrapper<MonitorAlarmSilence> wrapper = queryWrapper();
        wrapper.le(MonitorAlarmSilence::getSilenceStart, now);
        wrapper.ge(MonitorAlarmSilence::getSilenceEnd, now);
        return getBaseMapper().selectList(wrapper);
    }
}
