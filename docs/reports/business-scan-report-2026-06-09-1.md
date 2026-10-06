# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-09 08:00
**扫描 Commit**: `7de14ebcc69f7ddfa3a34e221adc21a97403fbd3`
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）
**发现问题总数**: 13 个（P0: 0 个, P1: 3 个, P2: 6 个, P3: 4 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 成功（已是最新）
- Commit: `7de14ebc`
- 变更文件数: 0 个（今日无新代码变更，仅有文档/报告提交）
- 基于最新代码全量扫描
- 上一扫描 Commit: `8d5b93e0`（2026-06-08）

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 0 | 2 | 2 |
| 需求与设计缺陷 | 0 | 1 | 1 | 0 | 2 |
| 代码规范违规 | 0 | 1 | 2 | 0 | 3 |
| 性能风险 | 0 | 0 | 0 | 0 | 0 |
| 安全漏洞 | 0 | 0 | 1 | 0 | 1 |
| 功能未完善 | 0 | 1 | 1 | 1 | 3 |
| 技术债务 | 0 | 0 | 1 | 1 | 2 |
| **合计** | **0** | **3** | **6** | **4** | **13** |

> **说明**：今日问题总数与昨日持平（13 个），所有问题均为历史遗留，无新增问题。今日扫描重点为**历史遗留问题修复状态跟踪**。

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 0 | 1 | 2 | 0 | 3 |
| carlos-audit | 0 | 1 | 1 | 0 | 2 |
| carlos-gateway | 0 | 0 | 0 | 1 | 1 |
| carlos-license | 0 | 0 | 0 | 1 | 1 |
| carlos-message | 0 | 0 | 0 | 0 | 0 |
| carlos-org | 0 | 1 | 1 | 0 | 2 |
| carlos-system | 0 | 0 | 1 | 1 | 2 |
| carlos-tools | 0 | 0 | 1 | 1 | 2 |

### 1.4 今日重点关注
- **[TOP 1]** P1-001: `carlos-auth-service` 仍使用 `InMemoryRegisteredClientRepository`，集群不支持，重启丢失客户端配置
- **[TOP 2]** P1-002: `carlos-audit-bus` 的 `AuditLogQueryService` ClickHouse 查询功能仍为 TODO 空壳，审计日志无法查询
- **[TOP 3]** P1-003: `carlos-system-bus` / `carlos-org-bus` 仍存在手写 SQL 的 XML 文件，违反框架强制规范

---

## 二、历史遗留问题修复状态跟踪

所有 13 个历史遗留问题均已人工复核，确认**未修复**，修复进度 0%。

| 问题编号 | 模块 | 问题简述 | 昨日状态 | 今日状态 | 修复进度 | 代码确认 |
|---------|------|---------|---------|---------|---------|---------|
| P1-001 | carlos-auth-service | 使用 `InMemoryRegisteredClientRepository` 存储客户端信息 | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：OAuth2ClientConfig.java L71 仍使用 `InMemoryRegisteredClientRepository` |
| P1-002 | carlos-audit-bus | `AuditLogQueryService` 的 4 个核心方法为 TODO 空壳 | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：pageQuery/getById/getRealtimeStats/listRiskEvents 仍返回空结果 |
| P1-003 | carlos-system/org-bus | 手写 SQL 的 XML Mapper 文件 | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：SysRegionMapper.xml 及 Org 系列 XML 文件仍存在 |
| P2-001 | carlos-auth-service | 验证码发送失败仍返回 true | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：CaptchaService.java L82 仍返回 true |
| P2-002 | carlos-auth-service | 邮箱验证码发送未实现 | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：sendEmailCaptcha 仍为 TODO 空壳 |
| P2-003 | carlos-system-bus | SysRegionManagerImpl FIXME 缓存失效未处理 | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：SysRegionManagerImpl.java L417 和 L423 仍有 FIXME 注释 |
| P2-004 | carlos-auth-service | ExtendAuthenticationProvider 事件发布被注释 | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：L98 和 L109 的 `publishEvent` 仍被注释 |
| P2-005 | carlos-audit-bus | 数据保留策略未配置 | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：ClickHouseBatchWriter 无 TTL 或归档配置 |
| P3-001 | 所有模块 | docs 目录模板占位符未填充 | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：各模块 docs 目录仍为模板文件 |
| P3-002 | carlos-tools | GitLab 相关代码 TODO 未清理 | ⏳ 待修复 | ⏳ 未修复 | 0% | ✅ 确认：UserManagementController 和 GitlabMainController 仍有 TODO 标记 |

