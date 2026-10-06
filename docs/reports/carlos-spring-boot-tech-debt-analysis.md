# carlos-spring-boot 技术债务详细分析

> 基于对全部 24 个子模块的系统扫描，逐项分析每个问题。
> 日期：2026-06-21
> 状态：待决策

---

## #8 — JacksonAutoConfiguration 覆盖 Spring Boot 原生 ObjectMapper

**文件**: `carlos-spring-boot-starter-json/src/main/java/com/carlos/json/jackson/config/JacksonAutoConfiguration.java`

### 现状

```
AutoConfiguration.imports → 只注册了 JsonAutoConfig (空壳，仅 @EnableConfigurationProperties)
                           → JacksonAutoConfiguration 未注册、未被 @Import
                           → 仅当应用包扫描到 com.carlos 包时才加载
```

当加载时，它创建 `@Primary @ConditionalOnMissingBean ObjectMapper`，完全替换 Spring Boot 自己的 `ObjectMapper`。这使得 `spring.jackson.*` 配置全部失效。

### 功能对比

| 功能 | `carlos.json.*` | `spring.jackson.*` 等效 |
|------|----------------|------------------------|
| dateFormat | `carlos.json.date-format` | `spring.jackson.date-format` |
| timeZone | `carlos.json.time-zone` | `spring.jackson.time-zone` |
| ignoreNull | `carlos.json.ignore-null` | `spring.jackson.default-property-inclusion: non_null` |
| indentOutput | `carlos.json.serialization.indent-output` | `spring.jackson.serialization.indent-output` |
| failOnEmptyBeans | `carlos.json.serialization.fail-on-empty-beans` | `spring.jackson.serialization.fail-on-empty-beans` |
| failOnUnknownProperties | `carlos.json.deserialization.fail-on-unknown-properties` | `spring.jackson.deserialization.fail-on-unknown-properties` |
| namingStrategy | `carlos.json.field-mapping.property-naming-strategy` | `spring.jackson.property-naming-strategy` |
| **longToString** | ✅ 支持 | ❌ 无内置 |
| **ignoreEmptyString** | ✅ 支持 | ❌ 无内置 |
| **allowSingleQuotes** | ✅ 支持 | ❌ 无内置 |
| **allowComments** | ✅ 支持 | ❌ 无内置 |
| **allowUnescapedControlChars** | ✅ 支持 | ❌ 无内置 |
| multi-engine (Jackson/Fastjson2/Gson) | ✅ JsonFactory 支持 | ❌ 仅 Jackson |

### 关键点

1. **加载不确定性**：`JacksonAutoConfiguration` 不在 `AutoConfiguration.imports` 中，加载取决于 `scanBasePackages`，行为不可预测
2. **`JsonAutoConfig` 是空壳**：只绑定属性，不创建任何 Bean
3. **`JsonFactory` 独立于 Spring**：内部自己 `new ObjectMapper()`，不依赖 Spring 上下文

### 可选方案

| 方案 | 操作 | 优点 | 缺点 |
|------|------|------|------|
| **A. 保持现状** | 不动 | 零工作量 | 加载不确定性、覆盖 spring.jackson 标准配置 |
| **B. 删除整个模块** | 删除 `carlos-spring-boot-starter-json` | 零维护、使用 Spring Boot 标准 | 丢失 longToString/ignoreEmptyString/多引擎切换 |
| **C. 改造成 Customizer** | 用 `Jackson2ObjectMapperBuilderCustomizer` 添加自定义配置，不再替换 ObjectMapper | 不破坏 `spring.jackson.*`，保留自定义能力 | 需重构，allowComments 等底层 JsonFactory 配置可能无法通过 Customizer 实现 |
| **D. 显式注册 + 互斥声明** | 在 AutoConfiguration.imports 中注册 JacksonAutoConfiguration，文档明确 `carlos.json.*` 与 `spring.jackson.*` 互斥 | 改动最小 | 框架覆盖 Spring Boot 标准的副作用永久化 |

### 需要决策的问题

1. `longToString`、`ignoreEmptyString`、`allowSingleQuotes` 这些差异化功能，项目是否实际使用？
2. 多引擎切换（Jackson/Fastjson2/Gson）是否有实际需求？是否曾经切换过？
3. 是否接受 `carlos.json.*` 完全替代 `spring.jackson.*`？

---

## #9 — RedisLettuceConnectionConfiguration 复制 Spring Boot 内部实现

