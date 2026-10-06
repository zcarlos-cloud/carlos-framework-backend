package com.carlos.monitor.mask.manager.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.monitor.mask.manager.MonitorDataMaskRuleManager;
import com.carlos.monitor.mask.mapper.MonitorDataMaskRuleMapper;
import com.carlos.monitor.mask.pojo.entity.MonitorDataMaskRule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 数据管理脱敏规则 查询封装实现类（规则 Caffeine 缓存 60s）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorDataMaskRuleManagerImpl extends BaseServiceImpl<MonitorDataMaskRuleMapper, MonitorDataMaskRule>
    implements MonitorDataMaskRuleManager {

    /**
     * 规则缓存 TTL（秒）
     */
    private static final long RULE_CACHE_SECONDS = 60L;

    private final Cache<String, List<MonitorDataMaskRule>> ruleCache = Caffeine.newBuilder()
        .expireAfterWrite(RULE_CACHE_SECONDS, TimeUnit.SECONDS)
        .maximumSize(1000)
        .build();

    @Override
    public List<MonitorDataMaskRule> listEffectiveRules(Long instanceId, String targetType) {
        String cacheKey = instanceId + ":" + targetType;
        List<MonitorDataMaskRule> cached = ruleCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<MonitorDataMaskRule> rules = loadRules(instanceId, targetType);
        ruleCache.put(cacheKey, rules);
        return rules;
    }

    @Override
    public void evictRuleCache() {
        ruleCache.invalidateAll();
    }

    private List<MonitorDataMaskRule> loadRules(Long instanceId, String targetType) {
        List<MonitorDataMaskRule> rules = new ArrayList<>();
        // 实例规则优先
        if (instanceId != null) {
            LambdaQueryWrapper<MonitorDataMaskRule> instanceWrapper = queryWrapper();
            instanceWrapper.eq(MonitorDataMaskRule::getInstanceId, instanceId);
            instanceWrapper.eq(MonitorDataMaskRule::getTargetType, targetType);
            instanceWrapper.eq(MonitorDataMaskRule::getEnabled, true);
            rules.addAll(getBaseMapper().selectList(instanceWrapper));
        }
        // 全局规则（instance_id IS NULL）兜底
        LambdaQueryWrapper<MonitorDataMaskRule> globalWrapper = queryWrapper();
        globalWrapper.isNull(MonitorDataMaskRule::getInstanceId);
        globalWrapper.eq(MonitorDataMaskRule::getTargetType, targetType);
        globalWrapper.eq(MonitorDataMaskRule::getEnabled, true);
        rules.addAll(getBaseMapper().selectList(globalWrapper));
        return rules;
    }
}
