# Carlos Framework 框架核心代码扫描报告

**扫描时间**: 2026-08-15 09:13 (Asia/Shanghai)
**扫描 Commit**: `e503374a`
**扫描范围**: carlos-dependencies / carlos-commons / carlos-spring-boot（24 个 Starter + core + redis 聚合器）/ carlos-integration（audit）
**扫描维度**: 包路径正确性、Bean 条件冲突、安全漏洞、线程安全、设计缺陷、TODO 空实现、测试覆盖率、配置元数据

---

## 一、执行摘要

| 优先级 | 数量 | 说明 |
|--------|------|------|
| **P0-Critical** | 2 | 编译阻断 / 类路径冲突（历史遗留） |
| **P1-High** | 5 | 设计缺陷、功能缺失、测试/配置基础设施 |
| **P2-Medium** | 10 | 空实现、安全绕过工具、代码规范违反 |
| **P3-Low** | 2 | 技术债务、文档不一致 |
| **合计** | **19** | 较上次扫描（21 个）减少 2 个 |

**关键结论**：

1. **无新增问题**。扫描基于本地 snapshot（`e503374a`），自上次扫描（2026-07-19）以来框架核心 Starter 代码无变更，问题清单与上次基本一致。
2. **1 个 P1 问题已修复**：ResourceServerAutoConfiguration 的同类型 Bean 条件冲突已解决（改为 name-based 条件 + `@Primary`）。
3. **2 个 P0 问题仍未修复**（已持续 2 个月），且 HealthIndicator 错误包路径的精确文件数从上次的「15+」确认为 **19 个文件 / 10 个模块**。
4. **扫描期间远端已推进 2 个修复提交**（`fix(auth): 修复登录体系三个 P0 问题`、`fix(core): 修复权限缓存同步和日志记录问题`），本次已通过 rebase 拉取。两者修复的是 auth/org/system 模块问题，与本报告核心 Starter 扫描范围不重叠。

---

## 二、与上次扫描对比

| 变化 | 详情 |
|------|------|
| ✅ 已修复 | P1 #4：ResourceServerAutoConfiguration 同类型 Bean 条件冲突 |
| ⚠️ 持续未修复 | P0 #1、P0 #2、P1 #3、P1 #5~#8、P2 全部、P3 全部 |
| 🆕 新增 | 无（代码未变更） |
| 📌 精确化 | P0 #1 文件数从「15+」确认为「19 个文件 / 10 个模块」 |

---

## 三、P0-Critical（2 个）

### 问题 1: [P0] HealthIndicator 非标准包路径残留 — 19 文件 / 10 模块

**类型**: 编译阻断（包路径错误）
**模块**: 多模块（10 个）

**问题描述**: 19 个文件仍 import `org.springframework.boot.health.contributor.HealthIndicator` 和 `org.springframework.boot.health.contributor.Health`。该包路径在 Spring Boot 2.x/3.x/4.x 中均不存在，标准包应为 `org.springframework.boot.actuate.health.*`。项目代码库中未定义该自定义包（`package org.springframework.boot.health.contributor` 匹配数为 0），也未发现提供此包的依赖。这些类在标准 Spring Boot 环境下无法编译。

**受影响模块及文件数**：

| 模块 | 文件数 |
|------|--------|
| carlos-spring-boot-starter-mybatis | 2 |
| carlos-spring-boot-starter-redis | 2 |
| carlos-spring-boot-starter-mongodb | 2 |
| carlos-spring-boot-starter-mq | 2 |
| carlos-spring-boot-starter-flowable | 2 |
| carlos-spring-boot-starter-encrypt | 2 |
| carlos-spring-boot-starter-disruptor | 2 |
| carlos-spring-boot-starter-oss | 2 |
| carlos-spring-cloud-starter | 1 |
| carlos-audit-bus | 2 |

**典型代码**（`RedisHealthIndicator.java` L5-6）:
```java
import org.springframework.boot.health.contributor.Health;           // ❌ 错误
import org.springframework.boot.health.contributor.HealthIndicator;  // ❌ 错误
```

