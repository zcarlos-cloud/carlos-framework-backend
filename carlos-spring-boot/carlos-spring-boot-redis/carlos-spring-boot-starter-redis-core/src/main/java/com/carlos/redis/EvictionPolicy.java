package com.carlos.redis;


/**
 * <p>
 *     缓存失效策略
 * </p>
 *
 * @author Carlos
 * @date 2025-12-07 16:51
 */
public enum EvictionPolicy {
    CacheAside,     // 业务自己维护
    ReadThrough,    // 缓存自动加载
    WriteThrough,   // 同步写穿
    WriteBehind     // 异步写回
}