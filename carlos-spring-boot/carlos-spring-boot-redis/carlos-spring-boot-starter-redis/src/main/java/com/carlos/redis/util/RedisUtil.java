package com.carlos.redis.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Redis 工具类
 * <p>
 * 注意事项：
 * 1. 所有方法都已处理空值和异常情况
 * 2. 批量操作使用 Pipeline 优化性能
 * 3. 使用线程池执行并行操作
 * </p>
 *
 * @author carlos
 * @date 2020/11/22 1:16
 */
@SuppressWarnings({"unchecked", "unused", "UnusedReturnValue"})
@Slf4j
public class RedisUtil {

    /**
     * 通配符
     */
    public static final String ALL = "*";

    /**
     * 默认批次大小
     */
    private static final int DEFAULT_BATCH = 5_000;

    /**
     * 默认每批删除大小
     */
    private static final int DEFAULT_DELETE_BATCH = 300;

    /**
     * 并行操作线程池 - 使用虚拟线程
     */
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private static final int LOCK_COUNT = 128;
    private static final ReentrantLock[] LOCKS = new ReentrantLock[LOCK_COUNT];

    static {
        for (int i = 0; i < LOCK_COUNT; i++) {
            LOCKS[i] = new ReentrantLock();
        }
    }

    private static RedisTemplate<String, Object> redisTemplate;
    private static ValueOperations<String, Object> valueOperations;
    private static HashOperations<String, String, Object> hashOperations;
    private static SetOperations<String, Object> setOperations;
    private static ZSetOperations<String, Object> zSetOperations;
    private static ListOperations<String, Object> listOperations;

    /**
     * 初始化 RedisUtil 静态字段
     */
    public static void init(RedisTemplate<String, Object> redisTemplate) {
        RedisUtil.redisTemplate = redisTemplate;
        RedisUtil.valueOperations = redisTemplate.opsForValue();
        RedisUtil.setOperations = redisTemplate.opsForSet();
        RedisUtil.zSetOperations = redisTemplate.opsForZSet();
        RedisUtil.listOperations = redisTemplate.opsForList();
        RedisUtil.hashOperations = redisTemplate.opsForHash();
        log.info("RedisUtil initialized");
    }

