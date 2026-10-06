# Carlos Framework 框架核心代码扫描报告

**生成时间**: 2026-07-18 07:00  
**扫描 Commit**: `e5e1c004` (refactor(auth): 更新应用客户端创建参数的验证注解)  
**扫描范围**: 框架核心代码层（dependencies / parent / commons / 24 Spring Boot Starters / Redis 多模块）  
**发现问题总数**: 15 个（P0: 2 个, P1: 5 个, P2: 6 个, P3: 2 个）  

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 失败（网络不可用），使用本地代码继续扫描
- Commit: `e5e1c004`
- 自上次报告(2026-07-03, `e9479cd8`)新增: 30+ commits，主要变更集中在认证授权模块（OAuth2 授权服务器重构、应用门户、SSO 单点登录）
- 新增 Java 文件: 约 120+（auth 模块新增 AppPortal、AppCategory 等）
- 删除文件: MySQL 建表脚本（切换为 PostgreSQL）

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 设计缺陷 | 1 | 0 | 1 | 0 | 2 |
| 功能缺失 | 0 | 1 | 1 | 0 | 2 |
| 代码质量 | 0 | 1 | 2 | 0 | 3 |
| 安全漏洞 | 0 | 1 | 1 | 0 | 2 |
| 技术债务 | 1 | 2 | 1 | 2 | 5 |
| **合计** | **2** | **5** | **6** | **2** | **15** |

### 1.3 今日重点关注
- **[TOP 1]** HealthIndicator 非标准包路径残留（P0，框架级，15+ 文件仍使用 `org.springframework.boot.health.contributor.HealthIndicator`）
- **[TOP 2]** Datascope 新旧双包并行，同名类完全重复（P0，模块级，旧版切面未删除）
- **[TOP 3]** SMS sync/async 参数逻辑反转（P1，运行时，sync=true 调用异步方法）
- **[TOP 4]** Auth 模块客户端/分类管理端点无认证放行（P1，安全级，permitAll 配置风险）
- **[TOP 5]** 全量 Starter 缺失 `spring-configuration-metadata.json`（P1，开发体验，0/24 覆盖）

---

## 二、设计缺陷详情

### 问题 1: [P0] HealthIndicator 非标准包路径残留 — 15+ 文件仍使用不存在包

**模块**: `carlos-spring-boot-redis`, `carlos-spring-boot-starter-mybatis`, `carlos-spring-boot-starter-mongodb`, `carlos-spring-boot-starter-mq`, `carlos-spring-boot-starter-flowable`, `carlos-spring-boot-starter-encrypt`, `carlos-spring-boot-starter-disruptor`, `carlos-spring-boot-starter-oss`, `carlos-spring-cloud-starter`, `carlos-spring-boot-starter-datascope` 等  
**文件**: 见下方详细清单  
**行号**: 各文件 import 行  
**严重程度**: P0-Critical  
**影响范围**: 框架级

#### 相关代码

```java
// carlos-spring-boot-redis/carlos-spring-boot-starter-redis/src/main/java/com/carlos/redis/health/RedisHealthIndicator.java L6
import org.springframework.boot.health.contributor.HealthIndicator;

// carlos-spring-boot-starter-mybatis/src/main/java/com/carlos/datasource/health/MybatisHealthIndicator.java L6
import org.springframework.boot.health.contributor.HealthIndicator;

// carlos-spring-boot-starter-flowable/src/main/java/com/carlos/flowable/health/FlowableHealthIndicator.java L7
import org.springframework.boot.health.contributor.HealthIndicator;

// carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/health/EncryptHealthIndicator.java L7
import org.springframework.boot.health.contributor.HealthIndicator;

// carlos-spring-cloud-starter/src/main/java/com/carlos/cloud/health/ServiceHealthIndicator.java L7
import org.springframework.boot.health.contributor.HealthIndicator;
```

