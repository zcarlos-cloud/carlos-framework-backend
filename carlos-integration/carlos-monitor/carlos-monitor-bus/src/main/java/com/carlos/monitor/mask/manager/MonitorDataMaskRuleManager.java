package com.carlos.monitor.mask.manager;

import com.carlos.datasource.base.BaseService;
import com.carlos.monitor.mask.pojo.entity.MonitorDataMaskRule;

import java.util.List;

/**
 * <p>
 * 数据管理脱敏规则 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
public interface MonitorDataMaskRuleManager extends BaseService<MonitorDataMaskRule> {

    /**
     * 查询生效规则（Caffeine 缓存 60s；实例规则优先，全局规则 instance_id IS NULL 兜底）
     *
     * @param instanceId 实例ID
     * @param targetType 作用目标：DB/REDIS
     * @return 规则列表（实例规则在前，全局规则在后）
     */
    List<MonitorDataMaskRule> listEffectiveRules(Long instanceId, String targetType);

    /**
     * 失效规则缓存（规则变更时调用）
     */
    void evictRuleCache();
}
