# Carlos Framework — 框架全量扫描报告

**扫描日期**: 2026-07-01 07:07 Asia/Shanghai  
**扫描范围**: 全部 25 个 carlos-spring-boot starters + carlos-dependencies + carlos-parent + carlos-commons + 8 个 integration 模块  
**扫描方式**: 多子智能体并行 + 主智能体补充直接扫描  
**当前 Commit**: ca253d3a (chore: daily optimization report 2026-04-30-0918)

---

## 📊 扫描结果总览

| 严重级别 | 数量 | 说明 |
|----------|------|------|
| **P0** 🔴 | 5 | 必须立即修复 — 安全漏洞 / 构建阻断 |
| **P1** 🟠 | 17 | 高优先级 — 功能缺陷 / 潜在风险 |
| **P2** 🟡 | 8 | 中优先级 — 可改进的设计问题 |
| **P3** 🟢 | 4 | 低优先级 — 体验/文档优化 |

---

## 🔴 P0 — 严重问题

### P0-001: ErrorCode 格式违规 — 含字母前缀的 Auth 错误码

- **文件**: `carlos-spring-boot/carlos-spring-boot-core/src/main/java/com/carlos/core/response/CommonErrorCode.java`
- **行**: 29, 30, 59
- **CWE**: CWE-1104 (Use of Unmaintained Error Code Scheme)

```java
UNAUTHORIZED("A0001", "未授权，请先登录", 401),      // line 29
FORBIDDEN("A0002", "禁止访问，权限不足", 403),          // line 30
AUTH_TOKEN_EXPIRED("A0004", "登录凭证已过期", 401),    // line 59
```

**分析**: `ErrorCode` 接口 JavaDoc 规定错误码必须是 **"5位数字字符串 A-BB-CC"**。`ErrorLevel.fromCode()` 提取首字符 `charAt(0)` 并期望数字 `'0'`–`'5'`，字母前缀会导致解析失败，落入 `SYSTEM_ERROR` 默认级别。

**修复**:
```java
UNAUTHORIZED("10106", "未授权，请先登录", 401),
FORBIDDEN("10107", "禁止访问，权限不足", 403),
// AUTH_TOKEN_EXPIRED 移至 AuthErrorCode("20211", ...)
```

---

### P0-002: knife4j-carlos SNAPSHOT 依赖 — 发布阻断

- **文件**: `carlos-dependencies/pom.xml`
- **行**: `knife4j-carlos.version` 属性

```xml
<!-- ⚠️ Carlos Fork: 需发布稳定版本替换 SNAPSHOT -->
<knife4j-carlos.version>4.6.0-SNAPSHOT</knife4j-carlos.version>
```

**影响**: 两个 Carlos fork 的 knife4j artifact 使用 SNAPSHOT 版本：
- `com.github.carlos:knife4j-openapi3-jakarta-spring-boot-starter`
- `com.github.carlos:knife4j-gateway-spring-boot-starter`

SNAPSHOT 不可重现构建，若 SNAPSHOT 仓库不可用则构建失败。

**修复**: 将 fork 发布为正式版本（如 `4.6.0`），替换 SNAPSHOT。

---