**文件**: `carlos-spring-boot-redis/carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/config/RedisLettuceConnectionConfiguration.java`

### 现状

该类（207行）近乎完整复制了 Spring Boot 内部的 `LettuceConnectionConfiguration`：

- 手动创建 `DefaultClientResources`、`LettuceConnectionFactory`
- 引用 Spring Boot **内部类**：`DataRedisProperties`, `DataRedisConnectionDetails`, `ClientResourcesBuilderCustomizer`, `LettuceClientConfigurationBuilderCustomizer`（这些都在 `org.springframework.boot.data.redis.autoconfigure` 包中，非公开 API）
- 额外创建第二个 `LettuceConnectionFactory`：`masterOnlyFactory`（强制 `ReadFrom.MASTER`）
- 添加自定义 `readFromCustomizer`（`REPLICA_PREFERRED`）

### 风险评估

每次 Spring Boot 升级，这些内部类可能变更（方法签名、构造参数、类名），导致编译失败或运行时异常。Spring Boot 不保证 `autoconfigure` 包内类的 API 稳定性。

### 差异化功能

1. `readFromCustomizer` — 设置 `ReadFrom.REPLICA_PREFERRED`（Spring Boot 可通过 `spring.data.redis.lettuce.read-from` 配置）
2. `masterOnlyFactory` — 第二个连接工厂，仅读主节点
3. 连接池配置 — Spring Boot 已通过 `spring.data.redis.lettuce.pool.*` 支持

### 可选方案

| 方案 | 操作 | 优点 | 缺点 |
|------|------|------|------|
| **A. 保持现状** | 不动 | 零工作量 | 每个 Spring Boot 升级有兼容风险 |
| **B. 删除，用标准配置** | 删除此类，用 `spring.data.redis.*` + `LettuceClientConfigurationBuilderCustomizer` | 零维护、跟随 Spring Boot 升级 | 丢失 `masterOnlyFactory` 双连接工厂（需确认是否有业务使用） |
| **C. 精简重构** | 保留 `readFromCustomizer` 和 `masterOnlyFactory`，删除其余重复逻辑 | 保留差异化功能，降低维护面 | 仍需维护部分 Lettuce 代码 |

### 需要决策的问题

1. `masterOnlyFactory` 是否被业务代码实际使用？哪些场景需要双连接工厂？
2. `readFromCustomizer` 的 `REPLICA_PREFERRED` 能否用 `spring.data.redis.lettuce.read-from=replica-preferred` 替代？

---

## #10 — ConverterAutoConfiguration 重复注册基础类型转换器

**文件**: `carlos-spring-boot-starter-web/src/main/java/com/carlos/boot/web/autoconfigure/ConverterAutoConfiguration.java`

### 现状

注册了 6 个 `Converter` Bean：

| Bean | 类型 |
|------|------|
| `stringToDateConverter` | `String → Date` |
| `stringToLocalDateTimeConverter` | `String → LocalDateTime` |
| `stringToLocalTimeConverter` | `String → LocalTime` |
| `stringToLocalDateConverter` | `String → LocalDate` |
| `stringToDoubleConverter` | `String → Double` |
| `stringToIntegerConverter` | `String → Integer` |

同时注册了2个 Jackson 日期序列化/反序列化组件。

### Spring Boot 内置能力

Spring Boot 的 `WebMvcAutoConfiguration` 通过 `FormattingConversionService` 已自动注册以下转换器：
- `String → Date`（通过 `spring.mvc.format.date` 配置日期格式）
- `String → LocalDateTime/LocalDate/LocalTime`（通过 `spring.mvc.format.date-time/date/time`）
- `String → Number`（Spring 内置 `StringToNumberConverterFactory`）

### 差异分析

Carlos 的转换器可能支持更多日期格式（如多格式尝试）。需确认 `StringConverters` 的实现是否有特殊逻辑。若仅做 `SimpleDateFormat.parse()` 单格式转换，则完全冗余。

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 删除** | 完全删除 ConverterAutoConfiguration，依赖 Spring Boot 内置转换 |
| **B. 保留日期，删除基础类型** | 删除 `Double`/`Integer` 转换器（Spring 已内置），保留日期转换器确认格式兼容性 |
| **C. 保持现状** | 不动（如果 Converter 有特殊逻辑） |

### 需要决策的问题

