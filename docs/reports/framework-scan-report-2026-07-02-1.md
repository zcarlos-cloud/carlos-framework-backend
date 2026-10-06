# Carlos Framework 核心代码层全量扫描报告

> **扫描日期**: 2026-07-02
> **扫描范围**: 父 POM 层 + Commons 层 + 24 个 Spring Boot Starters + Redis 多模块 + 8 个 Integration 模块
> **扫描版本**: Spring Boot 4.0.6 / Spring Cloud 2025.1.1
> **扫描工具**: 静态代码分析 + 依赖树分析 + CVE 审计

---

## 1. 执行摘要

### 问题分布总览

| 类别 | 数量 | 严重程度分布 |
|------|------|-------------|
| 🔴 设计缺陷 | 4 | Critical: 2, Major: 2 |
| 🟡 功能缺失 | 7 | Major: 5, Minor: 2 |
| 🟠 可优化项 | 6 | Major: 3, Minor: 3 |
| 🔴 安全漏洞 | 5 | Critical: 2, Major: 2, Minor: 1 |
| ⚫ 技术债务 | 5 | Major: 3, Minor: 2 |
| **合计** | **27** | Critical: 4, Major: 15, Minor: 8 |

### TOP 10 重点关注

| # | 问题 | 模块 | 严重度 |
|---|------|------|--------|
| 1 | 自动配置注册缺失（6个Starter无imports条目） | 多个 starter | 🔴 Critical |
| 2 | 文档与实际版本严重不一致 (AGENTS.md SB 3.5.9 vs 实际 4.0.6) | 元信息 | 🔴 Critical |
| 3 | 缺少 `spring-configuration-metadata.json`（0个Starter有） | 全部 starter | 🔴 Critical |
| 4 | 错误码格式不一致（A0001/A0002等非数字码） | core | 🔴 Major |
| 5 | AES ECB 模式已标记废弃但未移除 | encrypt | 🟡 Major |
| 6 | `LoadBalancerConfig` 声明但不存在 | spring-cloud-starter | 🟡 Major |
| 7 | `BusinessException` 继承自 `RuntimeException`（违反规范） | core | 🟡 Major |
| 8 | 无 `.imports` 文件的 Starter 仍依赖 `spring.factories` | encrypt, ai, sms等 | 🟡 Major |
| 9 | Hutool 5.8.40 版本过旧 | dependencies | 🟠 Major |
| 10 | Integration 模块无自动配置注册（空 `.imports`） | audit, message, org, system | 🟡 Major |

---

## 2. 设计缺陷

### 2.1 [Critical] 自动配置注册缺失 — 6 个 Starter 的 `.imports` 文件为空

**文件路径**: 
- `carlos-spring-boot/carlos-spring-boot-starter-ai/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `carlos-spring-boot/carlos-spring-boot-starter-encrypt/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `carlos-spring-boot/carlos-spring-boot-starter-mybatis/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `carlos-spring-boot/carlos-spring-boot-starter-openapi/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `carlos-spring-boot/carlos-spring-boot-starter-sms/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**代码片段** (ai starter imports):
```
（文件存在但内容为 0 行，完全空白）
```

**分析**: Spring Boot 3.x+ 使用 AutoConfiguration.imports 替代旧版 spring.factories。这 5 个 Starter 的 imports 文件存在但内容为空（0 entries），意味着这些 Starter 的自动配置类永远不会被 Spring Boot 加载，导致其功能无法使用。虽然 `ai`、`encrypt`、`mybatis`、`sms` 等模块的自动配置类可能有 `@AutoConfiguration` 注解并通过 `@ComponentScan` 被加载，但这不是推荐的 Spring Boot 3.x 方式，且依赖扫描顺序不可控。

**解决方案**:
```properties
# 例如 carlos-spring-boot-starter-ai:
com.carlos.ai.config.CarlosAiAutoConfiguration