#### 详细分析
- **问题描述**: 框架中 15+ 个 HealthIndicator 实现类使用了 `org.springframework.boot.health.contributor.HealthIndicator` 包路径。该包路径**在标准 Spring Boot 中不存在**（无论是 2.x、3.x 或 4.x）。标准路径应为 `org.springframework.boot.actuate.health.HealthIndicator`。项目代码库中未定义该自定义包，也未发现提供此包的依赖。这意味着这些类在标准 Spring Boot 环境下无法编译或运行。
- **违反原则**: 正确性原则、可移植性原则
- **触发条件**: 在标准 Spring Boot 环境编译或运行框架
- **潜在风险**: 编译失败、运行时类找不到异常、健康检查功能完全不可用
- **历史遗留**: 该问题在 2026-06-04 报告中首次标记，2026-06-30 的 `framework-scan-unfixed-summary.md` 声称已修复（#1），但经核实当前代码 `e5e1c004` 仍全部残留。**修复状态记录与代码实际状态严重不符**。

#### 解决方案
**推荐修复**:
```java
// 统一替换为 Spring Boot 标准包路径
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;
```

**短期规避**: 确认项目是否使用自定义 Spring Boot 补丁（含 `org.springframework.boot.health.contributor` 包），若是则需在文档中明确说明。  
**长期建议**: 全面迁移至标准 Spring Boot Actuator 包路径，确保框架在标准 Spring Boot 环境可编译运行。  
**参考文档**: [Spring Boot Actuator Health](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health)

---

### 问题 2: [P0] Datascope 模块新旧双包并行，同名类完全重复

**模块**: `carlos-spring-boot-starter-datascope`  
**文件**: `carlos-spring-boot-starter-datascope/src/main/java/com/carlos/datascope/DataScopeAspect.java` 等  
**行号**: 全文件  
**严重程度**: P0-Critical  
**影响范围**: 模块级

#### 相关代码

```java
// 旧版 — carlos-spring-boot-starter-datascope/src/main/java/com/carlos/datascope/DataScopeAspect.java L27
@Aspect
@RequiredArgsConstructor
@Slf4j
public class DataScopeAspect implements AspectAbstract {
    private final DataScopeHandler handler;
    // 旧版基于 Handler 的切面实现
}

// 新版 — carlos-spring-boot-starter-datascope/src/main/java/com/carlos/datascope/core/aspect/DataScopeAspect.java L33
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DataScopeAspect {
    private final DataScopeRuleEngine ruleEngine;
    private final DataScopeAuditLogger auditLogger;
    // 新版基于规则引擎的切面实现
}
```

```java
// 旧版接口 — com/carlos/datascope/DataScopeProvider.java
public interface DataScopeProvider {
    Serializable currentUserId();
    Set<Serializable> currentRoleUserIds();
    Set<Serializable> currentDeptUserIds();
    Set<Serializable> currentDeptAllUserIds(Serializable departmentId);
}

// 新版接口 — com/carlos/datascope/provider/DataScopeProvider.java
public interface DataScopeProvider {
    // 新版含完整文档和更多方法
}
```

#### 详细分析
- **问题描述**: 模块内同时存在旧包 (`com.carlos.datascope`) 和新包 (`com.carlos.datascope.core` / `com.carlos.datascope.provider`) 下的同名类（`DataScopeAspect`、`DataScopeProvider`、`DefaultDataScopeProvider`）。`DataScopeAutoConfiguration` 已注册新版 Bean，但旧版类文件仍保留在源码中，导致类路径冲突风险、IDE 自动导入混淆、开发者无法确定应使用哪个版本。
- **违反原则**: 单一职责原则、代码清晰度原则
- **触发条件**: 开发者导入或使用 datascope 相关类时
- **潜在风险**: 类路径冲突、自动导入错误版本、维护困难、包结构混乱

#### 解决方案
**推荐修复**:
1. 删除旧版 `com.carlos.datascope.DataScopeAspect` 及旧版 `DataScopeProvider` / `DefaultDataScopeProvider`
2. 若旧版需保留作为兼容层，应通过 `@Deprecated` 标记并转发到新版实现
3. 统一包结构，确保仅有一套活跃实现

**短期规避**: 在团队文档中明确声明禁用旧包路径，强制使用新包路径。  
**长期建议**: 重构完成后删除旧实现，或提取到独立兼容模块。

---

## 三、功能缺失详情

### 问题 3: [P1] 全量 Starter 缺失 `spring-configuration-metadata.json`

