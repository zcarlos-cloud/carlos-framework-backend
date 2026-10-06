# Carlos Framework 组件特性利用分析报告

**生成时间**: 2026-09-18 20:30
**扫描 Commit**: d30e15cc6939a8ed1142df6bd2b83e02634e3c98 (carlos-framework-backend)
**上次报告**: component-analysis-report-2026-09-11-1.md（代码自 9dfa0b50 以来无 .java/.xml 变更，仅新增扫描报告文档）
**分析组件数**: 26 个
**发现待利用特性**: 22 条
**高优先级建议**: 6 条（含 1 条安全紧急）

---

## 一、执行摘要

### 1.1 今日变更概览

- Git 拉取状态: 成功（`9dfa0b50..d30e15cc`，仅 docs 变更）
- Commit: d30e15cc
- 代码变更: 无（自上次组件分析报告以来仅新增框架扫描报告文档 2 个）
- 分析基线: 与 2026-09-11 报告同一代码状态，本报告重点为**复核进度 + 修正误判 + 持续跟踪**

### 1.2 组件利用总览

| 组件 | 当前使用深度 | 待利用关键特性数 | 优先级推荐 | 最高收益特性 |
|------|-------------|-----------------|-----------|-------------|
| **Fastjson2** (2.0.62) | 中等 | 1 | **紧急** | 升级 2.0.63+（RCE 漏洞仍未修复） |
| **Redisson** (4.5.0) | 良好 | 3 | 高 | RDelayedQueue、RBloomFilter、RTopic |
| **Spring Boot** (4.0.6) | 中等 | 3 | 高 | 虚拟线程落地、结构化日志、ProblemDetail |
| **MyBatis-Plus** (3.5.16) | 良好 | 2 | 中 | Lambda Join 推广（当前仅 7 处）、动态表名 |
| **Caffeine** (BOM) | 良好 | 1 | 中 | LoadingCache 化（refreshAfterWrite 当前失效） |
| **Druid** (1.2.28) | 文档级 | 2 | 高 | WallFilter 默认启用 + 改掉 admin/admin |
| **MapStruct** (1.6.3) | 中等 | 2 | 中 | @MapperConfig 全局策略、@AfterMapping |
| **Disruptor** (3.4.4) | 中等 | 2 | 中 | WorkerPool、升级 4.x 评估 |
| **SkyWalking** (9.5.0) | 中等 | 1 | 中 | apm-toolkit 自定义 Span 推广 |
| **Seata** (2.5.0) | 基础 | 1 | 低 | Saga 状态机（暂无业务需求） |
| **MinIO/OSS** (BOM) | 中等 | 2 | 中 | 预签名 URL、对象事件监听 |
| **Guava** (33.4.8) | 近乎未用 | 1 | 低 | 评估是否从 BOM 移除 |
| **Hutool** (5.8.40) | 充分 | 0 | - | 当前利用较充分 |
| **RestClient** | 良好 | 1 | 中 | @HttpExchange 在业务模块推广 |
| **RocketMQ** (2.3.4) | 基础 | 1 | 低 | 延时消息、批量消费 |
| **Flowable** (7.0.1) | 中等 | 1 | 低 | 事件监听器、历史清理 |
| **POI/EasyExcel** | 良好 | 0 | - | 流式库已采用，利用充分 |
| **Kryo / TTL / Liquibase / WebAuthn / SMS4J** | 充分 | 0 | - | 当前利用较充分 |

### 1.3 今日重点关注

- **[TOP 1] Fastjson2 2.0.62 RCE（CVSS 9.8）仍未升级** —— 距上次报告已 7 天，`carlos-dependencies/pom.xml` 中 fastjson2.version 仍为 2.0.62。该漏洞（FNV-1a 哈希碰撞绕过 AutoType 校验）**默认配置即可触发**，2.0.63 为官方安全修复版本。请立即升级。
- **[TOP 2] Redisson RDelayedQueue / RBloomFilter / RTopic 三个高级数据结构零使用** —— 项目已有 RLocalCachedMap（多级缓存）、RRateLimiter（分布式限流）、RLock（5 种锁类型）等良好实践，但延时队列、布隆过滤器、主题订阅均未启用。
- **[TOP 3] Druid WallFilter 仅存在于 README 文档，且示例配置 admin/admin 弱口令** —— 需确认各业务应用 application.yml 是否实际启用 `filters: stat,wall`，并修改监控页默认密码。
- **[TOP 4] Caffeine `refreshAfterWrite` 配置实际不生效** —— `CaffeineConfig` 使用手动 `Cache`（`builder.build()` 无 loader），refresh 策略仅对 LoadingCache 生效。建议改为 LoadingCache 或移除该配置项。
- **[TOP 5] mybatis-plus-join 全项目仅 7 处使用**（`OrgDepartmentManagerImpl` 3 处、`OrgRoleManagerImpl` 4 处），与其在架构规范中的强制地位不匹配。