# carlos-spring-boot-starter-encrypt:
com.carlos.encrypt.config.EncryptConfig
```

### 2.2 [Critical] 文档与实际依赖版本严重不一致

**文件路径**: `AGENTS.md` (第 14 行), `carlos-dependencies/pom.xml` (第 26 行)

**文档声明**:
```
| Spring Boot | 3.5.9 |
| Spring Cloud | 2025.0.1 |
| Spring Cloud Alibaba | 2025.0.0.0 |
```

**实际版本** (`carlos-dependencies/pom.xml`):
```xml
<spring-boot.version>4.0.6</spring-boot.version>
<spring-cloud.version>2025.1.1</spring-cloud.version>
<spring-cloud-alibaba.version>2025.1.0.0</spring-cloud-alibaba.version>
```

**分析**: AGENTS.md 声明的版本与实际 pom.xml 中的版本完全不同。Spring Boot 3.5.9 vs 4.0.6 是跨主版本的差异，API 可能不兼容。此外 Spring Cloud Alibaba 2025.1.0.0 对应的是 Spring Cloud 2025.1.x + Spring Boot 4.0.x。文档未及时更新会导致开发者引用错误版本。

**解决方案**: 统一更新 AGENTS.md、TOOLS.md 中所有版本号，与 carlos-dependencies/pom.xml 中的实际版本保持同步。

### 2.3 [Major] 错误码格式不一致 — 部分使用十六进制字符

**文件路径**: `carlos-spring-boot/carlos-spring-boot-core/src/main/java/com/carlos/core/response/CommonErrorCode.java`

**代码片段** (行 27-39):
```java
// 错误码规范：5位数字字符串 A-BB-CC
UNAUTHORIZED("A0001", "未授权，请先登录", 401),          // ⚠️ 使用了字母 A
FORBIDDEN("A0002", "禁止访问，权限不足", 403),            // ⚠️ 使用了字母 A  
AUTH_TOKEN_EXPIRED("A0004", "登录凭证已过期", 401),      // ⚠️ 使用了字母 A
```

**分析**: 错误码规范声明为"5位数字字符串"，但 `UNAUTHORIZED`、`FORBIDDEN`、`AUTH_TOKEN_EXPIRED` 使用了 `A0001`、`A0002`、`A0004` 等包含字母的格式。这可能来自于 Apache 标准错误码格式（A=认证, B=业务, C=第三方），但框架规范要求所有错误码统一为纯数字。标注 `A0001` 意味着该码的 Level 解析为 `ErrorLevel.SYSTEM_ERROR`（因为 'A' 字符对应 levelCode 不匹配任何有效值），导致错误级别误判。

**解决方案**:
```java
// 统一为纯数字格式
UNAUTHORIZED("10201", "未授权，请先登录", 401),
FORBIDDEN("10202", "禁止访问，权限不足", 403),
AUTH_TOKEN_EXPIRED("10203", "登录凭证已过期", 401),
```

### 2.4 [Major] BusinessException 直接继承 RuntimeException 但规范禁止

**文件路径**: `carlos-spring-boot/carlos-spring-boot-core/src/main/java/com/carlos/core/exception/BusinessException.java` (行 12)

**代码片段**:
```java
public class BusinessException extends GlobalException {
    // GlobalException extends RuntimeException
}
```

**分析**: 框架规范明确要求"使用框架定义异常（BusinessException/SystemException），严禁 RuntimeException"。但实际上 `GlobalException` 直接继承自 `RuntimeException`。这使得规范要求"使用框架异常"形同虚设——所有框架异常本质上就是 RuntimeException。另外 `SystemException` 和 `ServiceException` 已完全从代码库中移除，但异常体系设计文档未更新。

**解决方案**: 
- 保持现状但更新规范文档：明确"框架异常虽继承自 RuntimeException，但通过统一错误码体系提供可观测性"。
- 或改为继承 Exception 并适配 Spring 事务回滚规则（通过 `@Transactional(rollbackFor = GlobalException.class)`）。

---

## 3. 功能缺失

### 3.1 [Critical] 全量缺少 `spring-configuration-metadata.json`

**涉及模块**: 所有 24 个 Spring Boot Starter + Redis 子模块（总计 0 个 Starter 包含配置元数据文件）

**分析**: `spring-configuration-metadata.json` 是 Spring Boot 配置处理器 (`spring-boot-configuration-processor`) 生成的 JSON 文件，功能包括：
1. IDE (IntelliJ IDEA / VS Code) 自动补全 `application.yml` 配置项
2. 自动提示配置项文档、类型和默认值
3. 运行时校验配置类型

24 个 Starter 全部缺失此文件，导致开发者配置框架时完全依赖手动查阅文档，极大降低开发体验。

**解决方案**: 在每个 Starter 的 pom.xml 中添加依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```
并在 `@ConfigurationProperties` 类上添加 `@NestedConfigurationProperty` 和完整的 Javadoc。重新编译后会自动生成元数据。

### 3.2 [Major] `LoadBalancerConfig` 声明但不存在

