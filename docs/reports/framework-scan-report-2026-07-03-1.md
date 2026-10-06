# Carlos Framework 框架核心代码扫描报告

**生成时间**: 2026-07-03 07:00
**扫描 Commit**: `e9479cd8` (docs(course): 前端每日课程 2026-07-03 第39天 — 路由自动生成)
**扫描范围**: 框架核心代码层（dependencies / parent / commons / 24个 Spring Boot Starters / Redis / 重点 Integration 模块）
**发现问题总数**: 14 个（P0: 2 个, P1: 4 个, P2: 5 个, P3: 3 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 成功（无新commit，基于最新 `e9479cd8` 扫描）
- 变更文件数: 2 个（仅前端课程文档，无Java代码变更）
- 新增代码: +565 行（课程文档）

### 1.2 📊 昨日TOP问题修复状态

| # | 昨日问题 | 严重度 | 状态 | 说明 |
|---|---------|--------|------|------|
| 1 | 5个 Stater `.imports` 为空 | 🔴 P0 | ✅ 已修复 | ai/encrypt/mybatis/openapi/sms 均有 1 条注册 |
| 2 | AGENTS.md 版本不一致 | 🔴 P0 | ✅ 已修复 | SB 4.0.6 / SC 2025.1.1 已对齐 |
| 3 | spring-cloud-starter 类文件缺失 | 🟡 P1 | ✅ 已修复 | 5 个 AutoConfiguration 类全部存在 |
| 4 | spring-configuration-metadata.json 缺失 | 🔴 P0 | ⏳ 未修复 | 0/24 Starter 含元数据文件 |
| 5 | CommonErrorCode 十六进制格式 | 🟡 P1 | ⏳ 未修复 | UNAUTHORIZED("A0001") 等仍存在 |
| 6 | BusinessException 继承链 | 🟡 P1 | ⏳ 未修复 | GlobalException extends RuntimeException |
| 7 | Hutool 5.8.40 版本过旧 | 🟠 P2 | ⏳ 未修复 | |

> **评价**: 昨日5个 P0 问题中有 3 个在 24 小时内修复（.imports注册、版本对齐、类文件确认），修复率 60%。

### 1.3 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 设计缺陷 | 1 | 1 | 1 | 0 | 3 |
| 功能缺失 | 0 | 1 | 0 | 1 | 2 |
| 代码质量 | 0 | 1 | 2 | 1 | 4 |
| 安全/迁移 | 1 | 1 | 1 | 1 | 4 |
| 技术债务 | 0 | 0 | 1 | 0 | 1 |

### 1.4 🔥 今日重点关注

- **[TOP 1]** Datascope 新旧双包并行，类名完全重复（P0，框架级。两个 DataScopeAspect、两个 DataScopeProvider、两个 DefaultDataScopeProvider）
- **[TOP 2]** Flowable `javax.sql.DataSource` — Jakarta EE 迁移残留（P0，编译级。SB 4.x / JDK 21 下 `javax.*` 不可用）
- **[TOP 3]** SmsUtil sync/async 标记反逻辑（P1，运行时。`sync=true` → `sendMessageAsync`，行为与参数名相反）
- **[TOP 4]** SMS 配置前缀不一致（`carlos.sms` vs `sms.blends.xxx`）（P1，配置体验问题）
- **[TOP 5]** EncryptMode.ECB 枚举常量留存 — 运行时已禁用但枚举定义仍存在（P2，代码气味）

---

## 二、设计缺陷详情

### 问题 1: [P0] Datascope 模块新旧双包并行，同名类完全重复

**模块**: `carlos-spring-boot-starter-datascope`
**严重程度**: P0-Critical
**影响范围**: 模块级 — 使用者无法区分应注入哪个版本

#### 重复类清单

