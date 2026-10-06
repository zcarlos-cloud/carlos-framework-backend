# Carlos Framework 业务模块质量报告

**生成时间**: 2026-05-30 08:45
**扫描 Commit**: 03405cf9ba20145df7ff347c16bcdaf9100e63b7
**扫描范围**: 业务集成层（gateway / tools / audit / auth / license / message / org / system）
**发现问题总数**: 15 个（P0: 0 个, P1: 6 个, P2: 7 个, P3: 2 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 无需拉取（基于最新 Commit 03405cf9）
- Commit: 03405cf9 `docs: 框架核心代码扫描报告 2026-05-30`
- 变更文件数: 1 个（报告文件）
- 新增代码: +489 行 / 删除代码: 0 行

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 0 | 2 | 2 |
| 需求与设计缺陷 | 0 | 1 | 0 | 0 | 1 |
| 代码规范违规 | 0 | 1 | 2 | 0 | 3 |
| 性能风险 | 0 | 0 | 2 | 0 | 2 |
| 安全漏洞 | 0 | 2 | 1 | 0 | 3 |
| 功能未完善 | 0 | 2 | 2 | 0 | 4 |
| **合计** | **0** | **6** | **7** | **2** | **15** |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 0 | 3 | 0 | 0 | 3 |
| carlos-gateway | 0 | 0 | 1 | 0 | 1 |
| carlos-audit | 0 | 1 | 2 | 0 | 3 |
| carlos-message | 0 | 1 | 2 | 0 | 3 |
| carlos-org | 0 | 1 | 0 | 0 | 1 |
| carlos-system | 0 | 0 | 2 | 0 | 2 |
| carlos-license | 0 | 0 | 0 | 1 | 1 |
| carlos-tools | 0 | 0 | 0 | 1 | 1 |

### 1.4 今日重点关注
- [TOP 1] **P1-001** `carlos-auth-service` 默认启用 Refresh Token 重用（`reuseRefreshTokens = true`），增加 Token 被盗用风险
- [TOP 2] **P1-002** 所有 `-cloud` 模块违反依赖规范，直接依赖 `-bus`/`service` 而非 `-api`（auth-cloud → auth-service, audit-cloud → audit-bus, message-cloud → message-bus, org-cloud → org-bus, system-cloud → system-bus）
- [TOP 3] **P1-003** `carlos-auth-cloud` 直接依赖 `carlos-auth-service`，引入授权服务器核心代码到网关侧，存在严重依赖方向违规

---

## 二、安全漏洞详情

### 问题 P1-001: [carlos-auth-service] Refresh Token 默认重用，增加 Token 被盗用风险

**模块**: `carlos-auth-service`
**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/app/pojo/dto/Oauth2TokenSettings.java`
**行号**: L22
**严重程度**: P1-High
**影响范围**: 模块级（所有使用 OAuth2 Token 配置的客户端）

#### 相关代码

```java
// carlos-auth-service/src/main/java/com/carlos/auth/app/pojo/dto/Oauth2TokenSettings.java L22
@Data
public class Oauth2TokenSettings implements Serializable {

    /** 单位 分钟 */
    private Long authorizationCodeTimeToLive = 5L;

    /** 单位 分钟 */
    private Long accessTokenTimeToLive = 5L;

    /** 默认 @see OAuth2TokenFormat.SELF_CONTAINED */
    private String accessTokenFormat = OAuth2TokenFormat.SELF_CONTAINED.getValue();

    /** 默认true */
    private Boolean reuseRefreshTokens = true;  // ⚠️ 默认启用重用