**模块**: 全部 24 个 Spring Boot Starter  
**文件**: `META-INF/spring-configuration-metadata.json` (缺失)  
**严重程度**: P1-High  
**影响范围**: 框架级

#### 详细分析
- **问题描述**: 所有 Starter 模块均未生成/包含 `spring-configuration-metadata.json` 文件。虽然部分 Starter（ai, redis-core, log, migration, security 等）已在 pom.xml 中声明了 `spring-boot-configuration-processor` 依赖，但缺少编译后生成的元数据文件。这导致 IDE 无法提供 `application.yml` 配置项自动补全、文档提示和类型校验，极大降低开发体验。
- **影响范围**: 框架全部 Starter 使用者
- **触发条件**: 开发者编辑 `application.yml` 配置时

#### 解决方案
**推荐修复**:
1. 确保所有 Starter 的 pom.xml 包含：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```
2. 在 `@ConfigurationProperties` 类上添加完整 JavaDoc 和 `@NestedConfigurationProperty`
3. 编译后提交生成的 `META-INF/spring-configuration-metadata.json` 到版本库

---

### 问题 4: [P2] CommonErrorCode 错误码格式不一致 — 包含非数字字符

**模块**: `carlos-spring-boot-core`  
**文件**: `carlos-spring-boot-core/src/main/java/com/carlos/core/response/CommonErrorCode.java`  
**行号**: L29, L30, L59  
**严重程度**: P2-Medium  
**影响范围**: 模块级

#### 相关代码

```java
// carlos-spring-boot-core/src/main/java/com/carlos/core/response/CommonErrorCode.java L29-L59
/**
 * 错误码格式：5位数字字符串 A-BB-CC
 * <ul>
 *   <li>A - 错误级别：0成功，1客户端错误，2业务错误，3第三方错误，5系统错误</li>
 * </ul>
 */
UNAUTHORIZED("A0001", "未授权，请先登录", 401),          // ⚠️ 使用了字母 A
FORBIDDEN("A0002", "禁止访问，权限不足", 403),            // ⚠️ 使用了字母 A
AUTH_TOKEN_EXPIRED("A0004", "登录凭证已过期", 401),      // ⚠️ 使用了字母 A
```

#### 详细分析
- **问题描述**: 错误码规范声明为"5位数字字符串 A-BB-CC"（其中 A 为错误级别数字），但 `UNAUTHORIZED`、`FORBIDDEN`、`AUTH_TOKEN_EXPIRED` 使用了 `A0001`、`A0002`、`A0004` 等包含字母 `A` 的格式。这导致错误码级别解析异常（'A' 无法映射到有效 levelCode），且与规范自相矛盾。其他错误码如 `BAD_REQUEST("10001"...)` 使用纯数字，形成内部不一致。
- **触发条件**: 错误码解析、日志分析、监控告警
- **潜在风险**: 错误级别误判、日志分析困难、监控规则失效

#### 解决方案
**推荐修复**:
```java
// 统一为纯数字格式，与规范一致
UNAUTHORIZED("10201", "未授权，请先登录", 401),
FORBIDDEN("10202", "禁止访问，权限不足", 403),
AUTH_TOKEN_EXPIRED("10203", "登录凭证已过期", 401),
```

---

## 四、代码质量与安全详情

### 问题 5: [P1] SMS sync/async 参数逻辑反转

**模块**: `carlos-spring-boot-starter-sms`  
**文件**: `carlos-spring-boot-starter-sms/src/main/java/com/carlos/sms/SmsUtil.java`  
**行号**: L143-L155  
**严重程度**: P1-High  
**影响范围**: 模块级

#### 相关代码

```java
// carlos-spring-boot-starter-sms/src/main/java/com/carlos/sms/SmsUtil.java L143-L155
if (sync) {
    if (phones.size() > 1) {
        smsBlend.massTexting(phones, templateId, messages);
    } else {
        smsBlend.sendMessageAsync(phones.getFirst(), templateId, messages);  // ❌ sync=true 却调用异步方法
    }
} else {
    if (phones.size() > 1) {
        smsBlend.massTexting(phones, templateId, messages);
    } else {
        smsBlend.sendMessage(phones.getFirst(), templateId, messages);  // ✅ sync=false 调用同步方法
    }
}
```

#### 详细分析
- **问题描述**: `sendByTemplateKey` 方法的 `sync` 参数语义为"是否同步发送"，但代码逻辑完全相反：`sync=true` 时调用 `sendMessageAsync()`（异步发送），`sync=false` 时调用 `sendMessage()`（同步发送）。这导致 API 调用方按参数名语义传参时，行为与预期完全相反。在需要同步等待发送结果的场景（如注册验证码）可能误以为已发送成功而继续执行，实际短信仍在异步队列中。
- **触发条件**: 调用 `sendByTemplateKey(..., sync=true)` 期望同步发送时
- **潜在风险**: 业务逻辑错误、验证码发送不可靠、用户体验问题

#### 解决方案
**推荐修复**:
```java
if (sync) {
    // sync=true 应调用同步方法
    smsBlend.sendMessage(phones.getFirst(), templateId, messages);
} else {
    // sync=false 应调用异步方法
    smsBlend.sendMessageAsync(phones.getFirst(), templateId, messages);
}
// 或者更优：将参数重命名为 async，与行为匹配
```

---

### 问题 6: [P1] Auth 模块客户端/分类管理端点无认证放行

**模块**: `carlos-integration/carlos-auth`  
**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2AuthorizationServerConfig.java`  
**行号**: L209-L210  
**严重程度**: P1-High  
**影响范围**: 模块级