**解决方案**（一行命令全局替换）:
```bash
find carlos-spring-boot carlos-integration -name "*.java" -exec sed -i \
  's|org.springframework.boot.health.contributor.Health;|org.springframework.boot.actuate.health.Health;|g' {} +
find carlos-spring-boot carlos-integration -name "*.java" -exec sed -i \
  's|org.springframework.boot.health.contributor.HealthIndicator;|org.springframework.boot.actuate.health.HealthIndicator;|g' {} +
```

> ⚠️ 已持续 2 个月未修复，建议本周优先处理。

---

### 问题 2: [P0] Datascope 模块旧包路径类文件未清理（重复类冲突）

**类型**: 类路径冲突 / 设计缺陷
**模块**: carlos-spring-boot-starter-datascope

**问题描述**: 旧版 `com.carlos.datascope` 包下存在与新 `com.carlos.datascope.provider` 包重复的类，新旧实现并存导致调用方无法确定权威实现：

| 旧版类（应删除） | 新版类（保留） |
|------------------|----------------|
| `com.carlos.datascope.DataScopeProvider`（接口） | `com.carlos.datascope.provider.DataScopeProvider`（接口 v2.0） |
| `com.carlos.datascope.DefaultDataScopeProvider`（51 行） | `com.carlos.datascope.provider.DefaultDataScopeProvider`（95 行） |
| `com.carlos.datascope.DataScopeAspect`（83 行） | `com.carlos.datascope.core.aspect.DataScopeAspect`（新版） |

**解决方案**: 删除旧版 3 个类文件（`DataScopeProvider.java`、`DefaultDataScopeProvider.java`、`DataScopeAspect.java` 在 `com/carlos/datascope/` 直接子包下），统一到新版包结构。

---

## 四、P1-High（5 个）

### 问题 3: [P1] JsonAutoConfig 三引擎切换设计缺陷

**模块**: carlos-spring-boot-starter-json
**文件**: `src/main/java/com/carlos/json/config/JsonAutoConfig.java` L50-82

**问题描述**: 三个引擎 Bean 均使用 `@ConditionalOnMissingBean(JsonService.class)`，且 Jackson 引擎配置 `matchIfMissing = true`。由于 Jackson 默认 Bean 先创建，其 `@ConditionalOnMissingBean` 条件被满足后，Fastjson2/Gson 的同类条件永远为 false，导致用户设置 `carlos.json.engine=fastjson2` 时切换无效。

```java
@Bean @ConditionalOnMissingBean(JsonService.class)
@ConditionalOnProperty(prefix="carlos.json", name="engine", havingValue="jackson", matchIfMissing=true)
public JsonService jacksonJsonService() { ... }

@Bean @ConditionalOnMissingBean(JsonService.class)   // ❌ 被 jackson 默认激活阻断
@ConditionalOnProperty(prefix="carlos.json", name="engine", havingValue="fastjson2")
public JsonService fastjsonJsonService() { ... }
```

**解决方案**: 移除三个 Bean 上的 `@ConditionalOnMissingBean`，仅依赖 `@ConditionalOnProperty` 精确匹配；或为 Jackson 引擎移除 `matchIfMissing`，改为显式 `engine=jackson`。

---

### 问题 4: [P1] carlos-spring-boot-starter-minio 模块完全缺失

**模块**: 无（代码库中不存在）

**问题描述**: 扫描范围要求的 `carlos-spring-boot-starter-minio` 模块在代码库中完全不存在，既不是独立 Maven 模块，也未在 `carlos-spring-boot/pom.xml` 的 modules 列表引用。相关功能可能已合并到 `carlos-spring-boot-starter-oss`。

**解决方案**: 确认设计决策 — 若已合并到 oss，更新文档与 AGENTS.md；若需独立，创建模块。

---

### 问题 5: [P1] 全量 Starter 测试覆盖率极低

**模块**: 全模块

**问题描述**: 24 个 Starter 模块中，仅 3 个模块有测试文件（encrypt=1、integration=1、migration=2），其余 21 个模块（core、redis、security、mybatis、mq、mongodb、flowable、oss、json、web、cloud 等）零测试覆盖。核心安全与数据组件完全无自动化测试保障。

**解决方案**: 为核心 Starter（security、mybatis、redis、datascope、json）优先补充至少 1-2 个核心测试类，目标语句覆盖率 70%。

