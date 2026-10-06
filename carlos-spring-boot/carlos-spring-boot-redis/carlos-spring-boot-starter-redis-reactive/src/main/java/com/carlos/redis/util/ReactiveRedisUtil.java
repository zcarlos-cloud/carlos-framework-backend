package com.carlos.redis.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Reactive Redis 工具类
 * <p>
 * 为 Spring Cloud Gateway 等 WebFlux 应用提供 Reactive Redis 操作支持。
 * 所有方法返回 Mono/Flux，支持响应式编程模型。
 * </p>
 *
 * <p>
 * 注意事项：
 * 1. 所有方法都已处理空值和异常情况
 * 2. 使用 Reactive 流处理数据
 * 3. 支持缓存防击穿（互斥锁）
 * </p>
 *
 * @author carlos
 * @date 2026-03-24
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
@Slf4j
public class ReactiveRedisUtil {

    private static ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private static ReactiveValueOperations<String, Object> reactiveValueOperations;
    private static ReactiveHashOperations<String, String, Object> reactiveHashOperations;
    private static ReactiveListOperations<String, Object> reactiveListOperations;
    private static ReactiveSetOperations<String, Object> reactiveSetOperations;
    private static ReactiveZSetOperations<String, Object> reactiveZSetOperations;
    private static ReactiveRedisConnectionFactory connectionFactory;

    /**
     * 初始化 ReactiveRedisUtil 静态字段
     */
    public static void init(
        ReactiveRedisTemplate<String, Object> reactiveRedisTemplate,
        ReactiveValueOperations<String, Object> reactiveValueOperations,
        ReactiveHashOperations<String, String, Object> reactiveHashOperations,
        ReactiveListOperations<String, Object> reactiveListOperations,
        ReactiveSetOperations<String, Object> reactiveSetOperations,
        ReactiveZSetOperations<String, Object> reactiveZSetOperations,
        ReactiveRedisConnectionFactory connectionFactory) {
        ReactiveRedisUtil.reactiveRedisTemplate = reactiveRedisTemplate;
        ReactiveRedisUtil.reactiveValueOperations = reactiveValueOperations;
        ReactiveRedisUtil.reactiveHashOperations = reactiveHashOperations;
        ReactiveRedisUtil.reactiveListOperations = reactiveListOperations;
        ReactiveRedisUtil.reactiveSetOperations = reactiveSetOperations;
        ReactiveRedisUtil.reactiveZSetOperations = reactiveZSetOperations;
        ReactiveRedisUtil.connectionFactory = connectionFactory;
        log.info("ReactiveRedisUtil initialized");
    }

    // region----------------------  通用方法  ------------------------