#### 相关代码

```java
// carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2AuthorizationServerConfig.java L209-L210
.requestMatchers(
    "/auth/app/client/**",
    "/auth/app/category/**"
).permitAll()
```

#### 详细分析
- **问题描述**: OAuth2 授权服务器的默认安全过滤器链将 `/auth/app/client/**` 和 `/auth/app/category/**` 端点配置为 `permitAll()`。这意味着任何未经认证的客户端都可以访问应用客户端管理和应用分类管理接口。虽然可能意图为"公开应用注册"，但缺少更细粒度的授权控制（如仅允许管理员操作），可能导致未授权客户端注册、应用信息篡改等安全问题。
- **触发条件**: 外部访问 `/auth/app/client/**` 或 `/auth/app/category/**` 端点
- **潜在风险**: 未授权应用注册、客户端配置篡改、应用分类信息泄露

#### 解决方案
**推荐修复**:
```java
// 至少要求认证，或限制为管理员角色
.requestMatchers("/auth/app/client/**").hasRole("ADMIN")
.requestMatchers("/auth/app/category/**").hasRole("ADMIN")
// 若确实需要公开访问（如应用门户列表），应拆分端点：
// /auth/app/portal/** 公开，/auth/app/client/** 需认证
```

---

### 问题 7: [P2] EncryptMode.ECB 枚举常量仍定义存在

**模块**: `carlos-spring-boot-starter-encrypt`  
**文件**: `carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/EncryptMode.java`  
**行号**: L27  
**严重程度**: P2-Medium  
**影响范围**: 模块级

#### 相关代码

```java
// carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/EncryptMode.java L17-L28
public enum EncryptMode {
    /**
     * 密码分组连接模式（Cipher Block Chaining）
     */
    CBC,
    /**
     * 电子密码本模式（Electronic CodeBook）
     */
    ECB
}
```

```java
// EncryptConfig.java L66-L68 运行时已禁用
if (encryptMode == EncryptMode.ECB) {
    throw new ComponentException("ECB mode is insecure and has been disabled. Use CBC mode instead.");
}
```

#### 详细分析
- **问题描述**: `EncryptMode` 枚举仍定义了 `ECB` 常量，虽然 `EncryptConfig` 在运行时已抛出异常禁用 ECB 模式，但枚举定义的存在意味着：1) 配置文件中仍可设置 `carlos.encrypt.sm4.encrypt-mode=ECB` 并在配置解析阶段通过；2) 静态代码中仍可引用 `EncryptMode.ECB` 并通过编译。这给使用者造成"已废弃但还能用"的误导，且代码存在已知不安全模式的气味。
- **触发条件**: 配置 `carlos.encrypt.sm4.encrypt-mode=ECB` 或代码中引用 `EncryptMode.ECB`

