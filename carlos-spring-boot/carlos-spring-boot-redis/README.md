# Carlos Spring Boot Redis

## 简介

`carlos-spring-boot-redis` 是 Redis 相关的聚合器模块，统一管理 Redis 相关的三个子模块：

- **carlos-spring-boot-starter-redis-core**: Redis 核心功能（Jedis/Lettuce 连接池、序列化等）
- **carlos-spring-boot-starter-redis**: 同步 Redis 操作 + Redisson 分布式锁 + Caffeine 多级缓存
- **carlos-spring-boot-starter-redis-reactive**: 响应式 Redis 操作（Reactive）

## 模块结构

```
carlos-spring-boot-redis/          # 聚合器 POM
├── carlos-spring-boot-starter-redis-core/
├── carlos-spring-boot-starter-redis/
└── carlos-spring-boot-starter-redis-reactive/
```

## 依赖关系

```
carlos-spring-boot-starter-redis
    ↓ (依赖)
carlos-spring-boot-starter-redis-core

carlos-spring-boot-starter-redis-reactive
    ↓ (依赖)
carlos-spring-boot-starter-redis-core
```

## 使用方式

### 同步 Redis（默认）

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-redis</artifactId>
</dependency>
```

### 响应式 Redis

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-redis-reactive</artifactId>
</dependency>
```

## 配置示例

```yaml
carlos:
  redis:
    enabled: true
    host: localhost
    port: 6379
    password:
    database: 0
    cache-type: caffeine_redis  # 多级缓存: caffeine + redis
```

## 缓存策略

| 策略 | 说明 |
|------|------|
| `redis` | 仅使用 Redis |
| `caffeine` | 仅使用 Caffeine 本地缓存 |
| `caffeine_redis` | 多级缓存: Caffeine L1 + Redis L2 |

## 多级缓存说明

- **Caffeine L1**: 本地 JVM 缓存，读写性能极高，但无分布式一致性
- **Redis L2**: 分布式缓存，支持集群和持久化
- **自动同步**: L1 缓存过期时自动回源到 L2

## 注意事项

1. 生产环境建议开启 `caffeine_redis` 多级缓存
2. 需要分布式锁的场景使用 Redisson 提供的 `RLock`
3. 响应式场景使用 `starter-redis-reactive` 替代同步版本