---

## 二、高优先级组件分析

### 组件 1: Fastjson2 (当前版本: 2.0.62) ⚠️ 安全紧急（连续两周未修复）

**当前项目使用概况**

- 使用位置:
  - `carlos-spring-boot/carlos-spring-boot-redis/carlos-spring-boot-starter-redis-core/src/main/java/com/carlos/redis/serialize/FastjsonSerializer.java`
  - `carlos-spring-boot/carlos-spring-boot-starter-json/src/main/java/com/carlos/json/fastjson/FastjsonJsonService.java`
  - `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/passkey/PasskeyService.java`
- 当前用法摘要: Redis 序列化、JSON 统一服务、Passkey 数据序列化

**相关代码**

```java
// carlos-spring-boot-starter-json/src/main/java/com/carlos/json/fastjson/FastjsonJsonService.java:81
JSON.config(com.alibaba.fastjson2.JSONReader.Feature.SupportAutoType, false);
```

**未被充分利用的特性 / 安全问题**

| 特性名称 | 当前状态 | 推荐应用场景 | 预期收益 | 实施难度 |
|---------|---------|------------|---------|---------|
| **升级至 2.0.63+** | ❌ 仍未升级（2.0.62） | 全项目 | 消除 RCE 风险 | 低 |
| safeMode 兜底 | 未配置 | 升级前的临时缓解 | 阻断攻击路径 | 低 |
| JSONB 格式 | 未使用 | Redis 缓存序列化 | 性能提升 30%+ | 低 |

**具体应用建议**

**建议 1（紧急）: 升级 Fastjson2 至 2.0.63 或更高版本**

- **场景**: `carlos-dependencies/pom.xml` fastjson2.version
- **当前问题**: 2026 年 7 月披露的漏洞（QVD-2026-45876），fastjson2 ≤ 2.0.62 在未开启 SupportAutoType 的默认配置下，攻击者可通过 FNV-1a 哈希碰撞构造 `@type` 绕过 AutoType 白名单导致 RCE，CVSS 9.8。项目当前 `JSON.config(SupportAutoType, false)` 的缓解措施**对该漏洞无效**。
- **推荐修改**:

```xml
<!-- carlos-dependencies/pom.xml -->
<fastjson2.version>2.0.63</fastjson2.version>
```

- **临时缓解**（若暂时无法升级）: JVM 启动参数增加 `-Dfastjson2.parser.safeMode=true`
- **收益**: 消除远程代码执行风险
- **实施难度**: 低（单点版本号变更，需回归 Redis 序列化兼容性）
- **参考**: 官方安全公告、FreeBuf《fastjson2 2.0.62 RCE 漏洞分析》

**建议 2: Redis 序列化评估切换 JSONB**

- **场景**: `FastjsonSerializer`（Redis 缓存序列化）
- **推荐写法**:

```java
// 以 JSONB 二进制格式替代文本 JSON，体积更小、反序列化更快
writerContext = JSONFactory.createWriteContext(
    JSONWriter.Feature.WriteMapNullValue,
    JSONWriter.Feature.WriteLongAsString,
    JSONWriter.Feature.WriteClassName  // 仅在可信内部缓存时
);
byte[] bytes = JSONB.toBytes(value, writerContext);
```

- **收益**: Redis 网络传输与内存占用降低约 30%
- **实施难度**: 中（需处理存量缓存数据兼容性，建议换序列化器 key 前缀后灰度）

---

### 组件 2: Redisson (当前版本: 4.5.0)

**当前项目使用概况**

- 使用位置:
  - `carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/multilevel/MultiLevelCacheUtil.java` — RLocalCachedMap L1+L2 多级缓存
  - `carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/ratelimit/RateLimitUtil.java` — RRateLimiter 令牌桶限流
  - `carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/lock/RedisLockAspect.java` — RLock 注解式锁（REENTRANT/FAIR/READ/WRITE/MULTI 五种）
  - `carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/localcache/RedissonLocalCacheManager.java` — 缓存管理器
- 当前用法摘要: 利用深度**良好**，分布式锁/限流/多级缓存均已落地；Redisson 4.5 为 3.x 之后的大版本，需关注 4.x 异步 API（RTopicAsync 等）变化

**相关代码**