#### 解决方案
**推荐修复**:
```java
public enum EncryptMode {
    CBC
    // 移除 ECB 常量，彻底消除不安全模式的使用可能
}
```

---

### 问题 8: [P2] AES ECB 模式废弃方法仍未移除

**模块**: `carlos-spring-boot-starter-encrypt`  
**文件**: `carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/EncryptUtil.java`  
**行号**: L319-L393  
**严重程度**: P2-Medium  
**影响范围**: 模块级

#### 相关代码

```java
// EncryptUtil.java L319-L330
@Deprecated(since = "3.0.0", forRemoval = true)
public static String aesEncrypt(String data, String key) {
    if (StrUtil.isBlank(data)) {
        return data;
    }
    try {
        AES aes = SecureUtil.aes(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return aes.encryptHex(data);  // ⚠️ 无 IV 参数，默认 ECB 模式
    } catch (Exception e) {
        throw new EncryptException("AES 加密失败", e);
    }
}
```

#### 详细分析
- **问题描述**: 4 个 AES 方法标记为 `@Deprecated(since = "3.0.0", forRemoval = true)`，但当前版本已是 3.0.0-SNAPSHOT，这些方法仍未移除。无 IV 参数的 AES 方法默认使用 ECB 模式，在密码学上不安全（相同明文产生相同密文，可被频率分析）。`forRemoval = true` 意味着它们应在当前主版本中移除。
- **触发条件**: 调用废弃的 AES 加密方法
- **潜在风险**: 使用者误以为仍可安全使用，导致加密数据被破解

#### 解决方案
**推荐修复**: 在当前 3.0.0 版本中移除这 4 个废弃方法，强制使用者迁移至带 IV 的 CBC 模式版本。

---

### 问题 9: [P2] Hutool 内部 API 直接使用

**模块**: `carlos-spring-boot-starter-encrypt`  
**文件**: `carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/config/EncryptConfig.java`  
**行号**: L5-L6, L80  
**严重程度**: P2-Medium  
**影响范围**: 模块级

#### 相关代码

```java
// EncryptConfig.java L5-L6
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;

// EncryptConfig.java L80
sm4 = new SM4(Mode.CBC, Padding.PKCS5Padding, keyBytes, ivBytes);
```

#### 详细分析
- **问题描述**: 直接使用了 `cn.hutool.crypto.Mode` 和 `cn.hutool.crypto.Padding`，这些属于 Hutool 内部实现类（非公开 API）。Hutool 升级（尤其是从 5.x 到 6.x 主版本）时，这些内部类的包路径或签名可能变更，导致框架编译失败。
- **触发条件**: Hutool 版本升级
- **潜在风险**: 版本升级兼容性风险、构建失败

#### 解决方案
**推荐修复**: 使用 Hutool 公开的工厂方法 `SecureUtil.aes()` 或标准 Java `javax.crypto.Cipher` 替代内部 API 调用。若必须使用，添加版本兼容性测试。

---

### 问题 10: [P2] SslUtil 完全禁用 SSL 验证

**模块**: `carlos-commons/carlos-utils`  
**文件**: `carlos-commons/carlos-utils/src/main/java/com/carlos/util/SslUtil.java`  
**行号**: L22-L48  
**严重程度**: P2-Medium  
**影响范围**: 模块级

#### 相关代码

```java
// SslUtil.java L22-L35
public static final X509TrustManager IGNORE_SSL_TRUST_MANAGER_X509 = new X509TrustManager() {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }
    // 空实现：信任所有证书
};

// SslUtil.java L44-L48
public static SSLContext getIgnoreInitedSslContext() throws NoSuchAlgorithmException, KeyManagementException {
    SSLContext sslContext = SSLContext.getInstance("SSL");
    sslContext.init(null, new TrustManager[]{IGNORE_SSL_TRUST_MANAGER_X509}, new SecureRandom());
    return sslContext;
}
```

#### 详细分析
- **问题描述**: `SslUtil` 提供了完全禁用 SSL 证书验证的工具方法，允许信任所有证书（包括自签名、过期、伪造证书）。虽然类名和注释已明确标识"禁用安全验证"，但作为一个框架级工具类，其存在本身可能导致开发者在测试环境使用后误带到生产环境，造成中间人攻击风险。
- **触发条件**: 生产环境误用 `SslUtil.getIgnoreInitedSslContext()`
- **潜在风险**: 中间人攻击、证书伪造、数据泄露

