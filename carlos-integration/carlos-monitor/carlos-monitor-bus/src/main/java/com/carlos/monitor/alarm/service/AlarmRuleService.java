package com.carlos.monitor.alarm.service;

import com.carlos.core.pagination.Paging;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRule;
import com.carlos.monitor.alarm.pojo.param.AlarmRuleCreateParam;
import com.carlos.monitor.alarm.pojo.param.AlarmRulePageParam;
import com.carlos.monitor.alarm.pojo.param.AlarmRuleUpdateParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRuleTemplateVO;
import com.carlos.monitor.alarm.pojo.vo.AlarmRuleVO;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 告警规则 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
public interface AlarmRuleService {

    /**
     * 新增规则（规则缓存失效）
     */
    Long addRule(AlarmRuleCreateParam param);

    /**
     * 修改规则（规则缓存失效）
     */
    boolean updateRule(AlarmRuleUpdateParam param);

    /**
     * 删除规则（规则缓存失效）
     */
    boolean deleteRule(Serializable id);

    /**
     * 启停规则（规则缓存失效）
     */
    boolean updateEnabled(Serializable id, boolean enabled);

    /**
     * 规则详情
     */
    AlarmRuleVO getDetail(Serializable id);

    /**
     * 规则分页
     */
    Paging<AlarmRuleVO> getPage(AlarmRulePageParam param);

    /**
     * 内置规则模板清单（供前端一键选用）
     */
    List<AlarmRuleTemplateVO> listTemplates();
}