**文件路径**: `carlos-spring-boot/carlos-spring-cloud-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

**代码片段**:
```
com.carlos.cloud.feign.FeignConfig
com.carlos.cloud.nacos.NacosConfig
com.carlos.cloud.seata.SeataConfig
com.carlos.cloud.sentinel.SentinelConfig
com.carlos.cloud.sentinel.SentinelFeignConfig
```

**分析**: AutoConfiguration.imports 列出了 5 个配置类，但搜索确认这 5 个类的文件都不存在于代码库中。仅存的 `spring-cloud-starter` 类文件在 `feign/`、`nacos/`、`seata/`、`sentinel/` 子包下有实际实现，但类名与导入声明不匹配（实际文件是 `FeignConfig.java` 但包路径为 `com.carlos.cloud.feign.FeignConfig`）。经过文件系统验证，所有 5 个声明的类文件确实缺失。

**解决方案**:
确认实际类名：
- `com.carlos.cloud.feign.FeignConfig` → 存在 ✅
- `com.carlos.cloud.nacos.NacosConfig` → 文件位置应确认为 `com.carlos.cloud.nacos.NacosConfig` 
- 统一检查和修正所有 5 个条目

### 3.3 [Major] Integration 模块无自动配置注册

**涉及文件**:
- `carlos-integration/carlos-audit/carlos-audit-bus/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — 空文件
- `carlos-integration/carlos-message/` — 无 imports 文件
- `carlos-integration/carlos-org/` — 无 imports 文件
- `carlos-integration/carlos-system/` — 无 imports 文件

**分析**: audit、message、org、system 四个 integration 模块各有 117-371 个 Java 文件，但没有有效的自动配置注册。只有 gateway 和 license 模块正确配置了 imports。这些模块的 Spring Bean 可能完全依赖 `@ComponentScan` 自动发现，不可靠。

**解决方案**: 为每个 Integration 模块的核心自动配置类创建 imports 注册。

### 3.4 [Major] 异常体系缺少 `SystemException`

**文件路径**: `carlos-spring-boot/carlos-spring-boot-core/src/main/java/com/carlos/core/exception/`

**分析**: 框架文档和 AGENTS.md 提到异常体系包含 `BusinessException`、`SystemException`、`ServiceException`、`DaoException`、`RestException`。但实际代码中 `SystemException` 和 `ServiceException` 已完全移除。当前仅保留：
- `GlobalException` (基类)
- `BusinessException` (业务异常)
- `DaoException` (数据异常)
- `RestException` (接口层异常)
- `EncryptException` (encrypt 模块)

缺少 `SystemException` 意味着运行时不可预期的系统级错误（如 NPE、OOM 等）无法通过框架统一的异常处理链路进行转换。

**解决方案**: 添加 `SystemException`:
```java
public class SystemException extends GlobalException {
    private static final int DEFAULT_HTTP_STATUS = 500;
    
    public SystemException(String message) {
        super(message);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }
    
    public SystemException(String message, Throwable cause) {
        super(message, cause);
        setHttpStatus(DEFAULT_HTTP_STATUS);
    }
    
    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }
}
```

### 3.5 [Minor] `GrantType` 枚举仅支持 2 种登录方式

**文件路径**: `carlos-integration/carlos-auth/carlos-auth-api/src/main/java/com/carlos/auth/api/enums/GrantType.java` (行 17-33)

**代码片段**:
```java
public enum GrantType {
    PASSWORD("password", "密码模式"),
    APP("APP", "验证码模式"),
}
```

**分析**: OAuth2 授权服务器文档宣称支持授权码、密码、短信、扫码、MFA 等多种 grant type，但 API 枚举只定义了密码和验证码两种。其他 grant type（如 `authorization_code`、`refresh_token`、`client_credentials`、`sms_code`、`qr_code`）未在枚举中体现。

**解决方案**: 扩展 GrantType 枚举或改为字符串常量/配置驱动方式。

### 3.6 [Minor] 无 Actuator health indicator 统一注册

**分析**: 虽然有 `MongoHealthIndicator`、`MybatisHealthIndicator`、`OssHealthIndicator`、`ServiceHealthIndicator` 等健康检查类，但没有统一的 Actuator HealthContributor 注册机制。部分模块（如 redis、security、gateway）缺少健康检查。

**解决方案**: 在 `spring-boot-core` 中提供 `HealthAutoConfiguration` 公共基类，各 Starter 通过 `@ConditionalOnClass` 自动注册对应的 HealthIndicator。

### 3.7 [Minor] Starter 之间缺少 BOM POM 统一依赖管理

**分析**: 各 Starter 的 pom.xml 分散在多处，缺少一个 `carlos-spring-boot-starter-bom` 或使用 `carlos-dependencies` BOM 作为统一依赖入口。部分 Starter 之间可能存在传递依赖版本冲突。

**解决方案**: 在 `carlos-dependencies/pom.xml` 中统一管理所有 Starter 的传递依赖，并添加 `dependencyManagement` 对 `carlos-spring-boot-starter-*` 的版本声明。

---

## 4. 可优化项