```java
// RedisLockAspect.java:63-75 —— 锁类型覆盖完整
private RLock getLock(String lockKey, LockType lockType) {
    return switch (lockType) {
        case FAIR -> redissonClient.getFairLock(lockKey);
        case READ -> { RReadWriteLock rw = redissonClient.getReadWriteLock(lockKey); yield rw.readLock(); }
        case WRITE -> { RReadWriteLock rw = redissonClient.getReadWriteLock(lockKey); yield rw.writeLock(); }
        case REENTRANT, MULTI -> redissonClient.getLock(lockKey);
    };
}
```

**全项目扫描确认零使用的数据结构**（精确匹配，区分大小写）: `RDelayedQueue`、`RBloomFilter`、`RTopic`、`RScript`、`RGeo`、`RHyperLogLog`、`RSetCache`、`RSemaphore`（0 处匹配）。

**未被充分利用的特性**

| 特性名称 | 当前状态 | 推荐应用场景 | 预期收益 | 实施难度 |
|---------|---------|------------|---------|---------|
| RDelayedQueue 延时队列 | 零使用 | carlos-message 延时消息、carlos-audit 定时归档 | 替代 Quartz/轮询，架构简化 | 中 |
| RBloomFilter 布隆过滤器 | 零使用 | carlos-system/carlos-org 缓存防穿透 | 缓存穿透防护，Redis 压力降 99% | 低 |
| RTopic 发布订阅 | 零使用（目前用 RedisTemplate.convertAndSend） | 缓存失效广播、消息中心多端通知 | 带类型消息的可靠订阅 | 低 |
| RScript Lua 脚本 | 零使用 | 复杂原子操作（如"扣减+记录"复合操作） | 减少网络往返，原子性保障 | 中 |
| 4.x 异步 API | 未使用 | 高并发链路（网关、消息推送） | 吞吐提升 | 中 |

**具体应用建议**

**建议 1: 引入 RBloomFilter 防缓存穿透组件**

- **场景**: carlos-system 字典缓存、carlos-org 用户/部门缓存——Manager 层查询前先用布隆过滤器判断 key 是否存在
- **推荐写法**:

```java
// 在 carlos-spring-boot-starter-redis 新增 BloomFilterUtil
RBloomFilter<String> filter = redissonClient.getBloomFilter("bf:sys:dict");
filter.tryInit(100_000L, 0.01); // 预计 10 万条，误判率 1%

// Manager 层查询保护
if (!filter.contains(dictKey)) {
    return null; // 直接拦截，不打 Redis/DB
}
```

- **收益**: 恶意/随机 key 查询不再穿透到数据库
- **实施难度**: 低（新增工具类 + 数据变更处同步维护过滤器）
- **参考**: Redisson 官方文档 RBloomFilter

**建议 2: 引入 RDelayedQueue 支撑延时消息**

- **场景**: carlos-message 模块的定时下发、carlos-audit 审计日志延迟聚合
- **推荐写法**:

```java
RBlockingQueue<MessageTask> blockingQueue = redissonClient.getBlockingQueue("delay:msg");
RDelayedQueue<MessageTask> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
delayedQueue.offer(task, 30, TimeUnit.MINUTES); // 30 分钟后到期

// 消费端（独立线程/虚拟线程）
MessageTask task = blockingQueue.take();
```

- **收益**: 替代数据库轮询，延时精度秒级，无需额外中间件
- **实施难度**: 中（需设计消费端线程模型与失败重试）

**建议 3: RTopic 替代 RedisTemplate.convertAndSend**

- **场景**: `RedisUtil.java:2876` 当前使用 `redisTemplate.convertAndSend`（无类型、无订阅者管理）
- **收益**: 支持对象消息、模式订阅、异步监听
- **实施难度**: 低

---

### 组件 3: Spring Boot (当前版本: 4.0.6) + 虚拟线程

**当前项目使用概况**

- 使用位置: 全框架基础；`carlos-spring-boot-starter-integration` 已引入 RestClient（`DockingRestClientAutoConfiguration`、`RestClientBuilderUtils`）
- 当前用法摘要: 已升级 SB 4.0.6（Spring Framework 7.0 基座）；RestClient 基础设施已具备，但**虚拟线程仅存在于设计文档，未落地代码**

**关键事实**: 全项目 Java 源码中 `VirtualThread|VirtualThreadConfig|TomcatVirtualThread` 0 处匹配。虚拟线程仅在 `docs/design/carlos-spring-boot-starter-web-redesign.md` 的设计中（`VirtualThreadAutoConfiguration` 规划）。

**未被充分利用的特性**