1. `StringConverters` 的实现是否有特殊的日期格式兼容逻辑？
2. 是否接受 Spring Boot 标准 `spring.mvc.format.*` 配置替代？

---

## #11 — SentinelConfig 重复 Alibaba Sentinel Starter 内置 Bean

**文件**: `carlos-spring-cloud-starter/src/main/java/com/carlos/cloud/sentinel/SentinelConfig.java`

### 现状

注册两个 Bean：
- `SentinelResourceAspect` — `@SentinelResource` 注解切面
- `CustomBlockExceptionHandler` — 自定义限流异常处理器

### 与 Alibaba Starter 的重复

Alibaba 的 `SentinelAutoConfiguration`（`spring-cloud-starter-alibaba-sentinel` 内置）已注册：
- `SentinelResourceAspect` — 注解切面
- `BlockExceptionHandler` — 默认异常处理器

Carlos 的 `SentinelResourceAspect` 与 Alibaba 完全相同（无参构造），属于精确重复。

### 差异化功能

`CustomBlockExceptionHandler` 提供了**中文友好错误提示**（"访问过于频繁，请稍后重试"等），并统一返回 `Result<String>` JSON 格式。这是高于 Alibaba 默认的附加值。

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 删除 SentinelResourceAspect, 保留 CustomBlockExceptionHandler** | 最小改动，消除重复 |
| **B. 保持现状** | 不动，重复 Bean 无害（`@ConditionalOnMissingBean` 保护） |
| **C. 完全删除 SentinelConfig** | 另写一个 `BlockExceptionHandler` 实现类，通过标准 Bean 注册即可 |

### 推荐

**方案 A** — 删除 `sentinelResourceAspect()` 方法，保留 `blockExceptionHandler()`。

---

## #12 — FlowableAutoConfiguration 重写官方自动配置（已于 2026-09-24 处置：模块整体删除，改用 Warm-Flow，见 workflow-engine-selection-research-2026-09-24.md）

**文件**: `carlos-spring-boot-starter-flowable/src/main/java/com/carlos/flowable/config/FlowableAutoConfiguration.java`

### 现状

完全重写了 Flowable 官方的 `SpringBootAutoConfiguration`，手动创建 12+ 个 Bean：

- `SpringProcessEngineConfiguration` — 流程引擎配置
- `ProcessEngine` — 流程引擎
- `RepositoryService`, `RuntimeService`, `TaskService`, `HistoryService`, `ManagementService`, `IdentityService`, `DynamicBpmnService` — 7 个服务
- `FlowableProcessService`, `FlowableRuntimeService`, `FlowableTaskService`, `FlowableHistoryService`, `FlowableService` — 5 个自定义封装服务
- `FlowableHealthIndicator` — 健康检查

### 与 Flowable 官方的差异

| 项目 | Flowable 官方 | Carlos 自定义 |
|------|-------------|-------------|
| ID 生成器 | `StrongUuidGenerator`（UUID） | `DbIdGenerator`（数据库序列） |
| Schema 更新 | 默认 `true` | 默认 `false`（P0-007 修复） |
| 自动部署 | 官方已支持 `flowable.process.auto-deploy` | 自定义 `deployResources()` |
| 异步执行器 | 官方自动配置 | 自定义线程池配置 |
| 自定义服务封装 | 无 | 5 个自定义 Service |

### 风险评估

- Flowable 官方升级时，`SpringProcessEngineConfiguration` 的 API 可能变更
- `DbIdGenerator` 在高并发下有性能瓶颈（每次插入需要数据库序列查询）
- `deployResources()` 手动加载 `.bpmn20.xml` + `.bpmn` 文件，与官方 `flowable.process.auto-deploy` 可能冲突

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 保持现状** | 不动（如果自定义功能对业务至关重要） |
| **B. 保留封装层，删除重复的服务 Bean** | 删除 7 个 Service Bean（`RepositoryService` 等），让 Flowable 官方 auto-config 创建，仅保留 5 个自定义封装 Service |
| **C. 完全迁移到官方配置** | 用 `flowable.*` 标准配置替代自定义 `SpringProcessEngineConfiguration`，仅保留自定义 Service 封装层和 HealthIndicator |

### 需要决策的问题

1. `DbIdGenerator` 使用数据库序列生成 ID 是业务需求还是历史遗留？
2. 5 个自定义 Service（`FlowableProcessService` 等）是否有实质性的业务逻辑封装？
3. `databaseSchemaUpdate=false` 默认值是否可以通过 `flowable.database-schema-update=false` 标准配置实现？

