package com.carlos.datasource.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 属性-列名映射工具类（优化版）
 * </p>
 *
 * <p>
 * 使用 ConcurrentHashMap + 过期机制实现缓存：
 * 1. 自动过期（30分钟）
 * 2. 最大容量限制（1000个类）
 * 3. 线程安全
 * 4. LRU 淘汰策略
 * </p>
 *
 * @author carlos
 * @date 2020/3/2
 **/
@Slf4j
public class PropertyColumnUtil {

    /**
     * 缓存最大容量
     */
    private static final int MAXIMUM_SIZE = 1000;

    /**
     * 缓存过期时间（毫秒）
     */
    private static final long EXPIRE_AFTER_WRITE = 30 * 60 * 1000L;

    /**
     * 缓存数据结构
     */
    private static class CacheEntry {
        final Map<String, String> value;
        final long timestamp;

        CacheEntry(Map<String, String> value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRE_AFTER_WRITE;
        }
    }

    /**
     * 使用 ConcurrentHashMap 存储缓存
     */
    private static final ConcurrentHashMap<Class<?>, CacheEntry> CACHE = new ConcurrentHashMap<>();

    /**
     * 访问顺序队列（用于 LRU）
     */
    private static final ConcurrentLinkedQueue<Class<?>> ACCESS_ORDER = new ConcurrentLinkedQueue<>();

    /**
     * 定时清理线程池
     */
    private static final ScheduledExecutorService CLEANUP_EXECUTOR =
        Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());

    static {
        // 每10分钟执行一次清理
        CLEANUP_EXECUTOR.scheduleWithFixedDelay(PropertyColumnUtil::cleanup, 10, 10, TimeUnit.MINUTES);

        // JVM 关闭时关闭线程池
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CLEANUP_EXECUTOR.shutdown();
            try {
                if (!CLEANUP_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    CLEANUP_EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                CLEANUP_EXECUTOR.shutdownNow();
            }
        }));
    }

    /**
     * 清理过期缓存
     */
    private static void cleanup() {
        try {
            int beforeSize = CACHE.size();
            CACHE.entrySet().removeIf(entry -> {
                if (entry.getValue().isExpired()) {
                    ACCESS_ORDER.remove(entry.getKey());
                    return true;
                }
                return false;
            });
            int afterSize = CACHE.size();
            if (beforeSize != afterSize) {
                log.debug("缓存清理完成，清理前: {}, 清理后: {}", beforeSize, afterSize);
            }
        } catch (Exception e) {
            log.error("缓存清理异常", e);
        }
    }

    /**
     * 执行 LRU 淘汰
     */
    private static void evictIfNecessary() {
        while (CACHE.size() > MAXIMUM_SIZE && !ACCESS_ORDER.isEmpty()) {
            Class<?> oldest = ACCESS_ORDER.poll();
            if (oldest != null) {
                CACHE.remove(oldest);
                log.debug("LRU 淘汰: {}", oldest.getName());
            }
        }
    }

    /**
     * 从 MybatisPlus TableInfo 中加载字段映射
     *
     * @param clazz 实体类
     * @return 属性-列名映射
     */
    private static Map<String, String> loadTableFieldMap(Class<?> clazz) {
        try {
            TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
            if (tableInfo == null) {
                log.warn("无法获取 TableInfo: {}", clazz.getName());
                return null;
            }

            List<TableFieldInfo> tableFieldInfos = tableInfo.getFieldList();
            if (CollectionUtil.isEmpty(tableFieldInfos)) {
                log.warn("实体类 {} 没有字段", clazz.getName());
                return null;
            }

            Map<String, String> fieldMap = tableFieldInfos.stream()
                .collect(Collectors.toMap(
                    TableFieldInfo::getProperty,
                    TableFieldInfo::getColumn,
                    (v1, v2) -> v1  // 处理冲突，保留第一个
                ));

            log.debug("加载字段映射: {}，共 {} 个字段", clazz.getSimpleName(), fieldMap.size());
            return fieldMap;

        } catch (Exception e) {
            log.error("加载字段映射失败: {}", clazz.getName(), e);
            return null;
        }
    }

    /**
     * 获取属性-列名映射
     *
     * @param clazz 实体类
     * @return 属性-列名映射
     */
    public static Map<String, String> getPropertyColumnMap(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }

        // 检查是否需要 LRU 淘汰
        evictIfNecessary();

        // 更新访问顺序
        ACCESS_ORDER.remove(clazz);
        ACCESS_ORDER.offer(clazz);

        // 获取缓存
        CacheEntry entry = CACHE.get(clazz);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }

        // 加载新数据
        Map<String, String> data = loadTableFieldMap(clazz);
        if (data != null) {
            CACHE.put(clazz, new CacheEntry(data));
        }
        return data;
    }

    /**
     * 通过实体类类型和属性名称，获取对应的列名
     *
     * @param clazz    对象 Class
     * @param property 对象属性名称
     * @return 列名
     * @throws IllegalArgumentException 如果找不到映射
     */
    public static String getColumn(Class<?> clazz, String property) {
        if (clazz == null || StrUtil.isBlank(property)) {
            throw new IllegalArgumentException("类或属性名不能为空");
        }

        Map<String, String> propertyColumnMap = getPropertyColumnMap(clazz);
        if (MapUtil.isEmpty(propertyColumnMap)) {
            throw new IllegalArgumentException(
                String.format("没有找到对应的实体映射对象: %s", clazz.getName()));
        }

        String column = propertyColumnMap.get(property);
        if (StrUtil.isEmpty(column)) {
            // 尝试驼峰转下划线
            column = StrUtil.toUnderlineCase(property);
            log.debug("未找到映射，使用驼峰转下划线: {} -> {}", property, column);
        }

        return column;
    }

    /**
     * 通过实体类类型和属性名称，获取对应的列名（带默认值）
     *
     * @param clazz        对象 Class
     * @param property     对象属性名称
     * @param defaultValue 默认值
     * @return 列名，找不到时返回默认值
     */
    public static String getColumn(Class<?> clazz, String property, String defaultValue) {
        try {
            return getColumn(clazz, property);
        } catch (IllegalArgumentException e) {
            log.debug("获取列名失败，使用默认值: {}.{}, default={}",
                clazz != null ? clazz.getSimpleName() : null, property, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息
     */
    public static String getCacheStats() {
        return String.format("Cache size: %d, Access order size: %d",
            CACHE.size(), ACCESS_ORDER.size());
    }

    /**
     * 获取缓存当前大小
     *
     * @return 缓存大小
     */
    public static long getCacheSize() {
        return CACHE.size();
    }

    /**
     * 清空缓存
     */
    public static void clearCache() {
        CACHE.clear();
        ACCESS_ORDER.clear();
        log.info("PropertyColumnUtil 缓存已清空");
    }

    /**
     * 刷新指定类的缓存
     *
     * @param clazz 实体类
     */
    public static void refreshCache(Class<?> clazz) {
        if (clazz != null) {
            CACHE.remove(clazz);
            ACCESS_ORDER.remove(clazz);
            log.debug("缓存已刷新: {}", clazz.getName());
        }
    }
}