    /**
     * 设置缓存失效时间
     *
     * @param key     键
     * @param timeout 时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    public static Mono<Boolean> setExpire(@NonNull String key, long timeout, TimeUnit unit) {
        return reactiveRedisTemplate.expire(key, Duration.ofNanos(unit.toNanos(timeout)))
            .onErrorResume(e -> {
                log.error("ReactiveRedis setExpire error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 设置缓存失效时间（秒）
     *
     * @param key     键
     * @param timeout 时间（秒）
     * @return 是否设置成功
     */
    public static Mono<Boolean> setExpire(@NonNull String key, long timeout) {
        return setExpire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置缓存失效时间
     *
     * @param key    键
     * @param expire 过期时间
     * @return 是否设置成功
     */
    public static Mono<Boolean> setExpire(@NonNull String key, Duration expire) {
        if (expire == null) {
            return Mono.just(false);
        }
        return setExpire(key, expire.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 获取缓存失效时间
     *
     * @param key 键
     * @return 时间（秒）-2 表示 key 不存在，-1 表示永久有效
     */
    public static Mono<Long> getExpire(@NonNull String key) {
        return reactiveRedisTemplate.getExpire(key)
            .map(Duration::getSeconds)
            .onErrorResume(e -> {
                log.error("ReactiveRedis getExpire error, key: {}", key, e);
                return Mono.just(-2L);
            });
    }

    /**
     * key 是否存在
     *
     * @param key 键
     * @return true 存在，false 不存在或出错
     */
    public static Mono<Boolean> hasKey(@NonNull String key) {
        return reactiveRedisTemplate.hasKey(key)
            .onErrorResume(e -> {
                log.error("ReactiveRedis hasKey error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 删除单个缓存
     *
     * @param key 缓存 key
     * @return 是否删除成功
     */
    public static Mono<Boolean> delete(@NonNull String key) {
        return reactiveRedisTemplate.delete(key)
            .map(deleted -> deleted > 0)
            .doOnSuccess(result -> {
                if (Boolean.TRUE.equals(result)) {
                    log.debug("ReactiveRedis delete success, key: {}", key);
                } else {
                    log.warn("ReactiveRedis delete fail or key not exist, key: {}", key);
                }
            })
            .onErrorResume(e -> {
                log.error("ReactiveRedis delete error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 批量删除缓存
     *
     * @param keys 待删 key 集合
     * @return 实际删除条数
     */
    public static Mono<Long> delete(@NonNull Collection<String> keys) {
        if (keys.isEmpty()) {
            return Mono.just(0L);
        }
        return reactiveRedisTemplate.delete(keys.toArray(new String[0]))
            .onErrorResume(e -> {
                log.error("ReactiveRedis delete batch error, keys: {}", keys, e);
                return Mono.just(0L);
            });
    }

    /**
     * 使用通配符删除缓存（扫描并删除）
     *
     * @param pattern 带通配符的 key
     * @return 删除条数
     */
    public static Mono<Long> deletePattern(@NonNull String pattern) {
        return scanKeys(pattern)
            .collectList()
            .flatMap(keys -> {
                if (keys.isEmpty()) {
                    return Mono.just(0L);
                }
                return delete(keys);
            });
    }

    /**
     * 扫描并获取符合条件的 key
     *
     * @param pattern key 通配符匹配
     * @return 匹配到的 key 流
     */
    public static Flux<String> scanKeys(@NonNull String pattern) {
        return reactiveRedisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build())
            .onErrorResume(e -> {
                log.error("ReactiveRedis scanKeys error, pattern: {}", pattern, e);
                return Flux.empty();
            });
    }

    // endregion

    // region----------------------  字符串数据结构操作  ------------------------

    /**
     * 如果缓存不存在设置（永不过期）
     *
     * @param key   键
     * @param value 值
     * @return true 成功，false 失败或 key 已存在
     */
    public static Mono<Boolean> setIfAbsent(@NonNull String key, @NonNull Object value) {
        return reactiveValueOperations.setIfAbsent(key, value)
            .onErrorResume(e -> {
                log.error("ReactiveRedis setIfAbsent error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 如果缓存不存在设置
     *
     * @param key     键
     * @param value   值
     * @param timeout 时间
     * @param unit    时间单位
     * @return true 成功，false 失败或 key 已存在
     */
    public static Mono<Boolean> setIfAbsent(@NonNull String key, @NonNull Object value,
                                            long timeout, TimeUnit unit) {
        return reactiveValueOperations.setIfAbsent(key, value, Duration.ofNanos(unit.toNanos(timeout)))
            .onErrorResume(e -> {
                log.error("ReactiveRedis setIfAbsent error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 如果缓存不存在设置
     *
     * @param key    键
     * @param value  值
     * @param expire 过期时间
     * @return true 成功，false 失败或 key 已存在
     */
    public static Mono<Boolean> setIfAbsent(@NonNull String key, @NonNull Object value, Duration expire) {
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
    public static Mono<Boolean> setValue(@NonNull String key, @NonNull Object value) {
        return reactiveValueOperations.set(key, value)
            .then(Mono.just(true))
            .onErrorResume(e -> {
                log.error("ReactiveRedis setValue error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 设置普通缓存
     *
     * @param key     键
     * @param value   值
     * @param timeout 时间（秒，小于等于 0 时设为无限期）
     * @return true 成功，false 失败
     */
    public static Mono<Boolean> setValue(@NonNull String key, @NonNull Object value, long timeout) {
        return setValue(key, value, timeout, TimeUnit.SECONDS);
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
    public static Mono<Boolean> setValue(@NonNull String key, @NonNull Object value,
                                         long timeout, TimeUnit unit) {
        if (timeout <= 0) {
            return setValue(key, value);
        }
        return reactiveValueOperations.set(key, value, Duration.ofNanos(unit.toNanos(timeout)))
            .then(Mono.just(true))
            .onErrorResume(e -> {
                log.error("ReactiveRedis setValue error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 设置普通缓存
     *
     * @param key    键
     * @param value  值
     * @param expire 过期时间
     * @return true 成功，false 失败
     */
    public static Mono<Boolean> setValue(@NonNull String key, @NonNull Object value, Duration expire) {
        if (expire == null) {
            return setValue(key, value);
        }
        return setValue(key, value, expire.getSeconds(), TimeUnit.SECONDS);
    }

    /**
     * 获取普通缓存
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<T> getValue(@NonNull String key) {
        return (Mono<T>) reactiveValueOperations.get(key)
            .onErrorResume(e -> {
                log.error("ReactiveRedis getValue error, key: {}", key, e);
                return Mono.empty();
            });
    }

    /**
     * 获取缓存，如果不存在则加载（防缓存击穿）
     *
     * @param key    键
     * @param loader 加载函数
     * @return 值
     */
    public static <T> Mono<T> getValue(@NonNull String key, Supplier<Mono<T>> loader) {
        return getValue(key, loader, null, null);
    }

    /**
     * 获取缓存，如果不存在则加载（防缓存击穿）
     *
     * @param key      键
     * @param loader   加载函数
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<T> getValue(@NonNull String key, Supplier<Mono<T>> loader,
                                       Long timeout, TimeUnit timeUnit) {
        return ((Mono<T>) getValue(key))
            .switchIfEmpty(Mono.defer(() -> loadWithLock(key, loader, timeout, timeUnit)));
    }

    @SuppressWarnings("unchecked")
    private static <T> Mono<T> loadWithLock(String key, Supplier<Mono<T>> loader,
                                            Long timeout, TimeUnit timeUnit) {
        // 使用 Redis 锁防止缓存击穿
        String lockKey = "framework:lock:" + key;
        return setIfAbsent(lockKey, "1", Duration.ofSeconds(10))
            .flatMap(locked -> {
                if (Boolean.TRUE.equals(locked)) {
                    // 获取锁成功，再次检查缓存
                    return ((Mono<T>) getValue(key))
                        .switchIfEmpty(Mono.defer(() ->
                            loader.get()
                                .flatMap(value -> {
                                    if (value != null) {
                                        if (timeout != null && timeUnit != null) {
                                            return setValue(key, value, timeout, timeUnit)
                                                .thenReturn(value);
                                        } else {
                                            return setValue(key, value)
                                                .thenReturn(value);
                                        }
                                    }
                                    return Mono.just(value);
                                })
                        ));
                } else {
                    // 获取锁失败，短暂等待后重试
                    return Mono.delay(Duration.ofMillis(100))
                        .flatMap(t -> getValue(key, loader, timeout, timeUnit));
                }
            })
            .doFinally(signal -> delete(lockKey).subscribe());
    }

    /**
     * 批量获取缓存
     *
     * @param keys key 集合
     * @return 值列表，与 key 顺序一一对应
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<List<T>> getValueList(@NonNull Collection<String> keys) {
        if (keys.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }
        return reactiveValueOperations.multiGet(keys)
            .map(list -> list.stream()
                .filter(Objects::nonNull)
                .map(v -> (T) v)
                .toList())
            .onErrorResume(e -> {
                log.error("ReactiveRedis getValueList error, keys: {}", keys, e);
                return Mono.just(Collections.emptyList());
            });
    }

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加几（大于 0）
     * @return 加上指定值之后 key 的值
     */
    public static Mono<Long> incrementValue(@NonNull String key, long delta) {
        if (delta <= 0) {
            log.warn("Increment delta must be > 0, got: {}", delta);
            return Mono.empty();
        }
        return reactiveValueOperations.increment(key, delta)
            .onErrorResume(e -> {
                log.error("ReactiveRedis incrementValue error, key: {}", key, e);
                return Mono.empty();
            });
    }

    /**
     * 递增并设置过期时间
     *
     * @param key      键
     * @param delta    要增加几
     * @param timeout  过期时间
     * @param timeUnit 时间单位
     * @return 加上指定值之后 key 的值
     */
    public static Mono<Long> incrementValue(@NonNull String key, long delta,
                                            long timeout, TimeUnit timeUnit) {
        return incrementValue(key, delta)
            .flatMap(value -> setExpire(key, timeout, timeUnit)
                .thenReturn(value));
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少几（大于 0）
     * @return 减少指定值之后 key 的值
     */
    public static Mono<Long> decrementValue(@NonNull String key, long delta) {
        if (delta <= 0) {
            log.warn("Decrement delta must be > 0, got: {}", delta);
            return Mono.empty();
        }
        return reactiveValueOperations.decrement(key, delta)
            .onErrorResume(e -> {
                log.error("ReactiveRedis decrementValue error, key: {}", key, e);
                return Mono.empty();
            });
    }

    // endregion

    // region----------------------  Hash 操作  ------------------------

    /**
     * 获取 Hash 中的值
     *
     * @param key  键
     * @param item 项
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<T> getHash(@NonNull String key, @NonNull String item) {
        return reactiveHashOperations.get(key, item)
            .map(v -> (T) v)
            .onErrorResume(e -> {
                log.error("ReactiveRedis getHash error, key: {}, item: {}", key, item, e);
                return Mono.empty();
            });
    }

    /**
     * 获取 hashKey 对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public static Mono<Map<String, Object>> getHash(@NonNull String key) {
        return reactiveHashOperations.entries(key)
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .onErrorResume(e -> {
                log.error("ReactiveRedis getHash error, key: {}", key, e);
                return Mono.just(Collections.emptyMap());
            });
    }

    /**
     * 设置 Hash
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return 是否成功
     */
    public static Mono<Boolean> setHash(@NonNull String key, @NonNull String item, @NonNull Object value) {
        return reactiveHashOperations.put(key, item, value)
            .onErrorResume(e -> {
                log.error("ReactiveRedis setHash error, key: {}, item: {}", key, item, e);
                return Mono.just(false);
            });
    }

    /**
     * 批量设置 Hash
     *
     * @param key 键
     * @param map 多个键值
     * @return 是否成功
     */
    public static Mono<Boolean> setHash(@NonNull String key, @NonNull Map<String, Object> map) {
        return reactiveHashOperations.putAll(key, map)
            .then(Mono.just(true))
            .onErrorResume(e -> {
                log.error("ReactiveRedis setHash error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 删除 Hash 中的字段
     *
     * @param key   键
     * @param items 字段
     * @return 删除的字段数
     */
    public static Mono<Long> deleteHash(@NonNull String key, @NonNull Object... items) {
        return reactiveHashOperations.remove(key, items)
            .onErrorResume(e -> {
                log.error("ReactiveRedis deleteHash error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    /**
     * 判断 Hash 中是否有该项
     *
     * @param key  键
     * @param item 项
     * @return true 存在
     */
    public static Mono<Boolean> hasHashKey(@NonNull String key, @NonNull String item) {
        return reactiveHashOperations.hasKey(key, item)
            .onErrorResume(e -> {
                log.error("ReactiveRedis hasHashKey error, key: {}, item: {}", key, item, e);
                return Mono.just(false);
            });
    }

    // endregion

    // region----------------------  List 操作  ------------------------

    /**
     * 获取 List 内容
     *
     * @param key   键
     * @param start 开始
     * @param end   结束（0 到 -1 表示所有值）
     * @return 列表
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<List<T>> getList(@NonNull String key, long start, long end) {
        return reactiveListOperations.range(key, start, end)
            .map(v -> (T) v)
            .collectList()
            .onErrorResume(e -> {
                log.error("ReactiveRedis getList error, key: {}", key, e);
                return Mono.just(Collections.emptyList());
            });
    }

    /**
     * 获取 List 长度
     *
     * @param key 键
     * @return 长度
     */
    public static Mono<Long> getListSize(@NonNull String key) {
        return reactiveListOperations.size(key)
            .onErrorResume(e -> {
                log.error("ReactiveRedis getListSize error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    /**
     * 通过索引获取 List 中的值
     *
     * @param key   键
     * @param index 索引（index >= 0 时，0 表头；index < 0 时，-1 表尾）
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<T> getListByIndex(@NonNull String key, long index) {
        return reactiveListOperations.index(key, index)
            .map(v -> (T) v)
            .onErrorResume(e -> {
                log.error("ReactiveRedis getListByIndex error, key: {}", key, e);
                return Mono.empty();
            });
    }

    /**
     * 向 List 右侧 push
     *
     * @param key   键
     * @param value 值
     * @return 列表长度
     */
    public static Mono<Long> listRightPush(@NonNull String key, @NonNull Object value) {
        return reactiveListOperations.rightPush(key, value)
            .onErrorResume(e -> {
                log.error("ReactiveRedis listRightPush error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    /**
     * 批量向 List 右侧 push
     *
     * @param key    键
     * @param values 值列表
     * @return 列表长度
     */
    @SafeVarargs
    public static Mono<Long> listRightPushAll(@NonNull String key, @NonNull Object... values) {
        return reactiveListOperations.rightPushAll(key, values)
            .onErrorResume(e -> {
                log.error("ReactiveRedis listRightPushAll error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    /**
     * 向 List 左侧 push
     *
     * @param key   键
     * @param value 值
     * @return 列表长度
     */
    public static Mono<Long> listLeftPush(@NonNull String key, @NonNull Object value) {
        return reactiveListOperations.leftPush(key, value)
            .onErrorResume(e -> {
                log.error("ReactiveRedis listLeftPush error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    /**
     * 从 List 左侧 pop
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<T> listLeftPop(@NonNull String key) {
        return reactiveListOperations.leftPop(key)
            .map(v -> (T) v)
            .onErrorResume(e -> {
                log.error("ReactiveRedis listLeftPop error, key: {}", key, e);
                return Mono.empty();
            });
    }

    /**
     * 从 List 右侧 pop
     *
     * @param key 键
     * @return 值
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<T> listRightPop(@NonNull String key) {
        return reactiveListOperations.rightPop(key)
            .map(v -> (T) v)
            .onErrorResume(e -> {
                log.error("ReactiveRedis listRightPop error, key: {}", key, e);
                return Mono.empty();
            });
    }

    // endregion

    // region----------------------  Set 操作  ------------------------

    /**
     * 向 Set 中添加成员
     *
     * @param key    键
     * @param values 值
     * @return 成功个数
     */
    @SafeVarargs
    public static Mono<Long> setAdd(@NonNull String key, @NonNull Object... values) {
        return reactiveSetOperations.add(key, values)
            .onErrorResume(e -> {
                log.error("ReactiveRedis setAdd error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    /**
     * 获取 Set 的所有成员
     *
     * @param key 键
     * @return 成员集合
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<Set<T>> setMembers(@NonNull String key) {
        return reactiveSetOperations.members(key)
            .map(v -> (T) v)
            .collect(Collectors.toSet())
            .onErrorResume(e -> {
                log.error("ReactiveRedis setMembers error, key: {}", key, e);
                return Mono.just(Collections.emptySet());
            });
    }

    /**
     * 判断是否是 Set 的成员
     *
     * @param key    键
     * @param member 成员
     * @return 是否是成员
     */
    public static Mono<Boolean> setIsMember(@NonNull String key, @NonNull Object member) {
        return reactiveSetOperations.isMember(key, member)
            .onErrorResume(e -> {
                log.error("ReactiveRedis setIsMember error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 获取 Set 的大小
     *
     * @param key 键
     * @return 大小
     */
    public static Mono<Long> setSize(@NonNull String key) {
        return reactiveSetOperations.size(key)
            .onErrorResume(e -> {
                log.error("ReactiveRedis setSize error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    /**
     * 从 Set 中移除成员
     *
     * @param key    键
     * @param values 成员
     * @return 移除个数
     */
    @SafeVarargs
    public static Mono<Long> setRemove(@NonNull String key, @NonNull Object... values) {
        return reactiveSetOperations.remove(key, values)
            .onErrorResume(e -> {
                log.error("ReactiveRedis setRemove error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    // endregion

    // region----------------------  ZSet 操作  ------------------------

    /**
     * 向 ZSet 中添加成员
     *
     * @param key   键
     * @param value 值
     * @param score 分数
     * @return 是否成功
     */
    public static Mono<Boolean> zSetAdd(@NonNull String key, @NonNull Object value, double score) {
        return reactiveZSetOperations.add(key, value, score)
            .onErrorResume(e -> {
                log.error("ReactiveRedis zSetAdd error, key: {}", key, e);
                return Mono.just(false);
            });
    }

    /**
     * 获取 ZSet 范围（按分数从低到高）
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 成员集合
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<Set<T>> zSetRange(@NonNull String key, long start, long end) {
        return reactiveZSetOperations.range(key, org.springframework.data.domain.Range.closed(start, end))
            .map(v -> (T) v)
            .collect(Collectors.toSet())
            .onErrorResume(e -> {
                log.error("ReactiveRedis zSetRange error, key: {}", key, e);
                return Mono.just(Collections.emptySet());
            });
    }

    /**
     * 获取 ZSet 范围（按分数从高到低）
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 成员集合
     */
    @SuppressWarnings("unchecked")
    public static <T> Mono<Set<T>> zSetReverseRange(@NonNull String key, long start, long end) {
        return reactiveZSetOperations.reverseRange(key, org.springframework.data.domain.Range.closed(start, end))
            .map(v -> (T) v)
            .collect(Collectors.toSet())
            .onErrorResume(e -> {
                log.error("ReactiveRedis zSetReverseRange error, key: {}", key, e);
                return Mono.just(Collections.emptySet());
            });
    }

    /**
     * 获取 ZSet 大小
     *
     * @param key 键
     * @return 大小
     */
    public static Mono<Long> zSetSize(@NonNull String key) {
        return reactiveZSetOperations.size(key)
            .onErrorResume(e -> {
                log.error("ReactiveRedis zSetSize error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    /**
     * 移除 ZSet 成员
     *
     * @param key    键
     * @param values 成员
     * @return 移除个数
     */
    @SafeVarargs
    public static Mono<Long> zSetRemove(@NonNull String key, @NonNull Object... values) {
        return reactiveZSetOperations.remove(key, values)
            .onErrorResume(e -> {
                log.error("ReactiveRedis zSetRemove error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    /**
     * 按排名范围移除 ZSet 成员
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置
     * @return 移除个数
     */
    public static Mono<Long> zSetRemoveRangeByRank(@NonNull String key, long start, long end) {
        return reactiveZSetOperations.removeRange(key, org.springframework.data.domain.Range.closed(start, end))
            .onErrorResume(e -> {
                log.error("ReactiveRedis zSetRemoveRangeByRank error, key: {}", key, e);
                return Mono.just(0L);
            });
    }

    // endregion

    // region----------------------  脚本执行  ------------------------

    /**
     * 执行 Lua 脚本
     *
     * @param script Lua 脚本
     * @param keys   键列表
     * @param args   参数列表
     * @return 执行结果
     */
    public static Mono<Long> executeLua(String script, List<String> keys, Object... args) {
        RedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        return reactiveRedisTemplate.execute(redisScript, keys, args)
            .next()
            .onErrorResume(e -> {
                log.error("ReactiveRedis executeLua error", e);
                return Mono.just(0L);
            });
    }

    // endregion

    // region----------------------  原始 Template 获取  ------------------------

    /**
     * 获取 ReactiveRedisTemplate（用于高级操作）
     *
     * @return ReactiveRedisTemplate
     */
    public static ReactiveRedisTemplate<String, Object> getTemplate() {
        return reactiveRedisTemplate;
    }

    /**
     * 获取 ReactiveValueOperations
     *
     * @return ReactiveValueOperations
     */
    public static ReactiveValueOperations<String, Object> getValueOps() {
        return reactiveValueOperations;
    }

    /**
     * 获取 ReactiveHashOperations
     *
     * @return ReactiveHashOperations
     */
    public static ReactiveHashOperations<String, String, Object> getHashOps() {
        return reactiveHashOperations;
    }

    /**
     * 获取 ReactiveListOperations
     *
     * @return ReactiveListOperations
     */
    public static ReactiveListOperations<String, Object> getListOps() {
        return reactiveListOperations;
    }

    /**
     * 获取 ReactiveSetOperations
     *
     * @return ReactiveSetOperations
     */
    public static ReactiveSetOperations<String, Object> getSetOps() {
        return reactiveSetOperations;
    }

    /**
     * 获取 ReactiveZSetOperations
     *
     * @return ReactiveZSetOperations
     */
    public static ReactiveZSetOperations<String, Object> getZSetOps() {
        return reactiveZSetOperations;
    }

    // endregion
}