---

## #13 — MongoDB 模块泄漏 MyBatis-Plus 概念

**文件**: `carlos-spring-boot-starter-mongodb/src/main/java/com/carlos/mongodb/config/MongoAutoConfiguration.java`

### 现状

MongoDB 模块的 `DefaultMongoCommonField` 继承 `AbstractMybatisCommonField`（MyBatis-Plus 类），方法名包含 MyBatis 风格拼写错误：
- `primaryKeyFiledName()`（应为 `FieldName`）
- `updateTimeFiledName()`
- `logicDeleteFiledName()`

### 问题

1. **概念耦合**：MongoDB 是文档数据库，没有"乐观锁版本号"、"逻辑删除"等关系型数据库概念
2. **跨模块依赖**：`carlos-spring-boot-starter-mongodb` 依赖 MyBatis-Plus 的字段抽象接口
3. **拼写错误**：`Filed` → `Field` 在整个框架中传播

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 保持现状** | 不动（改动大，影响面广） |
| **B. 重命名 + 解耦** | 将 `MybatisCommonField` 重命名为 `CommonField`，修正 `Filed` → `Field`，MongoDB 定义独立接口 |
| **C. 仅修正拼写** | 全局 `Filed` → `Field` 重命名 |

### 需要决策的问题

1. 这是否值得现在重构？拼写错误修改涉及大量文件。

---

## #14 — DataScopeAutoConfiguration 双缓存 Bean 竞争

**文件**: `carlos-spring-boot-starter-datascope/src/main/java/com/carlos/datascope/auto/DataScopeAutoConfiguration.java`

### 现状

两个 Bean 方法产生 `DataScopeCache`：

```java
// 方法1 (第59行) — 主配置类中
@Bean @ConditionalOnMissingBean
public DataScopeCache dataScopeCache(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
    // 如果 redis 启用且有 StringRedisTemplate → RedisDataScopeCache
    // 否则 → CaffeineDataScopeCache
}

// 方法2 (第138行) — 内部类 RedissonCacheConfiguration 中
@Configuration @ConditionalOnClass(RedissonClient.class)
static class RedissonCacheConfiguration {
    @Bean @ConditionalOnMissingBean
    public DataScopeCache dataScopeCache(RedissonClient redissonClient, DataScopeProperties properties) {
        return new RedissonLocalDataScopeCache(...);
    }
}
```

### 问题

两个 `@ConditionalOnMissingBean(DataScopeCache.class)` 方法竞争：
- 无 Redisson → 方法1 创建（Caffeine 或 Redis）
- 有 Redisson → **两个方法都可能匹配**，取决于 Spring 处理顺序
- 方法2 所在的 `RedissonCacheConfiguration` 被 `@Import` 强制导入，可能先于方法1 注册 → 不确定哪个生效

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 合并单方法** | 将缓存选择逻辑统一到一个 `@Bean` 方法中：`if (redissonClient != null) → RedissonLocal; else if (redisTemplate != null) → Redis; else → Caffeine` |
| **B. 显式条件互斥** | 给方法1 添加 `@ConditionalOnMissingClass("org.redisson.api.RedissonClient")`，确保不竞争 |

### 推荐

**方案 A** — 合并为一个方法，逻辑清晰，无竞争。

---

## #15 — DisruptorAutoConfiguration 双 Metrics Bean

**文件**: `carlos-spring-boot-starter-disruptor/src/main/java/com/carlos/disruptor/config/DisruptorAutoConfiguration.java`

### 现状

```java
// 方法1 (第31行) — 有 Micrometer
@Bean @ConditionalOnMissingBean @ConditionalOnClass(MeterRegistry.class)
public DisruptorMetrics disruptorMetrics(MeterRegistry meterRegistry) { ... }

// 方法2 (第41行) — 无 Micrometer 兜底
@Bean @ConditionalOnMissingBean
public DisruptorMetrics disruptorMetricsWithoutMeter() { ... }
```

### 问题

- 当 `MeterRegistry` 在 classpath 上时，两个方法条件都满足，`@ConditionalOnMissingBean` 负责互斥
- 方法名不同（`disruptorMetrics` vs `disruptorMetricsWithoutMeter`），不会冲突
- 但依赖 Spring 处理顺序，不够明确

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 合并为单方法** | 使用 `ObjectProvider<MeterRegistry>` 可选注入 |
| **B. 保持现状** | 功能上无 bug，仅代码不优雅 |

