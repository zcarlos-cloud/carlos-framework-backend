package com.carlos.monitor.alarm.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRecord;
import com.carlos.monitor.alarm.pojo.param.AlarmRecordPageParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRecordVO;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 告警记录 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
public interface MonitorAlarmRecordManager extends BaseService<MonitorAlarmRecord> {

    /**
     * 新建 FIRING 记录
     */
    boolean createFiring(MonitorAlarmRecord record);

    /**
     * 查询规则+实例的未恢复记录（FIRING/CONFIRMED，无则 null）
     */
    MonitorAlarmRecord findUnresolved(Long ruleId, Long instanceId);

    /**
     * 刷新最近触发时间（条件更新：仅未恢复状态生效，不重复通知）
     *
     * @return 是否更新成功
     */
    boolean refreshTriggerTime(Serializable id);

    /**
     * 确认（条件更新 WHERE alarm_status=FIRING → CONFIRMED，防并发覆盖）
     *
     * @param id           记录ID
     * @param confirmBy    确认人
     * @param handleRemark 处理备注
     * @return 是否迁移成功
     */
    boolean confirm(Serializable id, Long confirmBy, String handleRemark);

    /**
     * 恢复（条件更新 WHERE alarm_status IN (FIRING,CONFIRMED) → RECOVERED + recover_time）
     *
     * @return 是否迁移成功
     */
    boolean recover(Serializable id);

    /**
     * 更新通知状态（PENDING/SENT/FAILED/SILENCED）
     */
    void updateNotifyStatus(Serializable id, String notifyStatus);

    /**
     * 未恢复告警计数（FIRING+CONFIRMED，供门户角标）
     */
    long countUnresolved();

    /**
     * 分页查询（状态/级别/实例/时间过滤）
     */
    Paging<AlarmRecordVO> getPage(AlarmRecordPageParam param);

    /**
     * 最近未恢复记录列表（大盘滚动条用）
     */
    List<MonitorAlarmRecord> listRecentUnresolved(int limit);
}