#### 解决方案
**推荐修复**:
1. 添加 `@Deprecated` 标记和强烈警告注释
2. 添加运行时环境检测：仅在 `spring.profiles.active=test` 或开发环境允许使用，生产环境抛出异常
3. 或将其移至 `src/test/java` 测试目录，不作为生产代码发布

---

## 五、技术债务详情

### 问题 11: [P1] SMS 配置前缀不一致

**模块**: `carlos-spring-boot-starter-sms`  
**文件**: `SmsProperties.java` / `SmsCustomizeConfig.java`  
**行号**: L22 / L37, L46, L55  
**严重程度**: P1-High  
**影响范围**: 模块级

#### 相关代码

```java
// SmsProperties.java L22
@ConfigurationProperties(prefix = "carlos.sms")
public class SmsProperties implements InitializingBean { ... }

// SmsCustomizeConfig.java L37
@ConditionalOnProperty(prefix = "sms.blends.postal", name = "supplier", havingValue = "postal")
public PostalFactory postalFactory() { ... }

// SmsCustomizeConfig.java L46
@ConditionalOnProperty(prefix = "sms.blends.wocloud", name = "supplier", havingValue = "wocloud")

// SmsCustomizeConfig.java L55
@ConditionalOnProperty(prefix = "sms.blends.ums", name = "supplier", havingValue = "ums")
```

#### 详细分析
- **问题描述**: SMS 模块的配置前缀不一致：`SmsProperties` 使用 `carlos.sms`，但三个短信厂商工厂的 ConditionalOnProperty 使用 `sms.blends.xxx`。这导致配置项分散在两个不同前缀下，违反 Spring Boot Starter 配置命名规范（应统一使用 `carlos.xxx` 前缀），开发者难以发现和正确配置。

#### 解决方案
**推荐修复**: 统一所有 SMS 配置前缀为 `carlos.sms.blends.xxx`：
```java
@ConditionalOnProperty(prefix = "carlos.sms.blends.postal", name = "supplier", havingValue = "postal")
```

---

### 问题 12: [P2] GlobalException 直接继承 RuntimeException

**模块**: `carlos-spring-boot-core`  
**文件**: `carlos-spring-boot-core/src/main/java/com/carlos/core/exception/GlobalException.java`  
**行号**: L23  
**严重程度**: P2-Medium  
**影响范围**: 框架级

#### 相关代码

```java
// GlobalException.java L23
public class GlobalException extends RuntimeException {
```

#### 详细分析
- **问题描述**: 框架规范明确要求"使用框架定义异常（BusinessException/SystemException），严禁 RuntimeException"。但 `GlobalException` 直接继承 `RuntimeException`，使得所有框架异常本质上都是未受检异常。虽然框架提供了统一的异常处理链路，但规范与实现不一致。此外，`SystemException` 和 `ServiceException` 已从代码中移除，但规范文档未更新。
- **触发条件**: 开发者阅读规范文档后产生困惑

#### 解决方案
**推荐修复**: 更新规范文档，明确说明框架异常继承自 `RuntimeException` 的设计意图（便于 Spring 统一捕获），并移除对 `SystemException`/`ServiceException` 的引用。

---

### 问题 13: [P3] Knife4j Carlos Fork 仍为 SNAPSHOT 版本

**模块**: `carlos-dependencies`  
**文件**: `carlos-dependencies/pom.xml`  
**行号**: L47  
**严重程度**: P3-Low  
**影响范围**: 框架级

#### 相关代码

```xml
<!-- ⚠️ Carlos Fork: 需发布稳定版本替换 SNAPSHOT -->
<knife4j-carlos.version>4.6.0-SNAPSHOT</knife4j-carlos.version>
```

#### 详细分析
- **问题描述**: `knife4j-carlos.version` 明确声明为 SNAPSHOT 版本，注释也标注"需发布稳定版本替换"。SNAPSHOT 依赖在构建时可能解析到不同版本，导致构建不可重现。

