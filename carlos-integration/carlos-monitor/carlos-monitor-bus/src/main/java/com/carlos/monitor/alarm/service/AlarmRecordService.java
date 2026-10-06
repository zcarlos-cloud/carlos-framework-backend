package com.carlos.monitor.alarm.service;

import com.carlos.core.pagination.Paging;
import com.carlos.monitor.alarm.pojo.param.AlarmRecordPageParam;
import com.carlos.monitor.alarm.pojo.param.AlarmSilenceCreateParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRecordVO;

import java.io.Serializable;

/**
 * <p>
 * 告警记录 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
public interface AlarmRecordService {

    /**
     * 记录分页
     */
    Paging<AlarmRecordVO> getPage(AlarmRecordPageParam param);

    /**
     * 确认告警（FIRING → CONFIRMED，条件更新防并发覆盖）
     *
     * @param id           记录ID
     * @param handleRemark 处理备注
     */
    void confirm(Serializable id, String handleRemark);

    /**
     * 新增静默
     *
     * @return 静默ID
     */
    Long addSilence(AlarmSilenceCreateParam param);

    /**
     * 删除静默
     */
    boolean deleteSilence(Serializable id);
}