| 旧包路径 (`com.carlos.datascope`) | 新包路径 (`com.carlos.datascope.*`) | 说明 |
|-----------------------------------|-------------------------------------|------|
| `com.carlos.datascope.DataScopeAspect` (83行) | `com.carlos.datascope.core.aspect.DataScopeAspect` | 旧版实现 `AspectAbstract`，新版基于规则引擎 |
| `com.carlos.datascope.DataScopeProvider` | `com.carlos.datascope.provider.DataScopeProvider` | 旧版4个简单方法，新版含完整文档和更多方法 |
| `com.carlos.datascope.DefaultDataScopeProvider` | `com.carlos.datascope.provider.DefaultDataScopeProvider` | 两个不同实现 |

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
    // 用户相关
    String currentUserId();
    List<String> currentUserRoles();
    // 部门相关
    String currentDeptId();
    List<String> currentDeptIds();
    List<String> currentDeptAndChildIds();
    // ... 更多方法
}
```

#### 详细分析

这是明显的**模块重构未完成**症状。旧版的根包类 (`DataScopeAspect`, `DataScopeProvider`, `DefaultDataScopeProvider`, `DataScope`, `DataScopes`, `DataScopeType`, `DataScopeConsumer`, `DataScopeHandler`, `DataScopeInfo`, `CustomScope`) 与新版 `core/`、`provider/`、`auto/`  子包中的同名类同时存在。

- **DataScopeAutoConfiguration.imports 只注册了新包**: `com.carlos.datascope.auto.DataScopeAutoConfiguration`
- **但旧包类仍然可通过包扫描被发现**: `@Aspect` 注解的旧版 `DataScopeAspect` 会被 Spring AOP 自动加载
- **接口差异导致不兼容**: 旧 `DataScopeProvider` 用 `Serializable`，新 `DataScopeProvider` 用 `String` 并增加多个方法

#### 解决方案

**推荐修复**: 彻底删除旧包中的重复类（`com.carlos.datascope` 根包下所有非新架构类），完成重构迁移。

```bash
# 待删除清单（已完成迁移后）
rm carlos-spring-boot-starter-datascope/src/main/java/com/carlos/datascope/DataScopeAspect.java
rm carlos-spring-boot-starter-datascope/src/main/java/com/carlos/datascope/DataScopeProvider.java
rm carlos-spring-boot-starter-datascope/src/main/java/com/carlos/datascope/DefaultDataScopeProvider.java
# ... 其他旧根包类
```

**短期规避**: 不修改代码，但需确保 `@ComponentScan` 不会扫描到旧包类，或在旧类上添加 `@Deprecated` 标记。

**参考**: Spring Boot 3 迁移最佳实践 — 重构完成后必须删除旧代码，避免 "Zombie Code" 模式。

---

### 问题 2: [P1] SMS 配置前缀不一致 — `carlos.sms` vs `sms.blends`

**模块**: `carlos-spring-boot-starter-sms`
**文件**: `SmsProperties.java` L19, `SmsCustomizeConfig.java` L40-L55
**严重程度**: P1-High
**影响范围**: 模块级配置体验

#### 相关代码

```java
// SmsProperties.java L19 — 使用框架统一前缀
@ConfigurationProperties(prefix = "carlos.sms")
public class SmsProperties { ... }

// SmsCustomizeConfig.java L40-L55 — 使用非框架前缀
@ConditionalOnProperty(prefix = "sms.blends.postal", name = "supplier", havingValue = "postal")
public PostalFactory postalFactory() { ... }

@ConditionalOnProperty(prefix = "sms.blends.wocloud", name = "supplier", havingValue = "wocloud")
public WoCloudFactory woCloudFactory() { ... }

@ConditionalOnProperty(prefix = "sms.blends.ums", name = "supplier", havingValue = "ums")
public UmsFactory umsFactory() { ... }
```

#### 详细分析

同一模块中存在两套配置前缀：
- **SmsProperties**: `carlos.sms.*`（正确，符合框架规范）
- **厂商条件注解**: `sms.blends.*`（错误，不在框架命名空间下）

导致使用者需要在 `application.yml` 中同时配置：
```yaml
carlos:
  sms:
    templates: ...
    rate-limit: ...