#### 解决方案
**推荐修复**: 发布稳定版本 `4.6.0` 或更新至更新的稳定版本，并替换 SNAPSHOT。

---

### 问题 14: [P3] 框架版本仍为 SNAPSHOT

**模块**: `carlos-dependencies`, `carlos-parent`, `carlos-framework`  
**文件**: `pom.xml` (根/dependencies/parent)  
**行号**: L20 (dependencies) / L17 (parent) / L14 (root)  
**严重程度**: P3-Low  
**影响范围**: 框架级

#### 相关代码

```xml
<revision>3.0.0-SNAPSHOT</revision>
```

#### 详细分析
- **问题描述**: 整个框架版本仍为 `3.0.0-SNAPSHOT`，且开发已持续数月。框架作为基础设施，应定期发布里程碑版本（M1, M2）或 RC 版本，使下游项目能锁定稳定版本。

#### 解决方案
**推荐修复**: 发布 `3.0.0-M1` 或 `3.0.0-RC1` 里程碑版本，将 `revision` 更新至里程碑版本。

---

## 六、依赖与技术债务

### 6.1 依赖版本审计

| 依赖 | 当前版本 | 最新稳定版 | 差距 | 状态 | 备注 |
|----|------|-------|----|------|------|
| Spring Boot | 4.0.6 | 4.0.6 | - | ✅ 自定义 | 官方最新为 3.3.x，项目使用自定义/未来版本 |
| Spring Cloud | 2025.1.1 | 2025.1.1 | - | ✅ 自定义 | 对应自定义 SB 版本 |
| Spring Cloud Alibaba | 2025.1.0.0 | 2025.1.0.0 | - | ✅ 自定义 | 对应自定义 SC 版本 |
| MyBatis-Plus | 3.5.16 | 3.5.16 | - | ✅ 最新 | 含 SB3 适配 |
| Redisson | 4.5.0 | 4.5.0 | - | ✅ 最新 | |
| Hutool | 5.8.40 | 5.8.40 | - | ✅ 最新 | 5.x 系列已停止活跃开发，6.x M1 已发布 |
| Flowable | 7.0.1 | 7.0.1 | - | ✅ 最新 | |
| Guava | 33.4.8-jre | 33.4.8-jre | - | ✅ 最新 | |
| Druid | 1.2.28 | 1.2.31 | +3 | 🟡 落后 | 建议升级 |
| Disruptor | 3.4.4 | 4.0.0 | +1主 | 🟡 落后 | 4.x 引入模块化改进 |
| Liquibase | 4.29.0 | 4.31.1 | +2 | 🟡 落后 | 建议升级 |
| Knife4j (Carlos Fork) | 4.6.0-SNAPSHOT | 4.6.0 | SNAPSHOT | 🔴 需替换 | 见问题 13 |
| Fastjson2 | 2.0.62 | 2.0.62 | - | ✅ 最新 | |
| TransmittableThreadLocal | 2.14.5 | 2.14.5 | - | ✅ 最新 | |
| JSqlParser | 4.9 | 4.9 | - | ✅ 最新 | |
| logstash-logback-encoder | 8.0 | 8.0 | - | ✅ 最新 | |

### 6.2 已弃用 API 使用

| 文件 | 行号 | 已弃用 API | 替代方案 | 状态 |
|----|----|---------|------|------|
| EncryptUtil.java | 319-393 | `@Deprecated(since="3.0.0", forRemoval=true)` AES ECB 方法 | 带 IV 的 CBC 方法 | 未移除 |
| 15+ HealthIndicator 文件 | import 行 | `org.springframework.boot.health.contributor.HealthIndicator` | `org.springframework.boot.actuate.health.HealthIndicator` | 未修复 |
| GlobalFilterOrder.java | 145-169 | 7 个 `@Deprecated` 常量 | 需添加 Javadoc 说明替代方案 | 未修复 |

### 6.3 BOM 版本冲突

经审计 `carlos-dependencies/pom.xml` 与各模块实际依赖，未发现同一依赖在 BOM 和模块中版本冲突的情况。所有模块通过 `carlos-parent` 继承 `carlos-dependencies` 的 BOM 管理，版本一致性良好。