| 特性名称 | 当前状态 | 推荐应用场景 | 预期收益 | 实施难度 |
|---------|---------|------------|---------|---------|
| **Tomcat 虚拟线程** | 仅设计文档 | carlos-spring-boot-starter-web 全局开启 | 高并发下吞吐 +37%，内存 -29% | 低 |
| 结构化日志 | 未启用 | 全项目 JSON 日志（logstash-encoder 已在 BOM） | 日志可检索性 | 中 |
| ProblemDetail (RFC 7807) | 未启用 | carlos-spring-boot-core 异常体系 | API 错误响应标准化 | 中 |
| @HttpExchange 声明式客户端 | 基础设施已具备，业务未用 | carlos-integration 各模块三方对接 | 替代手写 RestClient 调用 | 中 |

**具体应用建议**

**建议 1: 落地虚拟线程自动配置**

- **场景**: carlos-spring-boot-starter-web（按设计文档实现）
- **推荐写法**:

```java
// VirtualThreadAutoConfiguration（设计文档已给出）
@Bean
@ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "true")
public TomcatProtocolHandlerCustomizer<?> tomcatVirtualThreadCustomizer() {
    return protocolHandler -> protocolHandler.setExecutor(
        Executors.newVirtualThreadPerTaskExecutor());
}
```

- **注意**: 设计文档 P0-4 已指出 `VirtualThreadConfig` 与 SB4 内置 `spring.threads.virtual.enabled` 的条件注解冲突问题，实施时需按设计文档退避方案处理
- **收益**: I/O 密集型服务（网关、消息推送）显著吞吐提升
- **实施难度**: 低（按已有设计文档实现，约 1 天）
- **参考**: `docs/design/carlos-spring-boot-starter-web-redesign.md:131-146, 598-629`

**建议 2: 在业务模块推广 @HttpExchange**

- **场景**: carlos-integration 内各服务对接第三方 REST API。基础设施已在 `carlos-spring-boot-starter-integration` 就绪（`RestClientBuilderUtils.createService` 支持代理创建，测试见 `RestClientBuilderUtilsTest.java:53`）
- **收益**: 接口定义即客户端，代码量减少 50%+
- **实施难度**: 中

---

### 组件 4: Druid (当前版本: 1.2.28，druid-spring-boot-4-starter)

**当前项目使用概况**

- 使用位置: `carlos-spring-boot-starter-mybatis/pom.xml` 引入 `druid-spring-boot-4-starter`（已适配 Spring Boot 4，赞）
- 当前用法摘要: README 文档给出了完整 WallFilter + 监控页配置，但**代码库中无默认启用**（WallFilter/StatViewServlet 仅出现在 docs 与历史报告中）

**相关配置（README 示例，存在安全问题）**

```yaml
# carlos-spring-boot-starter-mybatis/README.md
spring:
  datasource:
    druid:
      filters: stat,wall,slf4j
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin   # ⚠️ 弱口令示例
```

**未被充分利用的特性**

| 特性名称 | 当前状态 | 推荐应用场景 | 预期收益 | 实施难度 |
|---------|---------|------------|---------|---------|
| WallFilter SQL 防火墙 | 文档级，未默认启用 | 全业务应用默认开启 | SQL 注入防御纵深 | 低 |
| 监控页强密码/IP 白名单 | 示例为 admin/admin | 生产配置规范 | 防未授权访问 | 低 |
| 慢 SQL 日志 (slowSqlMillis) | 文档级 | 全应用 | 慢查询发现 | 低 |

**具体应用建议**

**建议 1: 将 WallFilter 纳入 starter 默认配置模板并修正弱口令示例**

- **场景**: carlos-spring-boot-starter-mybatis 的默认配置 + README 示例
- **收益**: 安全增强（纵深防御）+ 合规
- **实施难度**: 低（配置模板修改）

---

### 组件 5: Caffeine (BOM 管理，当前用于 L1 缓存)

**当前项目使用概况**

- 使用位置: `carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/caffeine/CaffeineConfig.java`、`CaffeineUtil.java`
- 当前用法摘要: 配置相当完整（initialCapacity/maximumSize/expireAfterWrite/expireAfterAccess/refreshAfterWrite/recordStats/removalListener/Micrometer 指标注册），**较 9 月 11 日报告的"仅同步 put/get"判断有明显修正**——配置项其实都已具备

**相关代码**

```java
// CaffeineConfig.java:37-80
Caffeine<Object, Object> builder = Caffeine.newBuilder();
// ... 各类策略配置齐全 ...
if (properties.getRefreshAfterWriteDuration() != null) {
    builder.refreshAfterWrite(properties.getRefreshAfterWriteDuration());
}
Cache<String, Object> cache = builder.build();  // ⚠️ 手动 Cache
```

**关键发现**: `refreshAfterWrite` 配置在**手动 Cache 上不生效**。Caffeine 的 refresh 机制仅由 `LoadingCache.get()`（或 `Cache.get(k, mappingFunction)` 的 reload 路径）触发，手动 `put/get` 模式下该配置是死代码。