    /** 单位分钟 */
    private Long refreshTokenTimeToLive = 60L;
}
```

#### 详细分析

- **问题描述**: `Oauth2TokenSettings` 中 `reuseRefreshTokens` 默认值为 `true`，这意味着在客户端未显式配置的情况下，Refresh Token 会被重用。当 Refresh Token 被盗用时，攻击者可以持续获取新的 Access Token，直到 Token 过期或手动撤销。
- **违反规范**: OAuth2 最佳实践建议每次刷新都生成新的 Refresh Token（轮换），旧的 Refresh Token 应立即失效。RFC 6819 第 5.2.2.3 节明确建议实施 Refresh Token 轮换。
- **触发条件**: 任何使用默认 Token 配置的客户端
- **潜在风险**: Token 被盗用后无法通过刷新来撤销，攻击者可长期保持访问权限

#### 解决方案

**推荐修复**:
```java
@Data
public class Oauth2TokenSettings implements Serializable {
    // ... 其他字段
    
    /** 默认false，每次刷新都生成新的 Refresh Token（RFC 6819 建议） */
    private Boolean reuseRefreshTokens = false;
    
    // ... 其他字段
}
```

**短期规避**: 在文档中强制要求所有客户端显式配置 `reuseRefreshTokens = false`

**长期建议**: 实现 Refresh Token 轮换机制，每次刷新后旧的 Refresh Token 加入黑名单

**参考文档**: [RFC 6819 - OAuth 2.0 Threat Model](https://tools.ietf.org/html/rfc6819#section-5.2.2.3), [OAuth2 Token Rotation Best Practices](https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation)

---

### 问题 P1-002: [carlos-auth-cloud] 直接依赖 carlos-auth-service，违反模块依赖规范

**模块**: `carlos-auth-cloud`
**文件**: `carlos-auth-cloud/pom.xml`
**行号**: L26-L30
**严重程度**: P1-High
**影响范围**: 模块级（依赖方向错误）

#### 相关代码

```xml
<!-- carlos-auth-cloud/pom.xml L26-L30 -->
<dependencies>
    <dependency>
        <groupId>com.carlos.auth</groupId>
        <artifactId>carlos-auth-service</artifactId>
    </dependency>
    <!-- ... -->
</dependencies>
```

#### 详细分析

- **问题描述**: `carlos-auth-cloud` 作为 Feign 接口的实现层（REST 端点），应当只依赖 `carlos-auth-api`（接口定义）。直接依赖 `carlos-auth-service`（授权服务器核心）会导致业务服务引入授权服务器的完整依赖树，严重破坏模块边界。
- **违反规范**: 模块依赖规范要求 `-cloud` 只依赖 `-api`，`-api` 不依赖 `-bus`
- **触发条件**: 任何引入 `carlos-auth-cloud` 的业务服务
- **潜在风险**: 依赖冲突、授权服务器核心代码泄露到业务服务、循环依赖风险

#### 解决方案

**推荐修复**: 将 `carlos-auth-cloud` 的依赖改为只依赖 `carlos-auth-api`，将需要在 `-cloud` 中使用的 `auth-service` 代码提取到 `-api` 或新建共享模块中。

```xml
<dependencies>
    <dependency>
        <groupId>com.carlos.auth</groupId>
        <artifactId>carlos-auth-api</artifactId>
    </dependency>
    <!-- ... -->