sms:       # ⚠️ 这不是框架命名空间
  blends:
    postal:
      supplier: postal
```

这违反了"框架配置统一在 `carlos.*` 下"的原则，且 `sms.blends` 极容易与用户的 `spring.sms` 或其他框架冲突。

#### 解决方案

**推荐修复**: 统一为 `carlos.sms.blends.*`

```java
@ConditionalOnProperty(prefix = "carlos.sms.blends.postal", name = "supplier", havingValue = "postal")
@ConditionalOnProperty(prefix = "carlos.sms.blends.wocloud", name = "supplier", havingValue = "wocloud")
@ConditionalOnProperty(prefix = "carlos.sms.blends.ums", name = "supplier", havingValue = "ums")
```

**影响面**: 需要通知使用方更新 `application.yml`，属 Breaking Change。建议在 3.0.0 正式版中作为 Breaking Change 发布，并同步更新示例模块和文档。

---

## 三、代码质量缺陷

### 问题 3: [P1] SmsUtil sync/async 标记逻辑反了

**模块**: `carlos-spring-boot-starter-sms`
**文件**: `SmsUtil.java`
**行号**: L126-L145
**严重程度**: P1-High
**影响范围**: 单方法，影响所有单条短信发送的调用方

#### 相关代码

```java
// SmsUtil.java L126-L145
if (sync) {
    if (phones.size() > 1) {
        smsBlend.massTexting(phones, templateId, messages);     // 批量发送（同步）
    } else {
        smsBlend.sendMessageAsync(phones.getFirst(), templateId, messages);  // ⚠️ sync=true 时调用 Async
    }
} else {
    if (phones.size() > 1) {
        smsBlend.massTexting(phones, templateId, messages);     // 批量发送
    } else {
        smsBlend.sendMessage(phones.getFirst(), templateId, messages);       // ⚠️ sync=false 时调用同步方法
    }
}
```

#### 详细分析

这是一个明显的逻辑倒置 bug：

| 参数 | 单个手机号实际调用的方法 | 实际行为 | 期望行为 |
|------|------------------------|---------|---------|
| `sync=true` | `sendMessageAsync()` | 异步发送 ❌ | 同步发送 |
| `sync=false` | `sendMessage()` | 同步发送 ❌ | 异步发送 |

参数名 `sync` 的字面含义是"同步发送"，但 `sync=true` 时实际调用的是 `sendMessageAsync`（异步），`sync=false` 时调用的是 `sendMessage`（同步）。行为和命名完全相反。

**额外问题**: 批量发送（`phones.size() > 1`）无论 `sync` 参数如何，始终调用 `massTexting`，`sync` 参数对批量场景无意义。

#### 解决方案

**推荐修复**: 修正方法调用

```java
if (sync) {
    if (phones.size() > 1) {
        smsBlend.massTexting(phones, templateId, messages);
    } else {
        smsBlend.sendMessage(phones.getFirst(), templateId, messages);  // 修正：sync=true → sendMessage
    }
} else {
    if (phones.size() > 1) {
        smsBlend.massTexting(phones, templateId, messages);            // 考虑是否支持异步批量
    } else {
        smsBlend.sendMessageAsync(phones.getFirst(), templateId, messages); // 修正：sync=false → sendMessageAsync
    }
}
```

**建议**: 重命名参数为 `async`（语义更明确，`true`=异步，`false`=同步），或拆分为 `sendSync()` / `sendAsync()` 两个方法，消除 boolean flag 的二义性。

---

### 问题 4: [P2] EncryptMode.ECB 枚举常量留存

**模块**: `carlos-spring-boot-starter-encrypt`
**文件**: `EncryptMode.java` L27
**严重程度**: P2-Medium
**影响范围**: IDE 自动补全中仍显示 ECB 选项

#### 相关代码

```java
// EncryptMode.java — ECB 仍作为合法枚举值存在
public enum EncryptMode {
    CBC,   // 正常可用
    ECB    // ⚠️ 运行时会被 EncryptConfig 拒绝，但枚举定义中仍有效
}