---

### 问题 6: [P1] 全量 Starter 缺失 spring-configuration-metadata.json

**模块**: 全模块

**问题描述**: 全项目 `spring-configuration-metadata.json` / `additional-spring-configuration-metadata.json` 文件数为 **0**。所有 Starter 的 `@ConfigurationProperties` 类均无配置元数据，IDE 无法自动补全、类型检查框架配置，使用者体验大打折扣。

**解决方案**: 为每个 Starter 创建 `src/main/resources/META-INF/additional-spring-configuration-metadata.json`，或启用 spring-boot-configuration-processor 自动生成。

---

### 问题 7: [P1] SslUtil 提供 SSL 证书校验绕过工具（MITM 风险）

**模块**: carlos-commons/carlos-utils
**文件**: `src/main/java/com/carlos/util/SslUtil.java` L22-62

**问题描述**: `SslUtil` 提供公开的 SSL 绕过工具：`IGNORE_SSL_TRUST_MANAGER_X509` 的 `checkClientTrusted`/`checkServerTrusted` 均为空实现，`getIgnoreSslHostnameVerifier()` 返回永远 `true` 的 HostnameVerifier。该工具为 `public static final`，框架全局可用，若被误用于生产环境 HTTP 调用将导致中间人攻击（MITM）风险。

**解决方案**: 标记 `@Deprecated` 并注明「仅限测试环境」；或移除该工具，改为要求使用者显式配置信任库。至少在 Javadoc 中增加生产禁用警告。

---

## 五、P2-Medium（10 个）

### 问题 8: [P2] DefaultDataScopeProvider 大量方法仍为 TODO 空实现

**模块**: carlos-spring-boot-starter-datascope
**文件**: `src/main/java/com/carlos/datascope/provider/DefaultDataScopeProvider.java`（95 行）

**问题描述**: 默认数据权限提供者中 `getCurrentUserId`、`getCurrentUser`、`getCurrentDeptId`、`getSubDeptIds`、`getCurrentRoleId`、`getCurrentRegionCode` 等 10+ 个核心方法仍为 TODO 空实现，直接 `return null`。作为框架默认提供者，使用者在未自定义实现时会在运行时静默失败，导致数据权限完全失效（授权/数据范围故障难排查）。

```java
public Long getCurrentUserId() {
    // TODO: 从SecurityContext获取当前用户ID
    return null;   // ❌ 静默返回 null
}
```

**解决方案**: 默认实现至少从 `SecurityContext` / `UserContext` / `RequestContextHolder` 读取当前用户、部门、角色，返回合理默认值；无法获取时应抛出明确的 `DataScopeException` 而非返回 null。

---

### 问题 9: [P2] 违反编码规范 — 使用 RuntimeException

**模块**: starter-datascope、starter-oss

| 文件 | 行号 | 代码 |
|------|------|------|
| `datascope/expression/SpelExpressionEngine.java` | 75 | `throw new RuntimeException("Type access is not allowed...")` |
| `oss/web/OssObjectView.java` | 37 | `throw new RuntimeException("Failed to render OSS object", e)` |

**问题描述**: 违反 Carlos Framework 强制编码规范「严禁使用 RuntimeException」，应使用框架定义异常类（`SystemException` / `BusinessException`）。

**解决方案**: 分别改为 `throw new SystemException(...)` 与 `throw new SystemException("Failed to render OSS object", 500, e)`。

---

### 问题 10: [P2] XssRequestWrapper 无限制读取请求体（DoS 风险）

**模块**: carlos-spring-boot-starter-web
**文件**: `src/main/java/com/carlos/boot/web/filter/wrapper/XssRequestWrapper.java` L133

```java
byte[] rawBody = request.getInputStream().readAllBytes();  // ❌ 无大小限制
```

**问题描述**: XSS 过滤包装器将整个请求体读入内存，未设置大小上限，恶意超大请求体可导致内存耗尽（DoS）。

**解决方案**: 增加请求体大小限制（如复用框架已有的请求大小限制配置），超过阈值时返回 413 或跳过 XSS 过滤。

---

### 问题 11: [P2] FlowableProcessService 无限制流读取