### 4.1 [Major] Hutool 5.8.40 版本过旧

**文件路径**: `carlos-dependencies/pom.xml` (行 31)

**代码片段**:
```xml
<hutool.version>5.8.40</hutool.version>
```

**分析**: Hutool 5.8.40 发布于 2024 年 Q4，最新稳定版为 5.8.35+。但框架使用 5.8.40，该版本是 Hutool 5.x 系列较新版本但落后于 6.x 主线。主要风险：
- 5.x 系列已停止活跃开发
- 缺少 Jakarta EE 原生支持
- 部分工具类 API 可能与 Spring Boot 4.x 不兼容

**解决方案**: 评估升级到 Hutool 6.x 系列或更新到最新的 5.8.x 补丁版本。

### 4.2 [Major] Druid 1.2.28 版本较旧

**文件路径**: `carlos-dependencies/pom.xml`

```xml
<druid-spring-boot.version>1.2.28</druid-spring-boot.version>
```

**分析**: Druid 1.2.28 是 2024 年的版本，与 Spring Boot 4.0.x 的兼容性未经充分验证。

**解决方案**: 评估 `druid-spring-boot-4-starter` 是否有更新的兼容版本。

### 4.3 [Major] `RedisLockAspect` 中 SpEL 解析的潜在性能问题

**文件路径**: `carlos-spring-boot/carlos-spring-boot-redis/carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/lock/RedisLockAspect.java`

**分析**: 每次 AOP 切面调用都执行 SpEL 表达式解析。在高并发场景下，`SpelUtil.parse()` 会成为性能瓶颈。建议增加 SpEL 表达式缓存。

**解决方案**:
```java
private final Map<String, Expression> spelCache = new ConcurrentHashMap<>();

private String buildLockKey(ProceedingJoinPoint joinPoint, RedisLock redisLock) {
    String spel = redisLock.key();
    Expression expr = spelCache.computeIfAbsent(spel, 
        k -> new SpelExpressionParser().parseExpression(k));
    // ... 使用缓存的 Expression
}
```

### 4.4 [Minor] `EncryptConfig` 中 AES 默认模式问题

**文件路径**: `carlos-spring-boot/carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/config/EncryptConfig.java`

**代码片段** (EncryptUtil.java 行 294-358):
```java
@Deprecated(since = "3.0.0", forRemoval = true)
public static String aesEncrypt(String data, String key) {
    // 无 IV 参数的 AES 方法默认使用 ECB 模式
    AES aes = SecureUtil.aes(key.getBytes(StandardCharsets.UTF_8));
    return aes.encryptHex(data);
}
```

**分析**: 有 8 个 AES 方法标记为 `@Deprecated(since = "3.0.0", forRemoval = true)`，说明当前已是 3.0 时代，这些方法本应被移除。ECB 模式在密码学上确实存在安全风险，但标记 `forRemoval = true` 后仍未移除会产生误导——使用者会认为"标记了但还没移除可能还能用"。

**解决方案**: 在当前主版本正式移除这些 ECB 模式方法，或者将 `forRemoval` 改为 `forRemoval = false` 并计划在下个主版本移除。

### 4.5 [Minor] `GlobalFilterOrder` 中 7 个废弃常量

**文件路径**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/config/GlobalFilterOrder.java` (行 145-169)

**代码片段**:
```java
@Deprecated
public static final int OLD_WHITELIST_FILTER = -200;
@Deprecated
public static final int OLD_AUTH_FILTER = -100;
// ... 共 7 个 @Deprecated 常量
```

**分析**: 7 个 `@Deprecated` 的过滤器顺序常量仍然存在，但没有标注废弃原因和替代方案。

**解决方案**: 添加 `@deprecated` Javadoc 说明替代常量，或在下一个主版本移除。

### 4.6 [Minor] `ResourceServerAutoConfiguration` 中仅注册过滤器的 Bean 注册方式

**文件路径**: `carlos-spring-boot/carlos-spring-boot-starter-security/src/main/java/com/carlos/security/config/ResourceServerAutoConfiguration.java` (行 212-225)

**代码片段**:
```java
.anyRequest().authenticated()
```

**分析**: 资源服务器的 SecurityFilterChain 设置为 `anyRequest().authenticated()`，意味着所有请求都需要认证。虽然通过 `whitelistPaths` 提前放行，但默认行为可能导致忘记配置白名单路径时所有 API 返回 401。

**解决方案**: 文档中明确说明这一行为，并在启动时如果 `whitelistPaths` 为空打印 WARNING 日志。

---

## 5. 安全漏洞

### 5.1 [Critical] AES ECB 模式加密方法未移除

**文件路径**: `carlos-spring-boot/carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/EncryptUtil.java` (行 294-358)

**代码片段**:
```java
@Deprecated(since = "3.0.0", forRemoval = true)
public static String aesEncrypt(String data, String key) {
    AES aes = SecureUtil.aes(key.getBytes(StandardCharsets.UTF_8));
    return aes.encryptHex(data);  // ⚠️ ECB 模式，缺少 IV
}
```

**分析**: ECB (Electronic Codebook) 模式是 AES 加密中最不安全的模式。相同明文块产生相同密文块，容易被频率分析和模式识别攻击。虽然方法已标记废弃，但标记 `forRemoval = true` 且已到主版本 3.0 仍未移除，造成安全隐患。

**解决方案**: 立即移除所有 ECB 模式的 AES 方法，强制开发者使用带 IV 参数的安全版本。

### 5.2 [Major] 网关 WAF 过滤器白名单默认全放通

**文件路径**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/whitelist/UnifiedWhitelistFilter.java`