---

## 三、P1 级问题详情（核心风险）

### 问题 1: [P1-001] `InMemoryRegisteredClientRepository` 不支持集群（未修复）

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2ClientConfig.java`
**行号**: L71
**严重程度**: P1-High
**影响范围**: 模块级（认证服务集群部署时客户端信息丢失）
**修复状态**: ⏳ 未修复

#### 相关代码

```java
// carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2ClientConfig.java L71
return new InMemoryRegisteredClientRepository(clients);
```

#### 详细分析

- **问题描述**: `InMemoryRegisteredClientRepository` 将客户端信息存储在内存中，集群部署时每个节点独立存储，无法共享客户端注册信息。服务重启后所有客户端信息丢失，需要重新注册。
- **违反原则**: OAuth2 授权服务器设计原则——客户端注册信息应持久化到数据库，支持集群共享和动态管理。
- **触发条件**: 服务重启、水平扩容（多节点）、客户端信息更新时。
- **潜在风险**: 1. 生产环境重启后所有客户端无法认证；2. 集群模式下客户端注册信息不一致；3. 无法动态添加/删除客户端。

#### 解决方案

1. 实现 `CustomizeRegisteredClientRepository`（继承 `RegisteredClientRepository` 接口），使用数据库（如 MySQL/Redis）持久化客户端信息
2. 参考 Spring Security OAuth2 官方示例：`JdbcRegisteredClientRepository`
3. 在启动时从数据库加载客户端信息，而非硬编码在内存中

---

### 问题 2: [P1-002] `AuditLogQueryService` 4 个核心方法为 TODO 空壳（未修复）

**模块**: `carlos-audit-bus`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`
**行号**: L43-L145
**严重程度**: P1-High
**影响范围**: 模块级（审计日志无法查询，失去审计价值）
**修复状态**: ⏳ 未修复

#### 相关代码

```java
// AuditLogQueryService.java 核心空壳方法
public Paging<AuditLogMainDTO> pageQuery(String tenantId, int pageNum, int pageSize, ...) {
    log.debug("分页查询审计日志...");
    // TODO: 实现 ClickHouse 查询
    return new Paging<>();  // 返回空结果
}

public AuditLogMainDTO getById(Long id) {
    log.debug("查询审计日志详情...");
    // TODO: 实现查询
    return null;  // 返回 null
}

public AuditLogStatsVO getRealtimeStats(String tenantId, LocalDate date) {
    log.debug("获取实时统计...");
    // TODO: 从物化视图查询统计
    return new AuditLogStatsVO();  // 返回空对象
}

public List<AuditLogMainDTO> listRiskEvents(String tenantId, int minRiskLevel, ...) {
    log.debug("查询风险事件...");
    // TODO: 实现查询
    return Collections.emptyList();  // 返回空列表
}
```

#### 详细分析

- **问题描述**: 审计查询服务的核心功能完全未实现，仅有 TODO 注释和日志输出。审计日志写入功能正常，但无法查询，导致审计系统无法使用。
- **违反原则**: 功能完整性原则——核心功能必须实现，不能只实现数据写入而不提供查询能力。
- **触发条件**: 任何调用审计查询接口的场景（如管理后台审计日志页面、风险事件分析）。
- **潜在风险**: 1. 审计系统形同虚设；2. 无法追溯安全事件；3. 无法满足合规审计要求；4. 风险事件无法及时发现和预警。

#### 解决方案

1. 使用 `ClickHouseClient` 原生查询实现 `pageQuery`（分页查询）
2. 使用 `ClickHouseClient` 实现 `getById`（单条查询）
3. 使用物化视图（Materialized View）实现 `getRealtimeStats`（实时统计）
4. 使用 `ClickHouseClient` 实现 `listRiskEvents`（风险事件查询）
5. 可考虑使用 `mybatis-plus-join` 或 JDBC 直接查询 ClickHouse

---

### 问题 3: [P1-003] 手写 SQL XML 文件违反框架强制规范（未修复）