</dependencies>
```

---

## 三、需求与设计缺陷详情

### 问题 P1-003: [全部 -cloud 模块] 模块依赖方向普遍违规

**模块**: `carlos-audit-cloud`, `carlos-message-cloud`, `carlos-org-cloud`, `carlos-system-cloud`
**严重程度**: P1-High
**影响范围**: 多模块联动

#### 详细分析

- **问题描述**: 所有业务模块的 `-cloud` 层均直接依赖了 `-bus` 层，而非 `-api` 层。这违反了模块分层设计原则：`-cloud` 应仅依赖 `-api`，通过 `-api` 的接口定义对外提供服务，不应直接引入 `-bus` 的业务实现。
- **违规清单**:

| 模块 | -cloud 依赖 | 规范要求 | 建议操作 |
|----|------|------|------|
| carlos-auth | `carlos-auth-service` | `carlos-auth-api` | 提取共享接口到 `-api` |
| carlos-audit | `carlos-audit-bus` | `carlos-audit-api` | 同上 |
| carlos-message | `carlos-message-bus` | `carlos-message-api` | 同上 |
| carlos-org | `carlos-org-bus` | `carlos-org-api` | 同上 |
| carlos-system | `carlos-system-bus` | `carlos-system-api` | 同上 |

- **潜在风险**: 模块边界模糊、循环依赖、测试困难、服务拆分受阻

#### 解决方案

**推荐修复**: 逐步重构 `-cloud` 模块，将业务逻辑下沉到 `-bus` 并通过 `-api` 暴露接口。`-cloud` 仅负责 REST 端点封装和 Feign 调用转发。

---

## 四、代码规范违规详情

### 问题 P2-001: [carlos-system-api] ApiDict Feign 客户端 path 缺少前导斜杠

**模块**: `carlos-system-api`
**文件**: `carlos-system-api/src/main/java/com/carlos/system/api/ApiDict.java`
**行号**: L23
**严重程度**: P2-Medium
**影响范围**: 类级

#### 相关代码

```java
// carlos-system-api/src/main/java/com/carlos/system/api/ApiDict.java L23
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "dict", 
    path = "api/sys/dict",  // ⚠️ 缺少前导 /
    fallbackFactory = FeignDictFallbackFactory.class)
```

#### 详细分析

- **问题描述**: Feign 客户端的 `path` 属性缺少前导 `/`，虽然 Spring Cloud OpenFeign 通常会自动处理，但在某些网关路由配置或自定义负载均衡策略下可能导致路径拼接异常。
- **触发条件**: 特定网关配置或自定义路由规则下
- **潜在风险**: 请求路径错误，导致 404 或服务不可达

#### 解决方案

```java
@FeignClient(value = ServiceNameConstant.SYSTEM, contextId = "dict", 
    path = "/api/sys/dict",  // 添加前导 /
    fallbackFactory = FeignDictFallbackFactory.class)
```

---

### 问题 P2-002: [carlos-message-bus/carlos-audit-bus] BeanUtils.copyProperties 滥用

**模块**: `carlos-message-bus`, `carlos-audit-bus`
**文件**: 
- `carlos-message-bus/src/main/java/com/carlos/message/apiimpl/MessageSendApiImpl.java`
- `carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java`
**严重程度**: P2-Medium
**影响范围**: 方法级

#### 相关代码

```java
// MessageSendApiImpl.java L46
BeanUtils.copyProperties(param, createParam);