### 推荐

**方案 A** — 合并。

```java
@Bean @ConditionalOnMissingBean
public DisruptorMetrics disruptorMetrics(ObjectProvider<MeterRegistry> registryProvider) {
    return new DisruptorMetrics(registryProvider.getIfAvailable());
}
```

---

## #16 — MyBatis DbType 默认 POSTGRE_SQL

**文件**: `carlos-spring-boot-starter-mybatis/src/main/java/com/carlos/datasource/config/MybatisProperties.java`

### 现状

```java
private DbType dbType = DbType.POSTGRE_SQL;
```

### 问题

- 框架内部使用 MySQL 为主（历史配置文件、CLAUDE.md 多处提及 MySQL）
- 用户可能以 MySQL 部署，未配置 `carlos.mybatis.db-type` → `PaginationInnerInterceptor` 使用 `DbType.POSTGRE_SQL` 生成的分页 SQL 与 MySQL 不兼容

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 改为 MYSQL** | `private DbType dbType = DbType.MYSQL;` |
| **B. 删除默认值，强制配置** | 不设默认，启动时若未配置则报错提示 |
| **C. 保持现状** | PostgreSQL 是项目实际标准 |
| **D. 自动检测** | 通过 DataSource URL 自动推断数据库类型 |

### 需要决策的问题

1. 框架内部实际使用哪种数据库为主？
2. 是否有大量生产部署使用 PostgreSQL？

---

## #17 — SMS 使用非标准属性前缀

**文件**: `carlos-spring-boot-starter-sms/src/main/java/com/carlos/sms/config/SmsCustomizeConfig.java`

### 现状

```java
@ConditionalOnProperty(prefix = "sms.blends.postal", name = "supplier", havingValue = "postal")
@ConditionalOnProperty(prefix = "sms.blends.wocloud", name = "supplier", havingValue = "wocloud")
@ConditionalOnProperty(prefix = "sms.blends.ums", name = "supplier", havingValue = "ums")
```

而 `SmsProperties` 使用 `@ConfigurationProperties(prefix = "carlos.sms")`。

### 问题

- 两种前缀风格不一致：`carlos.sms.*` vs `sms.blends.*`
- 无 `matchIfMissing` 也无 `havingValue`，意味着对应的恰好等于对应值时才启用，这是正确的
- 但 `sms.blends.*` 前缀不是框架标准 `carlos.*` 前缀

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 统一为 carlos 前缀** | `sms.blends.postal.supplier` → `carlos.sms.blends.postal.supplier` |
| **B. 保持现状** | 改动影响用户配置 |

### 需要决策的问题

1. 是否已有生产配置使用 `sms.blends.*` 前缀？
2. 是否接受配置变更的迁移成本？

---

## #18 — CORS 配置重复 Spring Boot 标准

**文件**: `carlos-spring-boot-starter-web/src/main/java/com/carlos/boot/web/autoconfigure/CorsAutoConfiguration.java`

### 现状

```java
@ConditionalOnProperty(prefix = "carlos.web.cors", name = "enabled", havingValue = "true")
public class CorsAutoConfiguration implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) { ... }
}
```

### Spring Boot 标准方式

```yaml
spring:
  web:
    cors:
      allowed-origins: "https://example.com"
      allowed-methods: "GET,POST"
      max-age: 3600
```

Spring Boot 4.x 的 `WebMvcProperties.Cors` 原生支持 CORS 配置。

### 差异

- Carlos 使用 `allowedOriginPatterns`（支持通配符），Spring Boot 使用 `allowedOrigins`（精确匹配）
- Carlos 的 `carlos.web.cors.*` 属性结构与 `spring.web.cors.*` 不完全对应

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 迁移到 spring.web.cors** | 删除 CorsAutoConfiguration，使用 Spring Boot 标准配置 |
| **B. 保持现状** | 如果 `allowedOriginPatterns` 是关键差异化需求 |
| **C. 保持类，改为默认不启用** | `havingValue = "true"` + 无 `matchIfMissing`（当前状态 — 已经正确） |

### 推荐

**方案 A** — Spring Boot 4.x 标准配置足够使用。如需 `allowedOriginPatterns`（支持 `https://*.example.com` 格式），可通过自定义 `WebMvcConfigurer` Bean 而不需要框架级 AutoConfiguration。