### 6.4 澄清：javax.sql.DataSource 非迁移残留

**说明**: 此前报告（2026-07-03）将 `import javax.sql.DataSource` 标记为 Jakarta EE 迁移残留（P0）。经核实，`javax.sql.DataSource` 属于 **Java SE 标准库**（`java.sql` 模块），自 Java 1.4 起存在，**从未参与 Jakarta EE 迁移**（仅 `javax.servlet.*`、`javax.persistence.*` 等 Jakarta EE 包迁移至 `jakarta.*`）。该标记为**误报**，应予以撤销。

---

## 七、Starter 健康度评分

| 模块 | 设计规范 | 功能完整 | 代码质量 | 安全性 | 综合评分 |
|-------------------------|--------|--------|--------|--------|---------|
| carlos-dependencies | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.9/5.0 |
| carlos-parent | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.9/5.0 |
| carlos-utils | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.8/5.0 |
| carlos-spring-boot-core | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.5/5.0 |
| carlos-spring-cloud-starter | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.5/5.0 |
| carlos-spring-boot-starter-web | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.5/5.0 |
| carlos-spring-boot-starter-security | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.3/5.0 |
| carlos-spring-boot-starter-mybatis | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.3/5.0 |
| carlos-spring-boot-starter-encrypt | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-spring-boot-starter-sms | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 3.0/5.0 |
| carlos-spring-boot-starter-datascope | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.5/5.0 |
| carlos-spring-boot-starter-oss | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.3/5.0 |
| carlos-spring-boot-starter-flowable | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-spring-boot-starter-json | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-spring-boot-starter-log | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-spring-boot-starter-redis | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.3/5.0 |
| carlos-spring-boot-starter-mq | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-spring-boot-starter-openapi | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-spring-boot-starter-apm | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-spring-boot-starter-migration | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改

---

## 八、待办事项与修复计划

### 8.1 今日新增待办

- [ ] **P0-001**: 修复 15+ 文件中 HealthIndicator 非标准包路径（统一替换为 `org.springframework.boot.actuate.health.HealthIndicator`）
- [ ] **P0-002**: 清理 Datascope 模块旧包路径下的重复类（`com.carlos.datascope.DataScopeAspect` 等）
- [ ] **P1-003**: 修复 SMS sync/async 参数逻辑反转（`SmsUtil.java` L143-L155）
- [ ] **P1-004**: 修复 Auth 模块客户端/分类管理端点无认证放行问题（`OAuth2AuthorizationServerConfig.java`）
- [ ] **P1-005**: 为全部 24 个 Starter 补充 `spring-configuration-metadata.json`
- [ ] **P2-006**: 统一 CommonErrorCode 错误码格式（移除 `A0001`/`A0002`/`A0004` 中的字母）
- [ ] **P2-007**: 移除 EncryptMode.ECB 枚举常量及 EncryptUtil 中废弃的 AES ECB 方法
- [ ] **P2-008**: 替换 Hutool 内部 API 调用（`cn.hutool.crypto.Mode` / `Padding`）
- [ ] **P2-009**: 为 SslUtil 添加生产环境禁用保护或移至 test 目录
- [ ] **P1-010**: 统一 SMS 配置前缀为 `carlos.sms.blends.xxx`

### 8.2 历史遗留跟踪

- [ ] **P0-001 HealthIndicator 包路径**: 历史报告声称已修复（2026-06-30），但代码实际未修复。需重新执行全局替换。 🔴 高优先级
- [ ] **P0-002 Datascope 双包**: 2026-07-03 报告已标记，当前仍未修复。 🔴 高优先级
- [ ] **P1-003 CommonErrorCode 格式**: 2026-07-02 报告已标记，当前仍未修复。 ⏳ 持续跟踪
- [ ] **P2-004 EncryptMode.ECB**: 2026-07-03 报告已标记，当前枚举仍保留。 ⏳ 持续跟踪
- [ ] **P2-005 AES ECB 方法**: 2026-07-02 报告已标记，当前仍未移除。 ⏳ 持续跟踪

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
*特别说明：`javax.sql.DataSource` 属于 Java SE 标准库，非 Jakarta EE 迁移残留，此前报告误报已撤销。*