// ClickHouseBatchWriter.java L254
BeanUtils.copyProperties(log, row);
```

#### 详细分析

- **问题描述**: 框架编码规范要求使用 MapStruct 进行对象转换，但 `MessageSendApiImpl` 和 `ClickHouseBatchWriter` 中使用了 `Spring BeanUtils.copyProperties`。`BeanUtils.copyProperties` 是反射实现，性能差且无法编译期检查字段映射。
- **违反规范**: 框架编码规范强制使用 MapStruct
- **触发条件**: 任何对象转换操作
- **潜在风险**: 性能下降、字段映射错误（编译期无法发现）、类型安全问题

#### 解决方案

**推荐修复**: 使用 MapStruct 生成转换器，替换所有 `BeanUtils.copyProperties` 调用。

```java
// 使用 MapStruct
MessageCreateParam createParam = MessageConvert.INSTANCE.toCreateParam(param);
```

---

## 五、性能风险详情

### 问题 P2-003: [carlos-system-bus] SysRegionManagerImpl 缓存未考虑失效场景

**模块**: `carlos-system-bus`
**文件**: `carlos-system-bus/src/main/java/com/carlos/system/region/manager/impl/SysRegionManagerImpl.java`
**行号**: L417
**严重程度**: P2-Medium
**影响范围**: 方法级

#### 相关代码

```java
// SysRegionManagerImpl.java L417
@Override
public List<String> getParentsCode(String code) {
    // FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况
    return RedisUtil.getList(String.format(ANC_KEY, code));
}
```

#### 详细分析

- **问题描述**: `getParentsCode` 方法从 Redis 缓存读取地区编码的祖先链，但 FIXME 注释表明未考虑缓存失效场景。如果地区层级关系发生变更（如行政区划调整），缓存中的祖先链数据将过期，导致返回错误的层级关系。
- **触发条件**: 地区层级关系发生变更时
- **潜在风险**: 数据不一致、业务逻辑错误（如根据地区层级计算费用/权限时）

#### 解决方案

**推荐修复**: 在地区数据更新时，通过 Redis Pub/Sub 或事件机制通知所有缓存节点失效相关祖先链缓存。

---

### 问题 P2-004: [carlos-system-bus] SysRegionManagerImpl updateCache 未处理层级变更

**模块**: `carlos-system-bus`
**文件**: `carlos-system-bus/src/main/java/com/carlos/system/region/manager/impl/SysRegionManagerImpl.java`
**行号**: L423
**严重程度**: P2-Medium
**影响范围**: 方法级

#### 相关代码

```java
// SysRegionManagerImpl.java L423
@Override
public void updateCache(SysRegionDTO bean) {
    // FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容
    RedisUtil.putHash(String.format(SELF_KEY, bean.getRegionCode()), bean);
}
```

#### 详细分析

- **问题描述**: `updateCache` 方法只更新了地区基本信息（Hash），但如果地区层级发生变更（如从 A 省调整到 B 省），children 集合、desc 集合、anc 链等缓存结构未同步更新，导致缓存数据与数据库不一致。
- **触发条件**: 地区层级关系发生变更时
- **潜在风险**: 缓存数据不一致、树形结构查询返回错误结果

#### 解决方案

**推荐修复**: 在 `updateCache` 中检测层级变更，如果层级发生变更，触发全量缓存重建或级联更新相关缓存键。

---

## 六、功能未完善详情

### 问题 P1-004: [carlos-audit-bus] AuditLogQueryService ClickHouse 查询未实现

**模块**: `carlos-audit-bus`
**文件**: `carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`
**行号**: L48, L69, L83, L117
**严重程度**: P1-High
**影响范围**: 模块级

#### 相关代码

```java
// AuditLogQueryService.java L48
public Paging<AuditLogMainDTO> pageQuery(...) {
    // TODO: 实现 ClickHouse 查询
    return new Paging<>();
}

// L69
public AuditLogMainDTO getById(Long id) {
    // TODO: 实现查询
    return null;
}

// L83
public AuditLogStatsVO getRealtimeStats(String tenantId, LocalDate date) {
    // TODO: 从物化视图查询统计
    return new AuditLogStatsVO();
}