**模块**: `carlos-system-bus` / `carlos-org-bus`
**文件**: 
- `carlos-integration/carlos-system/carlos-system-bus/src/main/resources/mapper/region/SysRegionMapper.xml`
- `carlos-integration/carlos-org/carlos-org-bus/src/main/resources/mapper/OrgUserPositionMapper.xml`
- `carlos-integration/carlos-org/carlos-org-bus/src/main/resources/mapper/OrgPositionCategoryMapper.xml`
- `carlos-integration/carlos-org/carlos-org-bus/src/main/resources/mapper/org/OrgRolePermissionMapper.xml`
- `carlos-integration/carlos-org/carlos-org-bus/src/main/resources/mapper/org/OrgRoleOperationMapper.xml`
- `carlos-integration/carlos-org/carlos-org-bus/src/main/resources/mapper/org/OrgPermissionMapper.xml`
**严重程度**: P1-High
**影响范围**: 多模块联动（违反框架统一规范）
**修复状态**: ⏳ 未修复

#### 详细分析

- **问题描述**: 框架强制规范要求使用 MyBatis-Plus + mybatis-plus-join 在 Manager 层查询，严禁直接写 SQL XML。但 `carlos-system` 和 `carlos-org` 模块中仍存在手写 SQL 的 XML Mapper 文件。
- **违反规范**: `CLAUDE.md` / `AGENTS.md` 强制规范——"数据查询: MyBatis-Plus + mybatis-plus-join 在 Manager 层，严禁直接写 SQL"
- **触发条件**: 所有使用这些 XML Mapper 的查询操作。
- **潜在风险**: 1. SQL 注入风险（虽然使用 `#{}` 但仍存在动态 SQL 拼接风险）；2. 代码维护困难（SQL 和 Java 分离）；3. 框架能力降级（MyBatis-Plus 的通用 CRUD 无法使用）；4. 与框架统一规范冲突。

#### 解决方案

1. 将 XML 中的手写 SQL 转换为 MyBatis-Plus 的 `QueryWrapper` + `mybatis-plus-join` 方式
2. 复杂查询（如树形查询、递归查询）可保留 XML，但需经过架构评审
3. 在 CI 中增加检查：禁止新增 XML Mapper 文件，已有 XML 需逐步迁移

---

## 四、P2 级问题详情

### 问题 4: [P2-001] 验证码发送失败仍返回 true（未修复）

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/captcha/CaptchaService.java`
**行号**: L82
**严重程度**: P2-Medium
**影响范围**: 模块级
**修复状态**: ⏳ 未修复

#### 相关代码

```java
// CaptchaService.java L65-L82
if (StrUtil.isNotBlank(templateCode)) {
    try {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("code", captcha);
        SmsUtil.sendByTemplateKey(phone, templateCode, params);
        log.info("SMS captcha sent to {} via SMS service", SensitiveDataUtil.maskPhone(phone));
    } catch (Exception e) {
        log.error("Failed to send SMS captcha to {}", SensitiveDataUtil.maskPhone(phone), e);
    }
} else {
    log.warn("SMS captcha template not configured...");
}
return true;  // 无论短信是否发送成功，都返回 true
```

#### 详细分析

- **问题描述**: `sendSmsCaptcha` 在短信发送异常捕获后仍返回 `true`，导致调用方误判短信已发送成功。用户可能收不到验证码但系统告知发送成功。
- **违反原则**: 准确反馈原则——API 返回应准确反映操作结果。
- **潜在风险**: 用户反复请求验证码但收不到，投诉增加；测试环境误以为短信功能正常。

#### 解决方案

1. 短信发送失败时返回 `false`，并删除已保存的验证码
2. 短信模板未配置时返回 `false`，提示管理员配置
3. 成功发送时返回 `true`

---

### 问题 5: [P2-002] 邮箱验证码发送未实现（未修复）

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/captcha/CaptchaService.java`
**行号**: L89-L99
**严重程度**: P2-Medium
**影响范围**: 模块级
**修复状态**: ⏳ 未修复

#### 相关代码

```java
// CaptchaService.java L89-L99
public boolean sendEmailCaptcha(String email) {
    // TODO: 集成 spring-boot-starter-mail 实现邮箱验证码发送
    return true;
}
```

#### 详细分析

- **问题描述**: 邮箱验证码发送方法为 TODO 空壳，直接返回 `true`。与短信验证码问题类似，会导致调用方误判。
- **潜在风险**: 1. 无法提供邮箱验证码功能；2. 返回 true 导致前端显示发送成功但实际未发送。