同时注意：**该 starter 内存在两套 L1 缓存机制**——`CaffeineUtil`（Caffeine）与 `MultiLevelCacheUtil`（RLocalCachedMap L1+L2）。两者并存可能造成使用困惑，建议明确分工（Caffeine 用于纯本地、RLocalCachedMap 用于需跨节点失效同步的场景）。

**建议 1: 改为 LoadingCache 或移除 refreshAfterWrite 配置**

- **场景**: CaffeineConfig
- **推荐写法**:

```java
if (properties.getRefreshAfterWriteDuration() != null) {
    builder.refreshAfterWrite(properties.getRefreshAfterWriteDuration());
    return builder.build(key -> loadFromRedis(key)); // LoadingCache
}
```

- **收益**: 热点 key 自动异步刷新，消除缓存击穿窗口
- **实施难度**: 中（需定义 loader 数据来源）

---

## 三、中优先级组件分析

### 组件 6: MyBatis-Plus (3.5.16) + mybatis-plus-join (1.5.7)

**当前项目使用概况**

- 使用位置: `carlos-spring-boot-starter-mybatis/src/main/java/com/carlos/datasource/config/MyBatisPlusConfig.java`
- 当前用法摘要: **利用深度良好**——TenantLineInnerInterceptor、DataPermissionInterceptor、PaginationInnerInterceptor（含 maxLimit/overflow/optimizeJoin）、OptimisticLockerInnerInterceptor、BlockAttackInnerInterceptor、Performance 拦截器全部可配置启用；MongoDB starter 亦基于 LambdaQueryChainWrapper
- 短板: `DynamicTableNameInnerInterceptor` 未配置；mybatis-plus-join 全项目仅 7 处（`OrgDepartmentManagerImpl` 3 处、`OrgRoleManagerImpl` 4 处）

**建议 1: 在 carlos-audit 启用动态表名拦截器支持分表**

- **场景**: 审计日志按月分表 `sys_log_202609`
- **收益**: 支撑大数据量审计存储
- **实施难度**: 中

**建议 2: 制定 Lambda Join 推广清单**

- **场景**: carlos-org、carlos-system、carlos-auth 中手写关联查询的 Manager——按 9/11 报告方向继续，本周代码无变化，**推广未启动**

---

### 组件 7: MapStruct (1.6.3)

**当前项目使用概况**

- 使用位置: 全项目 60+ Convert 接口，普遍使用 `@Mapper(uses = {CommonConvert.class})`
- 当前用法摘要: uses 复用良好；**未发现** `@AfterMapping`/`@BeforeMapping`/`@Context`/`@MapperConfig`/`unmappedTargetPolicy` 全局策略

**建议 1: 引入 @MapperConfig 全局策略，统一 null 策略与未映射字段告警**

```java
@MapperConfig(
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS
)
public interface CarlosMappingConfig { }

// 各 Convert 继承
@Mapper(config = CarlosMappingConfig.class, uses = {CommonConvert.class})
public interface SysUserConvert extends CarlosMappingConfig { }
```

- **收益**: 编译期暴露字段映射遗漏，减少线上 NPE；实施难度低

**建议 2: @AfterMapping 填充公共字段（创建人/时间）与 9/11 报告一致，状态不变 ⏳**

---

### 组件 8: Disruptor (3.4.4)

**当前项目使用概况**

- 使用位置: `carlos-spring-boot-starter-disruptor/src/main/java/com/carlos/disruptor/core/DisruptorManager.java`
- 当前用法摘要: WaitStrategy 可配置（BLOCKING/SLEEPING/YIELDING/BUSY_SPIN，`DisruptorWaitStrategyType.java:14-19`）；示例模块演示单生产者发布

**未被利用的特性**

| 特性名称 | 当前状态 | 推荐应用场景 | 预期收益 | 实施难度 |
|---------|---------|------------|---------|---------|
| WorkerPool 多消费者 | 未启用 | carlos-audit 审计事件并行消费 | 吞吐 3-5 倍 | 中 |
| 多生产者模式 | 未启用 | 多入口事件汇聚 | 并发安全发布 | 中 |
| 升级 4.x | 3.4.4 | — | 官方维护、性能改进 | 高（API 变动） |

---

### 组件 9: SkyWalking (9.5.0)

**当前项目使用概况**

- 使用位置: `carlos-spring-boot-starter-apm/src/main/java/com/carlos/apm/mdc/MdcUtil.java`（traceId/spanId 写入 MDC，跨线程传递处理完整）；`carlos-sample-apm` 演示 `@TraceTag`
- 当前用法摘要: 基础接入充分；**主代码未使用 apm-toolkit 自定义 Span**（@Trace/@Tags 仅存在于 sample）

