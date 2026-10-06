package com.carlos.audit.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.audit.convert.AuditLogConfigConvert;
import com.carlos.audit.manager.AuditLogConfigManager;
import com.carlos.audit.mapper.AuditLogConfigMapper;
import com.carlos.audit.pojo.dto.AuditLogConfigDTO;
import com.carlos.audit.pojo.entity.AuditLogConfig;
import com.carlos.audit.pojo.param.AuditLogConfigPageParam;
import com.carlos.audit.pojo.vo.AuditLogConfigVO;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AuditLogConfigManagerImpl extends BaseServiceImpl<AuditLogConfigMapper, AuditLogConfig> implements AuditLogConfigManager {

    /**
     * 配置本地缓存 TTL（秒）
     */
    private static final long CONFIG_CACHE_TTL_SECONDS = 60L;

    private final LoadingCache<CacheKey, AuditLogConfigDTO> configCache = CacheBuilder.newBuilder()
        .expireAfterWrite(CONFIG_CACHE_TTL_SECONDS, TimeUnit.SECONDS)
        .build(CacheLoader.from(key -> loadEffectiveConfig(key.logType(), key.tenantId())));

    /**
     * 缓存键（避免字符串拼接导致的分隔符冲突）
     */
    private record CacheKey(String logType, String tenantId) {
    }

    @Override
    public boolean add(AuditLogConfigDTO dto) {
        AuditLogConfig entity = AuditLogConfigConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'AuditLogConfig' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        evictConfigCache(dto.getLogType(), dto.getTenantId());
        log.debug("Insert 'AuditLogConfig' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = removeById(id);
        if (!success) {
            log.warn("Remove 'AuditLogConfig' data fail, id:{}", id);
            return false;
        }
        configCache.invalidateAll();
        log.debug("Remove 'AuditLogConfig' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(AuditLogConfigDTO dto) {
        AuditLogConfig entity = AuditLogConfigConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'AuditLogConfig' data fail, entity:{}", entity);
            return false;
        }
        evictConfigCache(dto.getLogType(), dto.getTenantId());
        log.debug("Update 'AuditLogConfig' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public AuditLogConfigDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        AuditLogConfig entity = getBaseMapper().selectById(id);
        return AuditLogConfigConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<AuditLogConfigVO> getPage(AuditLogConfigPageParam param) {
        LambdaQueryWrapper<AuditLogConfig> wrapper = queryWrapper();
        wrapper.select(

            AuditLogConfig::getId,
            AuditLogConfig::getLogType,
            AuditLogConfig::getRetentionDays,
            AuditLogConfig::getSamplingRate,
            AuditLogConfig::getAsyncWrite,
            AuditLogConfig::getStoreDataChange,
            AuditLogConfig::getStoreTechnical,
            AuditLogConfig::getTenantId,
            AuditLogConfig::getCreateBy,
            AuditLogConfig::getCreateTime,
            AuditLogConfig::getUpdateBy,
            AuditLogConfig::getUpdateTime
        );
        wrapper.orderByDesc(AuditLogConfig::getCreateTime);

        wrapper.like(StrUtil.isNotBlank(param.getLogType()), AuditLogConfig::getLogType, param.getLogType());
        wrapper.eq(StrUtil.isNotBlank(param.getTenantId()), AuditLogConfig::getTenantId, param.getTenantId());
        wrapper.ge(null != param.getStart(), AuditLogConfig::getCreateTime, param.getStart());
        wrapper.le(null != param.getEnd(), AuditLogConfig::getCreateTime, param.getEnd());

        PageInfo<AuditLogConfig> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, AuditLogConfigConvert.INSTANCE::toVO);
    }

    @Override
    public AuditLogConfigDTO getEffectiveConfig(String logType, String tenantId) {
        String effectiveLogType = (logType == null || logType.isBlank()) ? "DEFAULT" : logType;
        String effectiveTenantId = (tenantId == null || tenantId.isBlank()) ? "0" : tenantId;

        CacheKey cacheKey = new CacheKey(effectiveLogType, effectiveTenantId);
        try {
            return configCache.get(cacheKey);
        } catch (ExecutionException e) {
            log.warn("Load audit config cache failed, key:{}|{}, error:{}",
                effectiveLogType, effectiveTenantId, e.getMessage(), e);
            return createDefaultConfig(effectiveLogType, effectiveTenantId);
        }
    }

    private void evictConfigCache(String logType, String tenantId) {
        String effectiveLogType = (logType == null || logType.isBlank()) ? "DEFAULT" : logType;
        String effectiveTenantId = (tenantId == null || tenantId.isBlank()) ? "0" : tenantId;
        CacheKey target = new CacheKey(effectiveLogType, effectiveTenantId);
        configCache.invalidate(target);
        // 系统级配置被修改时，所有依赖该 logType 做 fallback 的租户缓存也应失效
        if ("0".equals(effectiveTenantId)) {
            configCache.asMap().keySet().removeIf(key -> key.logType().equals(effectiveLogType));
        }
    }

    private AuditLogConfigDTO loadEffectiveConfig(String logType, String tenantId) {
        List<CacheKey> fallbackKeys = List.of(
            new CacheKey(logType, tenantId),
            new CacheKey(logType, "0"),
            new CacheKey("DEFAULT", "0")
        );
        for (CacheKey key : fallbackKeys) {
            AuditLogConfig entity = selectByKey(key);
            if (entity != null) {
                return AuditLogConfigConvert.INSTANCE.toDTO(entity);
            }
        }
        return createDefaultConfig(logType, tenantId);
    }

    private AuditLogConfig selectByKey(CacheKey key) {
        LambdaQueryWrapper<AuditLogConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuditLogConfig::getLogType, key.logType())
            .eq(AuditLogConfig::getTenantId, key.tenantId())
            .last("LIMIT 1");
        return getBaseMapper().selectOne(wrapper);
    }

    private AuditLogConfigDTO createDefaultConfig(String logType, String tenantId) {
        AuditLogConfigDTO defaultConfig = new AuditLogConfigDTO();
        defaultConfig.setLogType(logType);
        defaultConfig.setTenantId(tenantId);
        defaultConfig.setSamplingRate(BigDecimal.ONE);
        defaultConfig.setAsyncWrite(true);
        defaultConfig.setStoreDataChange(true);
        defaultConfig.setStoreTechnical(true);
        defaultConfig.setRetentionDays(90);
        return defaultConfig;
    }

}