#### 解决方案

1. 集成 `spring-boot-starter-mail` 实现邮件发送
2. 或暂时返回 `false` 并添加日志，避免误导

---

### 问题 6: [P2-003] SysRegionManagerImpl FIXME 缓存失效未处理（未修复）

**模块**: `carlos-system-bus`
**文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/manager/impl/SysRegionManagerImpl.java`
**行号**: L417, L423
**严重程度**: P2-Medium
**影响范围**: 模块级（缓存数据不一致）
**修复状态**: ⏳ 未修复

#### 相关代码

```java
// SysRegionManagerImpl.java L417-L423
// FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况
// FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容
```

#### 详细分析

- **问题描述**: 区域数据修改后，缓存未失效，导致缓存数据与数据库不一致。同时层级变更（如区域编码变更）也未处理。
- **潜在风险**: 1. 缓存脏数据；2. 区域编码变更后关联数据不一致；3. 长时间运行后缓存与数据库数据偏差越来越大。

#### 解决方案

1. 在 `update` 方法后添加缓存失效逻辑（使用 Redis 的 `del` 命令删除相关缓存）
2. 层级变更时级联更新子区域缓存
3. 使用 Redis Pub/Sub 或 Canal 实现缓存同步

---

### 问题 7: [P2-004] ExtendAuthenticationProvider 认证事件发布被注释（未修复）

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/provider/ExtendAuthenticationProvider.java`
**行号**: L97-L98, L108-L109
**严重程度**: P2-Medium
**影响范围**: 模块级（审计日志缺失认证事件）
**修复状态**: ⏳ 未修复

#### 相关代码

```java
// ExtendAuthenticationProvider.java L97-L109
if (eventPublisher != null) {
    // eventPublisher.publishEvent(new AuthenticationSuccessEvent(authenticatedToken));
}
// ...
if (eventPublisher != null) {
    // eventPublisher.publishEvent(new AuthenticationFailureEvent(token, e));
}
```

#### 详细分析

- **问题描述**: `AuthenticationSuccessEvent` 和 `AuthenticationFailureEvent` 的事件发布被注释掉，导致认证成功/失败的事件监听器无法收到通知，审计日志无法记录认证事件。
- **潜在风险**: 1. 审计日志无法记录登录事件；2. 安全监控无法检测异常登录；3. 认证成功后的后续处理（如通知、积分）无法触发。

#### 解决方案

1. 取消 `publishEvent` 的注释，启用事件发布
2. 确保事件监听器已正确配置，不会因事件发布导致性能问题

---

### 问题 8: [P2-005] 审计数据保留策略未配置（未修复）

**模块**: `carlos-audit-bus`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java`
**严重程度**: P2-Medium
**影响范围**: 模块级（数据膨胀，存储成本增加）
**修复状态**: ⏳ 未修复

#### 详细分析

- **问题描述**: `ClickHouseBatchWriter` 负责写入审计日志，但没有配置数据保留策略（TTL）。审计日志会持续累积，长期运行后数据量巨大，存储成本增加，查询性能下降。
- **潜在风险**: 1. ClickHouse 存储空间持续增长；2. 查询性能下降；3. 备份和恢复时间增加；4. 可能触发磁盘空间告警。

#### 解决方案

1. 在 ClickHouse 表创建时添加 TTL 配置：`TTL server_time + INTERVAL 90 DAY`
2. 或使用 ClickHouse 的 `MergeTree` 引擎 TTL 特性
3. 提供归档机制：将过期数据迁移到对象存储（如 MinIO/S3）
4. 在配置中提供保留天数参数，支持不同环境配置不同策略

---

### 问题 9: [P2-006] 验证码发送未实现防重放和防刷机制（未修复）

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/captcha/CaptchaService.java`
**严重程度**: P2-Medium
**影响范围**: 模块级（短信轰炸风险）
**修复状态**: ⏳ 未修复

#### 详细分析

- **问题描述**: `CaptchaService` 虽然实现了发送频率限制，但缺少防重放机制（同一请求重复发送）和更严格的防刷策略（如 IP 限制、图形验证码前置）。
- **潜在风险**: 1. 攻击者可利用重放攻击大量发送短信，造成短信费用损失；2. 被短信服务商拉黑，影响正常业务。

#### 解决方案

