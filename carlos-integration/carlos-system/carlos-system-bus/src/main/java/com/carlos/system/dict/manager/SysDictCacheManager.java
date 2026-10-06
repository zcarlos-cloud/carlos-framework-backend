package com.carlos.system.dict.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.core.base.Dict;
import com.carlos.core.exception.BusinessException;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.redis.ICacheManager;
import com.carlos.redis.util.RedisUtil;
import com.carlos.system.dict.pojo.dto.SysDictDTO;
import com.carlos.system.dict.pojo.dto.SysDictItemDTO;
import com.carlos.system.dict.pojo.entity.SysDict;
import com.carlos.system.enums.SystemErrorCode;
import com.google.common.collect.Lists;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SysDictCacheManager implements ICacheManager<SysDictDTO> {

    private final RLocalCachedMap<String, List<SysDictItemDTO>> dictItemLocalCache;

    private final SysDictManager dictManager;
    private final SysDictItemManager itemManager;

    public SysDictCacheManager(SysDictManager dictManager, SysDictItemManager itemManager,
                               RedissonClient redissonClient) {
        this.dictManager = dictManager;
        this.itemManager = itemManager;

        LocalCachedMapOptions<String, List<SysDictItemDTO>> options =
            LocalCachedMapOptions.<String, List<SysDictItemDTO>>defaults()
                .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
                .cacheSize(500)
                .timeToLive(60, TimeUnit.MINUTES)
                .maxIdle(30, TimeUnit.MINUTES)
                .storeMode(LocalCachedMapOptions.StoreMode.LOCALCACHE_REDIS)
                .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
                .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR);
        this.dictItemLocalCache = redissonClient.getLocalCachedMap("system:dict:item:list", options);
        log.info("SysDictCacheManager initialized with RLocalCachedMap");
    }

    /** 缓存空间 */
    public static final String PREFIX = "system:dict:";

    /**
     * Set 字段选项缓存： 参数：字典编码
     */
    public static final String DICT_ITEM_ID_KEY = PREFIX + "dict_item:%s";


    /**
     * Hash 字段选项Hash缓存key 参数：选项id
     */
    public static final String DICT_ITEM_HASH_KEY = PREFIX + "item:%s";

    @Override
    @SuppressWarnings("unchecked")
    public void initCache() {
        try {
            // 分页参数
            int pageSize = 5000; // 每批处理1000条记录
            int current = 1;
            boolean hasMore = true;

            // 使用分页方式处理大量数据
            while (hasMore) {
                PageInfo<SysDict> pageInfo = dictManager.page(new PageInfo<>(current, pageSize, false));
                List<SysDict> dicts = pageInfo.getRecords();
                if (CollUtil.isEmpty(dicts)) {
                    break;
                }
                List<SysDictItemDTO> items = itemManager.listItems(dicts.stream().map(SysDict::getId).collect(Collectors.toSet()), null, false);
                if (CollUtil.isEmpty(items)) {
                    continue;
                }
                // 分组
                Map<Serializable, List<SysDictItemDTO>> dictItemMap = items.stream().collect(Collectors.groupingBy(SysDictItemDTO::getDictId));

                RedisUtil.executePipelined(new SessionCallback<Object>() {
                    @Override
                    public Object execute(RedisOperations ops) throws DataAccessException {
                        dicts.forEach(item -> {
                            Serializable dictId = item.getId();
                            String dictCode = item.getDictCode();
                            String keyDictItem = keyDictItem(dictCode);
                            List<SysDictItemDTO> dictItems = dictItemMap.get(dictId);
                            if (CollUtil.isNotEmpty(dictItems)) {
                                dictItems.forEach(i -> {
                                    Serializable itemId = i.getId();
                                    ops.opsForHash().putAll(keyItemHash(itemId), BeanUtil.beanToMap(i, false, true));
                                    ops.opsForSet().add(keyDictItem, itemId);
                                });
                            }
                        });
                        return null;
                    }
                });
                // 判断是否还有更多数据
                hasMore = dicts.size() >= pageSize;
                current++;
            }
            log.info("init dict cache success");
        } catch (Exception e) {
            log.error("Failed to init dict cache with pagination", e);
            throw new BusinessException(SystemErrorCode.SYS_CACHE_ERROR, "初始化字典缓存失败", e);
        }


    }

    @Override
    public long clearCache() {
        long deleteCount = RedisUtil.deleteSpace(PREFIX);
        dictItemLocalCache.clear();
        log.info("dict cache has been cleaned, delete count: {}", deleteCount);
        return deleteCount;
    }


    public Dict getDictByItemId(Long id) {
        Dict dict = new Dict();
        return dict;
    }

    public Dict getDictByItemCode(String code) {
        if (StrUtil.isBlank(code)) {
            return null;
        }
        Dict dict = new Dict();
        return dict;
    }

    /**
     * 获取字典项
     *
     * @param dictCode 字典编码
     * @return 字典项
     */
    @SuppressWarnings("unchecked")
    public List<SysDictItemDTO> getDictItems(String dictCode) {
        // 1. 先读本地缓存（RLocalCachedMap 自动查本地 + Redis）
        List<SysDictItemDTO> items = dictItemLocalCache.get(dictCode);
        if (items != null) {
            return items;
        }

        // 2. 本地未命中，读 Redis 整 Hash
        Set<Serializable> itemIds = RedisUtil.getSet(keyDictItem(dictCode));
        if (CollUtil.isNotEmpty(itemIds)) {
            Set<String> keys = itemIds.stream().map(i -> keyItemHash(i.toString())).collect(Collectors.toSet());
            Map<String, SysDictItemDTO> map = RedisUtil.hashMultiGetAll(keys, SysDictItemDTO.class);
            if (CollUtil.isNotEmpty(map)) {
                items = Lists.newArrayList(map.values());
                // 回填本地缓存
                dictItemLocalCache.fastPut(dictCode, items);
                return items;
            }
        }

        if (CollUtil.isEmpty(items)) {
            // 3. Redis 也未命中，加载 DB 并回填缓存
            items = itemManager.listByDictCode(dictCode);
        }

        if (CollUtil.isNotEmpty(items)) {
            dictItemLocalCache.fastPut(dictCode, items);
        }
        return items;
    }


    /**
     * 字典选项排序
     *
     * @param items 参数0
     * @return java.util.List<com.carlos.system.dict.pojo.dto.SysDictItemDTO>
     * @author Carlos
     * @date 2025-12-15 16:32
     */
    private List<SysDictItemDTO> sortDictItems(List<SysDictItemDTO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        return items.stream().sorted(Comparator.comparingLong(SysDictItemDTO::getOrderNum)).collect(Collectors.toList());
    }


    /**
     * 字典选项关联key
     */
    private String keyDictItem(String dictCode) {
        return String.format(DICT_ITEM_ID_KEY, dictCode);
    }

    /**
     * 字典选项缓存key
     */
    private String keyItemHash(Serializable itemId) {
        return String.format(DICT_ITEM_HASH_KEY, itemId);
    }

    public void updateItemCache(SysDictItemDTO item) {
    }

    @SuppressWarnings("unchecked")
    public void deleteItems(SysDictDTO dict, Set<Long> ids) {
        String dictItemKey = keyDictItem(dict.getDictCode());
        RedisUtil.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(@NotNull RedisOperations ops) throws DataAccessException {
                ops.multi();
                ids.forEach(id -> {
                    ops.delete(keyItemHash(id));
                    ops.opsForSet().remove(dictItemKey, id);
                });
                return ops.exec();
            }
        });
        // 清除本地缓存
        dictItemLocalCache.remove(dict.getDictCode());

    }

    /**
     * addItemCache
     *
     * @param dict  字典信息
     * @param items 选项信息
     * @author Carlos
     * @date 2025-12-16 14:07
     */
    @SuppressWarnings("unchecked")
    public void addItemCache(SysDictDTO dict, List<SysDictItemDTO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        String keyDictItem = keyDictItem(dict.getDictCode());
        RedisUtil.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(@NotNull RedisOperations ops) throws DataAccessException {
                ops.multi();
                items.forEach(i -> {
                    Serializable itemId = i.getId();
                    ops.opsForHash().putAll(keyItemHash(itemId), BeanUtil.beanToMap(i, false, true));
                    ops.opsForSet().add(keyDictItem, itemId);
                });
                return ops.exec();
            }
        });
        // 更新本地缓存
        dictItemLocalCache.remove(dict.getDictCode());

    }

    /**
     * updateItemCache
     *
     * @param dict  字典信息
     * @param items 选项信息
     * @author Carlos
     */
    @SuppressWarnings("unchecked")
    public void updateItemCache(SysDictDTO dict, List<SysDictItemDTO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        // 删除字典相关缓存，重新添加
        String keyDictItem = keyDictItem(dict.getDictCode());
        Set<Serializable> oldIds = RedisUtil.getSet(keyDictItem);
        RedisUtil.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(@NotNull RedisOperations ops) throws DataAccessException {
                ops.multi();
                ops.delete(keyDictItem);
                oldIds.forEach(id -> ops.delete(keyItemHash(id.toString())));
                items.forEach(i -> {
                    Serializable itemId = i.getId();
                    ops.opsForHash().putAll(keyItemHash(itemId), BeanUtil.beanToMap(i, false, true));
                    ops.opsForSet().add(keyDictItem, itemId);
                });
                return ops.exec();
            }
        });
        // 清除本地缓存
        dictItemLocalCache.remove(dict.getDictCode());
    }

}