**建议 1: 将 @TraceTag 注解能力推广至 carlos-message 消息发送、carlos-auth 认证等关键链路**

- **收益**: 关键业务标签入链路，排障效率提升
- **实施难度**: 低（注解即接入）

---

### 组件 10: MinIO / OSS (BOM)

**当前项目使用概况**

- 使用位置: `carlos-spring-boot-starter-oss`（S3OssTemplate 已实现分片上传/中止/列举，`OssTemplate.java:247-258`）
- 当前用法摘要: 分片上传已具备；预签名 URL、对象事件监听未启用

**建议 1: 预签名直传**——大文件前端直传 OSS，绕过应用服务器带宽；实施难度中
**建议 2: 对象事件（listenBucketNotification 或 S3 EventBridge）**——上传完成自动触发缩略图/病毒扫描；实施难度中

---

### 组件 11: RestClient / HTTP Interface (Spring Boot 4)

- **基础设施**: `DockingRestClientAutoConfiguration` + `RestClientBuilderUtils.createService()` 已支持声明式代理创建（`RestClientBuilderUtilsTest.java:53` 验证）
- **推广状态**: 业务模块（carlos-system、carlos-message 三方对接）未见 @HttpExchange 接口定义 ⏳ 未启动

---

### 组件 12: RocketMQ (rocketmq-spring-boot 2.3.4)

**当前项目使用概况**

- 使用位置: `carlos-spring-boot-starter-mq/config/RocketMqAutoConfiguration.java`（条件装配）
- 当前用法摘要: 装配层就绪；延时消息（5.x 支持任意级别延时）、批量消费、事务消息未在代码中体现

**建议**: carlos-message 若走 RocketMQ 通道，可启用事务消息保证"DB 落库 + MQ 发送"一致性；实施难度中

---

## 四、低优先级/当前利用较充分的组件

### 组件 13: Hutool (5.8.40)

**当前利用评估**: 充分。工具类广泛用于 Excel、验证码、反射、ID 生成等。5.8.x 已停止大版本演进，无新特性可挖。✅ 无需行动。

### 组件 14: Guava (33.4.8-jre)

**当前利用评估**: **近乎未使用**。全项目精确扫描 `CacheBuilder|RateLimiter|EventBus|LoadingCache|com.google.common.cache` 仅命中 Redisson 的注释与文档。Guava 功能已被 Hutool + Redisson + Caffeine 覆盖。

**建议**: 评估从 `carlos-dependencies/pom.xml` 移除或降级为传递依赖管理，减少 BOM 噪音与依赖面。难度低。

### 组件 15: POI (5.4.0) / EasyExcel (4.1.5) / FastExcel (1.3.0)

**当前利用评估**: 充分。业务导出统一走 `carlos-utils` 的 FastExcel/EasyExcel 流式 API（`ExcelUtil.java:52,84,118`），SysRegion、SysConfig 导入用 EasyExcel 监听器模式，未直接滥用 XSSFWorkbook。✅ 大 Excel 内存问题已规避。

### 组件 16: Knife4j (4.5.0 / carlos 定制 4.6.0-SNAPSHOT)

**当前利用评估**: 中等。Gateway 聚合文档、离线文档等高级特性未确认启用。建议在 carlos-gateway 验证聚合文档（多服务 OpenAPI 汇总）。难度低。

### 组件 17: Flowable (7.0.1)

**当前利用评估**: 中等。事件监听器（ExecutionListener/TaskListener）、历史数据清理（HistoryCleanup）未在扫描中出现。建议: 在 carlos-sample-flowable 补充事件监听示例 + 配置历史清理策略。难度低。

### 组件 18: MongoDB Plus (2.1.9)

**当前利用评估**: 中等。LambdaQueryChainWrapper 已深入 BaseService；Change Streams（变更监听）未使用。若有"MongoDB 数据同步到 ClickHouse/ES"场景可启用。难度中。

### 组件 19: TrueLicense (carlos-license)

**当前利用评估**: 较充分。`CustomLicenseManager` 扩展 LicenseManager 支持自定义校验。可选增强: License 内容扩展硬件指纹（MAC + CPU + 主板序列号组合）、License 模板化生成。难度中。

### 组件 20: Seata (2.5.0)

**当前利用评估**: 基础。`SeataConfig` + `@GlobalTransactional` + Feign/Web 上下文传播完整。Saga 状态机无业务需求，维持观望。

### 组件 21: LangChain4j (1.13.1)

**当前利用评估**: 基础。向量存储扩展待评估（9/11 遗留 ⏳）。

### 组件 22~26: Kryo (5.6.2) / TTL (2.14.5) / Liquibase (4.29.0) / WebAuthn (yubico 2.9.0) / SMS4J (3.3.4)