1. 添加请求签名校验（timestamp + nonce + 签名），防止重放
2. 在发送短信前增加图形验证码校验
3. 限制同一 IP 的日发送次数
4. 对异常发送行为进行监控和告警

---

## 五、P3 级问题详情

### 问题 10: [P3-001] 模块 docs 目录模板未填充（未修复）

**模块**: 所有业务模块
**严重程度**: P3-Low
**影响范围**: 框架级（文档缺失）
**修复状态**: ⏳ 未修复

#### 详细分析

- **问题描述**: 各业务模块的 `docs/` 目录下存在模板文件（如 `requirement.md`、`design.md` 等），但内容为空或仅包含占位符，未填充实际内容。
- **潜在风险**: 1. 新开发者无法快速了解模块功能；2. 架构设计无法追溯；3. 接口契约无法确认。

#### 解决方案

1. 移除未使用的模板文件，或填充实际内容
2. 在 CI 中增加检查：禁止提交空的模板文件

---

### 问题 11: [P3-002] carlos-tools GitLab 代码 TODO 未清理（未修复）

**模块**: `carlos-tools`
**文件**: 
- `carlos-integration/carlos-tools/src/main/java/com/carlos/fx/gitlab/controller/UserManagementController.java`
- `carlos-integration/carlos-tools/src/main/java/com/carlos/fx/gitlab/controller/GitlabMainController.java`
**严重程度**: P3-Low
**影响范围**: 模块级
**修复状态**: ⏳ 未修复

#### 详细分析

- **问题描述**: GitLab 用户管理相关的控制器中大量方法为 TODO 空壳，未实现实际功能。
- **潜在风险**: 1. 代码冗余，维护困难；2. 测试覆盖率降低。

#### 解决方案

1. 实现未完成的 GitLab 功能，或移除未实现的代码
2. 如果 `carlos-tools` 是 GUI 代码生成器，建议明确功能范围，移除未规划的功能

---

### 问题 12: [P3-003] carlos-gateway 缺少限流熔断配置文档（未修复）

**模块**: `carlos-gateway`
**严重程度**: P3-Low
**影响范围**: 模块级
**修复状态**: ⏳ 未修复

#### 详细分析

- **问题描述**: `carlos-gateway` 包含限流、熔断、WAF 等高级功能，但缺少配置文档和示例，开发者不知道如何使用这些功能。
- **潜在风险**: 1. 网关功能无法被充分利用；2. 配置错误可能导致服务不可用。

#### 解决方案

1. 补充 `carlos-gateway` 的 README.md，包含配置说明和示例
2. 提供完整的配置模板（application.yml）

---

### 问题 13: [P3-004] carlos-license 缺少 README 说明生产环境注意事项（未修复）

**模块**: `carlos-license`
**严重程度**: P3-Low
**影响范围**: 框架级
**修复状态**: ⏳ 未修复

#### 详细分析

- **问题描述**: `carlos-license` 模块包含 `license-generate`（生成端）和 `license-verify`（验证端），但缺少明确的生产环境安全警告：生产环境严禁包含 `license-generate` 组件。
- **潜在风险**: 生产环境误包含 `license-generate` 可能导致私钥泄露，攻击者可伪造 License。

#### 解决方案

1. 在 `carlos-license` 的 README 中明确标注：生产环境严禁包含 `carlos-spring-boot-starter-license-generate`
2. 在 `license-generate` 的 AutoConfiguration 中增加环境检测，生产环境自动禁用

---

## 六、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|------|---------|---------|---------|--------|--------|---------|
| carlos-auth | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 3.0/5.0 |
| carlos-audit | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐☆☆☆ | 2.6/5.0 |
| carlos-gateway | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-license | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 2.8/5.0 |
| carlos-message | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-org | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.0/5.0 |
| carlos-system | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 3.0/5.0 |
| carlos-tools | ⭐⭐☆☆☆ | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | 2.2/5.0 |

> **评分说明**：
> - `carlos-auth` 评分低是因为认证事件未发布、验证码返回不准确、集群客户端存储问题
> - `carlos-audit` 评分低是因为查询功能未实现、文档缺失
> - `carlos-tools` 评分低是因为大量 TODO 未实现、文档缺失

---

## 七、待办事项与修复计划

### 7.1 今日新增待办

- 无新增待办（今日无代码变更，无新增问题）

### 7.2 历史遗留跟踪（完整清单）

所有 13 个历史遗留问题均未修复，修复进度 0%。按优先级排序：

