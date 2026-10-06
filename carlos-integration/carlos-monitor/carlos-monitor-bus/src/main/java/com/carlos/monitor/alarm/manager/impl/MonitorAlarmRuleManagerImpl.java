package com.carlos.monitor.alarm.manager.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.monitor.alarm.manager.MonitorAlarmRuleManager;
import com.carlos.monitor.alarm.mapper.MonitorAlarmRuleMapper;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRule;
import com.carlos.monitor.alarm.pojo.param.AlarmRulePageParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRuleVO;
import com.carlos.monitor.alarm.pojo.vo.NotifyTargetsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 告警规则 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorAlarmRuleManagerImpl extends BaseServiceImpl<MonitorAlarmRuleMapper, MonitorAlarmRule>
    implements MonitorAlarmRuleManager {

    @Override
    public boolean add(MonitorAlarmRule rule) {
        boolean success = save(rule);
        if (!success) {
            log.warn("Insert 'MonitorAlarmRule' data fail, ruleName:{}", rule.getRuleName());
        }
        return success;
    }

    @Override
    public boolean modify(MonitorAlarmRule rule) {
        boolean success = updateById(rule);
        if (!success) {
            log.warn("Update 'MonitorAlarmRule' data fail, id:{}", rule.getId());
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
    public MonitorAlarmRule getRuleById(Serializable id) {
        if (id == null) {
            return null;
        }
        return getBaseMapper().selectById(id);
    }

    @Override
    public Paging<AlarmRuleVO> getPage(AlarmRulePageParam param) {
        LambdaQueryWrapper<MonitorAlarmRule> wrapper = queryWrapper();
        wrapper.like(StrUtil.isNotBlank(param.getRuleName()), MonitorAlarmRule::getRuleName, param.getRuleName());
        wrapper.eq(StrUtil.isNotBlank(param.getMetricSource()), MonitorAlarmRule::getMetricSource, param.getMetricSource());
        wrapper.eq(StrUtil.isNotBlank(param.getAlarmLevel()), MonitorAlarmRule::getAlarmLevel, param.getAlarmLevel());
        wrapper.eq(param.getEnabled() != null, MonitorAlarmRule::getEnabled, param.getEnabled());
        wrapper.orderByDesc(MonitorAlarmRule::getCreateTime);

        PageInfo<MonitorAlarmRule> page = page(pageInfo(param), wrapper);
        return MybatisPage.map(page, this::toVO);
    }

    @Override
    public List<MonitorAlarmRule> listEnabled() {
        LambdaQueryWrapper<MonitorAlarmRule> wrapper = queryWrapper();
        wrapper.eq(MonitorAlarmRule::getEnabled, true);
        return getBaseMapper().selectList(wrapper);
    }

    @Override
    public boolean updateEnabled(Serializable id, boolean enabled) {
        LambdaUpdateWrapper<MonitorAlarmRule> wrapper = updateWrapper();
        wrapper.eq(MonitorAlarmRule::getId, id);
        wrapper.set(MonitorAlarmRule::getEnabled, enabled);
        return update(wrapper);
    }

    private AlarmRuleVO toVO(MonitorAlarmRule entity) {
        AlarmRuleVO vo = new AlarmRuleVO();
        vo.setId(entity.getId());
        vo.setRuleName(entity.getRuleName());
        vo.setMetricSource(entity.getMetricSource());
        vo.setTargetType(entity.getTargetType());
        vo.setTargetId(entity.getTargetId());
        vo.setMetricName(entity.getMetricName());
        vo.setConditionOp(entity.getConditionOp());
        vo.setThresholdValue(entity.getThresholdValue());
        vo.setDurationCount(entity.getDurationCount());
        vo.setAlarmLevel(entity.getAlarmLevel());
        vo.setNotifyChannels(entity.getNotifyChannels());
        vo.setNotifyTargets(parseNotifyTargets(entity.getNotifyTargets()));
        vo.setEnabled(entity.getEnabled());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    /**
     * notify_targets JSON 转对象
     */
    public static NotifyTargetsVO parseNotifyTargets(String notifyTargets) {
        NotifyTargetsVO vo = new NotifyTargetsVO();
        if (StrUtil.isBlank(notifyTargets) || !JSONUtil.isTypeJSON(notifyTargets)) {
            return vo;
        }
        JSONObject json = JSONUtil.parseObj(notifyTargets);
        vo.setUserIds(json.getJSONArray("userIds") == null ? List.of()
            : json.getJSONArray("userIds").toList(Long.class));
        vo.setRoleIds(json.getJSONArray("roleIds") == null ? List.of()
            : json.getJSONArray("roleIds").toList(Long.class));
        return vo;
    }
}