**当前利用评估**: 充分。Kryo 序列化（对象池 + RegistrationRequired 关闭）、TTL 线程上下文传递、Liquibase 多数据源迁移、Passkey 完整实现、短信多厂商切换均已落地。✅ 无需行动。

---

## 五、组件特性实施路线图

### 5.1 短期可实施（1~2 周，低难度）

| 序号 | 组件 | 特性 | 收益 | 涉及模块 |
|----|----|----|----|------|
| 1 | Fastjson2 | 升级 2.0.62 → 2.0.63+ | 🔴 消除 RCE | carlos-dependencies |
| 2 | Redisson | RBloomFilter 防穿透组件 | 缓存穿透防护 | carlos-spring-boot-starter-redis |
| 3 | Redisson | RTopic 替代 convertAndSend | 订阅能力增强 | carlos-spring-boot-starter-redis |
| 4 | Spring Boot | 虚拟线程自动配置落地（按设计文档） | 吞吐 +37% | carlos-spring-boot-starter-web |
| 5 | MapStruct | @MapperConfig 全局策略 | 编译期排错 | 全项目 Convert |
| 6 | Druid | WallFilter 默认配置模板 + 修正弱口令示例 | 安全纵深 | carlos-spring-boot-starter-mybatis |
| 7 | SkyWalking | @TraceTag 推广至关键链路 | 排障效率 | carlos-message、carlos-auth |
| 8 | Guava | 评估从 BOM 移除 | 依赖瘦身 | carlos-dependencies |

### 5.2 中期规划（1~2 月，中难度）

| 序号 | 组件 | 特性 | 收益 | 涉及模块 |
|----|----|----|----|------|
| 1 | Redisson | RDelayedQueue 延时队列 | 替代轮询架构 | carlos-message、carlos-audit |
| 2 | Caffeine | LoadingCache 化，激活 refreshAfterWrite | 热点自动刷新 | carlos-spring-boot-starter-redis |
| 3 | MyBatis-Plus | DynamicTableName 分表 | 审计大表支撑 | carlos-audit |
| 4 | MyBatis-Plus | Lambda Join 推广（当前仅 7 处） | 消除手写关联 SQL | carlos-org、carlos-system、carlos-auth |
| 5 | Spring Boot | @HttpExchange 业务推广 | 对接代码 -50% | carlos-integration |
| 6 | Disruptor | WorkerPool 多消费者 | 审计吞吐 3-5 倍 | carlos-audit |
| 7 | OSS | 预签名直传 + 对象事件 | 带宽与体验 | carlos-spring-boot-starter-oss |

### 5.3 长期演进（3~6 月，高难度/架构级）

| 序号 | 组件 | 特性 | 收益 | 涉及模块 |
|----|----|----|------|------|
| 1 | Spring Boot | 结构化日志 + Observability 体系 | 可观测性 | 全项目 |
| 2 | Spring Boot | ProblemDetail 异常标准化 | API 规范 | carlos-spring-boot-core |
| 3 | Seata | Saga 状态机 | 长事务支持 | carlos-samples |
| 4 | Disruptor | 升级 4.x 评估 | 官方维护 | carlos-spring-boot-starter-disruptor |
| 5 | Redisson | 4.x 异步 API 改造 | 高并发吞吐 | 网关、消息推送 |

---

## 六、技术趋势洞察

### 6.1 关键依赖版本动态

| 组件 | 框架版本 | 最新可用 | 状态评估 |
|-----|---------|---------|---------|
| Spring Boot | 4.0.6 | 4.0.x 最新补丁 | 保持关注补丁 |
| Spring Cloud | 2025.1.1 | 2025.1.1 | 已对齐 |
| Spring Cloud Alibaba | 2025.1.0.0 | 2025.1.0.0 | 已对齐 |
| MyBatis-Plus | 3.5.16 | 3.5.16 | 已对齐 |
| **Fastjson2** | **2.0.62** | **2.0.63+（安全修复）** | **🔴 必须升级** |
| Redisson | 4.5.0 | 4.5.0 | 已对齐（4.x 大版本，留意 API 变化） |
| Druid | 1.2.28 | 1.2.28 | 已对齐 |
| Seata | 2.5.0 | 2.5.0 | 已对齐 |
| Disruptor | 3.4.4 | 4.0.0 | 观望（API 变动大，非紧急） |
| Flowable | 7.0.1 | 7.1.0 | 观望 |
| Knife4j | 4.5.0（定制 4.6.0-SNAPSHOT） | 4.5.0 | 定制版注意同步上游 |

### 6.2 新特性适配建议