---

## #19 — LiquibaseAutoConfiguration 与 Spring Boot 内置并行

**文件**: `carlos-spring-boot-starter-migration/src/main/java/com/carlos/migration/config/LiquibaseAutoConfiguration.java`

### 现状

- 自定义 `MultiDataSourceLiquibase` 支持多数据源场景
- 此配置与 Spring Boot 内置 `LiquibaseAutoConfiguration` **不互斥** — 两者可能同时激活

### 风险

- 单数据源场景下，两个 Liquibase 都可能执行 migration → 数据不一致
- `MultiDataSourceLiquibase` 的 migration 行为与 `SpringLiquibase` 不同

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 加互斥条件** | 当配置了多数据源时才启用此配置，否则使用 Spring Boot 标准 Liquibase |
| **B. 完全替换** | 用 `@AutoConfigureBefore` 确保此配置覆盖 Spring Boot 内置 |
| **C. 改为 opt-in** | 去掉 `matchIfMissing = true`，用户必须显式 `carlos.migration.enabled=true` |

### 推荐

**方案 C** — 最低风险做法，改为 opt-in。

---

## #20 — EncryptProperties 启动日志输出密钥

**文件**: `carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/config/EncryptProperties.java`

### 现状

```java
@Slf4j
public class EncryptProperties implements InitializingBean {
    @Override
    public void afterPropertiesSet() {
        log.info("encrypt config:{}", JSONUtil.toJsonStr(this));  // ← 输出完整配置，包括密钥!
    }
}
```

### 问题

如果 SM2 私钥和 SM4 密钥在配置文件中，启动日志会明文打印：
```
encrypt config:{"sm2":{"enabled":true,"publicKey":"04...","privateKey":"00..."},"sm4":{"enabled":true,"key":"0123456789abcdef","iv":"..."}}
```

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 脱敏输出** | 保持日志，但对 key/privateKey 字段脱敏（如 `01234567***`） |
| **B. 删除日志** | 移除 `log.info()` 行 |
| **C. 改为 DEBUG 级别** | `log.debug()` |

### 推荐

**方案 A** — 保留调试便利性，脱敏关键字段。

---

## #21 — ResourceServerDisabledConfiguration 逻辑反转

**文件**: `carlos-spring-boot-starter-security/src/main/java/com/carlos/security/config/ResourceServerDisabledConfiguration.java`

### 现状

```java
@ConditionalOnProperty(prefix = "carlos.resource-server", name = "enabled", havingValue = "false", matchIfMissing = true)
```

### 逻辑

| 用户配置 | 效果 |
|---------|------|
| 无任何配置 | `matchIfMissing=true` → 激活 → **permit-all!** |
| `carlos.resource-server.enabled=true` | 不激活（havingValue=false 不匹配） |
| `carlos.resource-server.enabled=false` | 激活 → permit-all |

### 问题

用户不配置任何安全属性 = **所有请求无认证放行**。这与"默认安全"原则相悖。但注释说明这阻止了 Spring Boot 默认的表单登录重定向（`/login`），是有意为之。

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 保持现状** | 这是有意设计，防止 Spring Boot 默认 form-login 接管 |
| **B. 改为默认需要认证** | 去掉 `matchIfMissing`，用户必须显式选择 |
| **C. 添加启动警告** | 保持行为但打印 WARN 日志提醒 |

---

## #22 — ApmConfig 6 个 matchIfMissing=true + 全量采样

**文件**: `carlos-spring-boot-starter-apm/src/main/java/com/carlos/apm/config/ApmConfig.java`

### 现状

6 个 `matchIfMissing = true`，其中 `Sampler` 默认 `sampleRate=1.0` (ALWAYS_SAMPLE)：

| Bean | 条件前缀 | 默认 |
|------|---------|------|
| `mdcFilterRegistration` | `carlos.apm.mdc.enabled` | ✅ 启用 |
| `apmSampler` | `carlos.apm.tracing.enabled` | ✅ 启用 + 100%采样 |
| `apmPropagationFactory` | `carlos.apm.tracing.enabled` | ✅ 启用 |
| `traceTagAspect` | `carlos.apm.tracing.enabled` | ✅ 启用 |
| `tracingFeignInterceptor` | `carlos.apm.feign.enabled` | ✅ 启用 |
| 整体模块 | `carlos.apm.enabled` | ✅ 启用 |