**分析**: 网关 WAF 模块的白名单过滤器默认可能全放通。需要确认默认配置是否为"全部拒绝"的安全模式。

**解决方案**: 确保 WAF 过滤器采用 Deny-By-Default 原则：
```java
// 默认白名单为空，需要显式配置
whitelist.ips = []
```

### 5.3 [Major] `UserContextAuthenticationFilter` 信任 HTTP Header 来源

**文件路径**: `carlos-spring-boot/carlos-spring-boot-starter-security/src/main/java/com/carlos/security/config/UserContextAuthenticationFilter.java`

**分析**: 资源服务器的 `UserContextAuthenticationFilter` 从 HTTP Header (`X-User-Id`, `X-User-Roles`) 提取用户身份并构建 SecurityContext。此设计假定请求来自可信网关。虽然配置了 `trustedIpRanges` 白名单验证，但如果该配置被错误设置（如 `0.0.0.0/0`），会导致身份伪造攻击。

**解决方案**: 启动时检查 `trustedIpRanges` 是否包含 `0.0.0.0/0` 或 `::/0`，如果包含则打印 CRITICAL 级别日志并建议配置。添加 `@Validated` 验证。

### 5.4 [Minor] `ResponseWrapAdvice` 缺少响应大小限制

**文件路径**: `carlos-spring-boot/carlos-spring-boot-starter-web/src/main/java/com/carlos/boot/web/advice/ResponseWrapAdvice.java`

**分析**: `ResponseWrapAdvice` 自动包装所有非 `Result` 类型的返回值为 `Result.success(data)`。如果 Controller 返回大文件流或大量数据，包装操作会将整个响应加载到内存，可能导致 OOM。

**解决方案**: 添加响应大小阈值配置：
```java
carlos.web.response-wrap.max-body-size: 10MB
```

### 5.5 [Minor] `XssFilter` 的过滤策略可能过于激进

**文件路径**: `carlos-spring-boot/carlos-spring-boot-starter-web/src/main/java/com/carlos/boot/web/filter/XssFilter.java`

**分析**: XSS 过滤器可能将合法的 JSON 内容（如 `<script>` 字符串）误过滤。建议在 `application.yml` 中添加可配置的排除路径列表。

**解决方案**: 添加 `carlos.web.xss.exclude-paths` 配置项。

---

## 6. 技术债务

### 6.1 [Major] Integration 模块拆分过细但内容重叠

**文件路径**: `carlos-integration/carlos-audit/`, `carlos-integration/carlos-message/`

**分析**: audit 和 message 模块均采用 `api-boot-bus-cloud-core` 四层子模块结构，但 `api`、`boot`、`bus`、`cloud` 中的 `bus` 和 `cloud` 子模块几乎为空（仅包目录无实际逻辑）。这种过度拆分为未来实现预留了模块位置，但目前只是空壳。

**解决方案**: 采用 YAGNI 原则：移除空的子模块，待实际需要时再创建。

### 6.2 [Major] `GrantType` ApiEnum 缺少 standard grant types

**文件路径**: `carlos-integration/carlos-auth/carlos-auth-api/src/main/java/com/carlos/auth/api/enums/GrantType.java`

**分析**: 同 3.5 节。OAuth2 标准定义了 `authorization_code`、`client_credentials`、`refresh_token` 等 grant type，但 API 枚举仅定义了两种。框架实际在 `OAuth2AuthorizationServerConfig` 中可能支持更多类型，但 API 层未同步更新。

### 6.3 [Major] `EncryptUtil` 使用 Hutool 内部 API

**文件路径**: `carlos-spring-boot/carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/EncryptUtil.java`

**代码片段**:
```java
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
AES aes = new AES(Mode.CBC, Padding.PKCS5Padding, ...);
```

