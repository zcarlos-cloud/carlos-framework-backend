# carlos-spring-boot-starter-snowflake

基于雪花算法的分布式 ID 生成器，支持 Redis 模式和本地模式两种运行方式。

## 特性

- **双模式支持**：
    - **Redis 模式**：分布式环境下多实例自动协调 workerId/dataCenterId
    - **本地模式**：单机/开发环境，基于 IP 哈希或配置生成 ID，无 Redis 依赖
- **自动检测**：自动根据 classpath 中是否存在 Redis 选择合适的模式
- **轻量级**：本地模式无需任何外部依赖

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-snowflake</artifactId>
</dependency>
```

### 2. 配置（可选）

```yaml
carlos:
  snowflake:
    # 服务标签，默认使用 spring.application.name
    tag: my-service
    
    # 本地模式配置（可选）
    # 若不配置，则基于 IP 地址自动生成
    worker-id: 1        # 工作节点 ID (0-31)
    data-center-id: 1   # 数据中心 ID (0-31)
```

### 3. 使用

```java
@Service
public class UserService {
    
    public void createUser() {
        // 生成 Long 类型 ID
        Long id = SnowflakeUtil.longId();
        
        // 生成 String 类型 ID
        String idStr = SnowflakeUtil.strId();
    }
}
```

## 模式说明

### Redis 模式（分布式环境推荐）

当 classpath 中存在 `carlos-spring-boot-starter-redis` 时自动启用：

- 多实例间通过 Redis 协调，确保每个实例使用唯一的 workerId/dataCenterId
- 支持最多 1024 个节点（32 dataCenter × 32 worker）
- 实例下线时自动释放 workerId（通过 TTL + 定时续期）

```yaml
# Redis 模式额外配置
carlos:
  snowflake:
    namespace: snowflake      # Redis key 前缀
    redis-expire: 24h         # Redis 过期时间
```

### 本地模式（轻量级/开发环境）

当 classpath 中不存在 Redis 时自动启用：

- **自动模式**：基于本机 IP 地址哈希生成 workerId/dataCenterId
- **配置模式**：通过配置指定固定的 workerId/dataCenterId

```yaml
# 自动模式（默认）- 基于 IP 生成
carlos:
  snowflake:
    tag: my-service

# 配置模式 - 使用固定值
carlos:
  snowflake:
    tag: my-service
    worker-id: 1
    data-center-id: 1
```

**注意**：本地模式下，如果多台机器使用相同的 workerId/dataCenterId，可能会产生重复的 ID。建议：

- 单机部署使用自动模式
- 多机部署时为每台机器配置唯一的 workerId/dataCenterId

## 配置项

| 配置项                                 | 说明             | 默认值                       | 适用模式     |
|-------------------------------------|----------------|---------------------------|----------|
| `carlos.snowflake.tag`              | 服务标签           | `spring.application.name` | 全部       |
| `carlos.snowflake.worker-id`        | 工作节点 ID (0-31) | `null`（自动）                | 本地模式     |
| `carlos.snowflake.data-center-id`   | 数据中心 ID (0-31) | `null`（自动）                | 本地模式     |
| `carlos.snowflake.namespace`        | Redis key 前缀   | `snowflake`               | Redis 模式 |
| `carlos.snowflake.redis-expire`     | Redis 过期时间     | `24h`                     | Redis 模式 |
| `carlos.snowflake.use-system-clock` | 使用系统时钟         | `false`                   | 全部       |

## 工作原理

### 雪花算法结构

```
| 1 bit | 41 bit | 5 bit | 5 bit | 12 bit |
| 符号位 | 时间戳 | 数据中心 | 工作节点 | 序列号 |
```

- **时间戳**：41 位，约可使用 69 年
- **数据中心**：5 位，最多 32 个
- **工作节点**：5 位，最多 32 个
- **序列号**：12 位，每毫秒每节点可生成 4096 个 ID

### 模式选择逻辑

```
classpath 中存在 RedisUtil?
    ├── 是 → Redis 模式（分布式协调）
    └── 否 → 本地模式（IP/配置）
```

## 注意事项

1. **时钟回拨**：雪花算法依赖系统时间，若发生时钟回拨可能导致 ID 重复或生成失败
2. **本地模式多机部署**：多台机器使用本地模式时，需确保 workerId/dataCenterId 不重复
3. **Redis 模式依赖**：Redis 模式下必须保证 Redis 可用，否则服务启动会失败