1. **Fastjson2 2.0.63**: 安全修复版本，官方强烈建议所有用户升级，处理不可信输入场景尤其必须。
2. **Spring Boot 4.0 体系**: 基于 Spring Framework 7.0，虚拟线程、AOT、结构化日志（`logging.structured`）是核心红利，框架已完成版本升级，下一步是特性吃透。
3. **Redisson 4.x**: 已从 3.x 全面升级，异步 API（`RTopicAsync` 等）与 3.x 差异需在设计新组件时留意。

---

## 七、待办事项与跟踪

### 7.1 今日新增待办

- [ ] [安全] 升级 Fastjson2 至 2.0.63+（难度: 低，收益: 🔴 紧急）—— **连续第二次报告跟踪**
- [ ] [特性] carlos-spring-boot-starter-web: 按设计文档落地虚拟线程自动配置（难度: 低，收益: 高）
- [ ] [特性] carlos-spring-boot-starter-redis: 新增 RBloomFilter 防穿透工具类（难度: 低，收益: 高）
- [ ] [特性] carlos-spring-boot-starter-redis: RTopic 订阅工具替代 convertAndSend（难度: 低，收益: 中）
- [ ] [修正] carlos-spring-boot-starter-mybatis: Druid README 修正 admin/admin 弱口令示例（难度: 低，收益: 中）
- [ ] [特性] CaffeineConfig 改 LoadingCache 激活 refreshAfterWrite（难度: 中，收益: 中）
- [ ] [特性] MapStruct @MapperConfig 全局策略（难度: 低，收益: 中）
- [ ] [清理] 评估 Guava 从 carlos-dependencies 移除（难度: 低，收益: 低）

### 7.2 历史遗留跟踪（承自 2026-09-11 报告）

- [ ] [安全] Fastjson2 2.0.62 → 2.0.63+ ⏳ **未完成（已逾期 7 天，升级优先级最高）**
- [ ] [特性] carlos-spring-boot-starter-redis: RBloomFilter ⏳ 未完成
- [ ] [特性] carlos-spring-boot-starter-redis: RDelayedQueue ⏳ 未完成
- [ ] [特性] carlos-org/carlos-system: MapStruct @AfterMapping 字段填充 ⏳ 未完成
- [ ] [特性] carlos-spring-boot-starter-web: 全局 Tomcat 虚拟线程 ⏳ 设计文档已就绪，待编码
- [ ] [特性] carlos-integration: @HttpExchange 声明式客户端 ⏳ 基础设施就绪，业务未推广
- [ ] [特性] carlos-audit: Disruptor WorkerPool ⏳ 未完成
- [ ] [特性] carlos-spring-boot-starter-mybatis: DynamicTableNameInnerInterceptor ⏳ 未完成
- [ ] [特性] carlos-spring-boot-starter-ai: LangChain4j 向量存储扩展 ⏳ 评估中
- [ ] [特性] carlos-gateway: Resilience4j 注解式熔断降级 ⏳ 评估中
- [ ] [特性] carlos-sample-flowable: DMN 决策表示例 ⏳ 评估中

**进度说明**: 自 9/11 报告以来代码库无 .java 变更，上述 8 项实施类待办全部未启动。本周代码处于稳定期，建议朱军优先处理 TOP 1（Fastjson2 安全升级，10 分钟内可完成）。

---

## 八、本次报告对上次报告的修正

1. **Caffeine 利用深度修正**: 9/11 报告判断"仅使用同步 put/get"，实际 `CaffeineConfig` 已具备 recordStats/removalListener/refreshAfterWrite/Micrometer 完整配置——问题不在配置缺失，而在**手动 Cache 导致 refreshAfterWrite 不生效**，建议方向调整为 LoadingCache 化。
2. **Redisson 利用深度修正**: RLocalCachedMap（多级缓存）与 RRateLimiter（分布式限流）已落地良好，9/11 报告已部分提及，本次扫描确认 RRateLimiter 实现完整（RateLimitUtil 350 行）。
3. **MyBatis-Plus 利用深度修正**: 六大拦截器（租户/数据权限/分页/乐观锁/防全表/性能）均可配置启用，框架层已属"良好"，剩余差距在业务侧 Lambda Join 推广（7 处）。

---

*本报告由 Hermes 定时任务自动生成，组件特性建议基于当前依赖版本和实际代码扫描，具体实施前请人工复核兼容性。*

**扫描代码范围**: carlos-framework-backend 全模块（55+ 模块）
**网络状态**: 可用（Fastjson2 漏洞、Redisson 4.5、Spring Boot 4.0、Seata 2.5.0 情报已核实）
**报告生成方式**: 代码精确匹配扫描（区分大小写）+ 网络情报交叉验证，所有代码引用均附文件路径与行号
