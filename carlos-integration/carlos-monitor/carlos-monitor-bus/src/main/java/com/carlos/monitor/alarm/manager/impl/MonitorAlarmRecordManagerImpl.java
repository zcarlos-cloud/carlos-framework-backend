package com.carlos.monitor.alarm.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.monitor.alarm.manager.MonitorAlarmRecordManager;
import com.carlos.monitor.alarm.mapper.MonitorAlarmRecordMapper;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRecord;
import com.carlos.monitor.alarm.pojo.param.AlarmRecordPageParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 告警记录 查询封装实现类
 * 状态迁移全部走条件更新（WHERE alarm_status = 期望值）防并发覆盖（设计 3.5）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorAlarmRecordManagerImpl extends BaseServiceImpl<MonitorAlarmRecordMapper, MonitorAlarmRecord>
    implements MonitorAlarmRecordManager {

    public static final String STATUS_FIRING = "FIRING";

    public static final String STATUS_CONFIRMED = "CONFIRMED";

    public static final String STATUS_RECOVERED = "RECOVERED";

    public static final String NOTIFY_PENDING = "PENDING";

    public static final String NOTIFY_SENT = "SENT";

    public static final String NOTIFY_FAILED = "FAILED";

    public static final String NOTIFY_SILENCED = "SILENCED";

    @Override
    public boolean createFiring(MonitorAlarmRecord record) {
        boolean success = save(record);
        if (!success) {
            log.warn("Insert 'MonitorAlarmRecord' data fail, ruleId:{}", record.getRuleId());
        }
        return success;
    }

    @Override
    public MonitorAlarmRecord findUnresolved(Long ruleId, Long instanceId) {
        LambdaQueryWrapper<MonitorAlarmRecord> wrapper = queryWrapper();
        wrapper.eq(MonitorAlarmRecord::getRuleId, ruleId);
        wrapper.eq(MonitorAlarmRecord::getInstanceId, instanceId);
        wrapper.in(MonitorAlarmRecord::getAlarmStatus, STATUS_FIRING, STATUS_CONFIRMED);
        wrapper.orderByDesc(MonitorAlarmRecord::getLastTriggerTime);
        wrapper.last("LIMIT 1");
        return getBaseMapper().selectOne(wrapper);
    }

    @Override
    public boolean refreshTriggerTime(Serializable id) {
        LambdaUpdateWrapper<MonitorAlarmRecord> wrapper = updateWrapper();
        wrapper.eq(MonitorAlarmRecord::getId, id);
        wrapper.in(MonitorAlarmRecord::getAlarmStatus, STATUS_FIRING, STATUS_CONFIRMED);
        wrapper.set(MonitorAlarmRecord::getLastTriggerTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public boolean confirm(Serializable id, Long confirmBy, String handleRemark) {
        LambdaUpdateWrapper<MonitorAlarmRecord> wrapper = updateWrapper();
        wrapper.eq(MonitorAlarmRecord::getId, id);
        wrapper.eq(MonitorAlarmRecord::getAlarmStatus, STATUS_FIRING);
        wrapper.set(MonitorAlarmRecord::getAlarmStatus, STATUS_CONFIRMED);
        wrapper.set(MonitorAlarmRecord::getConfirmBy, confirmBy);
        wrapper.set(MonitorAlarmRecord::getConfirmTime, LocalDateTime.now());
        wrapper.set(StrUtil.isNotBlank(handleRemark), MonitorAlarmRecord::getHandleRemark, handleRemark);
        return update(wrapper);
    }

    @Override
    public boolean recover(Serializable id) {
        LambdaUpdateWrapper<MonitorAlarmRecord> wrapper = updateWrapper();
        wrapper.eq(MonitorAlarmRecord::getId, id);
        wrapper.in(MonitorAlarmRecord::getAlarmStatus, STATUS_FIRING, STATUS_CONFIRMED);
        wrapper.set(MonitorAlarmRecord::getAlarmStatus, STATUS_RECOVERED);
        wrapper.set(MonitorAlarmRecord::getRecoverTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public void updateNotifyStatus(Serializable id, String notifyStatus) {
        LambdaUpdateWrapper<MonitorAlarmRecord> wrapper = updateWrapper();
        wrapper.eq(MonitorAlarmRecord::getId, id);
        wrapper.set(MonitorAlarmRecord::getNotifyStatus, notifyStatus);
        update(wrapper);
    }

    @Override
    public long countUnresolved() {
        LambdaQueryWrapper<MonitorAlarmRecord> wrapper = queryWrapper();
        wrapper.in(MonitorAlarmRecord::getAlarmStatus, STATUS_FIRING, STATUS_CONFIRMED);
        return count(wrapper);
    }

    @Override
    public Paging<AlarmRecordVO> getPage(AlarmRecordPageParam param) {
        LambdaQueryWrapper<MonitorAlarmRecord> wrapper = queryWrapper();
        wrapper.eq(StrUtil.isNotBlank(param.getAlarmStatus()), MonitorAlarmRecord::getAlarmStatus, param.getAlarmStatus());
        wrapper.eq(StrUtil.isNotBlank(param.getAlarmLevel()), MonitorAlarmRecord::getAlarmLevel, param.getAlarmLevel());
        wrapper.eq(param.getInstanceId() != null, MonitorAlarmRecord::getInstanceId, param.getInstanceId());
        wrapper.eq(param.getRuleId() != null, MonitorAlarmRecord::getRuleId, param.getRuleId());
        wrapper.ge(param.getStart() != null, MonitorAlarmRecord::getLastTriggerTime, param.getStart());
        wrapper.le(param.getEnd() != null, MonitorAlarmRecord::getLastTriggerTime, param.getEnd());
        wrapper.orderByDesc(MonitorAlarmRecord::getLastTriggerTime);

        PageInfo<MonitorAlarmRecord> page = page(pageInfo(param), wrapper);
        return MybatisPage.map(page, this::toVO);
    }

    private AlarmRecordVO toVO(MonitorAlarmRecord entity) {
        AlarmRecordVO vo = new AlarmRecordVO();
        vo.setId(entity.getId());
        vo.setRuleId(entity.getRuleId());
        vo.setRuleName(entity.getRuleName());
        vo.setInstanceId(entity.getInstanceId());
        vo.setInstanceName(entity.getInstanceName());
        vo.setMetricName(entity.getMetricName());
        vo.setMetricValue(entity.getMetricValue());
        vo.setThresholdValue(entity.getThresholdValue());
        vo.setAlarmLevel(entity.getAlarmLevel());
        vo.setAlarmStatus(entity.getAlarmStatus());
        vo.setAlarmContent(entity.getAlarmContent());
        vo.setFirstTriggerTime(entity.getFirstTriggerTime());
        vo.setLastTriggerTime(entity.getLastTriggerTime());
        vo.setRecoverTime(entity.getRecoverTime());
        vo.setNotifyStatus(entity.getNotifyStatus());
        vo.setConfirmBy(entity.getConfirmBy());
        vo.setConfirmTime(entity.getConfirmTime());
        vo.setHandleRemark(entity.getHandleRemark());
        return vo;
    }

    /**
     * 最近未恢复记录列表（大盘滚动条用）
     */
    public List<MonitorAlarmRecord> listRecentUnresolved(int limit) {
        LambdaQueryWrapper<MonitorAlarmRecord> wrapper = queryWrapper();
        wrapper.in(MonitorAlarmRecord::getAlarmStatus, STATUS_FIRING, STATUS_CONFIRMED);
        wrapper.orderByDesc(MonitorAlarmRecord::getLastTriggerTime);
        wrapper.last("LIMIT " + Math.min(Math.max(limit, 1), 50));
        return getBaseMapper().selectList(wrapper);
    }
}