### 问题

- 引入此 starter = 全部 APM 组件默认激活
- 100% 采样率在生产环境有显著性能开销（Brave tracer 为每个请求创建 span）
- 如果 SkyWalking 已部署，Brave 相关组件可能冲突

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 全部改为 opt-in** | 去掉所有 `matchIfMissing`，用户必须显式启用 |
| **B. 降低默认采样率** | `sampleRate` 默认改为 0.1（10%），保留 `matchIfMissing` |
| **C. 保持现状** | 如果全量追踪是框架标准要求 |

---

## #23 — CaffeineConfig 默认启用本地缓存

**文件**: `carlos-spring-boot-redis/carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/caffeine/CaffeineConfig.java`

### 现状

```java
@ConditionalOnProperty(prefix = "carlos.cache.caffeine", name = "enabled", havingValue = "true", matchIfMissing = true)
```

### 问题

只要 Caffeine 在 classpath 上（已作为依赖引入），就自动创建 Caffeine 本地缓存，无需用户 opt-in。

### 可选方案

| 方案 | 操作 |
|------|------|
| **A. 改为 opt-in** | 去掉 `matchIfMissing` |
| **B. 保持现状** | Caffeine 是轻量级库，自动启用无显著负面影响 |

---

## #24 — 全局 matchIfMissing=true 问题

### 统计

`carlos-spring-boot/` 下共 **48 处** `matchIfMissing = true`，分布在 33 个文件中。

### 问题本质

大多数模块是 **opt-out 模式**（引入即启用），而非更安全的 opt-in 模式。

### 典型模式

```java
@ConditionalOnProperty(prefix = "carlos.xxx", name = "enabled", havingValue = "true", matchIfMissing = true)
```

### 唯一正确案例

`SeataConfig` 使用 `matchIfMissing = false`（opt-in），因为分布式事务不应默认启用。

### 建议策略

| 模块类型 | 策略 |
|---------|------|
| 核心功能（web, json, mybatis, redis） | 可保留 `matchIfMissing = true`（starter 引入即预期使用） |
| 高级功能（apm, flowable, migration, sentinel, disruptor, datascope） | 应改为 opt-in（性能/功能影响大） |
| 安全相关（security/disabled, encrypt） | 必须 opt-in |

---

## 决策汇总表

| # | 标题 | 严重性 | 推荐方案 |
|---|------|--------|---------|
| 8 | JacksonAutoConfiguration 覆盖 ObjectMapper | 🟡 HIGH | 待用户决策 |
| 9 | RedisLettuceConnection 复制内部实现 | 🟡 HIGH | 待用户决策 |
| 10 | ConverterAutoConfiguration 重复基础转换 | 🟡 HIGH | B. 保留日期，删除基础类型 |
| 11 | SentinelConfig 重复 SentinelResourceAspect | 🟡 HIGH | A. 删除 SentinelResourceAspect |
| 12 | FlowableAutoConfiguration 重写官方配置 | 🟡 HIGH | 已处置：模块已删除，改用 Warm-Flow |
| 13 | MongoDB 泄漏 MyBatis-Plus 概念 | 🟠 MEDIUM | 待用户决策 |
| 14 | DataScope 双缓存 Bean 竞争 | 🟠 MEDIUM | A. 合并单方法 |
| 15 | Disruptor 双 Metrics Bean | 🟠 MEDIUM | A. 合并+ObjectProvider |
| 16 | DbType 默认 POSTGRE_SQL | 🟠 MEDIUM | 待用户决策 |
| 17 | SMS 非标准前缀 | 🟠 MEDIUM | 待用户决策 |
| 18 | CORS 配置重复 Spring Boot 标准 | 🟠 MEDIUM | A. 迁移 spring.web.cors |
| 19 | Liquibase 与 Spring Boot 并行 | 🟠 MEDIUM | C. 改为 opt-in |
| 20 | EncryptProperties 日志输出密钥 | ⚠️ LOW | A. 脱敏输出 |
| 21 | ResourceServer 默认 permit-all | ⚠️ LOW | 待用户决策 |
| 22 | ApmConfig 默认全量追踪 | ⚠️ LOW | 待用户决策 |
| 23 | CaffeineConfig 默认启用 | ⚠️ LOW | A/B 均可 |
| 24 | 48处 matchIfMissing=true | ⚠️ LOW | 逐模块评估 |