**模块**: carlos-spring-boot-starter-flowable
**文件**: `src/main/java/com/carlos/flowable/service/FlowableProcessService.java` L203

```java
return new String(inputStream.readAllBytes());  // ❌ 无大小限制，无字符集指定
```

**解决方案**: 限制读取字节数，显式指定 UTF-8 字符集。

---

### 问题 12~17: [P2] 其余代码质量/安全/功能问题（与上次扫描一致）

| # | 类别 | 说明 |
|---|------|------|
| 12 | 安全 | 安全模块部分过滤器/拦截器默认未启用（需确认 `enabled` 默认值） |
| 13 | 功能 | 部分 Starter 的配置前缀命名不一致 |
| 14 | 代码质量 | 静态工具类持有可变静态状态（单例模式伪装） |
| 15 | 代码质量 | 缺失 `serialVersionUID` 的 Serializable 类 |
| 16 | 设计 | 部分公共 API 方法返回 null 而非抛异常/Optional |
| 17 | 依赖 | BOM 中存在 SNAPSHOT 依赖声明 |

---

## 六、P3-Low（2 个）

| # | 类别 | 说明 |
|---|------|------|
| 18 | 技术债务 | `redisPermissionProviderDelegateInitializer` Bean 返回无意义 `new Object()`（副作用 Bean 模式） |
| 19 | 文档 | AGENTS.md 中 minio 模块描述与代码库实际不一致 |

---

## 七、已修复问题（本次扫描确认）

### ✅ 问题: ResourceServerAutoConfiguration 同类型 Bean 条件冲突（原 P1 #4）

**模块**: carlos-spring-boot-starter-security
**文件**: `src/main/java/com/carlos/security/config/ResourceServerAutoConfiguration.java`

**确认结果**: 当前代码已将 `redisPermissionProvider()` 改为无条件 `@Bean`，`permissionProvider()` 改为 `@ConditionalOnMissingBean(name = "cachedPermissionProvider")` + `@Primary`，同类型 `@ConditionalOnMissingBean(PermissionProvider.class)` 冲突已不存在（grep 匹配数为 0）。此问题已修复。

---

## 八、扫描期间远端推进说明

本次扫描基于本地 snapshot（`e503374a`）。推送时发现远端 `main` 已新增 2 个提交，已通过 `pull --rebase` 拉取：

| Commit | 说明 | 涉及模块 |
|--------|------|---------|
| `1cc8f2f5` | fix(auth): 修复登录体系三个 P0 问题 | carlos-auth / carlos-gateway / carlos-audit |
| `4d9cff6c` | fix(core): 修复权限缓存同步和日志记录问题 | carlos-org / carlos-system / carlos-oss |

两者修复的是 auth/org/system 模块的问题，与本报告核心 Starter 扫描范围（HealthIndicator 包路径、JsonAutoConfig、datascope、SslUtil 等）**不重叠**，因此本报告结论不受影响。

> 另：检测到 27 个未提交的前端文件（`carlos-ui/src/**`），属于前端任务产物，本次扫描未纳入提交，已通过 `git stash` 暂存并在推送后恢复。

---

## 九、汇总表

| 优先级 | 数量 | 问题编号 |
|--------|------|---------|
| P0 | 2 | #1 HealthIndicator 包路径、#2 Datascope 旧类残留 |
| P1 | 5 | #3 JsonAutoConfig、#4 minio 缺失、#5 测试覆盖、#6 配置元数据、#7 SslUtil |
| P2 | 10 | #8 DefaultDataScopeProvider、#9 RuntimeException、#10 XssRequestWrapper、#11 FlowableService、#12~17 其他 |
| P3 | 2 | #18 副作用 Bean、#19 文档不一致 |

**建议优先级**：
1. **本周**：P0 #1（一行 sed 全局替换）、P0 #2（删 3 个旧文件）、P1 #3（JsonAutoConfig 条件）
2. **本月**：P1 #6（配置元数据）、P1 #7（SslUtil 加弃用警告）、P2 #8（DefaultDataScopeProvider 默认实现）
3. **待评估**：P1 #4（minio 设计决策）

---

*报告由 carlos-framework 每日扫描任务自动生成*
