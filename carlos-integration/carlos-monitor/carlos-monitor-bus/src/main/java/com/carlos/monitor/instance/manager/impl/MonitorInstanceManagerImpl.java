package com.carlos.monitor.instance.manager.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.monitor.constant.CacheKeyConstant;
import com.carlos.monitor.instance.connection.InstanceConnectionInvalidEvent;
import com.carlos.monitor.instance.convert.MonitorInstanceConvert;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.mapper.MonitorInstanceMapper;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.instance.pojo.entity.MonitorInstance;
import com.carlos.monitor.instance.pojo.param.InstancePageParam;
import com.carlos.monitor.instance.pojo.param.InstanceStatusParam;
import com.carlos.monitor.instance.pojo.vo.InstanceVO;
import com.carlos.monitor.util.MonitorCacheUtil;
import com.carlos.redis.caffeine.CaffeineUtil;
import com.carlos.redis.util.RedisUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 监控实例 查询封装实现类
 * 实例详情/分页查询使用 Caffeine(L1) + Redis(L2) 二级缓存；写操作失效缓存，
 * 连接配置变更（addresses/username/passwordCipher/dbName/dbIndex/extConfig 任一变化）
 * 额外发布 {@link InstanceConnectionInvalidEvent}，由连接注册中心失效并关闭连接
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MonitorInstanceManagerImpl extends BaseServiceImpl<MonitorInstanceMapper, MonitorInstance>
    implements MonitorInstanceManager {

    /**
     * 实例详情缓存 TTL（秒）
     */
    private static final long DETAIL_CACHE_TTL_SECONDS = 300L;

    /**
     * 分页缓存 TTL（秒）
     */
    private static final long PAGE_CACHE_TTL_SECONDS = 60L;

    /**
     * 健康回写节流窗口（毫秒）：同实例同状态 10s 内只写一次库（设计 2.1 要点 4）
     */
    private static final long HEALTH_WRITE_THROTTLE_MS = 10_000L;

    /**
     * 健康回写节流缓存：instanceId -> 最近回写时间/状态
     */
    private final Cache<String, Long> healthWriteCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();

    private final Cache<String, String> healthStatusCache = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build();

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public boolean add(MonitorInstanceDTO dto) {
        MonitorInstance entity = MonitorInstanceConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'MonitorInstance' data fail, instanceName:{}", dto.getInstanceName());
            return false;
        }
        dto.setId(entity.getId());
        bumpPageVersion();
        log.debug("Insert 'MonitorInstance' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean modify(MonitorInstanceDTO dto) {
        MonitorInstance old = getBaseMapper().selectById(dto.getId());
        if (old == null) {
            log.warn("Update 'MonitorInstance' but not found, id:{}", dto.getId());
            return false;
        }
        MonitorInstance entity = MonitorInstanceConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'MonitorInstance' data fail, id:{}", dto.getId());
            return false;
        }
        evictDetailCache(dto.getId());
        bumpPageVersion();
        // 连接配置变更：发布连接失效事件，注册中心关闭并剔除缓存连接
        if (connectionChanged(old, entity)) {
            eventPublisher.publishEvent(new InstanceConnectionInvalidEvent(this, dto.getId(), "CONNECTION_CHANGED"));
        }
        log.debug("Update 'MonitorInstance' data by id:{}", dto.getId());
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
            log.warn("Remove 'MonitorInstance' data fail, id:{}", id);
            return false;
        }
        evictDetailCache(id);
        bumpPageVersion();
        // 逻辑删除 + 连接失效 + 停采（采集域监听同事件，后续里程碑接入）
        Long instanceId = Long.valueOf(id.toString());
        eventPublisher.publishEvent(new InstanceConnectionInvalidEvent(this, instanceId, "INSTANCE_DELETED"));
        log.debug("Remove 'MonitorInstance' data by id:{}", id);
        return true;
    }

    @Override
    public MonitorInstanceDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        String key = CacheKeyConstant.instanceKey(id);
        // L1 Caffeine
        Object cached = CaffeineUtil.get(key);
        if (cached instanceof MonitorInstanceDTO dto) {
            return dto;
        }
        // L2 Redis（MonitorCacheUtil 兼容各种序列化器形态，解析失败按未命中回源）
        MonitorInstanceDTO dto = MonitorCacheUtil.getObject(key, MonitorInstanceDTO.class);
        if (dto == null) {
            MonitorInstance entity = getBaseMapper().selectById(id);
            dto = MonitorInstanceConvert.INSTANCE.toDTO(entity);
            if (dto != null) {
                MonitorCacheUtil.setObject(key, dto, DETAIL_CACHE_TTL_SECONDS);
            }
        }
        if (dto != null) {
            CaffeineUtil.put(key, dto);
        }
        return dto;
    }

    @Override
    public Paging<InstanceVO> getPage(InstancePageParam param) {
        String key = CacheKeyConstant.instancePageKey(pageVersion(), Objects.hash(
            param.getCurrent(), param.getSize(), param.getInstanceName(), param.getInstanceType(),
            param.getEnv(), param.getGroupId(), param.getHealthStatus(), param.getKeyword()));
        // L1 Caffeine
        Object cached = CaffeineUtil.get(key);
        if (cached instanceof Paging<?> paging) {
            return (Paging<InstanceVO>) paging;
        }
        // L2 Redis
        Paging<InstanceVO> paging = MonitorCacheUtil.getObject(key, new TypeReference<Paging<InstanceVO>>() {
        });
        if (paging == null) {
            paging = doPageQuery(param);
            MonitorCacheUtil.setObject(key, paging, PAGE_CACHE_TTL_SECONDS);
        }
        CaffeineUtil.put(key, paging);
        return paging;
    }

    private Paging<InstanceVO> doPageQuery(InstancePageParam param) {
        LambdaQueryWrapper<MonitorInstance> wrapper = queryWrapper();
        wrapper.like(StrUtil.isNotBlank(param.getInstanceName()), MonitorInstance::getInstanceName, param.getInstanceName());
        wrapper.eq(param.getInstanceType() != null, MonitorInstance::getInstanceType, param.getInstanceType());
        wrapper.eq(param.getEnv() != null, MonitorInstance::getEnv, param.getEnv());
        wrapper.eq(param.getGroupId() != null, MonitorInstance::getGroupId, param.getGroupId());
        wrapper.eq(StrUtil.isNotBlank(param.getHealthStatus()), MonitorInstance::getHealthStatus, param.getHealthStatus());
        wrapper.and(StrUtil.isNotBlank(param.getKeyword()), w -> w
            .like(MonitorInstance::getInstanceName, param.getKeyword())
            .or()
            .like(MonitorInstance::getAddresses, param.getKeyword()));
        wrapper.orderByDesc(MonitorInstance::getCreateTime);

        PageInfo<MonitorInstance> page = page(pageInfo(param), wrapper);
        return MybatisPage.map(page, entity -> {
            MonitorInstanceDTO dto = MonitorInstanceConvert.INSTANCE.toDTO(entity);
            InstanceVO vo = MonitorInstanceConvert.INSTANCE.toVO(dto);
            vo.setHasPassword(StrUtil.isNotBlank(entity.getPasswordCipher()));
            return vo;
        });
    }

    @Override
    public boolean updateStatus(Serializable id, InstanceStatusParam param) {
        if (id == null || param == null) {
            log.warn("id or param is null");
            return false;
        }
        LambdaUpdateWrapper<MonitorInstance> wrapper = updateWrapper();
        wrapper.eq(MonitorInstance::getId, id);
        wrapper.set(param.getCollectEnabled() != null, MonitorInstance::getCollectEnabled, param.getCollectEnabled());
        wrapper.set(param.getManageEnabled() != null, MonitorInstance::getManageEnabled, param.getManageEnabled());
        wrapper.set(param.getSqlWriteEnabled() != null, MonitorInstance::getSqlWriteEnabled, param.getSqlWriteEnabled());
        boolean success = update(wrapper);
        if (success) {
            evictDetailCache(id);
            bumpPageVersion();
            // 关闭采集等同停采信号，复用连接失效事件让注册中心/采集域感知
            if (Boolean.FALSE.equals(param.getCollectEnabled())) {
                Long instanceId = Long.valueOf(id.toString());
                eventPublisher.publishEvent(new InstanceConnectionInvalidEvent(this, instanceId, "COLLECT_DISABLED"));
            }
        }
        return success;
    }

    @Override
    public List<MonitorInstanceDTO> listDtosByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<MonitorInstance> entities = getBaseMapper().selectBatchIds(ids);
        return MonitorInstanceConvert.INSTANCE.toDTO(entities);
    }

    @Override
    public MonitorInstanceDTO getByNameEnv(String instanceName, String env) {
        LambdaQueryWrapper<MonitorInstance> wrapper = queryWrapper();
        wrapper.eq(MonitorInstance::getInstanceName, instanceName);
        wrapper.eq(MonitorInstance::getEnv, env);
        wrapper.last("LIMIT 1");
        return MonitorInstanceConvert.INSTANCE.toDTO(getBaseMapper().selectOne(wrapper));
    }

    @Override
    public long countByNameEnv(String instanceName, String env, Serializable excludeId) {
        LambdaQueryWrapper<MonitorInstance> wrapper = queryWrapper();
        wrapper.eq(MonitorInstance::getInstanceName, instanceName);
        wrapper.eq(MonitorInstance::getEnv, env);
        wrapper.ne(ObjUtil.isNotEmpty(excludeId), MonitorInstance::getId, excludeId);
        return count(wrapper);
    }

    @Override
    public List<MonitorInstanceDTO> listCollectEnabled() {
        LambdaQueryWrapper<MonitorInstance> wrapper = queryWrapper();
        wrapper.select(MonitorInstance::getId, MonitorInstance::getInstanceType,
            MonitorInstance::getAddresses, MonitorInstance::getDbName, MonitorInstance::getDbIndex,
            MonitorInstance::getUsername, MonitorInstance::getPasswordCipher, MonitorInstance::getExtConfig,
            MonitorInstance::getCollectEnabled, MonitorInstance::getCollectInterval,
            MonitorInstance::getConnectTimeout, MonitorInstance::getSocketTimeout,
            MonitorInstance::getHealthStatus, MonitorInstance::getLastCollectTime);
        wrapper.eq(MonitorInstance::getCollectEnabled, true);
        List<MonitorInstance> entities = getBaseMapper().selectList(wrapper);
        return MonitorInstanceConvert.INSTANCE.toDTO(entities);
    }

    @Override
    public List<MonitorInstanceDTO> listAllInstances() {
        LambdaQueryWrapper<MonitorInstance> wrapper = queryWrapper();
        wrapper.select(MonitorInstance::getId, MonitorInstance::getInstanceName,
            MonitorInstance::getInstanceType, MonitorInstance::getEnv, MonitorInstance::getGroupId,
            MonitorInstance::getHealthStatus, MonitorInstance::getLastCollectTime,
            MonitorInstance::getCollectInterval, MonitorInstance::getCollectEnabled);
        List<MonitorInstance> entities = getBaseMapper().selectList(wrapper);
        return MonitorInstanceConvert.INSTANCE.toDTO(entities);
    }

    @Override
    public void updateHealth(Serializable id, String healthStatus, LocalDateTime lastCollectTime) {
        if (id == null || StrUtil.isBlank(healthStatus)) {
            return;
        }
        // Caffeine 合并节流：同实例同状态 10s 内只写一次库；状态翻转立即写
        if (!shouldWriteHealth(id, healthStatus)) {
            return;
        }
        LambdaUpdateWrapper<MonitorInstance> wrapper = updateWrapper();
        wrapper.eq(MonitorInstance::getId, id);
        wrapper.set(MonitorInstance::getHealthStatus, healthStatus);
        wrapper.set(lastCollectTime != null, MonitorInstance::getLastCollectTime, lastCollectTime);
        boolean success = update(wrapper);
        if (success) {
            evictDetailCache(id);
        }
    }

    /**
     * 健康回写节流判定：Caffeine 记录实例最近一次回写时间与状态，
     * 同状态 10s 内跳过；不同状态（翻转）立即放行
     */
    private boolean shouldWriteHealth(Serializable id, String healthStatus) {
        long now = System.currentTimeMillis();
        Long lastWrite = healthWriteCache.getIfPresent(id.toString());
        if (lastWrite != null && now - lastWrite < HEALTH_WRITE_THROTTLE_MS) {
            Object lastStatus = healthStatusCache.getIfPresent(id.toString());
            if (healthStatus.equals(lastStatus)) {
                return false;
            }
        }
        healthWriteCache.put(id.toString(), now);
        healthStatusCache.put(id.toString(), healthStatus);
        return true;
    }

    /**
     * 连接配置字段是否发生变化
     */
    private boolean connectionChanged(MonitorInstance oldEntity, MonitorInstance newEntity) {
        return !Objects.equals(oldEntity.getAddresses(), newEntity.getAddresses())
            || !Objects.equals(oldEntity.getUsername(), newEntity.getUsername())
            || !Objects.equals(oldEntity.getPasswordCipher(), newEntity.getPasswordCipher())
            || !Objects.equals(oldEntity.getDbName(), newEntity.getDbName())
            || !Objects.equals(oldEntity.getDbIndex(), newEntity.getDbIndex())
            || !Objects.equals(oldEntity.getExtConfig(), newEntity.getExtConfig());
    }

    private void evictDetailCache(Serializable id) {
        CaffeineUtil.evict(CacheKeyConstant.instanceKey(id));
        RedisUtil.delete(CacheKeyConstant.instanceKey(id));
    }

    /**
     * 分页缓存版本号：写操作递增，历史分页键整体失效（L1/L2 键均含版本号）
     */
    private long pageVersion() {
        return MonitorCacheUtil.getLong(CacheKeyConstant.INSTANCE_PAGE_VERSION, 0L);
    }

    private void bumpPageVersion() {
        RedisUtil.incrementValue(CacheKeyConstant.INSTANCE_PAGE_VERSION, 1L);
    }
}