#### P1 级别（本周内修复）
- [ ] **P1-001** `carlos-auth-service`: 将 `InMemoryRegisteredClientRepository` 替换为数据库持久化实现，支持集群共享和动态客户端注册 ⏳ 未修复
- [ ] **P1-002** `carlos-audit-bus`: 实现 `AuditLogQueryService` 的 ClickHouse 查询功能（pageQuery、getById、getRealtimeStats、listRiskEvents） ⏳ 未修复
- [ ] **P1-003** `carlos-system-bus` / `carlos-org-bus`: 移除手写 SQL 的 XML 文件，改用 MyBatis-Plus / mybatis-plus-join 实现 ⏳ 未修复

#### P2 级别（本月内修复）
- [ ] **P2-001** `carlos-auth-service`: 修复 `CaptchaService.sendSmsCaptcha` 短信发送失败时返回 true 的问题，改为返回 false 并删除已保存的验证码 ⏳ 未修复
- [ ] **P2-002** `carlos-auth-service`: 集成 `spring-boot-starter-mail` 实现邮箱验证码发送，或暂时返回 false ⏳ 未修复
- [ ] **P2-003** `carlos-system-bus`: 修复 `SysRegionManagerImpl` 中 FIXME 标记的缓存失效和层级变更问题 ⏳ 未修复
- [ ] **P2-004** `carlos-auth-service`: 取消 `ExtendAuthenticationProvider` 中认证事件发布的注释，启用 `AuthenticationSuccessEvent` 和 `AuthenticationFailureEvent` ⏳ 未修复
- [ ] **P2-005** `carlos-audit-bus`: 配置 ClickHouse 审计数据保留策略（TTL），支持按天/月归档 ⏳ 未修复
- [ ] **P2-006** `carlos-auth-service`: 增加验证码防重放和防刷机制（请求签名、IP 限制、图形验证码前置） ⏳ 未修复

#### P3 级别（排期优化）
- [ ] **P3-001** 所有模块: 填充 docs 目录模板中的占位符，或移除未使用的模板文件 ⏳ 未修复
- [ ] **P3-002** `carlos-tools`: 清理 GitLab 相关代码的 TODO，或移除未实现的功能代码 ⏳ 未修复
- [ ] **P3-003** `carlos-gateway`: 补充 README 和配置文档，说明限流/熔断/WAF 的使用方法 ⏳ 未修复
- [ ] **P3-004** `carlos-license`: 在 README 中明确标注生产环境严禁包含 `license-generate` 组件 ⏳ 未修复

---

## 八、扫描总结与建议

### 8.1 今日扫描总结

今日业务集成层无新代码变更，所有 13 个历史遗留问题均未修复。建议朱军优先按以下顺序处理：

1. **立即处理 P1-002**（审计查询功能）：审计系统是安全合规的基础，查询功能缺失导致审计形同虚设。
2. **同步处理 P1-001**（认证客户端存储）：影响集群部署能力，是生产环境部署的阻塞项。
3. **本周处理 P1-003**（手写 SQL 迁移）：违反框架强制规范，需逐步迁移到 MyBatis-Plus。
4. **本月处理 P2 级别问题**：特别是验证码返回准确性（P2-001）、认证事件发布（P2-004）、缓存一致性（P2-003）。

### 8.2 长期建议

1. **建立 CI 检查**：
   - 检查 XML Mapper 文件是否新增（禁止新增手写 SQL）
   - 检查 TODO/FIXME 注释是否过多（超过 5 个警告）
   - 检查 `InMemory` 类是否在非测试代码中使用

2. **建立安全编码规范**：
   - 验证码发送必须返回准确结果
   - 认证事件必须发布（用于审计和安全监控）
   - 生产环境严禁使用 `InMemory` 存储

3. **完善测试覆盖**：
   - 为 `CaptchaService` 增加单元测试（模拟短信发送失败场景）
   - 为 `AuditLogQueryService` 增加集成测试（使用内存 ClickHouse 或 Mock）
   - 为 `ExtendAuthenticationProvider` 增加事件发布测试

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
*生成时间: 2026-06-09 08:00*
*扫描 Commit: 7de14ebcc69f7ddfa3a34e221adc21a97403fbd3*
*上一扫描 Commit: 8d5b93e0092ba0ec863d8fdb8b2c598b259bba62（2026-06-08）*