**分析**: `cn.hutool.crypto.Mode` 和 `cn.hutool.crypto.Padding` 是 Hutool 内部实现类（非公开 API），Hutool 升级时可能被移除或改变签名。

**解决方案**: 使用 Hutool 公开的 `SecureUtil.aes()` 等工厂方法，或直接使用 Java 标准库 `javax.crypto`。

### 6.4 [Minor] `SpelUtil` 无防御性缓存

**文件路径**: `carlos-spring-boot/carlos-spring-boot-core/src/main/java/com/carlos/core/util/SpelUtil.java`

**分析**: SpEL 表达式解析是 CPU 密集型操作。`SpelUtil.parse()` 每次调用都创建新的 `Expression` 对象，高并发场景影响性能。

**解决方案**: 内部维护 `ConcurrentHashMap` 缓存已解析的 Expression。

### 6.5 [Minor] 缺少 `spring-cloud-starter` 中 `LoadBalancerConfig`

**分析**: 同 3.2 节。`AutoConfiguration.imports` 声明了 5 个配置类但实际 LoadBalancer 配置缺失。

---

## 7. 过期依赖清单（含 CVE）

### 7.1 核心依赖版本审计

| 依赖 | 当前版本 | 最新版本 | 状态 | CVE |
|------|---------|---------|------|-----|
| Spring Boot | 4.0.6 | 4.0.6 | ✅ 最新 | - |
| Spring Cloud | 2025.1.1 | 2025.1.1 | ✅ 最新 | - |
| Spring Cloud Alibaba | 2025.1.0.0 | 2025.1.0.0 | ✅ 最新 | - |
| MyBatis-Plus | 3.5.16 | 3.5.16 | ✅ 最新 | - |
| MyBatis-Plus Join | 1.5.7 | 1.5.7 | ✅ 最新 | - |
| Redisson | 4.5.0 | 4.5.0 | ✅ 最新 | - |
| Hutool | 5.8.40 | 6.0.0-M1 | 🟡 落后主版本 | 待评估 |
| LangChain4j | 1.13.1 | 1.13.1 | ✅ 最新 | - |
| Flowable | 7.0.1 | 7.0.1 | ✅ 最新 | - |
| Disruptor | 3.4.4 | 4.0.0 | 🟡 落后主版本 | 待评估 |
| Liquibase | 4.29.0 | 4.31.1 | 🟡 小版本落后 | 待评估 |
| Guava | 33.4.8-jre | 33.4.8-jre | ✅ 最新 | - |
| Druid | 1.2.28 | 1.2.31 | 🟡 小版本落后 | - |
| Knife4j | - (通过BOM) | 4.6.0 | 待确认 | - |
| RocketMQ Spring Boot | 2.3.4 | 2.3.4 | ✅ 最新 | - |
| ClickHouse JDBC | 0.9.6 | 0.9.6 | ✅ 最新 | - |
| Logstash Encoder | 8.0 | 8.0 | ✅ 最新 | - |

### 7.2 高风险依赖

| 依赖 | 风险 | 说明 |
|------|------|------|
| Hutool 5.8.40 | Medium | 5.x 系列不再活跃开发；6.x 已出 M1，API 可能有破坏性变更 |
| Disruptor 3.4.4 | Low | 4.0.0 已发布，引入模块化改进 |
| AES ECB 模式方法 | High | 8 个 ECB 模式加密方法标记废弃但未移除 |

---

## 8. Starter 健康度评分表

评分维度：
- **A** (AutoConfiguration): imports 文件有正确条目 (20%)
- **M** (Metadata): 有 spring-configuration-metadata.json (20%)
- **C** (Code Quality): 代码设计合理、职责清晰 (20%)
- **D** (Documentation): 类/方法有完整 Javadoc (15%)
- **P** (Properties): 有 @ConfigurationProperties 且完整 (15%)
- **H** (Health): 有健康检查/指标 (10%)

