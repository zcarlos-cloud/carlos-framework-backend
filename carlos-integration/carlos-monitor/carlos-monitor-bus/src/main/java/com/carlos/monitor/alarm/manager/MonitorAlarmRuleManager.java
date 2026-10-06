package com.carlos.monitor.alarm.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRule;
import com.carlos.monitor.alarm.pojo.param.AlarmRulePageParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRuleVO;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 告警规则 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
public interface MonitorAlarmRuleManager extends BaseService<MonitorAlarmRule> {

    /**
     * 新增规则
     */
    boolean add(MonitorAlarmRule rule);

    /**
     * 修改规则
     */
    boolean modify(MonitorAlarmRule rule);

    /**
     * 逻辑删除规则
     */
    boolean delete(Serializable id);

    /**
     * 规则详情
     */
    MonitorAlarmRule getRuleById(Serializable id);

    /**
     * 分页查询
     */
    Paging<AlarmRuleVO> getPage(AlarmRulePageParam param);

    /**
     * 全部启用规则（RuleMatcher 索引用）
     */
    List<MonitorAlarmRule> listEnabled();

    /**
     * 启停规则
     */
    boolean updateEnabled(Serializable id, boolean enabled);
}
