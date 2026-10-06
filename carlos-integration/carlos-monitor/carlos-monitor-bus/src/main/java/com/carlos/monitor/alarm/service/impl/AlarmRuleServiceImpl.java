package com.carlos.monitor.alarm.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.monitor.alarm.engine.RuleMatcher;
import com.carlos.monitor.alarm.manager.MonitorAlarmRuleManager;
import com.carlos.monitor.alarm.manager.impl.MonitorAlarmRuleManagerImpl;
import com.carlos.monitor.alarm.pojo.entity.MonitorAlarmRule;
import com.carlos.monitor.alarm.pojo.param.AlarmRuleCreateParam;
import com.carlos.monitor.alarm.pojo.param.AlarmRulePageParam;
import com.carlos.monitor.alarm.pojo.param.AlarmRuleUpdateParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRuleTemplateVO;
import com.carlos.monitor.alarm.pojo.vo.AlarmRuleVO;
import com.carlos.monitor.alarm.service.AlarmRuleService;
import com.carlos.monitor.constant.MetricNameConstant;
import com.carlos.monitor.enums.MonitorErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 告警规则 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmRuleServiceImpl implements AlarmRuleService {

    private final MonitorAlarmRuleManager alarmRuleManager;

    private final RuleMatcher ruleMatcher;

    @Override
    public Long addRule(AlarmRuleCreateParam param) {
        MonitorAlarmRule rule = new MonitorAlarmRule();
        applyCreate(rule, param);
        alarmRuleManager.add(rule);
        ruleMatcher.invalidate();
        log.info("新增告警规则, id:{}, ruleName:{}", rule.getId(), rule.getRuleName());
        return rule.getId();
    }

    @Override
    public boolean updateRule(AlarmRuleUpdateParam param) {
        MonitorAlarmRule existing = alarmRuleManager.getRuleById(param.getId());
        if (existing == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("规则不存在或已删除");
        }
        MonitorAlarmRule rule = new MonitorAlarmRule();
        rule.setId(param.getId());
        applyUpdate(rule, param);
        boolean success = alarmRuleManager.modify(rule);
        if (success) {
            ruleMatcher.invalidate();
        }
        return success;
    }

    @Override
    public boolean deleteRule(Serializable id) {
        MonitorAlarmRule existing = alarmRuleManager.getRuleById(id);
        if (existing == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("规则不存在或已删除");
        }
        boolean success = alarmRuleManager.delete(id);
        if (success) {
            ruleMatcher.invalidate();
        }
        return success;
    }

    @Override
    public boolean updateEnabled(Serializable id, boolean enabled) {
        MonitorAlarmRule existing = alarmRuleManager.getRuleById(id);
        if (existing == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("规则不存在或已删除");
        }
        boolean success = alarmRuleManager.updateEnabled(id, enabled);
        if (success) {
            ruleMatcher.invalidate();
        }
        return success;
    }

    @Override
    public AlarmRuleVO getDetail(Serializable id) {
        MonitorAlarmRule rule = alarmRuleManager.getRuleById(id);
        if (rule == null) {
            throw MonitorErrorCode.MONITOR_INSTANCE_NOT_FOUND.exception("规则不存在或已删除");
        }
        AlarmRuleVO vo = new AlarmRuleVO();
        vo.setId(rule.getId());
        vo.setRuleName(rule.getRuleName());
        vo.setMetricSource(rule.getMetricSource());
        vo.setTargetType(rule.getTargetType());
        vo.setTargetId(rule.getTargetId());
        vo.setMetricName(rule.getMetricName());
        vo.setConditionOp(rule.getConditionOp());
        vo.setThresholdValue(rule.getThresholdValue());
        vo.setDurationCount(rule.getDurationCount());
        vo.setAlarmLevel(rule.getAlarmLevel());
        vo.setNotifyChannels(rule.getNotifyChannels());
        vo.setNotifyTargets(MonitorAlarmRuleManagerImpl.parseNotifyTargets(rule.getNotifyTargets()));
        vo.setEnabled(rule.getEnabled());
        vo.setRemark(rule.getRemark());
        vo.setCreateTime(rule.getCreateTime());
        vo.setUpdateTime(rule.getUpdateTime());
        return vo;
    }

    @Override
    public Paging<AlarmRuleVO> getPage(AlarmRulePageParam param) {
        return alarmRuleManager.getPage(param);
    }

    @Override
    public List<AlarmRuleTemplateVO> listTemplates() {
        return List.of(
            template("REDIS_MEMORY_HIGH", "Redis 内存使用率过高", "REDIS",
                MetricNameConstant.REDIS_MEMORY_USED_PERCENT, "GT", "85", "CRITICAL", 3,
                "内存使用率超过 85%，可能触发 maxmemory 淘汰"),
            template("REDIS_HIT_RATE_LOW", "Redis 命中率过低", "REDIS",
                MetricNameConstant.REDIS_STATS_HIT_RATE, "LT", "90", "WARN", 3,
                "keyspace 命中率低于 90%，存在大量缓存穿透"),
            template("REDIS_REPLICATION_BROKEN", "Redis 主从断链", "REDIS",
                MetricNameConstant.REDIS_REPLICATION_MASTER_LINK_STATUS, "LT", "1", "CRITICAL", 2,
                "从节点与主节点复制链路中断"),
            template("REDIS_CLIENTS_HIGH", "Redis 连接数过高", "REDIS",
                MetricNameConstant.REDIS_CLIENTS_CONNECTED, "GT", "5000", "WARN", 3,
                "客户端连接数超过 5000，接近连接上限风险"),
            template("DB_POOL_HIGH", "DB 连接池水位过高", "DB",
                MetricNameConstant.DB_POOL_ACTIVE_COUNT, "GTE", "7", "WARN", 3,
                "连接池活跃连接达到上限（默认池 8）的 85%"),
            template("INSTANCE_DOWN", "实例 DOWN", "REDIS",
                MetricNameConstant.INSTANCE_UNREACHABLE, "GTE", "1", "CRITICAL", 1,
                "实例采集连续失败不可达（Redis 类实例；DB 实例请复制后改来源为 DB）"),
            template("SERVICE_DOWN", "服务实例 DOWN", "SERVICE",
                "service.instance.up", "LT", "1", "CRITICAL", 2,
                "微服务实例健康检查失败（N7 微服务域落地后生效）"),
            template("SERVICE_ERROR_RATE_HIGH", "服务错误率过高", "SERVICE",
                "service.http.error_rate", "GT", "5", "WARN", 3,
                "HTTP 5xx 错误率超过 5%（N7 微服务域落地后生效）")
        );
    }

    private AlarmRuleTemplateVO template(String code, String name, String source, String metric,
                                         String op, String threshold, String level, int durationCount,
                                         String description) {
        AlarmRuleTemplateVO vo = new AlarmRuleTemplateVO();
        vo.setTemplateCode(code);
        vo.setRuleName(name);
        vo.setMetricSource(source);
        vo.setMetricName(metric);
        vo.setConditionOp(op);
        vo.setThresholdValue(new BigDecimal(threshold));
        vo.setAlarmLevel(level);
        vo.setDurationCount(durationCount);
        vo.setDescription(description);
        return vo;
    }

    private void applyCreate(MonitorAlarmRule rule, AlarmRuleCreateParam param) {
        rule.setRuleName(param.getRuleName());
        rule.setMetricSource(param.getMetricSource());
        rule.setTargetType(param.getTargetType());
        rule.setTargetId(param.getTargetId());
        rule.setMetricName(param.getMetricName());
        rule.setConditionOp(param.getConditionOp());
        rule.setThresholdValue(param.getThresholdValue());
        rule.setDurationCount(param.getDurationCount());
        rule.setAlarmLevel(StrUtil.nullToDefault(param.getAlarmLevel(), "WARN"));
        rule.setNotifyChannels(StrUtil.nullToDefault(param.getNotifyChannels(), "SITE"));
        rule.setNotifyTargets(buildNotifyTargets(param.getUserIds(), param.getRoleIds()));
        rule.setEnabled(true);
        rule.setRemark(param.getRemark());
    }

    private void applyUpdate(MonitorAlarmRule rule, AlarmRuleUpdateParam param) {
        rule.setRuleName(param.getRuleName());
        rule.setMetricSource(param.getMetricSource());
        rule.setTargetType(param.getTargetType());
        rule.setTargetId(param.getTargetId());
        rule.setMetricName(param.getMetricName());
        rule.setConditionOp(param.getConditionOp());
        rule.setThresholdValue(param.getThresholdValue());
        rule.setDurationCount(param.getDurationCount());
        rule.setAlarmLevel(param.getAlarmLevel());
        rule.setNotifyChannels(param.getNotifyChannels());
        rule.setNotifyTargets(buildNotifyTargets(param.getUserIds(), param.getRoleIds()));
        rule.setRemark(param.getRemark());
    }

    /**
     * notifyTargets JSON 构建：{"userIds":[],"roleIds":[]}
     */
    private String buildNotifyTargets(List<Long> userIds, List<Long> roleIds) {
        Map<String, Object> targets = new HashMap<>();
        targets.put("userIds", userIds == null ? List.of() : userIds);
        targets.put("roleIds", roleIds == null ? List.of() : roleIds);
        return JSONUtil.toJsonStr(targets);
    }
}