### P0-003: 安全白名单包含 `/actuator/**` 和 `/error` — 信息泄漏

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-security/src/main/java/com/carlos/security/config/ResourceServerProperties.java`
- **行**: 89–96
- **CWE**: CWE-200 (Exposure of Sensitive Information)

```java
private Set<String> whitelistPaths = new HashSet<>() {{
    add("/actuator/**");    // 🔴 未认证可访问 actuator
    add("/error");           // 🔴 未认证可访问错误页
    add("/swagger-ui/**");
    // ...
}};
```

**分析**: `/error` 默认白名单下 Spring Boot 错误页可能泄露异常栈、框架版本。`/actuator/**` 暴露健康检查/指标，可能包含敏感数据。建议仅白名单 Swagger 文档路径。

**修复**: 从默认白名单移除 `/actuator/**` 和 `/error`，需要时由用户显式配置。

---

### P0-004: tests 默认跳过 + 失败不阻断构建

- **文件**: `carlos-parent/pom.xml`, maven-surefire-plugin 配置

```xml
<configuration>
    <skipTests>true</skipTests>                         <!-- 🔴 默认跳过所有测试 -->
    <testFailureIgnore>true</testFailureIgnore>          <!-- 🔴 测试失败不阻断构建 -->
</configuration>
```

**分析**: 双重失效保护 — 即使用 `-DskipTests=false`，测试失败也不会阻断构建。完全丧失 CI 门控能力。

**修复**: 立即移除 `testFailureIgnore=true`。

---

### P0-005: Datascope v1 遗留代码与 v2 共存 — 同名的两个 @DataScope 注解

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-datascope/`
  - v1: `DataScope.java` (root package), `conf/DataScopeConfig.java`, `conf/DefaultDataScopeHandler.java`
  - v2: `annotation/DataScope.java`, `auto/DataScopeAutoConfiguration.java`

**分析**: 同一个模块中存在两个完全不兼容的 `@DataScope` 注解，v1 版有 `caller()`/`methodPoint()` 属性，v2 版有 `dimension()`/`tables()`/`priority()` 属性。v1 的 `DataScopeConfig` 虽然是 `@Configuration` 但不在 `AutoConfiguration.imports` 中，属于死代码。若被 component-scan 意外扫描，将导致运行时冲突。

**修复**: 删除 v1 遗留代码（`conf/` 目录、root package 的 `DataScope.java`、`DataScopeAspect.java`）。

---

## 🟠 P1 — 高优先级问题

### P1-001: Commons Fileupload 1.5 含已知 CVE

- **文件**: `carlos-dependencies/pom.xml`
- **CVE**: CVE-2023-24998 — DoS via file count manipulation

**修复**: 升级至 ≥ 1.5.1。

---

### P1-002: SMS 限流默认关闭

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-sms/src/main/java/com/carlos/sms/config/SmsProperties.java`
- **行**: ~58

```java
private boolean enabled = false;   // 🔴 默认关闭
```

**分析**: 短信是付费服务，限流默认关闭可能导致短信额度被耗尽。

**修复**: 默认值改为 `true`，`accountMax` 调整为更保守值。

---

### P1-003: SMS 限流 TOCTOU 竞态条件

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-sms/src/main/java/com/carlos/sms/SmsUtil.java`
- **行**: ~75-80 (check) / ~110-115 (record)

```java
if (!rateLimiter.allowSend(phone)) { ... }   // 检查（非原子）
smsBlend.sendMessage(...);                    // 中间隔着网络 I/O
rateLimiter.recordSend(phone);                // 记录（非原子）
```

**分析**: check-then-act 反模式。恶意用户可在 `allowSend()` 通过后瞬间并发发送海量请求。

**修复**: 合并为 `tryAcquire()` 原子操作。

---

### P1-004: SMS 限流无过期 Key 清理 — 内存泄漏

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-sms/src/main/java/com/carlos/sms/ratelimit/LocalSmsRateLimiter.java`

**分析**: `dayCounters`/`minuteCounters` 使用 `ConcurrentHashMap` 按手机号+时间分区存储，永不过期回收。每天新增 key，长期运行 OOM。

**修复**: 使用 Guava Cache 或 Caffeine 的 TTL 特性，或定期清理过期 key。

---

### P1-005: EncryptUtil 在 Bean 未初始化时静默返回原文

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/EncryptUtil.java`
- **行**: ~78-82, ~118-121, ~133-136, ~183-186

```java
public static String sm4EncryptHex(String str) {
    if (null == sm4) {
        return str;  // 🔴 静默返回原文！
    }
}
```

**分析**: fail-open 设计。若 encrypt starter 未正确配置，加密方法直接返回原文，密码以明文存储。

**修复**: `sm4 == null` 时抛出 `EncryptException`，而非静默返回。

---

### P1-006: Datacenter 模块未完成 — 多个 TODO/空方法

- **文件**: 
  - `carlos-spring-boot/carlos-spring-boot-starter-datacenter/src/main/java/.../config/DatacenterMainConfig.java` — `init()` 空方法、TODO
  - `carlos-spring-boot/carlos-spring-boot-starter-datacenter/src/main/java/.../instance/DatacenterInstance.java` — `data()` 空方法、TODO `2024/9/13 待设计`
  - `DatacenterFactory.java` — `register(instance, weight)` 接受 weight 但静默丢弃

**分析**: Datacenter 是一个未完成的空壳。核心抽象设计清晰但无具体实现。

**修复**: 完成 TODO 或标记为实验性模块并在文档中说明。

---

### P1-007: DataScope 模块脱敏方法为空实现

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-datascope/src/main/java/.../core/aspect/DataScopeAspect.java`
- **行**: `applyMasking()` 方法

```java
// TODO: 实现具体的脱敏逻辑
// 当前无任何脱敏操作，数据原样返回
```

**分析**: 对外承诺的数据脱敏功能完全未实现，调用方误以为数据已被脱敏。

**修复**: 实现脱敏逻辑，或明确文档说明脱敏功能尚未就绪。

---

### P1-008: DataScope MyBatis 拦截器只拦截 SELECT

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-datascope/src/main/java/.../interceptor/MyBatisDataScopeInterceptor.java`

```java
// 注释明确写明："UPDATE 和 DELETE 暂不处理"
```

**分析**: 写操作（UPDATE/DELETE）完全绕过数据权限控制，用户可修改/删除不在其数据范围内的记录。

**修复**: 扩展拦截器覆盖 UPDATE/DELETE 语句。

---

### P1-009: 无 CORS 自动配置 — 依赖 Spring Boot 默认

- **参考**: `AGENTS.md` §8 声明了 `ApplicationCorsConfig` 但实际不存在

**分析**: 文档承诺的 CORS 配置类未实现。Spring Boot 默认不限制跨域，生产部署若未手动配置 CORS 存在安全风险。

**修复**: 实现 `CorsAutoConfiguration`，默认要求显式配置 `allowed-origins`。

---

### P1-010: LoadBalancerConfig 文档声明但未实现

- **参考**: `AGENTS.md` §8 列出 `LoadBalancerConfig` 为 spring-cloud-starter 注册配置

**分析**: 文档声称存在 `LoadBalancerConfig`，但 `spring-cloud-starter` 中无此文件。实际使用 Spring Cloud 内置 `LoadBalancerAutoConfiguration`。

**修复**: 更正文档或实现 Carlos 自定义负载均衡配置。

---

### P1-011: Disruptor bufferSize 无非幂数校验

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-disruptor/src/main/java/.../config/DisruptorProperties.java`

**分析**: `bufferSize` 默认 1024。若用户设置非 2 的幂，Disruptor 在运行时抛出 `IllegalArgumentException`，无启动时校验。

**修复**: 在 Properties 中增加 `@AssertTrue` 或 `afterPropertiesSet()` 校验。

---

### P1-012: DataScope ContextHolder 使用 TransmittableThreadLocal 但未包装线程池

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-datascope/src/main/java/.../core/context/DataScopeContextHolder.java`

**分析**: 使用了 `TransmittableThreadLocal`，但框架未自动包装 `@Async` 或其他自定义线程池为 `TtlExecutors`。在线程池场景下上下文会丢失。

**修复**: 在 AutoConfiguration 中通过 `BeanPostProcessor` 自动包装 `ExecutorService` Bean。

---

### P1-013: DefaultDataScopeProvider 返回 null — 无声失效

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-datascope/src/main/java/.../auto/DataScopeProviderConfiguration.java`

```java
// 日志: "please implement your own DataScopeProvider for production"
// 但 DefaultDataScopeProvider 查找角色/部门/区域均返回 null
```

**分析**: 默认提供者返回 null，若用户部署时未覆盖，**无任何数据权限生效**，属于无声失效。

**修复**: 至少记录 WARN 级别日志；或在生产环境 profile 下启动时 fail-fast。

---

### P1-014: DatacenterFactory 静态 ConcurrentHashMap 无清理机制

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-datacenter/src/main/java/.../DatacenterFactory.java`

**分析**: `INSTANCES` 持有所有实例，无任何 eviction 机制。若动态创建/销毁实例，无法回收。

---

### P1-015: MqAutoConfiguration 使用已废弃的 HealthIndicator API

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-mq/src/main/java/com/carlos/mq/config/MqAutoConfiguration.java`
- **行**: ~53

```java
import org.springframework.boot.health.contributor.HealthIndicator;  // 非标准 API
```

**分析**: 使用了非标准 `HealthIndicator` 包路径。标准路径应为 `org.springframework.boot.actuate.health.HealthIndicator`。

---

### P1-016: MongoDB AutoConfiguration 启用条件严格要求 carlos.mongodb.enabled=true

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-mongodb/src/main/java/com/carlos/mongodb/config/MongoAutoConfiguration.java`

```java
@ConditionalOnProperty(prefix = "carlos.mongodb", name = "enabled", havingValue = "true")
// 注意：没有 matchIfMissing=true
```

**分析**: 与其他 starter 不一致（大部分使用 `matchIfMissing=true`）。用户引入 mongo-plus 后必须显式开启，否则不生效。这可能是故意的保守策略，但与其他模块行为不一致。

---

### P1-017: ApmConfig TracingFeignInterceptor — Tracer 可能为 null

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-apm/src/main/java/com/carlos/apm/config/ApmConfig.java`
- **行**: tracingFeignInterceptor Bean 创建处

```java
return new TracingFeignInterceptor(tracerProvider.getIfAvailable());  // 可能为 null
```

**分析**: `Tracer` 仅在 Brave 条件装配满足时存在。若 Feign 模块存在但 Tracer 不存在（如 SkyWalking 模式下），Feign 拦截器收到 null Tracer。需要确认 `TracingFeignInterceptor` 对 null Tracer 的防御性处理。

---

## 🟡 P2 — 中优先级问题

### P2-001: AI 对话无输入过滤 — Prompt 注入风险

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-ai/src/main/java/com/carlos/ai/service/impl/AiChatServiceImpl.java`
- **行**: ~77, ~93, ~108, ~120

```java
return model.chat(message);   // 用户输入直接透传
```

**修复**: 增加输入长度上限、Prompt Guard 输入过滤层。

---

### P2-002: AI 流式对话无超时控制

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-ai/src/main/java/com/carlos/ai/service/impl/AiChatServiceImpl.java`

**分析**: `chatStream*` 方法通过回调处理流式响应，无超时机制。AI 模型长时间无响应时调用方无限等待。

**修复**: 使用 `CompletableFuture.orTimeout()` 包装。

---

### P2-003: SM4 密钥仅检查长度不检查质量

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/config/EncryptConfig.java`

**分析**: 仅验证密钥 16 字节，不检查全零/全一/重复模式等弱密钥。

**修复**: 增加密钥熵值检查，低于阈值打印 WARN。

---

### P2-004: EncryptMode.ECB 枚举保留但运行时禁用

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/EncryptMode.java`

**分析**: ECB 枚举值保留但运行时抛出异常，配置期无法感知，体验差。

**修复**: 在 `afterPropertiesSet()` 中提前校验。

---

### P2-005: AES 无 IV 方法已 @Deprecated(forRemoval=true) 但未确认内部无调用

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/EncryptUtil.java`

**修复**: 全局搜索调用点完成迁移后直接删除。

---

### P2-006: Datascope RuleEngine — v2 rule 表达式类型枚举缺失值处理

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-datascope/src/main/java/.../core/engine/DefaultRuleEngine.java`

**分析**: 规则类型 switch 语句缺少 default 分支，未知类型时可能穿透。

**修复**: 添加 default 分支打印 WARN 日志并跳过。

---

### P2-007: DisruptorEvent.clear() 无对象池 — 调用浪费

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-disruptor/src/main/java/.../core/DisruptorEvent.java`

**分析**: `clear()` 用于"对象池复用"，但实际上 `DisruptorEventFactory` 每次 `newInstance()` 都创建新对象。要么实现对象池，要么移除 `clear()`。

---

### P2-008: 多个 Configuration 类使用 @Autowired(required=false) 字段注入

- **文件**: 
  - `OssAutoConfiguration.java` — `@Autowired(required = false) S3Client s3Client`
  - `MyBatisPlusConfig.java` — 混合构造器和 `@Autowired`

**分析**: 字段注入不利于测试和不可变性。应统一使用构造器注入。

---

## 🟢 P3 — 低优先级问题

### P3-001: Datascope v2 DataScopeAspect 使用 @AspectAbstract（可能为遗留依赖）

**分析**: 需要确认 `@AspectAbstract` 是否为 Carlos 框架自定义注解，避免依赖 v1 抽象层。

---

### P3-002: Disruptor EventHandlerGroup.toEventHandlers() 无异常处理

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-disruptor/src/main/java/.../chain/EventHandlerGroup.java`

**分析**: 该方法包装 handler 时无异常捕获。若被外部调用，异常可导致 Disruptor worker 线程崩溃。当前方法未被使用，但其存在可能误导未来开发者。

---

### P3-003: AI ChatMemory 缓存无淘汰监听 — 运维不可观测

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-ai/src/main/java/com/carlos/ai/service/impl/AiChatServiceImpl.java`

**分析**: Caffeine 缓存达上限时静默淘汰，无日志或 metrics。

**修复**: 添加 `removalListener` 和 Micrometer metrics。

---

### P3-004: DatacenterInstanceInitializer 在构造函数中调用 onApplicationEvent()

- **文件**: `carlos-spring-boot/carlos-spring-boot-starter-datacenter/src/main/java/.../config/DatacenterInstanceInitializer.java`

**分析**: 构造函数中执行重量级初始化可能引发循环依赖。建议延迟到 `@PostConstruct` 或 `ApplicationListener`。

---

## 📋 依赖版本评估

### 非标准版本依赖（Maven Central 不可查）

| 依赖 | 声明版本 | Maven Central | 状态 |
|------|----------|---------------|------|
| Spring Boot | 4.0.6 | 3.5.3 | 🔴 私有源 |
| Redisson | 4.5.0 | 3.50.0 | 🔴 私有源 |
| Hutool | 5.8.40 | 5.8.36 | 🟡 私有源 |
| MyBatis-Plus | 3.5.16 | 3.5.7 | 🟡 私有源 |
| Fastjson2 | 2.0.62 | 2.0.57 | 🟡 私有源 |
| LangChain4j | 1.13.1 | 1.0.0 | 🟡 私有源 |
| Spring Cloud Alibaba | 2025.1.0.0 | 2023.1.3.3 | 🔴 大版本差距 |

> **评估**: 若从私有仓库 `zcarlos.com:8081` / `192.168.3.30:8081` 解析成功，则正常。但应文档化内部版本发布策略。

### 已知过期依赖

| 依赖 | 当前 | 推荐 | 落后 |
|------|------|------|------|
| Seata | 2.5.0 | 2.6.0 | 1 minor |
| Liquibase | 4.29.0 | 4.32.0 | 3 minor |
| Commons Fileupload | 1.5 | 1.6.0 | 1 minor + CVE |
| Freemarker | 2.3.31 | 2.3.34 | 3 minor |
| Disruptor | 3.4.4 | 4.0.0 | 1 major |

### 构建配置

| 问题 | 文件 | 严重度 |
|------|------|--------|
| maven-enforcer 缺少 `dependencyConvergence` 规则 | carlos-parent/pom.xml | P2 |
| 缺少 OWASP dependency-check 插件 | carlos-parent/pom.xml | P2 |
| Resource filtering 只过滤 application*.yml — 遗漏 logback-spring.xml 等 | carlos-parent/pom.xml | P3 |

---

## 📊 模块健康度评分

| 模块 | 设计质量 | 完成度 | 安全性 | 评分 |
|------|----------|--------|--------|------|
| carlos-spring-boot-core | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | 7.5 |
| starter-web | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 9.0 |
| starter-security | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8.5 |
| starter-mybatis | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 9.5 |
| starter-redis | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 9.5 |
| starter-json | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8.0 |
| starter-log | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8.0 |
| starter-mq | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8.0 |
| starter-flowable | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 8.5 |
| starter-apm | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 9.0 |
| starter-encrypt | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | 7.5 |
| starter-ai | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | 6.5 |
| starter-sms | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | 5.5 |
| starter-disruptor | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8.0 |
| starter-datacenter | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ | 5.0 |
| starter-datascope | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | 6.5 |
| starter-oss | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8.0 |
| starter-mongodb | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | 7.0 |
| starter-migration | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8.0 |
| starter-snowflake | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8.0 |
| starter-translation | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | 7.0 |
| starter-integration | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | 7.0 |
| starter-openapi | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 8.0 |
| spring-cloud-starter | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | 7.5 |

---

## 🎯 优先修复路线图

### 第1周 — P0 修复
1. 修复 CommonErrorCode 字母前缀错误码（P0-001）
2. 发布 knife4j-carlos 稳定版本，替换 SNAPSHOT（P0-002）
3. 修复安全白名单，移除 `/actuator/**` 和 `/error` 默认值（P0-003）
4. 移除 `testFailureIgnore=true`（P0-004）
5. 删除 datascope v1 遗留代码（P0-005）

### 第2周 — P1 修复
1. 升级 Commons Fileupload 至 1.6.0（P1-001）
2. SMS 限流默认开启 + 修复 TOCTOU 竞态（P1-002, P1-003）
3. SMS 限流内存泄漏修复（P1-004）
4. EncryptUtil fail-open 改为 fail-close（P1-005）
5. DataScope 脱敏功能完成或标记不可用（P1-007）
6. DataScope 拦截器扩展覆盖 UPDATE/DELETE（P1-008）

### 第3-4周 — P2 优化
1. AI 模块输入过滤 + 超时控制（P2-001, P2-002）
2. SM4 密钥质量检查（P2-003）
3. Datacenter 模块完成 TODO 或标记实验性
4. 其余 P2 项

---

*报告由 Carlos Framework 开发助手自动生成*