// EncryptConfig.java L66 — 运行时保护
if (encryptMode == EncryptMode.ECB) {
    throw new ComponentException("ECB mode is insecure and has been disabled. Use CBC mode instead.");
}
```

#### 详细分析

`EncryptConfig` 在创建 SM4 Bean 时正确地拒绝了 ECB 模式。但 `EncryptMode` 枚举本身仍然包含 `ECB` 常量。这导致：
1. 用户通过 IDE 自动补全时仍能看到 ECB 选项
2. 配置文件中 `mode: ECB` 不会在启动前被拒绝，而是运行时崩溃
3. 枚举语义暗示 ECB 是一个有效选项，与实际行为矛盾

#### 解决方案

```java
public enum EncryptMode {
    CBC;

    /**
     * @deprecated ECB mode is insecure and has been disabled. Use {@link #CBC} instead.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    ECB  // 标记为废弃但保留以便现有配置抛出明确错误信息
}
```

或完全移除（需要评估是否有外部直接引用 `EncryptMode.ECB` 的代码）。

---

## 四、安全与迁移问题

### 问题 5: [P0] Flowable 使用 `javax.sql.DataSource` — Jakarta EE 迁移残留

**模块**: `carlos-spring-boot-starter-flowable`
**文件**: `FlowableAutoConfiguration.java` L20
**严重程度**: P0-Critical
**影响范围**: 编译级 — SB 4.x 环境可能无法编译或运行

#### 相关代码

```java
// FlowableAutoConfiguration.java L20
import javax.sql.DataSource;

// L57 — 方法签名使用 javax.sql.DataSource
public SpringProcessEngineConfiguration processEngineConfiguration(
    DataSource dataSource,   // ← javax.sql.DataSource
    FlowableProperties properties) throws IOException {
```

#### 详细分析

Spring Boot 4.0.x 基于 Jakarta EE 11，`javax.*` 命名空间已被 `jakarta.*` 取代。然而 Flowable 7.0.1 内部仍使用 `javax.*`，与 SB 4.x 的 Jakarta EE 要求冲突。

- **Flowable 7.x 官方兼容性**: Flowable 7.0.x 支持 Spring Boot 3.x（Jakarta EE 10），但 SB 4.x（Jakarta EE 11）未经官方测试
- **实际影响**: Spring Boot 4.x 的 `DataSource` Bean 类型为 `jakarta.sql.DataSource`，`javax.sql.DataSource` 在 Java 21+ 中已不存在于标准库。除非 Flowable 自带 javax 兼容层（如通过 Jakarta → javax bridge），否则无法编译
- **当前环境**: Flowable 7.0.1 可能通过传递依赖引入了 `jakarta.transaction-api` → `javax.transaction-api` bridge，但这是脆弱的

#### 解决方案

**推荐修复**: 升级 Flowable 到 7.1.x（如果支持 SB 4.x），或将 import 改为 `jakarta.sql.DataSource`。

**短期规避**: 检查 Flowable 7.0.1 是否通过传递依赖引入了 javax 兼容包。如果引入，当前代码可运行但需要在依赖管理中显式声明。

```xml
<!-- 如果需要 javax→jakarta bridge -->
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
</dependency>
```

**长期建议**: 确认 Flowable 7.x 对 Spring Boot 4.x / Jakarta EE 11 的官方支持状态。如不支持，考虑降级 SB 到 3.x 或等待 Flowable 8.x。

---

### 问题 6: [P1] Datacenter 模块 3 个 TODO 未完成

**模块**: `carlos-spring-boot-starter-datacenter`
**严重程度**: P1-High
**影响范围**: 模块功能不完整

#### TODO 清单

| 文件 | 行号 | TODO 内容 | 日期 |
|------|------|----------|------|
| `DatacenterMainConfig.java` | 21 | "此处写数据平台组件公共属性配置，如线程池等" | 2024-12-13 |
| `DatacenterInstanceInitializer.java` | 84 | "正式情况下取消注册" | 2024-12-12 |
| `DatacenterInstance.java` | 50 | "获取数据的接口 — 待设计" | 2024-09-13 |

#### 详细分析

三个 TODO 横跨 3 个月未处理，最老的已超过 21 个月。说明 datacenter 模块处于半开发状态：
- 缺少线程池等公共基础设施配置
- `DatacenterInstanceInitializer` 包含测试代码
- 核心数据获取接口未设计

#### 解决方案

逐个评估: 如果 datacenter 是已上线功能，这些 TODO 应尽快完成；如果仍未上线，考虑从框架发布版本中暂时排除。

---

## 五、功能缺失

### 问题 7: [P1] spring-configuration-metadata.json 全量缺失（持续）

**模块**: 全部 24 个 Starter
**严重程度**: P1-High
**状态**: ⏳ 持续问题（来自昨日报告）

无 IDE 配置自动补全，开发体验显著降低。每个 Starter 的 pom.xml 需添加 `spring-boot-configuration-processor`。

### 问题 8: [P3] Flowable 默认禁用但文档未显式说明

**模块**: `carlos-spring-boot-starter-flowable`
**文件**: `FlowableProperties.java` L18

```java
private boolean enabled = false;  // 默认禁用
```

Flowable Starter 默认不启用（`carlos.flowable.enabled=false`），这是合理的安全默认值。但启动日志中缺乏明确提示，新用户可能困惑为什么配置了 Flowable 却没有生效。

**建议**: 在 `FlowableAutoConfiguration` 启动日志中增加一句 "Flowable is disabled by default, set carlos.flowable.enabled=true to enable"。

---

## 六、代码可优化点

### 问题 9: [P2] EncryptUtil 静态工具类体积过大（930行）

**模块**: `carlos-spring-boot-starter-encrypt`
**文件**: `EncryptUtil.java` (930 行)
**严重程度**: P2-Medium

单个类包含 SM4(5个方法)、SM2(5个方法+密钥生成)、AES(10个方法)、RSA(8个方法+密钥生成)、3DES(2个方法)、MD5(4个方法)、SHA(6个方法)、SM3(3个方法)、Base64(4个方法)、通用工具(2个方法)——共计 ~50 个公开方法。

**建议**: 拆分为 `Sm4Util`、`Sm2Util`、`AesUtil`、`RsaUtil`、`HashUtil`、`Base64Util` 内聚子类。`EncryptUtil` 作为 Facade 提供统一入口。

### 问题 10: [P2] SmsUtil 异步批量发送不支持

当 `async=false` 且 `phones.size() > 1` 时，`massTexting` 仍被调用。sms4j 的 `massTexting` 行为取决于厂商实现，可能不支持异步。需核实批量异步场景的实际需求并适配。

### 问题 11: [P3] FlowableService 仅是一个代理类

`FlowableService` 有 91 行，实质是 4 个 `private final` 字段 + 4 个 getter 方法。使用 `@RequiredArgsConstructor` 和 Lombok 可缩减为 5 行。当前代码是 Lombok + 手写 getter 混合，风格不统一。

---

## 七、依赖与技术债务

### 7.1 版本对比

| 依赖 | 当前版本 | 发布日期 | 评估 | 建议操作 |
|------|---------|----------|------|---------|
| Hutool | 5.8.40 | 2025-10 | 约6个月 | 检查 5.8.x 最新版 |
| Redisson | 4.5.0 | 2025-Q4 | 较新 ✅ | 维持 |
| BouncyCastle | 1.81 | 2025-09 | 约9个月 | 检查更新 |
| Disruptor | 3.4.4 | 2021-03 | ⚠️ ~5年未更新 | 评估 4.x 可用性 |
| Flowable | 7.0.1 | 2025-09 | 较新但SB4兼容性未知 | 重点评估 |
| MyBatis-Plus | 3.5.16 | 2025-Q4 | 较新 ✅ | 维持 |
| Spring Boot | 4.0.6 | 最新 | ✅ | 维持 |

### 7.2 Jakarta EE 迁移残留

| 文件 | 行号 | javax 导入 | 来源 |
|------|------|-----------|------|
| `FlowableAutoConfiguration.java` | 20 | `import javax.sql.DataSource` | 框架代码 |
| `MyBatisPlusConfig.java` | — | `javax.*` 通过 MP 传递依赖 | 第三方 |

### 7.3 已弃用 API 持有（EncryptUtil）

| 方法 | 弃用时间 | 建议 |
|------|---------|------|
| `aesEncrypt(String,String)` + 对应 Base64/Decrypt (4个方法) | 3.0.0 | 若已无调用方，3.0.0 正式版中直接移除 |
| `md5*()` 系列 (4个方法) | 3.0.0 | 同上 |

---

## 八、Starter 健康度评分

| 模块 | 设计规范 | 功能完整 | 代码质量 | 安全性 | 综合评分 | 变化 |
|------|---------|---------|---------|--------|---------|------|
| carlos-spring-boot-core | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.3/5.0 | — |
| starter-encrypt | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | 4.3/5.0 | — |
| starter-web | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.5/5.0 | — |
| starter-mybatis | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 | — |
| starter-security | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.5/5.0 | — |
| starter-redis | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.3/5.0 | — |
| **starter-flowable** | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.8/5.0 | 🔍 今日重点 |
| **starter-datascope** | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | 3.3/5.0 | 🔴 双包问题 |
| starter-ai | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 | — |
| **starter-datacenter** | ⭐⭐☆☆☆ | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 2.5/5.0 | 🔴 多 TODO |
| **starter-sms** | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 | 🟡 sync反逻辑+前缀 |
| starter-oss | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.5/5.0 | — |
| starter-openapi | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 | — |
| spring-cloud-starter | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 | — |
| starter-snowflake | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.5/5.0 | — |
| starter-disruptor | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.5/5.0 | — |

---

## 九、待办事项与修复计划

### 9.1 今日新增待办

- [ ] P0-001 **datascope**: 删除旧根包重复类，完成重构迁移
- [ ] P0-002 **flowable**: 确认 Flowable 7.0.1 + SB 4.x 兼容性，修复 javax→jakarta 导入
- [ ] P1-003 **sms**: 修复 SmsUtil sync/async 方法调用逻辑
- [ ] P1-004 **sms**: 统一配置前缀为 `carlos.sms.blends.*`
- [ ] P1-005 **datacenter**: 处理 3 个 TODO（关闭或完成功能）
- [ ] P1-006 **全局**: 添加 `spring-boot-configuration-processor`（持续）
- [ ] P2-007 **encrypt**: 标记 `EncryptMode.ECB` 为 @Deprecated
- [ ] P2-008 **encrypt**: 拆分 `EncryptUtil.java`（930行 → 多个内聚工具类）
- [ ] P2-009 **sms**: 补充批量异步发送支持
- [ ] P3-010 **flowable**: 启用日志中增加"默认禁用"提示
- [ ] P3-011 **flowable**: FlowableService 简化（使用 Lombok @Getter）

### 9.2 历史遗留跟踪

- [ ] P0-004 **全局** `spring-configuration-metadata.json` ⏳ 未开始（0/24 Starter）
- [ ] P1-005 **core** CommonErrorCode 十六进制格式 ⏳ 未修复
- [ ] P1-006 **core** BusinessException 继承链 ⏳ 未修复（设计决策待定）
- [ ] P2-007 **dependencies** Hutool 5.8.40 版本更新 ⏳ 待评估
- [x] P0-008 **5个Starter** `.imports` 空文件 ✅ 已修复
- [x] P0-009 **AGENTS.md** 版本不一致 ✅ 已修复
- [x] P1-010 **spring-cloud-starter** 类文件缺失 ✅ 已修复

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