    /**
     * 优雅关闭线程池
     */
    public static void destroy() {
        log.info("Shutting down RedisUtil executor...");
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
                EXECUTOR.shutdownNow();
                if (!EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("RedisUtil executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("RedisUtil executor shutdown complete");
    }

    // region----------------------  通用方法 start  ------------------------

    /**
     * 设置缓存失效时间
     *
     * @param key     键
     * @param timeout 时间（秒）
     * @return 是否设置成功
     */
    public static Boolean setExpire(@NonNull String key, long timeout) {
        return setExpire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存失效时间
     *
     * @param key      键
     * @param timeout  时间
     * @param timeUnit 时间单位
     * @return 是否设置成功，失败返回 false 不抛异常
     */
    public static Boolean setExpire(@NonNull String key, long timeout, TimeUnit timeUnit) {
        try {
            return redisTemplate.expire(key, timeout, timeUnit);
        } catch (Exception e) {
            log.error("Redis setExpire error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存失效时间
     *
     * @param key    键
     * @param expire 过期时间
     * @return 是否设置成功
     */
    public static Boolean setExpire(@NonNull String key, Duration expire) {
        if (expire == null) {
            return false;
        }
        return setExpire(key, expire.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 获取缓存失效时间
     *
     * @param key 键
     * @return 时间（秒）-2 表示 key 不存在，-1 表示永久有效
     */
    public static Long getExpire(@NonNull String key) {
        try {
            return redisTemplate.getExpire(key, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Redis getExpire error, key: {}", key, e);
            return -2L;
        }
    }

    /**
     * key 是否存在
     *
     * @param key 键
     * @return true 存在，false 不存在或出错
     */
    public static Boolean hasKey(@NonNull String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("Redis hasKey error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取相关 key 的数量
     *
     * @param pattern key 模糊匹配
     * @return key 数量
     */
    public static long getKeyNum(@NonNull String pattern) {
        AtomicLong keyNum = new AtomicLong(0);
        scanKeys(pattern, 1000, s -> keyNum.incrementAndGet());
        return keyNum.get();
    }

    /**
     * 删除单个缓存
     *
     * @param key 缓存 key
     * @return 是否删除成功
     */
    public static boolean delete(@NonNull String key) {
        try {
            Boolean success = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(success)) {
                log.debug("Redis delete success, key: {}", key);
                return true;
            } else {
                log.warn("Redis delete fail or key not exist, key: {}", key);
                return false;
            }
        } catch (Exception e) {
            log.error("Redis delete error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 安全批量删除，避免 Redis 慢查询
     *
     * @param keys 待删 key 集合
     * @return 实际删除条数
     */
    public static long delete(Collection<?> keys) {
        if (CollUtil.isEmpty(keys)) {
            return 0L;
        }

        List<? extends List<?>> partitions = CollUtil.split(keys, DEFAULT_DELETE_BATCH);
        AtomicLong total = new AtomicLong(0L);

        for (List<?> batch : partitions) {
            try {
                List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
                    @Override
                    public Object execute(RedisOperations operations) {
                        batch.forEach(operations::delete);
                        return null;
                    }
                });

                long deleted = results.stream()
                    .mapToLong(r -> r instanceof Long ? (Long) r : 0L)
                    .sum();
                total.addAndGet(deleted);
            } catch (Exception e) {
                log.error("Redis delete batch fail, batchSize={}", batch.size(), e);
            }
        }
        return total.get();
    }

    /**
     * 序列化 key
     *
     * @param key 缓存 key
     * @return 字节数组
     */
    @SuppressWarnings("unchecked")
    public static byte[] rawKey(String key) {
        RedisSerializer<String> serializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();
        return serializer.serialize(key);
    }

    /**
     * 序列化 value
     *
     * @param value 值
     * @return 字节数组
     */
    @SuppressWarnings("unchecked")
    public static byte[] rawValue(Object value) {
        RedisSerializer<Object> serializer = (RedisSerializer<Object>) redisTemplate.getValueSerializer();
        return serializer.serialize(value);
    }

    /**
     * 使用通配符删除缓存
     *
     * @param pattern 带通配符的 key
     * @return 删除条数
     */
    public static long deletePattern(@NonNull String pattern) {
        long deleted = 0L;
        if (StrUtil.isBlank(pattern)) {
            return deleted;
        }

        List<String> batch = new ArrayList<>(DEFAULT_DELETE_BATCH);
        ScanOptions options = ScanOptions.scanOptions()
            .match(ObjUtil.defaultIfNull(rawKey(pattern), pattern.getBytes(StandardCharsets.UTF_8)))
            .count(DEFAULT_DELETE_BATCH)
            .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                batch.add(key);
                if (batch.size() == DEFAULT_DELETE_BATCH) {
                    deleted += delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                deleted += delete(batch);
            }
        } catch (Exception e) {
            log.error("Redis deletePattern error, pattern: {}", pattern, e);
        }
        return deleted;
    }

    /**
     * 匹配符合条件的 key（慎用，大数据量时使用 scanKeys）
     *
     * @param pattern key 通配符匹配
     * @return 匹配到的 key
     */
    public static Set<String> keys(@NonNull String pattern) {
        try {
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            log.error("Redis keys error, pattern: {}", pattern, e);
            return Collections.emptySet();
        }
    }

    /**
     * 扫描并处理符合条件的 key
     *
     * @param pattern   key 通配符匹配
     * @param batchSize 单次扫描条数
     * @param processor 处理函数
     */
    public static void scanKeys(String pattern, int batchSize, Consumer<String> processor) {
        if (processor == null || ObjUtil.isEmpty(pattern)) {
            return;
        }

        ScanOptions options = ScanOptions.scanOptions()
            .match(ObjUtil.defaultIfNull(rawKey(pattern), pattern.getBytes(StandardCharsets.UTF_8)))
            .count(batchSize)
            .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                processor.accept(cursor.next());
            }
        } catch (Exception e) {
            log.error("Redis scanKeys error, pattern: {}", pattern, e);
        }
    }

    /**
     * 扫描并获取符合条件的 key
     *
     * @param pattern   key 通配符匹配
     * @param batchSize 单次扫描条数
     * @return 匹配到的 key
     */
    public static Set<String> scanKeys(@NonNull String pattern, int batchSize) {
        Set<String> sets = new HashSet<>();
        scanKeys(pattern, batchSize, sets::add);
        return sets;
    }

    /**
     * 执行 Redis 命令（支持事务）
     *
     * @param session RedisSession
     * @return 执行结果
     */
    public static <T> T execute(SessionCallback<T> session) {
        try {
            return redisTemplate.execute(session);
        } catch (Exception e) {
            log.error("Redis execute error", e);
            return null;
        }
    }

    /**
     * 使用 RedisConnection 执行原生命令
     *
     * @param callback RedisCallback
     * @return 执行结果
     */
    public static <T> T execute(RedisCallback<T> callback) {
        try {
            return redisTemplate.execute(callback);
        } catch (Exception e) {
            log.error("Redis execute error", e);
            return null;
        }
    }

    /**
     * 执行 Pipeline 操作
     *
     * @param session RedisSession
     * @return 执行结果列表
     */
    public static List<Object> executePipelined(SessionCallback<?> session) {
        try {
            return redisTemplate.executePipelined(session);
        } catch (Exception e) {
            log.error("Redis executePipelined error", e);
            return Collections.emptyList();
        }
    }

    /**
     * 执行 Pipeline 操作（原生）
     *
     * @param callback RedisCallback
     * @return 执行结果列表
     */
    public static List<Object> executePipelined(RedisCallback<?> callback) {
        try {
            return redisTemplate.executePipelined(callback);
        } catch (Exception e) {
            log.error("Redis executePipelined error", e);
            return Collections.emptyList();
        }
    }

    // endregion----------------------   通用方法 end   ------------------------

    // region----------------------  字符串数据结构操作 start  ------------------------

    /**
     * 如果缓存不存在设置（永不过期）
     *
     * @param key   键
     * @param value 值
     * @return true 成功，false 失败或 key 已存在
     */
    public static <T> Boolean setIfAbsent(@NonNull String key, @NonNull T value) {
        try {
            return valueOperations.setIfAbsent(key, value);
        } catch (Exception e) {
            log.error("Redis setIfAbsent error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 如果缓存不存在设置
     *
     * @param key     键
     * @param value   值
     * @param timeout 时间（小于等于 0 时设为无限期）
     * @param unit    时间单位
     * @return true 成功，false 失败或 key 已存在
     */
    public static <T> Boolean setIfAbsent(@NonNull String key, @NonNull T value, long timeout, TimeUnit unit) {
        try {
            return valueOperations.setIfAbsent(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("Redis setIfAbsent error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 如果缓存不存在设置
     *
     * @param key    键
     * @param value  值
     * @param expire 过期时间
     * @return true 成功，false 失败或 key 已存在
     */
    public static <T> Boolean setIfAbsent(@NonNull String key, @NonNull T value, Duration expire) {
        if (expire == null) {
            return setIfAbsent(key, value);
        }
        return setIfAbsent(key, value, expire.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 设置普通缓存（永不过期）
     *
     * @param key   键
     * @param value 值
     * @return true 成功，false 失败
     */
    public static <T> boolean setValue(@NonNull String key, @NonNull T value) {
        try {
            valueOperations.set(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis setValue error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 设置普通缓存
     *
     * @param key     键
     * @param value   值
     * @param timeout 时间（秒，小于等于 0 时设为无限期）
     * @return true 成功，false 失败
     */
    public static <T> boolean setValue(@NonNull String key, @NonNull T value, long timeout) {
        return setValue(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置普通缓存
     *
     * @param key    键
     * @param value  值
     * @param expire 过期时间
     * @return true 成功，false 失败
     */
    public static <T> boolean setValue(@NonNull String key, @NonNull T value, Duration expire) {
        if (expire == null) {
            return setValue(key, value);
        }
        return setValue(key, value, expire.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 设置普通缓存
     *
     * @param key     键
     * @param value   值
     * @param timeout 时间（小于等于 0 时设为无限期）
     * @param unit    时间单位
     * @return true 成功，false 失败
     */
    public static <T> boolean setValue(@NonNull String key, @NonNull T value, long timeout, TimeUnit unit) {
        try {
            valueOperations.set(key, value, timeout, unit);
            return true;
        } catch (Exception e) {
            log.error("Redis setValue error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 批量设置缓存
     *
     * @param values key 为缓存 key，value 为对应的值
     * @param expire 过期时长，null 表示永不过期
     */
    public static <T> void setValueList(@NonNull Map<String, T> values, Duration expire) {
        if (CollUtil.isEmpty(values)) {
            return;
        }

        try {
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations ops) {
                    if (expire == null) {
                        values.forEach((k, v) -> ops.opsForValue().set(k, v));
                    } else {
                        values.forEach((k, v) -> ops.opsForValue().set(k, v, expire));
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            log.error("Redis setValueList error", e);
        }
    }

    /**
     * 批量插入 Value（永不过期）
     *
     * @param values key 为缓存 key，value 为对应的值
     */
    public static <T> void setValueList(@NonNull Map<String, T> values) {
        setValueList(values, null);
    }


    /**
     * 偏移量开始设置为指定值
     *
     * @param key    键
     * @param value  值
     * @param offset 偏移量
     * @return true成功 false 失败
     */
    public static <T> boolean setValueRang(
        @NonNull String key, @NonNull T value, long offset) {
        try {
            valueOperations.set(key, value, offset);
            return true;
        } catch (Throwable e) {
            log.error("RedisUtil setValueRang error, key:{} value:{}", key, value, e);
            return false;
        }
    }

    /**
     * 将字节串看成二进制位串，修改偏移量位offset的值为value
     *
     * @param key    键
     * @param value  值
     * @param offset 偏移量
     * @return true成功 false 失败
     */
    public static boolean setValueBit(
        @NonNull String key, long offset, boolean value) {
        try {
            valueOperations.setBit(key, offset, value);
            return true;
        } catch (Throwable e) {
            log.error("RedisUtil setValueBit error, key:{}", key, e);
            return false;
        }
    }

    /**
     * 将字节串看成二进制位串，获取偏移量位offset的值, 超出末尾视为0
     *
     * @param key    键
     * @param offset 偏移量
     * @return 值
     */
    public static Object getValueBit(@NonNull String key, long offset) {
        try {
            return valueOperations.getBit(key, offset);
        } catch (Throwable e) {
            log.error("RedisUtil getValueBit error, key:{}", key, e);
            return null;
        }
    }

    /**
     * 获取普通缓存
     *
     * @param key 键
     * @return 值，出错返回 null
     */
    public static <T> T getValue(@NonNull String key) {
        try {
            return (T) valueOperations.get(key);
        } catch (Exception e) {
            log.error("Redis getValue error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取缓存，如果不存在则加载
     *
     * @param key    键
     * @param loader 加载函数
     * @return 值
     */
    public static <T> T getValue(@NonNull String key, Supplier<T> loader) {
        return getValue(key, loader, null, null);
    }

    /**
     * 获取缓存，如果不存在则加载
     *
     * @param key      键
     * @param loader   加载函数
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 值
     */
    public static <T> T getValue(@NonNull String key, Supplier<T> loader, Long timeout, TimeUnit timeUnit) {
        T value = getValue(key);
        if (value != null) {
            return value;
        }

        // 加锁防止缓存击穿
        ReentrantLock lock = LOCKS[Math.floorMod(key.hashCode(), LOCK_COUNT)];
        lock.lock();
        try {
            // 双重检查
            value = getValue(key);
            if (value != null) {
                return value;
            }

            // 加载数据
            value = loader.get();
            if (value != null) {
                if (timeout != null && timeUnit != null) {
                    setValue(key, value, timeout, timeUnit);
                } else {
                    setValue(key, value);
                }
            }
            return value;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 批量获取缓存（分批次并行获取）
     *
     * @param keys      key 集合
     * @param batchSize 每批数量
     * @return 值列表，与 key 顺序一一对应
     */
    public static <T> Map<String, T> getValueMap(@NonNull Set<String> keys, int batchSize) {
        return getValueMap(Lists.newArrayList(keys), batchSize);
    }

    /**
     * 批量获取缓存（分批次并行获取）
     *
     * @param keys      key 集合
     * @param batchSize 每批数量
     * @return 值列表，与 key 顺序一一对应
     */
    public static <T> List<T> getValueList(@NonNull List<String> keys, int batchSize) {
        Map<String, T> keyValMap = getValueMap(keys, batchSize);
        // 按照原始 key 的顺序返回结果
        return keys.stream()
            .map(keyValMap::get)
            .collect(Collectors.toList());
    }


    /**
     * 批量获取缓存（分批次并行获取）
     *
     * @param keys      key 集合
     * @param batchSize 每批数量
     * @return 值列表，与 key 顺序一一对应
     */
    public static <T> Map<String, T> getValueMap(@NonNull List<String> keys, int batchSize) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyMap();
        }

        if (batchSize <= 0) {
            batchSize = DEFAULT_DELETE_BATCH;
        }

        // 使用 key 作为 Map 的 key，而不是索引
        Map<String, T> keyValMap = new ConcurrentHashMap<>(keys.size());
        List<List<String>> batches = Lists.partition(keys, batchSize);

        // 使用 CompletableFuture 并行处理
        List<CompletableFuture<Void>> futures = batches.stream()
            .map(batch -> CompletableFuture.runAsync(() -> {
                try {
                    List<T> vs = (List<T>) valueOperations.multiGet(batch);
                    if (vs != null) {
                        // 直接将 key 和 value 对应起来
                        for (int i = 0; i < batch.size() && i < vs.size(); i++) {
                            keyValMap.put(batch.get(i), vs.get(i));
                        }
                    }
                } catch (Exception e) {
                    log.error("Redis mget batch error, batchSize={}", batch.size(), e);
                }
            }, EXECUTOR))
            .toList();

        // 等待全部完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return keyValMap;
    }

    /**
     * 批量获取缓存（默认批次大小）
     *
     * @param keys keys
     * @return 值列表
     */
    public static <T> Map<String, T> getValueList(@NonNull Set<String> keys) {
        return getValueMap(keys, DEFAULT_DELETE_BATCH);
    }

    /**
     * 批量获取缓存（默认批次大小）
     *
     * @param keys keys
     * @return 值列表
     */
    public static <T> Map<String, T> getValueMap(@NonNull Set<String> keys) {
        return getValueMap(new ArrayList<>(keys), DEFAULT_DELETE_BATCH);
    }

    /**
     * 批量获取缓存（默认批次大小）
     *
     * @param keys keys
     * @return 值列表
     */
    public static <T> List<T> getValueList(@NonNull List<String> keys) {
        return getValueList(keys, DEFAULT_DELETE_BATCH);
    }

    /**
     * 模糊查找 Value list
     *
     * @param pattern key 通配符匹配
     * @return 值列表
     */
    public static <T> List<T> getValueListByPattern(String pattern) {
        if (StrUtil.isEmpty(pattern)) {
            return Collections.emptyList();
        }
        Set<String> keys = scanKeys(pattern, 1000);
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyList();
        }
        return getValueList(new ArrayList<>(keys));
    }


    /**
     * 批量获取缓存, 当key数目较大是，存在性能问题
     *
     * @param keys key集合
     * @return java.util.List<T>
     * @author Carlos
     * @date 2024/3/8 15:44
     */
    public static <T> List<T> mGet(@NonNull Set<String> keys) {
        try {
            return (List<T>) valueOperations.multiGet(keys);
        } catch (Exception e) {
            log.error("RedisUtil getValueList error,", e);
            return null;
        }
    }

    /**
     * 获取指定范围的字符串, 超出字符串末尾的数据会视为是空串
     *
     * @param key   键
     * @param start 起始索引
     * @param end   终止位置
     * @return 值
     */
    public static <T> T getValueRang(@NonNull String key, long start, long end) {
        return (T) valueOperations.get(key, start, end);
    }


    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几（大于 0）
     * @return 加上指定值之后 key 的值，失败返回 null
     */
    public static Long incrementValue(@NonNull String key, long delta) {
        if (delta <= 0) {
            log.warn("Increment delta must be > 0, got: {}", delta);
            return null;
        }
        try {
            return valueOperations.increment(key, delta);
        } catch (Exception e) {
            log.error("Redis incrementValue error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 递增并设置过期时间
     *
     * @param key      键
     * @param delta    要增加几（大于 0）
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 加上指定值之后 key 的值
     */
    public static Long incrementValue(@NonNull String key, long delta, long timeout, TimeUnit timeUnit) {
        Long value = incrementValue(key, delta);
        if (value != null) {
            setExpire(key, timeout, timeUnit);
        }
        return value;
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几（大于 0）
     * @return 减少指定值之后 key 的值，失败返回 null
     */
    public static Long decrementValue(@NonNull String key, long delta) {
        if (delta <= 0) {
            log.warn("Decrement delta must be > 0, got: {}", delta);
            return null;
        }
        try {
            return valueOperations.decrement(key, delta);
        } catch (Exception e) {
            log.error("Redis decrementValue error, key: {}", key, e);
            return null;
        }
    }

    // endregion----------------------   字符串数据结构操作 end   ------------------------

    // region----------------------  Hash 操作 start  ------------------------

    /**
     * 获取 Hash 中的值
     *
     * @param key  键
     * @param item 项
     * @return 值
     */
    public static <T> T getHash(@NonNull String key, @NonNull String item) {
        try {
            return (T) hashOperations.get(key, item);
        } catch (Exception e) {
            log.error("Redis getHash error, key: {}, item: {}", key, item, e);
            return null;
        }
    }

    /**
     * 获取整个 hash 并转换为对象
     *
     * @param key   key
     * @param clazz 目标类型
     * @return 对象
     */
    public static <T> T getHash(@NonNull String key, Class<T> clazz) {
        Map<Object, Object> hash = getHash(key);
        if (MapUtil.isEmpty(hash)) {
            return null;
        }
        try {
            return BeanUtil.toBean(hash, clazz);
        } catch (Exception e) {
            log.error("Redis getHash convert error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取 hashKey 对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    @SuppressWarnings("unchecked")
    public static Map<Object, Object> getHash(@NonNull String key) {
        try {
            return (Map<Object, Object>) (Map<?, ?>) hashOperations.entries(key);
        } catch (Exception e) {
            log.error("Redis getHash error, key: {}", key, e);
            return Collections.emptyMap();
        }
    }

    /**
     * 批量获取多个 Hash 的部分字段
     *
     * @param keys      Hash key 集合
     * @param fields    字段列表
     * @param batchSize 每页数量
     * @param clazz     目标实体类型
     * @return key → 实体对象
     */
    public static <T> Map<String, T> hashMultiGet(List<String> keys, List<String> fields, int batchSize, Class<T> clazz) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyMap();
        }

        List<List<String>> pages = Lists.partition(keys, batchSize);
        Map<String, T> result = new ConcurrentHashMap<>(keys.size());

        List<CompletableFuture<Map<String, T>>> futures = pages.stream()
            .map(page -> CompletableFuture.supplyAsync(
                () -> hashMultiGet(page, fields, clazz), EXECUTOR))
            .collect(Collectors.toList());

        futures.forEach(f -> result.putAll(f.join()));
        return result;
    }

    /**
     * 批量获取多个 Hash 的部分字段
     *
     * @param keys   Hash key 集合
     * @param fields 字段列表
     * @param clazz  目标实体类型
     * @return key → 实体对象
     */
    public static <T> Map<String, T> hashMultiGet(Collection<String> keys, List<String> fields, Class<T> clazz) {
        if (CollUtil.isEmpty(keys)) {
            return Collections.emptyMap();
        }

        try {
            List<Object> rawResults = redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) {
                    for (String key : keys) {
                        if (CollUtil.isEmpty(fields)) {
                            operations.opsForHash().entries(key);
                        } else {
                            operations.opsForHash().multiGet(key, fields);
                        }
                    }
                    return null;
                }
            });

            Map<String, T> ans = new LinkedHashMap<>(keys.size());
            int index = 0;
            for (String key : keys) {
                Object row = rawResults.get(index++);
                if (row == null) {
                    continue;
                }
                Map<Object, Object> entries = parseHashResult(row, fields);
                if (CollUtil.isNotEmpty(entries)) {
                    try {
                        T bean = BeanUtil.toBean(entries, clazz);
                        ans.put(key, bean);
                    } catch (Exception e) {
                        log.error("Redis hashMultiGet convert error, key: {}", key, e);
                    }
                }
            }
            return ans;
        } catch (Exception e) {
            log.error("Redis hashMultiGet error", e);
            return Collections.emptyMap();
        }
    }

    private static Map<Object, Object> parseHashResult(Object row, List<String> fields) {
        if (row instanceof List) {
            Map<Object, Object> entries = new HashMap<>();
            List<Object> list = (List<Object>) row;
            if (fields != null) {
                for (int i = 0; i < list.size() && i < fields.size(); i++) {
                    Object o = list.get(i);
                    if (o != null) {
                        entries.put(fields.get(i), o);
                    }
                }
            }
            return entries;
        } else if (row instanceof Map) {
            return (Map<Object, Object>) row;
        }
        return Collections.emptyMap();
    }

    /**
     * 批量获取多个 Hash 的全部字段
     *
     * @param keys  Hash key 集合
     * @param clazz 目标实体类型
     * @return key → 实体对象
     */
    public static <T> Map<String, T> hashMultiGetAll(Collection<String> keys, Class<T> clazz) {
        return hashMultiGet(keys, null, clazz);
    }

    /**
     * 批量获取多个 Hash 的全部字段
     *
     * @param keys      Hash key 集合
     * @param batchSize 每页数量
     * @param clazz     目标实体类型
     * @return key → 实体对象
     */
    public static <T> Map<String, T> hashMultiGetAll(List<String> keys, int batchSize, Class<T> clazz) {
        return hashMultiGet(keys, null, batchSize, clazz);
    }

    /**
     * HashSet
     *
     * @param key 键
     * @param map 对应多个键值
     * @return 是否成功
     */
    @SuppressWarnings("unchecked")
    public static boolean putHash(@NonNull String key, @NonNull Map<String, Object> map) {
        try {
            hashOperations.putAll(key, (Map<String, Object>) (Map<?, ?>) map);
            return true;
        } catch (Exception e) {
            log.error("Redis putHash error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 将对象保存为 hash
     *
     * @param key  key
     * @param hash hash 对象
     * @return 是否成功
     */
    public static boolean putHash(@NonNull String key, @NonNull Object hash) {
        try {
            Map<String, Object> map = BeanUtil.beanToMap(hash);
            return putHash(key, map);
        } catch (Exception e) {
            log.error("Redis putHash error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 将对象保存为 hash 并设置过期时间
     *
     * @param key     key
     * @param hash    hash 对象
     * @param timeout 过期时间（秒）
     * @return 是否成功
     */
    public static boolean putHash(@NonNull String key, @NonNull Object hash, long timeout) {
        if (putHash(key, hash) && timeout > 0) {
            return setExpire(key, timeout);
        }
        return false;
    }

    /**
     * HashSet 并设置时间
     *
     * @param key     键
     * @param map     对应多个键值
     * @param timeout 时间(秒)
     * @return {boolean}
     */
    public static boolean putHash(@NonNull String key, @NonNull Map<String, Object> map, long timeout) {
        putHash(key, map);
        if (timeout > 0) {
            setExpire(key, timeout);
        }
        return true;
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return 是否成功
     */
    public static <T> boolean putHash(@NonNull String key, @NonNull String item, @NonNull T value) {
        try {
            hashOperations.put(key, item, value);
            return true;
        } catch (Exception e) {
            log.error("Redis putHash error, key: {}, item: {}", key, item, e);
            return false;
        }
    }

    /**
     * 向一张hash表中放入数据,如果不存在将创建
     *
     * @param key     键
     * @param item    项
     * @param value   值
     * @param timeout 时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return {boolean}
     */
    public static <T> boolean putHash(@NonNull String key, @NonNull String item, @NonNull T value, long timeout) {
        putHash(key, item, value);
        if (timeout > 0) {
            setExpire(key, timeout);
        }
        return true;
    }

    /**
     * 删除 hash 表中的值
     *
     * @param key  键
     * @param item 项，可以使多个
     */
    public static void deleteHash(@NonNull String key, @NonNull Object... item) {
        try {
            hashOperations.delete(key, item);
        } catch (Exception e) {
            log.error("Redis deleteHash error, key: {}", key, e);
        }
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @param by   要增加几(大于0)
     * @return 加上指定值之后 key 的值
     */
    public static double incrementHash(@NonNull String key, @NonNull String item, double by) {
        return hashOperations.increment(key, item, by);
    }

    /**
     * hash递增 如果不存在,就会创建一个 并把新增后的值返回
     *
     * @param key  键
     * @param item 项
     * @return true 存在，false 不存在或出错
     */
    public static boolean hasKeyHash(@NonNull String key, @NonNull String item) {
        try {
            return Boolean.TRUE.equals(hashOperations.hasKey(key, item));
        } catch (Exception e) {
            log.error("Redis hasKeyHash error, key: {}, item: {}", key, item, e);
            return false;
        }
    }

    /**
     * hash递减
     *
     * @param key  键
     * @param item 项
     * @param by   要减少记(小于0)
     * @return 减少指定值之后 key 的值
     */
    public static double decrementHash(@NonNull String key, @NonNull String item, long by) {
        return incrementHash(key, item, -by);
    }
    // endregion----------------------   Hash操作 end   ------------------------

    // region----------------------  集合数据结构操作 start  ------------------------

    /**
     * 将 set 数据放入缓存
     *
     * @param key    键
     * @param values 值，可以是多个
     * @return 成功个数
     */
    @SafeVarargs
    public static <T> Long addSet(@NonNull String key, @NonNull T... values) {
        try {
            return setOperations.add(key, values);
        } catch (Exception e) {
            log.error("Redis addSet error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 将集合中的数据放入 set 缓存
     *
     * @param key  键
     * @param list 值
     * @return 成功个数
     */
    public static Long addSet(@NonNull String key, @NonNull Set<?> list) {
        return addSet(key, list.toArray());
    }

    /**
     * 将集合中的数据放入set缓存
     *
     * @param key    键
     * @param list   值 可以是多个
     * @param expire 过期时间
     * @return 成功个数
     */
    public static Long addSet(@NonNull String key, @NonNull Set<?> list, Duration expire) {
        Long count = addSet(key, list.toArray());
        if (expire != null) {
            setExpire(key, expire);
        }
        return count;
    }

    /**
     * 将set数据放入缓存
     *
     * @param key    键
     * @param expire 过期时间
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public static <T> Long addSet(@NonNull String key, Duration expire, @NonNull T... values) {
        Long count = addSet(key, values);
        if (expire != null) {
            setExpire(key, expire);
        }
        return count;
    }

    /**
     * 根据 key 获取 Set 中的所有值
     *
     * @param key 键
     * @return Set
     */
    public static <T> Set<T> getSet(@NonNull String key) {
        try {
            return (Set<T>) setOperations.members(key);
        } catch (Exception e) {
            log.error("Redis getSet error, key: {}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * 根据 value 查询是否存在
     *
     * @param key   键
     * @param value 值
     * @return true 存在
     */
    public static <T> boolean hasKeySet(@NonNull String key, @NonNull T value) {
        try {
            return Boolean.TRUE.equals(setOperations.isMember(key, value));
        } catch (Exception e) {
            log.error("Redis hasKeySet error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取 set 缓存的长度
     *
     * @param key 键
     * @return 缓存长度
     */
    public static Long getSetSize(@NonNull String key) {
        try {
            return setOperations.size(key);
        } catch (Exception e) {
            log.error("Redis getSetSize error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 移除值为 value 的
     *
     * @param key    键
     * @param values 值，可以是多个
     * @return 移除的个数
     */
    public static Long removeSet(@NonNull String key, @NonNull Object... values) {
        try {
            return setOperations.remove(key, values);
        } catch (Exception e) {
            log.error("Redis removeSet error, key: {}", key, e);
            return 0L;
        }
    }

    // endregion----------------------   集合数据结构操作 end   ------------------------

    // region----------------------  列表数据结构操作 start  ------------------------

    /**
     * 将 list 放入缓存（左侧插入）
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public static <T> boolean leftPushList(@NonNull String key, @NonNull T value) {
        try {
            listOperations.leftPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis leftPushList error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 将 list 放入缓存（右侧插入）
     *
     * @param key   键
     * @param value 值
     * @return 是否成功
     */
    public static <T> boolean rightPushList(@NonNull String key, @NonNull T value) {
        try {
            listOperations.rightPush(key, value);
            return true;
        } catch (Exception e) {
            log.error("Redis rightPushList error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取 list 缓存的内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束（0 到 -1 代表所有值）
     * @return list 缓存内容
     */
    public static <T> List<T> getList(@NonNull String key, @NonNull Long start, @NonNull Long end) {
        try {
            return (List<T>) listOperations.range(key, start, end);
        } catch (Exception e) {
            log.error("Redis getList error, key: {}", key, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取 list 缓存的所有内容
     *
     * @param key 键
     * @return list 缓存内容
     */
    public static <T> List<T> getList(@NonNull String key) {
        return getList(key, 0L, -1L);
    }

    // endregion----------------------   列表数据结构操作 end   ------------------------

    // region----------------------  数据库操作 start  ------------------------

    /**
     * 清所有库，线上慎用！
     */
    public static void flushAll() {
        try {
            redisTemplate.execute((RedisCallback<Void>) conn -> {
                conn.serverCommands().flushAll();
                return null;
            });
            log.info("Redis flushAll executed");
        } catch (Exception e) {
            log.error("Redis flushAll error", e);
        }
    }

    /**
     * 清理当前 DB
     */
    public static void flushDb() {
        try {
            redisTemplate.execute((RedisCallback<Void>) conn -> {
                conn.serverCommands().flushDb();
                return null;
            });
            log.info("Redis flushDb executed");
        } catch (Exception e) {
            log.error("Redis flushDb error", e);
        }
    }

    /**
     * 清理指定 DB
     *
     * @param dbIndex DB 索引
     */
    public static void flushDb(int dbIndex) {
        try {
            redisTemplate.execute((RedisCallback<Void>) conn -> {
                conn.select(dbIndex);
                conn.serverCommands().flushDb();
                return null;
            });
            log.info("Redis flushDb executed for db {}", dbIndex);
        } catch (Exception e) {
            log.error("Redis flushDb error, dbIndex: {}", dbIndex, e);
        }
    }

    /**
     * 删除指定前缀空间
     *
     * @param keyPrefix 前缀，如 "system:region:"
     * @return 实际删除条数
     */
    public static long deleteSpace(String keyPrefix) {
        return deleteSpace(keyPrefix, DEFAULT_BATCH);
    }

    /**
     * 带限流的删除
     *
     * @param keyPrefix 前缀
     * @param batchSize 每批 SCAN 数量
     * @return 删除条数
     */
    public static long deleteSpace(String keyPrefix, int batchSize) {
        if (!keyPrefix.endsWith(":")) {
            keyPrefix = keyPrefix + ":";
        }
        String pattern = keyPrefix + "*";

        final AtomicLong deleted = new AtomicLong(0);
        List<String> list = new ArrayList<>();

        scanKeys(pattern, batchSize, key -> {
            list.add(key);
            if (list.size() >= batchSize) {
                deleted.addAndGet(delete(list));
                list.clear();
            }
        });

        if (CollUtil.isNotEmpty(list)) {
            deleted.addAndGet(delete(list));
        }

        log.info("Redis deleteSpace finish, prefix={}, deleted={}", keyPrefix, deleted.get());
        return deleted.get();
    }

    // endregion----------------------   数据库操作 end   ------------------------

    // region----------------------  Lua 脚本工具 start  ------------------------

    /**
     * 加载脚本
     *
     * @param script 脚本内容
     * @return SHA1 摘要
     */
    public static String loadScripts(String script) {
        if (StrUtil.isBlank(script)) {
            return null;
        }
        try {
            return (String) redisTemplate.execute((RedisConnection c) ->
                c.scriptingCommands().scriptLoad(script.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            log.error("Redis loadScripts error", e);
            return null;
        }
    }

    /**
     * 判断脚本是否存在（支持单机 & 集群）
     *
     * @param sha SHA1 摘要
     * @return true=已存在
     */
    public static List<Boolean> scriptExists(String... sha) {
        if (sha == null || sha.length == 0) {
            return List.of(false);
        }
        try (RedisConnection conn = Objects.requireNonNull(redisTemplate.getConnectionFactory()).getConnection()) {
            if (conn instanceof RedisClusterConnection) {
                RedisClusterConnection cluster = (RedisClusterConnection) conn;
                for (RedisClusterNode node : cluster.clusterGetNodes()) {
                    if (node.isMaster()) {
                        return cluster.scriptingCommands().scriptExists(sha);
                    }
                }
            } else {
                return conn.scriptingCommands().scriptExists(sha);
            }
        } catch (Exception e) {
            log.error("Redis scriptExists error", e);
        }
        return List.of(false);
    }

    /**
     * 执行 Lua 脚本
     *
     * @param script  脚本
     * @param keys    KEYS 数组
     * @param args    ARGV 数组
     * @return 执行结果
     */
    public static <T> T lua(RedisScript<T> script, List<String> keys, Object... args) {
        return redisTemplate.execute(script, keys, args);
    }

    /**
     * 执行已预加载的 Lua（SHA 模式）
     *
     * @param sha         预加载得到的 SHA1
     * @param returnType  返回值类型
     * @param numKeys     key个数
     * @param keysAndArgs KEY  ARGV 数组
     * @param <T>         返回泛型
     * @return 执行结果
     */
    public static <T> T evalSha(String sha, ReturnType returnType, int numKeys, byte[]... keysAndArgs) {
        try {
            return redisTemplate.execute((RedisCallback<T>) conn ->
                conn.scriptingCommands().evalSha(sha, returnType, numKeys, keysAndArgs));
        } catch (Exception e) {
            log.error("Redis evalSha error, sha: {}", sha, e);
            return null;
        }
    }

    /**
     * 执行一次性 Lua（整串脚本，低频用）
     *
     * <p>⚠️ ARGV 用**字符串序列化器**逐个下发，不经过 value 序列化器 —— 后者默认是 JSON，
     * 会把字符串参数写成带引号的 {@code "1"}，Lua 侧 {@code tonumber(ARGV[1])} 直接拿到 nil。
     * 需要整数/字符串参数的脚本必须走本方法（或下面显式传 {@code String...} 的那个重载）。</p>
     *
     * @param script  完整 Lua 源码
     * @param keys    KEYS 数组
     * @param args    ARGV 数组
     * @param retType 返回 Java 类型
     * @param <T>     返回泛型
     * @deprecated 参数传 {@code List} 极易踩坑：{@code RedisTemplate.execute(script, keys, Object... args)}
     * 的第三个参数是**可变参数**，而 Java 不会把集合展开成可变参数，
     * 因此整个 List 会变成**唯一一个** ARGV（Lua 侧 {@code ARGV[1]} 是一坨列表字面量、{@code ARGV[2]} 为 nil）。
     * 请改用 {@link #lua(String, List, Class, String...)}（本方法已改为委托给它，行为已修正）。
     */
    @Deprecated(since = "3.0.0")
    public static <T> T lua(String script, List<String> keys, List<String> args, Class<T> retType) {
        String[] argArray = args == null ? new String[0] : args.toArray(new String[0]);
        return lua(script, keys, retType, argArray);
    }

    /**
     * 执行一次性 Lua（整串脚本，低频用）
     *
     * <p>ARGV 逐个作为 {@code String} 下发，序列化器固定为字符串（见
     * {@link #lua(String, List, List, Class)} 的说明）。调用示例：</p>
     *
     * <pre>
     *   Long value = RedisUtil.lua(SCRIPT, List.of(key), Long.class, "1", "7200");
     * </pre>
     *
     * @param script  完整 Lua 源码
     * @param keys    KEYS 数组
     * @param retType 返回 Java 类型
     * @param args    ARGV 数组（逐个标量）
     * @param <T>     返回泛型
     */
    @SuppressWarnings("unchecked")
    public static <T> T lua(String script, List<String> keys, Class<T> retType, String... args) {
        RedisScript<T> redisScript = new DefaultRedisScript<>(script, retType);
        RedisSerializer<String> stringSerializer = RedisSerializer.string();
        return (T) redisTemplate.execute(redisScript, stringSerializer, (RedisSerializer<T>) stringSerializer,
            keys, args);
    }

    /**
     * 执行一次性 Lua（整串脚本，低频用）
     *
     * @param script   完整 Lua 源码
     * @param retType  返回 Java 类型
     * @param <T>      返回泛型
     */
    public static <T> T lua(String script, Class<T> retType) {
        return lua(script, Collections.emptyList(), retType);
    }

    /**
     * 执行一次性 Lua（整串脚本，低频用）
     *
     * @param script   完整 Lua 源码
     * @param key      KEYS[1]
     * @param retType  返回 Java 类型
     * @param <T>      返回泛型
     */
    public static <T> T lua(String script, String key, Class<T> retType) {
        return lua(script, List.of(key), retType);
    }

    /**
     * 执行一次性 Lua（整串脚本，低频用）
     *
     * @param script   完整 Lua 源码
     * @param key      KEYS[1]
     * @param arg      ARGV[1]
     * @param retType  返回 Java 类型
     * @param <T>      返回泛型
     */
    public static <T> T lua(String script, String key, String arg, Class<T> retType) {
        return lua(script, List.of(key), List.of(arg), retType);
    }

    // endregion----------------------   Lua 脚本工具 end   ------------------------

    // region----------------------  分布式锁 start  ------------------------

    /**
     * 尝试获取分布式锁（SET key value NX EX）
     *
     * @param lockKey       锁 key
     * @param requestId     请求标识（用于安全释放锁，如 UUID）
     * @param expireSeconds 过期时间（秒）
     * @return true 获取成功，false 获取失败
     */
    public static boolean tryLock(@NonNull String lockKey, @NonNull String requestId, long expireSeconds) {
        try {
            Boolean success = valueOperations.setIfAbsent(lockKey, requestId, expireSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(success);
        } catch (Exception e) {
            log.error("Redis tryLock error, lockKey: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 尝试获取分布式锁（带重试）
     *
     * @param lockKey        锁 key
     * @param requestId      请求标识
     * @param expireSeconds  过期时间（秒）
     * @param retryTimes     重试次数
     * @param retryInterval  重试间隔（毫秒）
     * @return true 获取成功，false 获取失败
     */
    public static boolean tryLock(@NonNull String lockKey, @NonNull String requestId, long expireSeconds,
                                  int retryTimes, long retryInterval) {
        for (int i = 0; i <= retryTimes; i++) {
            if (tryLock(lockKey, requestId, expireSeconds)) {
                return true;
            }
            if (i < retryTimes) {
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 释放分布式锁（使用 Lua 脚本确保原子性）
     * <p>只有锁的持有者才能释放锁</p>
     *
     * @param lockKey   锁 key
     * @param requestId 请求标识（必须与加锁时一致）
     * @return true 释放成功，false 释放失败或不是锁的持有者
     */
    public static boolean unlock(@NonNull String lockKey, @NonNull String requestId) {
        // Lua 脚本：只有 value 匹配时才删除 key
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        try {
            RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
            Long result = redisTemplate.execute(redisScript, List.of(lockKey), requestId);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Redis unlock error, lockKey: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 续期分布式锁（看门狗机制）
     * <p>只有锁的持有者才能续期</p>
     *
     * @param lockKey       锁 key
     * @param requestId     请求标识
     * @param expireSeconds 新的过期时间（秒）
     * @return true 续期成功，false 续期失败或锁不存在/不属于当前持有者
     */
    public static boolean renewLock(@NonNull String lockKey, @NonNull String requestId, long expireSeconds) {
        // Lua 脚本：只有 value 匹配时才续期
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('expire', KEYS[1], ARGV[2]) else return 0 end";
        try {
            RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
            Long result = redisTemplate.execute(redisScript, List.of(lockKey), requestId, String.valueOf(expireSeconds));
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Redis renewLock error, lockKey: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 检查锁是否被当前请求持有
     *
     * @param lockKey   锁 key
     * @param requestId 请求标识
     * @return true 是当前持有者，false 不是持有者或锁不存在
     */
    public static boolean isLockHolder(@NonNull String lockKey, @NonNull String requestId) {
        try {
            Object value = valueOperations.get(lockKey);
            return requestId.equals(value);
        } catch (Exception e) {
            log.error("Redis isLockHolder error, lockKey: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 获取锁的剩余过期时间
     *
     * @param lockKey 锁 key
     * @return 剩余秒数，-1 表示未设置过期时间，-2 表示 key 不存在
     */
    public static Long getLockTtl(@NonNull String lockKey) {
        return getExpire(lockKey);
    }

    // endregion----------------------   分布式锁 end   ------------------------

    // region----------------------  ZSet 操作 start  ------------------------

    /**
     * 向有序集合添加成员
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return true 添加成功，false 添加失败
     */
    public static <T> boolean addZSet(@NonNull String key, @NonNull T value, double score) {
        try {
            Boolean added = zSetOperations.add(key, value, score);
            return Boolean.TRUE.equals(added);
        } catch (Exception e) {
            log.error("Redis addZSet error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 向有序集合添加成员（带过期时间）
     *
     * @param key     键
     * @param value   值
     * @param score   分数
     * @param timeout 过期时间（秒）
     * @return true 添加成功
     */
    public static <T> boolean addZSet(@NonNull String key, @NonNull T value, double score, long timeout) {
        boolean added = addZSet(key, value, score);
        if (added && timeout > 0) {
            setExpire(key, timeout);
        }
        return added;
    }

    /**
     * 批量添加成员到有序集合
     *
     * @param key          键
     * @param typedTuples  成员和分数集合
     * @return 添加成功的数量
     */
    public static Long addZSet(@NonNull String key, @NonNull Set<ZSetOperations.TypedTuple<Object>> typedTuples) {
        try {
            Long added = zSetOperations.add(key, typedTuples);
            return added != null ? added : 0L;
        } catch (Exception e) {
            log.error("Redis addZSet batch error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 移除有序集合中的成员
     *
     * @param key    键
     * @param values 要移除的值
     * @return 移除成功的数量
     */
    public static Long removeZSet(@NonNull String key, Object... values) {
        try {
            Long removed = zSetOperations.remove(key, values);
            return removed != null ? removed : 0L;
        } catch (Exception e) {
            log.error("Redis removeZSet error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 增加成员的分数
     *
     * @param key   键
     * @param value 值
     * @param delta 增量
     * @return 增加后的分数
     */
    public static Double incrementZSet(@NonNull String key, @NonNull Object value, double delta) {
        try {
            return zSetOperations.incrementScore(key, value, delta);
        } catch (Exception e) {
            log.error("Redis incrementZSet error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取有序集合中指定成员的分数
     *
     * @param key   键
     * @param value 值
     * @return 分数，成员不存在返回 null
     */
    public static Double scoreZSet(@NonNull String key, @NonNull Object value) {
        try {
            return zSetOperations.score(key, value);
        } catch (Exception e) {
            log.error("Redis scoreZSet error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取成员在有序集合中的排名（从 0 开始，分数从小到大）
     *
     * @param key   键
     * @param value 值
     * @return 排名，不存在返回 null
     */
    public static Long rankZSet(@NonNull String key, @NonNull Object value) {
        try {
            return zSetOperations.rank(key, value);
        } catch (Exception e) {
            log.error("Redis rankZSet error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取成员在有序集合中的排名（从 0 开始，分数从大到小）
     *
     * @param key   键
     * @param value 值
     * @return 排名，不存在返回 null
     */
    public static Long reverseRankZSet(@NonNull String key, @NonNull Object value) {
        try {
            return zSetOperations.reverseRank(key, value);
        } catch (Exception e) {
            log.error("Redis reverseRankZSet error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取指定范围内的成员（分数从小到大）
     *
     * @param key   键
     * @param start 起始位置（包含）
     * @param end   结束位置（包含，-1 表示最后一个）
     * @return 成员集合
     */
    public static <T> Set<T> rangeZSet(@NonNull String key, long start, long end) {
        try {
            return (Set<T>) zSetOperations.range(key, start, end);
        } catch (Exception e) {
            log.error("Redis rangeZSet error, key: {}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取指定范围内的成员（分数从大到小）
     *
     * @param key   键
     * @param start 起始位置（包含）
     * @param end   结束位置（包含，-1 表示最后一个）
     * @return 成员集合
     */
    public static <T> Set<T> reverseRangeZSet(@NonNull String key, long start, long end) {
        try {
            return (Set<T>) zSetOperations.reverseRange(key, start, end);
        } catch (Exception e) {
            log.error("Redis reverseRangeZSet error, key: {}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取指定范围内的成员和分数（分数从小到大）
     *
     * @param key   键
     * @param start 起始位置
     * @param end   结束位置
     * @return 成员和分数集合
     */
    public static Set<ZSetOperations.TypedTuple<Object>> rangeZSetWithScores(@NonNull String key, long start, long end) {
        try {
            return zSetOperations.rangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error("Redis rangeZSetWithScores error, key: {}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取指定分数范围内的成员（分数从小到大）
     *
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 成员集合
     */
    public static <T> Set<T> rangeByScoreZSet(@NonNull String key, double min, double max) {
        try {
            return (Set<T>) zSetOperations.rangeByScore(key, min, max);
        } catch (Exception e) {
            log.error("Redis rangeByScoreZSet error, key: {}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取指定分数范围内的成员（分数从大到小）
     *
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 成员集合
     */
    public static <T> Set<T> reverseRangeByScoreZSet(@NonNull String key, double min, double max) {
        try {
            return (Set<T>) zSetOperations.reverseRangeByScore(key, min, max);
        } catch (Exception e) {
            log.error("Redis reverseRangeByScoreZSet error, key: {}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取指定分数范围内的成员数量
     *
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 数量
     */
    public static Long countZSet(@NonNull String key, double min, double max) {
        try {
            return zSetOperations.count(key, min, max);
        } catch (Exception e) {
            log.error("Redis countZSet error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 获取有序集合的大小
     *
     * @param key 键
     * @return 大小
     */
    public static Long sizeZSet(@NonNull String key) {
        try {
            return zSetOperations.size(key);
        } catch (Exception e) {
            log.error("Redis sizeZSet error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 移除指定排名范围内的成员
     *
     * @param key   键
     * @param start 起始排名
     * @param end   结束排名
     * @return 移除的数量
     */
    public static Long removeRangeZSet(@NonNull String key, long start, long end) {
        try {
            Long removed = zSetOperations.removeRange(key, start, end);
            return removed != null ? removed : 0L;
        } catch (Exception e) {
            log.error("Redis removeRangeZSet error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 移除指定分数范围内的成员
     *
     * @param key 键
     * @param min 最小分数
     * @param max 最大分数
     * @return 移除的数量
     */
    public static Long removeRangeByScoreZSet(@NonNull String key, double min, double max) {
        try {
            Long removed = zSetOperations.removeRangeByScore(key, min, max);
            return removed != null ? removed : 0L;
        } catch (Exception e) {
            log.error("Redis removeRangeByScoreZSet error, key: {}", key, e);
            return 0L;
        }
    }

    // endregion----------------------   ZSet 操作 end   ------------------------

    // region----------------------  List 操作完善 start  ------------------------

    /**
     * 从列表左侧弹出元素
     *
     * @param key 键
     * @return 弹出的元素，不存在返回 null
     */
    public static <T> T leftPopList(@NonNull String key) {
        try {
            return (T) listOperations.leftPop(key);
        } catch (Exception e) {
            log.error("Redis leftPopList error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 从列表左侧阻塞弹出元素
     *
     * @param key     键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 弹出的元素，超时返回 null
     */
    public static <T> T leftPopList(@NonNull String key, long timeout, TimeUnit unit) {
        try {
            return (T) listOperations.leftPop(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis leftPopList block error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 从列表右侧弹出元素
     *
     * @param key 键
     * @return 弹出的元素，不存在返回 null
     */
    public static <T> T rightPopList(@NonNull String key) {
        try {
            return (T) listOperations.rightPop(key);
        } catch (Exception e) {
            log.error("Redis rightPopList error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 从列表右侧阻塞弹出元素
     *
     * @param key     键
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 弹出的元素，超时返回 null
     */
    public static <T> T rightPopList(@NonNull String key, long timeout, TimeUnit unit) {
        try {
            return (T) listOperations.rightPop(key, timeout, unit);
        } catch (Exception e) {
            log.error("Redis rightPopList block error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 从 sourceKey 右侧弹出元素并压入 destinationKey 左侧（原子操作）
     *
     * @param sourceKey      源列表
     * @param destinationKey 目标列表
     * @return 弹出的元素
     */
    public static <T> T rightPopAndLeftPush(@NonNull String sourceKey, @NonNull String destinationKey) {
        try {
            return (T) listOperations.rightPopAndLeftPush(sourceKey, destinationKey);
        } catch (Exception e) {
            log.error("Redis rightPopAndLeftPush error, sourceKey: {}, destinationKey: {}", sourceKey, destinationKey, e);
            return null;
        }
    }

    /**
     * 从 sourceKey 右侧阻塞弹出元素并压入 destinationKey 左侧
     *
     * @param sourceKey      源列表
     * @param destinationKey 目标列表
     * @param timeout        超时时间
     * @param unit           时间单位
     * @return 弹出的元素
     */
    public static <T> T rightPopAndLeftPush(@NonNull String sourceKey, @NonNull String destinationKey,
                                            long timeout, TimeUnit unit) {
        try {
            return (T) listOperations.rightPopAndLeftPush(sourceKey, destinationKey, timeout, unit);
        } catch (Exception e) {
            log.error("Redis rightPopAndLeftPush block error, sourceKey: {}, destinationKey: {}",
                sourceKey, destinationKey, e);
            return null;
        }
    }

    /**
     * 获取列表大小
     *
     * @param key 键
     * @return 列表大小
     */
    public static Long sizeList(@NonNull String key) {
        try {
            return listOperations.size(key);
        } catch (Exception e) {
            log.error("Redis sizeList error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 从列表中移除元素
     *
     * @param key   键
     * @param count 移除数量（0=全部，正数=从左往右，负数=从右往左）
     * @param value 要移除的值
     * @return 实际移除的数量
     */
    public static Long removeList(@NonNull String key, long count, Object value) {
        try {
            Long removed = listOperations.remove(key, count, value);
            return removed != null ? removed : 0L;
        } catch (Exception e) {
            log.error("Redis removeList error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 修剪列表，只保留指定范围内的元素
     *
     * @param key   键
     * @param start 起始位置
     * @param end   结束位置
     * @return true 成功
     */
    public static boolean trimList(@NonNull String key, long start, long end) {
        try {
            listOperations.trim(key, start, end);
            return true;
        } catch (Exception e) {
            log.error("Redis trimList error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取元素在列表中的索引
     *
     * @param key   键
     * @param value 值
     * @return 索引位置，不存在返回 null
     */
    public static Long indexOfList(@NonNull String key, Object value) {
        try {
            return listOperations.indexOf(key, value);
        } catch (Exception e) {
            log.error("Redis indexOfList error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 设置列表中指定位置的值
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return true 成功
     */
    public static <T> boolean setList(@NonNull String key, long index, T value) {
        try {
            listOperations.set(key, index, value);
            return true;
        } catch (Exception e) {
            log.error("Redis setList error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取列表中指定位置的元素
     *
     * @param key   键
     * @param index 索引
     * @return 元素
     */
    public static <T> T indexList(@NonNull String key, long index) {
        try {
            return (T) listOperations.index(key, index);
        } catch (Exception e) {
            log.error("Redis indexList error, key: {}", key, e);
            return null;
        }
    }

    // endregion----------------------   List 操作完善 end   ------------------------

    // region----------------------  Set 运算 start  ------------------------

    /**
     * 获取两个集合的交集
     *
     * @param key1 集合1
     * @param key2 集合2
     * @return 交集
     */
    public static <T> Set<T> intersectSet(@NonNull String key1, @NonNull String key2) {
        try {
            return (Set<T>) setOperations.intersect(key1, key2);
        } catch (Exception e) {
            log.error("Redis intersectSet error, key1: {}, key2: {}", key1, key2, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取多个集合的交集
     *
     * @param keys 集合 key 列表
     * @return 交集
     */
    public static <T> Set<T> intersectSet(Collection<String> keys) {
        try {
            return (Set<T>) setOperations.intersect(keys);
        } catch (Exception e) {
            log.error("Redis intersectSet error, keys: {}", keys, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取两个集合的交集并存储到新集合
     *
     * @param destKey 目标集合
     * @param key1    集合1
     * @param key2    集合2
     * @return 交集大小
     */
    public static Long intersectAndStoreSet(@NonNull String destKey, @NonNull String key1, @NonNull String key2) {
        try {
            Long size = setOperations.intersectAndStore(key1, key2, destKey);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("Redis intersectAndStoreSet error, key1: {}, key2: {}", key1, key2, e);
            return 0L;
        }
    }

    /**
     * 获取两个集合的并集
     *
     * @param key1 集合1
     * @param key2 集合2
     * @return 并集
     */
    public static <T> Set<T> unionSet(@NonNull String key1, @NonNull String key2) {
        try {
            return (Set<T>) setOperations.union(key1, key2);
        } catch (Exception e) {
            log.error("Redis unionSet error, key1: {}, key2: {}", key1, key2, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取多个集合的并集
     *
     * @param keys 集合 key 列表
     * @return 并集
     */
    public static <T> Set<T> unionSet(Collection<String> keys) {
        try {
            return (Set<T>) setOperations.union(keys);
        } catch (Exception e) {
            log.error("Redis unionSet error, keys: {}", keys, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取两个集合的并集并存储到新集合
     *
     * @param destKey 目标集合
     * @param key1    集合1
     * @param key2    集合2
     * @return 并集大小
     */
    public static Long unionAndStoreSet(@NonNull String destKey, @NonNull String key1, @NonNull String key2) {
        try {
            Long size = setOperations.unionAndStore(key1, key2, destKey);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("Redis unionAndStoreSet error, key1: {}, key2: {}", key1, key2, e);
            return 0L;
        }
    }

    /**
     * 获取两个集合的差集（key1 中有但 key2 中没有）
     *
     * @param key1 集合1
     * @param key2 集合2
     * @return 差集
     */
    public static <T> Set<T> differenceSet(@NonNull String key1, @NonNull String key2) {
        try {
            return (Set<T>) setOperations.difference(key1, key2);
        } catch (Exception e) {
            log.error("Redis differenceSet error, key1: {}, key2: {}", key1, key2, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取多个集合的差集
     *
     * @param key      基准集合
     * @param otherKeys 其他集合
     * @return 差集
     */
    public static <T> Set<T> differenceSet(@NonNull String key, Collection<String> otherKeys) {
        try {
            return (Set<T>) setOperations.difference(key, otherKeys);
        } catch (Exception e) {
            log.error("Redis differenceSet error, key: {}, otherKeys: {}", key, otherKeys, e);
            return Collections.emptySet();
        }
    }

    /**
     * 获取两个集合的差集并存储到新集合
     *
     * @param destKey 目标集合
     * @param key1    集合1
     * @param key2    集合2
     * @return 差集大小
     */
    public static Long differenceAndStoreSet(@NonNull String destKey, @NonNull String key1, @NonNull String key2) {
        try {
            Long size = setOperations.differenceAndStore(key1, key2, destKey);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.error("Redis differenceAndStoreSet error, key1: {}, key2: {}", key1, key2, e);
            return 0L;
        }
    }

    /**
     * 随机获取一个元素（不移除）
     *
     * @param key 键
     * @return 随机元素
     */
    public static <T> T randomMemberSet(@NonNull String key) {
        try {
            return (T) setOperations.randomMember(key);
        } catch (Exception e) {
            log.error("Redis randomMemberSet error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 随机获取多个元素（不移除，可能有重复）
     *
     * @param key   键
     * @param count 数量
     * @return 随机元素列表
     */
    public static <T> List<T> randomMembersSet(@NonNull String key, long count) {
        try {
            return (List<T>) setOperations.randomMembers(key, count);
        } catch (Exception e) {
            log.error("Redis randomMembersSet error, key: {}", key, e);
            return Collections.emptyList();
        }
    }

    /**
     * 随机获取多个不重复元素（不移除）
     *
     * @param key   键
     * @param count 数量
     * @return 随机元素集合
     */
    public static <T> Set<T> distinctRandomMembersSet(@NonNull String key, long count) {
        try {
            return (Set<T>) setOperations.distinctRandomMembers(key, count);
        } catch (Exception e) {
            log.error("Redis distinctRandomMembersSet error, key: {}", key, e);
            return Collections.emptySet();
        }
    }

    /**
     * 随机移除并返回指定数量的元素
     *
     * @param key   键
     * @param count 数量
     * @return 移除的元素列表
     */
    public static <T> List<T> popSet(@NonNull String key, long count) {
        try {
            return (List<T>) setOperations.pop(key, count);
        } catch (Exception e) {
            log.error("Redis popSet error, key: {}", key, e);
            return Collections.emptyList();
        }
    }

    // endregion----------------------   Set 运算 end   ------------------------

    // region----------------------  位图操作 start  ------------------------

    /**
     * 设置位图中指定偏移量的值
     *
     * @param key    键
     * @param offset 偏移量（从 0 开始）
     * @param value  true/false
     * @return 设置前的值
     */
    public static Boolean setBit(@NonNull String key, long offset, boolean value) {
        try {
            return valueOperations.setBit(key, offset, value);
        } catch (Exception e) {
            log.error("Redis setBit error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 获取位图中指定偏移量的值
     *
     * @param key    键
     * @param offset 偏移量
     * @return true/false
     */
    public static Boolean getBit(@NonNull String key, long offset) {
        try {
            return valueOperations.getBit(key, offset);
        } catch (Exception e) {
            log.error("Redis getBit error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 统计位图中值为 1 的位数
     *
     * @param key 键
     * @return 1 的数量
     */
    public static Long bitCount(@NonNull String key) {
        try {
            return redisTemplate.execute((RedisCallback<Long>) conn -> conn.stringCommands().bitCount(rawKey(key)));
        } catch (Exception e) {
            log.error("Redis bitCount error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 统计位图中指定字节范围内值为 1 的位数
     *
     * @param key   键
     * @param start 起始字节
     * @param end   结束字节
     * @return 1 的数量
     */
    public static Long bitCount(@NonNull String key, long start, long end) {
        try {
            return redisTemplate.execute((RedisCallback<Long>) conn -> conn.stringCommands().bitCount(rawKey(key), start, end));
        } catch (Exception e) {
            log.error("Redis bitCount range error, key: {}", key, e);
            return 0L;
        }
    }

    /**
     * 位图与运算（BITOP AND）
     *
     * @param destKey 目标 key
     * @param keys    源 key 数组
     * @return 结果位图的大小（字节数）
     */
    public static Long bitOpAnd(@NonNull String destKey, String... keys) {
        try {
            return redisTemplate.execute((RedisCallback<Long>) conn -> {
                byte[][] rawKeys = Arrays.stream(keys).map(RedisUtil::rawKey).toArray(byte[][]::new);
                return conn.stringCommands().bitOp(RedisStringCommands.BitOperation.AND, rawKey(destKey), rawKeys);
            });
        } catch (Exception e) {
            log.error("Redis bitOpAnd error, destKey: {}", destKey, e);
            return 0L;
        }
    }

    /**
     * 位图或运算（BITOP OR）
     *
     * @param destKey 目标 key
     * @param keys    源 key 数组
     * @return 结果位图的大小（字节数）
     */
    public static Long bitOpOr(@NonNull String destKey, String... keys) {
        try {
            return redisTemplate.execute((RedisCallback<Long>) conn -> {
                byte[][] rawKeys = Arrays.stream(keys).map(RedisUtil::rawKey).toArray(byte[][]::new);
                return conn.stringCommands().bitOp(RedisStringCommands.BitOperation.OR, rawKey(destKey), rawKeys);
            });
        } catch (Exception e) {
            log.error("Redis bitOpOr error, destKey: {}", destKey, e);
            return 0L;
        }
    }

    /**
     * 位图异或运算（BITOP XOR）
     *
     * @param destKey 目标 key
     * @param keys    源 key 数组
     * @return 结果位图的大小（字节数）
     */
    public static Long bitOpXor(@NonNull String destKey, String... keys) {
        try {
            return redisTemplate.execute((RedisCallback<Long>) conn -> {
                byte[][] rawKeys = Arrays.stream(keys).map(RedisUtil::rawKey).toArray(byte[][]::new);
                return conn.stringCommands().bitOp(RedisStringCommands.BitOperation.XOR, rawKey(destKey), rawKeys);
            });
        } catch (Exception e) {
            log.error("Redis bitOpXor error, destKey: {}", destKey, e);
            return 0L;
        }
    }

    /**
     * 位图非运算（BITOP NOT）
     *
     * @param destKey 目标 key
     * @param key     源 key
     * @return 结果位图的大小（字节数）
     */
    public static Long bitOpNot(@NonNull String destKey, String key) {
        try {
            return redisTemplate.execute((RedisCallback<Long>) conn ->
                conn.stringCommands().bitOp(RedisStringCommands.BitOperation.NOT, rawKey(destKey), rawKey(key)));
        } catch (Exception e) {
            log.error("Redis bitOpNot error, destKey: {}", destKey, e);
            return 0L;
        }
    }

    /**
     * 查找第一个值为指定 bit 的位置
     *
     * @param key 键
     * @param bit true/false
     * @return 位置，不存在返回 -1
     */
    public static Long bitPos(@NonNull String key, boolean bit) {
        try {
            return redisTemplate.execute((RedisCallback<Long>) conn -> conn.stringCommands().bitPos(rawKey(key), bit));
        } catch (Exception e) {
            log.error("Redis bitPos error, key: {}", key, e);
            return -1L;
        }
    }

    /**
     * 在指定字节范围内查找第一个值为指定 bit 的位置
     *
     * @param key   键
     * @param bit   true/false
     * @param start 起始字节
     * @param end   结束字节
     * @return 位置，不存在返回 -1
     */
    public static Long bitPos(@NonNull String key, boolean bit, long start, long end) {
        try {
            return redisTemplate.execute((RedisCallback<Long>) conn -> conn.stringCommands().bitPos(rawKey(key), bit, Range.closed(start, end)));
        } catch (Exception e) {
            log.error("Redis bitPos range error, key: {}", key, e);
            return -1L;
        }
    }

    // endregion----------------------   位图操作 end   ------------------------

    // region----------------------  HyperLogLog start  ------------------------

    /**
     * 添加元素到 HyperLogLog
     *
     * @param key      键
     * @param elements 元素数组
     * @return true 添加了新元素，false 未添加新元素（可能已存在）
     */
    public static boolean addHyperLogLog(@NonNull String key, Object... elements) {
        try {
            Long added = redisTemplate.opsForHyperLogLog().add(key, elements);
            return added > 0;
        } catch (Exception e) {
            log.error("Redis addHyperLogLog error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取 HyperLogLog 的基数估算值
     *
     * @param keys 键数组
     * @return 基数估算值
     */
    public static Long sizeHyperLogLog(String... keys) {
        try {
            return redisTemplate.opsForHyperLogLog().size(keys);
        } catch (Exception e) {
            log.error("Redis sizeHyperLogLog error, keys: {}", Arrays.toString(keys), e);
            return 0L;
        }
    }

    /**
     * 合并多个 HyperLogLog 到新 key
     *
     * @param destKey     目标 key
     * @param sourceKeys  源 key 数组
     * @return true 成功
     */
    public static boolean unionHyperLogLog(@NonNull String destKey, String... sourceKeys) {
        try {
            Long result = redisTemplate.opsForHyperLogLog().union(destKey, sourceKeys);
            return result != null;
        } catch (Exception e) {
            log.error("Redis unionHyperLogLog error, destKey: {}", destKey, e);
            return false;
        }
    }

    /**
     * 删除 HyperLogLog
     *
     * @param key 键
     * @return true 成功
     */
    public static boolean deleteHyperLogLog(@NonNull String key) {
        return delete(key);
    }

    // endregion----------------------   HyperLogLog end   ------------------------

    // region----------------------  通用方法完善 start  ------------------------

    /**
     * 重命名 key
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return true 成功
     */
    public static boolean renameKey(@NonNull String oldKey, @NonNull String newKey) {
        try {
            redisTemplate.rename(oldKey, newKey);
            return true;
        } catch (Exception e) {
            log.error("Redis renameKey error, oldKey: {}, newKey: {}", oldKey, newKey, e);
            return false;
        }
    }

    /**
     * 仅当 newKey 不存在时才重命名
     *
     * @param oldKey 旧 key
     * @param newKey 新 key
     * @return true 成功，false newKey 已存在
     */
    public static boolean renameKeyIfAbsent(@NonNull String oldKey, @NonNull String newKey) {
        try {
            Boolean renamed = redisTemplate.renameIfAbsent(oldKey, newKey);
            return Boolean.TRUE.equals(renamed);
        } catch (Exception e) {
            log.error("Redis renameKeyIfAbsent error, oldKey: {}, newKey: {}", oldKey, newKey, e);
            return false;
        }
    }

    /**
     * 将 key 移动到指定数据库
     *
     * @param key     键
     * @param dbIndex 数据库索引
     * @return true 成功
     */
    public static boolean moveKey(@NonNull String key, int dbIndex) {
        try {
            Boolean moved = redisTemplate.move(key, dbIndex);
            return Boolean.TRUE.equals(moved);
        } catch (Exception e) {
            log.error("Redis moveKey error, key: {}, dbIndex: {}", key, dbIndex, e);
            return false;
        }
    }

    /**
     * 获取随机 key
     *
     * @return 随机 key，不存在返回 null
     */
    public static String randomKey() {
        try {
            return redisTemplate.randomKey();
        } catch (Exception e) {
            log.error("Redis randomKey error", e);
            return null;
        }
    }

    /**
     * 移除 key 的过期时间，使其永久有效
     *
     * @param key 键
     * @return true 成功
     */
    public static boolean persistKey(@NonNull String key) {
        try {
            Boolean persisted = redisTemplate.persist(key);
            return Boolean.TRUE.equals(persisted);
        } catch (Exception e) {
            log.error("Redis persistKey error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取当前数据库的 key 数量
     *
     * @return key 数量
     */
    public static Long getDbSize() {
        try {
            return redisTemplate.execute(RedisServerCommands::dbSize);
        } catch (Exception e) {
            log.error("Redis getDbSize error", e);
            return 0L;
        }
    }

    /**
     * 原子设置值并同时设置过期时间（SET key value EX seconds NX）
     *
     * @param key     键
     * @param value   值
     * @param seconds 过期时间（秒）
     * @return true 设置成功，false 键已存在
     */
    public static <T> boolean setValueWithSetNxEx(@NonNull String key, @NonNull T value, long seconds) {
        try {
            Boolean set = valueOperations.setIfAbsent(key, value, seconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(set);
        } catch (Exception e) {
            log.error("Redis setValueWithSetNxEx error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 比较并设置值（CAS 操作）
     *
     * @param key      键
     * @param expected 期望值
     * @param newValue 新值
     * @return true 设置成功（原值匹配），false 设置失败
     */
    public static <T> boolean compareAndSet(@NonNull String key, T expected, T newValue) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then redis.call('set', KEYS[1], ARGV[2]) return 1 else return 0 end";
        try {
            RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
            Long result = redisTemplate.execute(redisScript, List.of(key), expected, newValue);
            return result != null && result > 0;
        } catch (Exception e) {
            log.error("Redis compareAndSet error, key: {}", key, e);
            return false;
        }
    }

    // endregion----------------------   通用方法完善 end   ------------------------

    // region----------------------   Pub/Sub 发布订阅   ------------------------

    /**
     * 发布消息到指定频道
     *
     * @param channel 频道名称
     * @param message 消息内容
     */
    public static void convertAndSend(String channel, Object message) {
        try {
            redisTemplate.convertAndSend(channel, message);
            log.debug("Published message to channel: {}", channel);
        } catch (Exception e) {
            log.error("Failed to publish message to channel: {}", channel, e);
        }
    }

    // endregion----------------------   Pub/Sub 发布订阅 end   ------------------------

}