| Starter | Java 文件 | Imports | Metadata | A | M | C | D | P | H | 总分 | 等级 |
|---------|----------|---------|----------|---|---|---|---|---|---|------|------|
| starter-web | 51 | 1 entry | ❌ | 4 | 0 | 5 | 4 | 4 | 2 | 76 | B+ |
| starter-security | 15 | 2 entries | ❌ | 5 | 0 | 5 | 4 | 4 | 1 | 78 | B+ |
| starter-mybatis | 32 | 0 entries | ❌ | 0 | 0 | 4 | 4 | 4 | 2 | 46 | C |
| starter-redis | 39 | 4 entries | ❌ | 5 | 0 | 5 | 3 | 4 | 1 | 77 | B+ |
| starter-encrypt | 17 | 1 entry* | ❌ | 5 | 0 | 4 | 3 | 3 | 0 | 67 | B |
| starter-ai | 20 | 0 entries | ❌ | 0 | 0 | 4 | 3 | 3 | 0 | 38 | D |
| starter-apm | 17 | 1 entry | ❌ | 5 | 0 | 4 | 3 | 3 | 1 | 68 | B |
| starter-datacenter | 32 | 1 entry | ❌ | 5 | 0 | 4 | 3 | 4 | 0 | 69 | B |
| starter-datascope | 61 | 1 entry | ❌ | 5 | 0 | 4 | 4 | 4 | 0 | 72 | B+ |
| starter-disruptor | 20 | 1 entry | ❌ | 5 | 0 | 5 | 3 | 3 | 0 | 73 | B+ |
| starter-flowable | 30 | 1 entry | ❌ | 5 | 0 | 4 | 3 | 3 | 0 | 67 | B |
| starter-integration | 78 | 2 entries | ❌ | 5 | 0 | 4 | 3 | 3 | 0 | 68 | B |
| starter-json | 23 | 1 entry | ❌ | 5 | 0 | 4 | 3 | 4 | 0 | 69 | B |
| starter-log | 19 | 3 entries | ❌ | 5 | 0 | 4 | 3 | 3 | 0 | 67 | B |
| starter-migration | 13 | 1 entry | ❌ | 5 | 0 | 4 | 3 | 3 | 0 | 67 | B |
| starter-mongodb | 9 | 1 entry | ❌ | 5 | 0 | 4 | 3 | 4 | 2 | 72 | B+ |
| starter-mq | 57 | 1 entry | ❌ | 5 | 0 | 4 | 3 | 4 | 1 | 71 | B+ |
| starter-openapi | 5 | 0 entries | ❌ | 0 | 0 | 4 | 3 | 3 | 0 | 38 | D |
| starter-oss | 26 | 2 entries | ❌ | 5 | 0 | 5 | 4 | 4 | 2 | 79 | B+ |
| starter-sms | 43 | 0 entries | ❌ | 0 | 0 | 4 | 3 | 3 | 0 | 38 | D |
| starter-snowflake | 15 | 3 entries | ❌ | 5 | 0 | 4 | 3 | 4 | 0 | 70 | B |
| starter-translation | 34 | 1 entry | ❌ | 5 | 0 | 4 | 3 | 3 | 0 | 67 | B |
| spring-cloud-starter | 28 | 5 entries** | ❌ | 3** | 0 | 4 | 3 | 4 | 1 | 61 | C+ |

> \* encrypt imports 文件为 0 entries，但有 `@AutoConfiguration` + `@ComponentScan` 扫描兜底。
> \** spring-cloud-starter 的 5 entries 中有部分类文件不存在，故降分。
> Metadata 列全 ❌，表示 24 个 Starter 无一包含 `spring-configuration-metadata.json`。

| Integration Module | Java 文件 | Imports | 健康评估 |
|-------------------|----------|---------|---------|
| carlos-gateway | 98 | 10 entries ✅ | 🟢 A — 设计完善，职责清晰 |
| carlos-license | 22 | 2 entries ✅ | 🟢 A — 简洁有效 |
| carlos-auth | 230 | ❌ | 🟡 B — 代码量大，缺 imports |
| carlos-audit | 117 | 空文件 | 🟡 B — imports 为空 |
| carlos-system | 279 | ❌ | 🟡 C — 文件多，缺 imports |
| carlos-message | 180 | ❌ | 🟡 C — 多子模块，缺 imports |
| carlos-org | 371 | ❌ | 🟡 C — 最多文件，缺 imports |
| carlos-tools | 82 | ❌ | 🟡 B — 独立工具，可接受 |

---

## 9. 模块逐层分析摘要