// L117
public List<AuditLogMainDTO> listRiskEvents(...) {
    // TODO: 实现查询
    return Collections.emptyList();
}
```

#### 详细分析

- **问题描述**: `AuditLogQueryService` 是审计日志查询的核心服务，但所有方法均标记为 TODO，未实现 ClickHouse 查询逻辑。这意味着审计日志的查询功能（分页查询、详情查询、统计查询、风险事件查询）均不可用。
- **触发条件**: 任何调用审计日志查询功能的场景
- **潜在风险**: 审计日志查询功能完全不可用，无法查看操作日志、风险事件、统计报表

#### 解决方案

**推荐修复**: 实现 ClickHouse 查询逻辑，使用 `ClickHouseClient` 或 JDBC 执行查询。优先实现 `pageQuery` 和 `getById` 两个高频查询。

---

### 问题 P1-005: [carlos-message-bus] 批量发送功能返回错误提示

**模块**: `carlos-message-bus`
**文件**: `carlos-message-bus/src/main/java/com/carlos/message/apiimpl/MessageSendApiImpl.java`
**行号**: L83
**严重程度**: P1-High
**影响范围**: 方法级

#### 相关代码

```java
// MessageSendApiImpl.java L83
@Override
@PostMapping("/sendBatch")
@Operation(summary = "批量发送消息")
public Result<SendResultAO> sendBatch(@RequestBody List<MessageSendParam> params) {
    log.info("批量发送消息请求, 数量: {}", params.size());
    return Result.error("批量发送功能待实现");
}
```

#### 详细分析

- **问题描述**: `sendBatch` 方法已暴露为 REST 端点，但返回 "批量发送功能待实现" 错误。这会导致调用方收到错误响应，且没有降级处理。
- **触发条件**: 调用批量发送消息接口时
- **潜在风险**: 业务功能不可用、调用方异常处理逻辑触发

#### 解决方案

**推荐修复**: 
1. 实现批量发送逻辑（循环调用单条发送或批量插入消息队列）
2. 或暂时移除该端点，避免误导调用方

---

### 问题 P2-005: [carlos-tools] GUI 代码生成器多模块模板支持不完整

**模块**: `carlos-tools`
**文件**: `carlos-tools/src/main/java/com/carlos/fx/codege/service/CodeGeneratorService.java`
**行号**: L95
**严重程度**: P2-Medium
**影响范围**: 方法级

#### 相关代码

```java
// CodeGeneratorService.java L95
// TODO: Carlos 2020/9/9 如果是多模块模板  这个目录下会有多个src/main/java目录
```

#### 详细分析

- **问题描述**: 代码生成器的 TODO 从 2020 年遗留至今，多模块模板支持未实现。对于多模块项目（如 `carlos-auth`），代码生成器可能生成到错误的目录。
- **触发条件**: 使用多模块模板生成代码时
- **潜在风险**: 代码生成位置错误，需要手动调整

---

## 七、技术债务详情

### 7.1 模块间依赖合规性

| 模块 | 违规依赖 | 规范要求 | 建议操作 |
|----|------|------|------|
| carlos-auth-cloud | carlos-auth-service | carlos-auth-api | 提取共享接口到 -api |
| carlos-audit-cloud | carlos-audit-bus | carlos-audit-api | 同上 |
| carlos-message-cloud | carlos-message-bus | carlos-message-api | 同上 |
| carlos-org-cloud | carlos-org-bus | carlos-org-api | 同上 |
| carlos-system-cloud | carlos-system-bus | carlos-system-api | 同上 |

### 7.2 已弃用 API 使用

| 文件 | 行号 | 已弃用 API | 替代方案 |
|----|----|---------|------|
| 无 | - | - | - |

> ✅ 好消息：未发现 `javax.*` 包残留或 Spring Boot 2.x 迁移残留。

### 7.3 TODO/FIXME 遗留

| 文件 | 行号 | 标记内容 | 建议 |
|----|----|--------|------|
| AuditLogQueryService.java | L48 | TODO: 实现 ClickHouse 查询 | 立即实现（P1） |
| AuditLogQueryService.java | L69 | TODO: 实现查询 | 立即实现（P1） |
| AuditLogQueryService.java | L83 | TODO: 从物化视图查询统计 | 立即实现（P1） |
| AuditLogQueryService.java | L117 | TODO: 实现查询 | 立即实现（P1） |
| SysRegionManagerImpl.java | L417 | FIXME: 是否需要考虑缓存失效 | 评估并实现（P2） |
| SysRegionManagerImpl.java | L423 | FIXME: 当前只考虑更改基本信息 | 完善缓存更新逻辑（P2） |
| SysConfigService.java | L130 | TODO: 如果是文件类型，将文件转换成地址 | 排期实现（P3） |
| SysRegionService.java | L356 | FIXME: 方式优化 | 代码重构（P3） |
| LicenseCreatorController.java | L32 | TODO: 该接口的功能应该用一个脚本代替 | 排期优化（P3） |
| CodeGeneratorService.java | L95 | TODO: 多模块模板支持 | 排期实现（P3） |
| Gitlab 控制器 | 多处 | TODO: GUI 功能待实现 | 评估优先级（P3） |

---

## 八、文档缺失详情

### 问题 P3-001: [全部业务模块] 无 CHANGELOG 文件

**模块**: 全部业务模块
**严重程度**: P3-Low
**影响范围**: 框架级

#### 详细分析

- **问题描述**: 扫描发现所有 8 个业务模块（audit, auth, gateway, license, message, org, system, tools）均无 CHANGELOG 文件。CHANGELOG 是版本管理和变更追溯的重要文档，缺失会导致版本升级困难、变更不可追溯。
- **触发条件**: 版本升级、变更追溯时
- **潜在风险**: 版本升级无指南、变更不可追溯、新成员难以了解模块演进

#### 解决方案

**推荐修复**: 为每个业务模块添加 `CHANGELOG.md`，遵循 [Keep a Changelog](https://keepachangelog.com/) 格式。

---

## 九、业务模块健康度评分

| 模块          | 文档完整  | 需求合理  | 规范落实  | 性能安全  | 功能完整  | 综合评分    |
|-------------|-------|-------|-------|-------|-------|---------|
| carlos-auth | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.6/5.0 |
| carlos-gateway | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 5.0/5.0 |
| carlos-audit | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐☆☆☆ | 3.0/5.0 |
| carlos-message | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.2/5.0 |
| carlos-org | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-system | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.6/5.0 |
| carlos-license | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.5/5.0 |
| carlos-tools | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.2/5.0 |

> 评分说明：`carlos-auth` 因 Refresh Token 安全问题和模块依赖违规扣分；`carlos-audit` 因查询功能未实现扣分；`carlos-message` 因批量发送未实现和规范违规扣分；`carlos-tools` 因 TODO 遗留多扣分。

---

## 十、待办事项与修复计划

### 10.1 今日新增待办

- [ ] **P1-001** `carlos-auth-service`: 将 `Oauth2TokenSettings.reuseRefreshTokens` 默认值改为 `false`
- [ ] **P1-002** `carlos-auth-cloud`: 移除 `carlos-auth-service` 依赖，改为依赖 `carlos-auth-api`
- [ ] **P1-003** `carlos-audit-cloud`: 移除 `carlos-audit-bus` 依赖，改为依赖 `carlos-audit-api`
- [ ] **P1-003** `carlos-message-cloud`: 移除 `carlos-message-bus` 依赖，改为依赖 `carlos-message-api`
- [ ] **P1-003** `carlos-org-cloud`: 移除 `carlos-org-bus` 依赖，改为依赖 `carlos-org-api`
- [ ] **P1-003** `carlos-system-cloud`: 移除 `carlos-system-bus` 依赖，改为依赖 `carlos-system-api`
- [ ] **P1-004** `carlos-audit-bus`: 实现 `AuditLogQueryService` 的 ClickHouse 查询逻辑
- [ ] **P1-005** `carlos-message-bus`: 实现批量发送功能或移除该端点
- [ ] **P2-001** `carlos-system-api`: 修正 `ApiDict` 的 path 为 `/api/sys/dict`
- [ ] **P2-002** `carlos-message-bus/carlos-audit-bus`: 将 `BeanUtils.copyProperties` 替换为 MapStruct
- [ ] **P2-003** `carlos-system-bus`: 完善 `SysRegionManagerImpl` 缓存失效逻辑
- [ ] **P2-004** `carlos-system-bus`: 完善 `SysRegionManagerImpl` 层级变更缓存更新
- [ ] **P2-005** `carlos-tools`: 实现多模块模板支持或移除 TODO
- [ ] **P3-001** 全部业务模块: 添加 CHANGELOG.md

### 10.2 历史遗留跟踪

- [ ] P0-xxx `carlos-auth`: MD5PasswordEncoder 已标记 `@Deprecated`，需确认是否已移除生产环境引用 ⏳ 待确认
- [ ] P1-xxx `carlos-auth-service`: Refresh Token 重用默认启用 ⏳ 新增，需修复

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
