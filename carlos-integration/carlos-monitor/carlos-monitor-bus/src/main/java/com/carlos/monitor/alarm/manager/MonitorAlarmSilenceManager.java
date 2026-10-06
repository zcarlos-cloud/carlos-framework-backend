package com.carlos.monitor.alarm.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmSilence;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 告警静默 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
public interface MonitorAlarmSilenceManager extends BaseService<MonitorAlarmSilence> {

    /**
     * 新增静默
     */
    boolean add(MonitorAlarmSilence silence);

    /**
     * 删除静默
     */
    boolean delete(Serializable id);

    /**
     * 查询生效中静默（now between start/end）
     *
     * @param now 当前时间
     * @return 生效中静默列表
     */
    List<MonitorAlarmSilence> listEffective(LocalDateTime now);
}