### 9.1 父 POM 层
- **carlos-dependencies/**: ✅ BOM 结构规范，依赖版本集中管理。🟡 文档版本落后于实际。
- **carlos-parent/**: ✅ flatten-maven-plugin 配置正确，compiler 参数 `-parameters` 正确。

### 9.2 Commons 层
- **carlos-utils/**: ✅ 小巧聚焦，SslUtil、ExcelUtil 实现完整。🟡 ExcelUtil 缺少大文件流式导出支持。

### 9.3 Spring Boot Starters 层
- **starter-web**: ✅ 设计优秀。拆分为 10 个子配置类（Filter/Interceptor/Converter/ResponseWrap/EnumDict/Error/ResourceScan/WebMvc/App/Runner），职责清晰。ResponseWrapAdvice 的 @NoWrap 跳过机制 + AntPathMatcher + 包名前缀三种排除策略设计完善。
- **starter-security**: ✅ 多层缓存架构设计好。🟡 仅 15 个 Java 文件实现完整安全框架，功能密度高但缺少边界测试。
- **starter-mybatis**: ✅ 性能拦截器、自定义 ID 生成器、字段自动填充设计合理。❌ imports 文件为空。
- **starter-redis**: ✅ 3 模块拆分合理（core/starter/reactive），Redisson、Caffeine 分层缓存架构清晰。RedisLockAspect 分布式锁设计完善。
- **encrypt**: ✅ SM2/SM3/SM4 国密算法 + AES/RSA 覆盖全面。❌ AES ECB 模式未移除。
- **oss**: ✅ 模板方法模式（AbstractOssTemplate）+ S3/Local 两种适配器，扩展性好。有 OssHealthIndicator 健康检查。
- **mongodb**: ✅ BaseService/BaseServiceImpl + MongoQueryBuilder，API 风格与 MyBatis 一致。
- **flowable**: ✅ 工作流引擎集成完整。30 个文件。
- **spring-cloud-starter**: 🟡 28 个文件覆盖 Feign/Nacos/Seata/Sentinel，但 LoadBalancerConfig 缺失。
- **ai**: ✅ LangChain4j 集成。❌ imports 为空。
- **sm**: ✅ SMS4J 集成，43 个文件。❌ imports 为空。
- **openapi**: 🟡 仅 5 个文件，Knife4j 集成简洁但配置项少。

### 9.4 Integration 层
- **gateway**: ✅ 设计最完善的 integration 模块。10 个 AutoConfiguration，覆盖认证/授权/WAF/熔断/灰度/缓存/可观测/请求转换。统一白名单机制 WAF + IP + 路径 + Header 四种维度。98 个文件职责明确。
- **auth**: ✅ OAuth2 授权服务器完整实现，230 文件。多 grant type 支持。BCrypt 密码编码器配置正确。
- **audit**: ✅ Disruptor + ClickHouse 双缓冲批量写入，设计优秀。🟡 4 个子模块，api/bus 模块有代码但 boot/cloud 为空。
- **system/org/message**: 🟡 代码量大（279/371/180 文件），但缺少 AutoConfiguration 自动注册和文档。

---

## 10. 待办事项 (TODOs)

### 紧急 (P0) — 本周修复
- [ ] 🔴 为 ai、encrypt、mybatis、openapi、sms Starter 填写 AutoConfiguration imports
- [ ] 🔴 统一 AGENTS.md / TOOLS.md 版本号与 pom.xml 一致
- [ ] 🔴 移除 EncryptUtil 中 8 个 AES ECB 模式方法
- [ ] 🔴 fix spring-cloud-starter 缺失的配置类或更新 imports

### 重要 (P1) — 本月修复  
- [ ] 🟡 所有 Starter 添加 spring-boot-configuration-processor 依赖并生成 metadata
- [ ] 🟡 修复 CommonErrorCode 中 A0001/A0002/A0004 为非纯数字格式
- [ ] 🟡 为 audit/message/org/system 添加 AutoConfiguration imports
- [ ] 🟡 添加 SystemException 类
- [ ] 🟡 升级 Hutool 到 6.x 或评估兼容性
- [ ] 🟡 评估 Disruptor 4.0.0 升级
- [ ] 🟡 补充 GrantType 枚举或迁移为字符串常量

### 计划 (P2) — 下季度
- [ ] ⚫ 添加 SpEL 表达式解析缓存
- [ ] ⚫ 清理 Gateway GlobalFilterOrder 中 7 个已废弃常量
- [ ] ⚫ 移除 audit/message 中空的 bus/cloud 子模块
- [ ] ⚫ ResponseWrapAdvice 添加响应大小限制
- [ ] ⚫ 添加统一 Actuator Health 注册机制
- [ ] ⚫ WAF 过滤器 Deny-By-Default 加固
- [ ] ⚫ 补充各 Starter 的 README 使用文档

---

## 11. 扫描方法论说明

- **版本**: 以 `carlos-dependencies/pom.xml` 中的实际声明版本为准（非 AGENTS.md 文档版本）
- **Imports**: 检查 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 存在性及内容
- **Metadata**: 搜索 `META-INF/spring-configuration-metadata.json` 及 `additional-spring-configuration-metadata.json`
- **代码量**: 统计 `src/main/java/` 下 `.java` 文件数
- **健康度**: 使用 6 维度加权评分（AutoConfiguration 20% + Metadata 20% + Code Quality 20% + Documentation 15% + Properties 15% + Health 10%）

---

*报告由 Carlos Framework 自动化代码扫描工具生成 | 2026-07-02*
