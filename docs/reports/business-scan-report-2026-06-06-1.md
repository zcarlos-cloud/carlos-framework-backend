# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-06 00:00
**扫描 Commit**: de5a790c14ad76bdf58d197dcc87ad38b187cbdf
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system）
**发现问题总数**: 18 个（P0: 2 个, P1: 5 个, P2: 6 个, P3: 5 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 基于本地代码扫描（网络可能不稳定）
- Commit: de5a790c14ad76bdf58d197dcc87ad38b187cbdf
- 变更文件数: 1 个（docs: 框架核心代码扫描报告 2026-06-06）
- 新增代码: +0 行 / 删除代码: -0 行

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 1 | 2 | 2 | 5 |
| 需求与设计缺陷 | 1 | 2 | 1 | 0 | 4 |
| 代码规范违规 | 0 | 2 | 1 | 0 | 3 |
| 性能风险 | 0 | 0 | 1 | 0 | 1 |
| 安全漏洞 | 1 | 1 | 0 | 0 | 2 |
| 功能未完善 | 0 | 1 | 2 | 0 | 3 |
| 技术债务 | 0 | 0 | 2 | 3 | 5 |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 1 | 2 | 1 | 0 | 4 |
| carlos-gateway | 0 | 0 | 1 | 0 | 1 |
| carlos-audit | 1 | 1 | 2 | 1 | 5 |
| carlos-message | 0 | 1 | 1 | 0 | 2 |
| carlos-org | 0 | 0 | 1 | 1 | 2 |
| carlos-system | 0 | 1 | 1 | 2 | 4 |
| carlos-license | 1 | 0 | 1 | 0 | 2 |
| carlos-tools | 0 | 0 | 2 | 3 | 5 |

### 1.4 今日重点关注
- [TOP 1] carlos-license 生产环境包含 license-generate 模块（P0，安全风险）
- [TOP 2] carlos-audit 使用已弃用的 Spring Boot Health API（P0，兼容性问题）
- [TOP 3] carlos-auth 核心认证提供者被完全注释掉（P1，功能缺失）

---

## 二、文档缺失详情

### 问题 1: carlos-integration 业务模块缺乏模块级 README

**模块**: `carlos-integration`
**文件**: `carlos-integration/*/README.md`（不存在）
**严重程度**: P1-High
**影响范围**: 模块级

#### 详细分析

- **问题描述**: 所有业务集成模块（auth, audit, gateway, license, message, org, system）根目录下均缺少 README.md 文件
- **违反规范**: AGENTS.md 中要求每个模块应有清晰的文档资产
- **触发条件**: 新开发者接入、运维排查、模块升级时
- **潜在风险**: 增加维护成本、知识传递困难、模块边界不清晰

#### 解决方案

**推荐修复**:
为每个业务模块创建 README.md，包含：
1. 模块简介与定位
2. 核心功能列表
3. 快速开始（依赖引入、配置示例）
4. 接口文档链接（Knife4j）
5. 常见问题排查

**参考模板**:
```markdown
# carlos-xxx 模块

## 模块定位
xxx 模块负责 ...

## 核心功能
- 功能1
- 功能2

## 快速开始
### 依赖引入
```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-xxx-boot</artifactId>
</dependency>
```

### 配置示例
```yaml
carlos:
  xxx:
    enabled: true
```

## 接口文档
启动后访问：`http://localhost:8080/doc.html`

## 常见问题
...
```

**参考文档**: [Spring Boot 模块文档最佳实践](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#documentation)

---

### 问题 2: 业务模块缺少 CHANGELOG

**模块**: `carlos-integration`
**文件**: `carlos-integration/*/CHANGELOG.md`（不存在）
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 详细分析

- **问题描述**: 所有业务模块缺少版本变更日志，无法追溯功能变更
- **触发条件**: 版本升级、回滚、问题定位时
- **潜在风险**: 版本兼容性风险、升级困难

#### 解决方案

**推荐修复**:
为每个模块创建 CHANGELOG.md，遵循 [Keep a Changelog](https://keepachangelog.com/) 格式：
```markdown
# Changelog

## [1.0.0] - 2026-06-06
### Added
- 新增功能X

### Fixed
- 修复问题Y

### Changed
- 优化逻辑Z
```

---

### 问题 3: 接口契约文档化不足

**模块**: `carlos-audit-api`, `carlos-auth-api`, `carlos-message-api`, `carlos-org-api`, `carlos-system-api`
**文件**: Feign 接口定义文件
**严重程度**: P2-Medium
**影响范围**: 多模块联动

#### 详细分析

- **问题描述**: Feign 接口缺乏详细的接口契约文档（请求参数、响应结构、错误码、限流策略）
- **违反规范**: 文档完整性评估要求接口设计文档必须包含接口契约
- **触发条件**: 跨服务调用、接口对接、问题排查时
- **潜在风险**: 接口误解、集成错误、联调困难

#### 解决方案

**推荐修复**:
在 Feign 接口上添加 JavaDoc 和 Swagger 注解：
```java
/**
 * 审计日志主记录接口
 *
 * <p>接口契约：</p>
 * <ul>
 *   <li>限流：100次/秒</li>
 *   <li>超时：5秒</li>
 *   <li>降级：返回空列表</li>
 * </ul>
 *
 * @see AuditLogMainDTO
 */
@FeignClient(name = "carlos-audit", path = "/api/audit/log/main")
public interface ApiAuditLogMain {
    // ...
}
```

---

## 三、需求与设计缺陷详情

### 问题 4: carlos-auth 核心认证提供者被完全注释掉

**模块**: `carlos-auth-service`
**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/oauth2/grant/BaseAuthenticationProvider.java`
**行号**: L1-L176（整个文件被注释）
**严重程度**: P1-High
**影响范围**: 模块级

#### 相关代码

```java
// package com.carlos.auth.oauth2.grant;
//
// import com.carlos.auth.exception.UserNotFoundException;
// ...
//
// @Slf4j
// public abstract class BaseAuthenticationProvider<T extends BaseAuthenticationToken> implements AuthenticationProvider {
//     // ... 整个类被注释掉
// }
```

#### 详细分析

- **问题描述**: `BaseAuthenticationProvider` 是 OAuth2 自定义授权流程的核心基类，但整个文件被注释掉，导致所有基于它的认证提供者（密码模式、短信模式、扫码模式等）无法正常工作
- **违反规范**: 功能完整度要求核心功能必须完整实现
- **触发条件**: 任何使用自定义授权模式的登录请求
- **潜在风险**: 认证功能完全不可用，系统无法登录

#### 解决方案

**推荐修复**:
1. 恢复 `BaseAuthenticationProvider` 代码
2. 检查子类是否也被注释
3. 添加单元测试确保认证流程正常

**短期规避**:
如果该基类已废弃，应删除文件并迁移到新的实现方式，而非保留被注释的代码。

**长期建议**:
- 建立代码审查机制，禁止提交被注释的核心代码
- 添加 CI 检查，确保核心类未被意外注释

---

### 问题 5: carlos-audit 审计查询功能完全未实现

**模块**: `carlos-audit-bus`
**文件**: `carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`
**行号**: L48, L69, L83, L117
**严重程度**: P1-High
**影响范围**: 模块级

#### 相关代码

```java
// L48
public List<AuditLogMainDTO> queryByCondition(AuditLogQueryParam param) {
    // TODO: 实现 ClickHouse 查询
    return Collections.emptyList();
}

// L69
public Paging<AuditLogMainDTO> queryPage(AuditLogQueryParam param) {
    // TODO: 实现查询
    return new Paging<>();
}

// L83
public Map<String, Object> queryStatistics(AuditLogQueryParam param) {
    // TODO: 从物化视图查询统计
    return Collections.emptyMap();
}

// L117
public List<AuditLogMainDTO> queryRiskLogs(AuditLogQueryParam param) {
    // TODO: 实现查询
    return Collections.emptyList();
}
```

#### 详细分析

- **问题描述**: 审计模块的核心查询功能（按条件查询、分页查询、统计查询、风险日志查询）全部标记为 TODO，未实现
- **违反规范**: 功能完整度要求核心功能必须完整实现
- **触发条件**: 任何审计日志查询请求
- **潜在风险**: 审计功能形同虚设，无法满足合规要求

#### 解决方案

**推荐修复**:
1. 实现基于 ClickHouse 的审计查询
2. 使用 mybatis-plus-join 或 JDBC 查询 ClickHouse
3. 添加分页支持（ClickHouse 不支持传统分页，需使用 LIMIT + ORDER BY）

```java
public List<AuditLogMainDTO> queryByCondition(AuditLogQueryParam param) {
    StringBuilder sql = new StringBuilder("SELECT * FROM audit_log_main WHERE 1=1");
    if (param.getStartTime() != null) {
        sql.append(" AND event_time >= ").append(param.getStartTime());
    }
    // ... 其他条件
    sql.append(" ORDER BY event_time DESC LIMIT ").append(param.getLimit());
    return clickHouseClient.query(sql.toString(), AuditLogMainDTO.class);
}
```

**短期规避**:
在查询接口返回 `Result.error("审计查询功能开发中")`，而非空列表，避免前端误解。

**长期建议**:
- 建立 ClickHouse 查询 DSL
- 添加审计查询缓存（热点查询结果缓存 5 分钟）

---

### 问题 6: carlos-message 批量发送功能未实现

**模块**: `carlos-message-bus`
**文件**: `carlos-message-bus/src/main/java/com/carlos/message/apiimpl/MessageSendApiImpl.java`
**行号**: L84-L89
**严重程度**: P2-Medium
**影响范围**: 类级

#### 相关代码

```java
@Override
@PostMapping("/sendBatch")
@Operation(summary = "批量发送消息")
public Result<SendResultAO> sendBatch(@RequestBody List<MessageSendParam> params) {
    log.info("批量发送消息请求, 数量: {}", params.size());
    return Result.error("批量发送功能待实现");
}
```

#### 详细分析

- **问题描述**: 批量发送 API 已暴露但返回错误，影响用户体验
- **触发条件**: 调用批量发送接口
- **潜在风险**: 功能不可用、用户体验差

#### 解决方案

**推荐修复**:
1. 实现批量发送逻辑（复用单条发送逻辑，添加事务控制）
2. 或暂时移除该 API，待实现后再暴露

```java
@Override
@PostMapping("/sendBatch")
@Operation(summary = "批量发送消息")
public Result<SendResultAO> sendBatch(@RequestBody List<MessageSendParam> params) {
    log.info("批量发送消息请求, 数量: {}", params.size());
    List<String> messageIds = new ArrayList<>();
    for (MessageSendParam param : params) {
        Result<String> result = send(param);
        if (result.isSuccess()) {
            messageIds.add(result.getData());
        }
    }
    SendResultAO ao = new SendResultAO();
    ao.setMessageIds(messageIds);
    ao.setSuccess(!messageIds.isEmpty());
    return Result.success(ao);
}
```

---

## 四、代码规范违规详情

### 问题 7: carlos-message 使用 BeanUtils.copyProperties 而非 MapStruct

**模块**: `carlos-message-bus`
**文件**: `carlos-message-bus/src/main/java/com/carlos/message/apiimpl/MessageSendApiImpl.java`
**行号**: L46, L64, L84
**严重程度**: P1-High
**影响范围**: 类级

#### 相关代码

```java
@Override
@PostMapping("/send")
@Operation(summary = "同步发送消息")
public Result<SendResultAO> send(@RequestBody MessageSendParam param) {
    log.info("发送消息请求: {}", param);
    MessageCreateParam createParam = new MessageCreateParam();
    BeanUtils.copyProperties(param, createParam);  // 违反规范
    // ...
}
```

#### 详细分析

- **问题描述**: 使用 `BeanUtils.copyProperties` 进行对象拷贝，违反框架规范（应使用 MapStruct）
- **违反规范**: SKILL.md 要求 Convert 规范必须使用 MapStruct
- **触发条件**: 每次请求转换
- **潜在风险**: 运行时性能损耗、字段映射错误、类型安全问题

#### 解决方案

**推荐修复**:
1. 创建 `MessageSendApiImplConvert` 接口（MapStruct）
2. 替换所有 `BeanUtils.copyProperties` 调用

```java
@Mapper
public interface MessageSendApiImplConvert {
    MessageSendApiImplConvert INSTANCE = Mappers.getMapper(MessageSendApiImplConvert.class);
    
    MessageCreateParam toCreateParam(MessageSendParam param);
}
```

```java
// 替换后
MessageCreateParam createParam = MessageSendApiImplConvert.INSTANCE.toCreateParam(param);
```

---

### 问题 8: carlos-auth 设备指纹使用 MD5 哈希

**模块**: `carlos-auth-service`
**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/util/DeviceFingerprint.java`
**行号**: L45-L46
**严重程度**: P1-High
**影响范围**: 类级

#### 相关代码

```java
/**
 * 生成设备指纹（简化版）
 *
 * @param request HTTP请求
 * @return 设备指纹（MD5哈希）
 */
public String generate(HttpServletRequest request) {
    String fingerprintData = request.getHeader("User-Agent") + "|" + request.getRemoteAddr();
    // 生成MD5指纹
    return SecureUtil.md5(fingerprintData);  // 使用MD5
}
```

#### 详细分析

- **问题描述**: 使用 MD5 生成设备指纹，虽然设备指纹不直接用于密码存储，但 MD5 已被证明存在碰撞风险，不适合任何安全场景
- **违反规范**: AGENTS.md 安全红线要求生产环境使用 BCrypt，MD5 仅用于旧系统兼容
- **触发条件**: 每次登录请求生成设备指纹
- **潜在风险**: 设备指纹碰撞、伪造设备指纹

#### 解决方案

**推荐修复**:
使用 SHA-256 或更安全的哈希算法替代 MD5：

```java
public String generate(HttpServletRequest request) {
    String fingerprintData = request.getHeader("User-Agent") + "|" + request.getRemoteAddr();
    return SecureUtil.sha256(fingerprintData);
}
```

**长期建议**:
- 引入前端指纹库（如 FingerprintJS）获取更丰富的设备信息
- 考虑使用 HMAC-SHA256 增加伪造难度

---

### 问题 9: carlos-gateway 使用已弃用的 Spring Boot Health API

**模块**: `carlos-audit-bus`
**文件**: `carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseHealthIndicator.java`
**行号**: L5-L6
**严重程度**: P2-Medium
**影响范围**: 类级

#### 相关代码

```java
import org.springframework.boot.health.contributor.Health;        // 已弃用
import org.springframework.boot.health.contributor.HealthIndicator; // 已弃用
```

#### 详细分析

- **问题描述**: 使用 Spring Boot 2.x 的 `org.springframework.boot.health.contributor.HealthIndicator` API，在 Spring Boot 3.x 中已弃用，应使用 `org.springframework.boot.actuate.health.HealthIndicator`
- **违反规范**: 技术债务要求迁移已弃用 API
- **触发条件**: Spring Boot 升级时
- **潜在风险**: 未来版本兼容性问题、编译警告

#### 解决方案

**推荐修复**:
```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
```

---

## 五、性能风险详情

### 问题 10: carlos-org 部门树全量加载无深度控制

**模块**: `carlos-org-bus`
**文件**: `carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`
**行号**: L245-L251
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 相关代码

```java
@Override
public List<OrgDepartmentDTO> listAll() {
    Integer scope = dataScopeHelper.getCurrentDataScope();
    // 只有全部数据权限时才走缓存
    if (scope == null || scope.equals(OrgDataScopeEnum.ALL.getCode())) {
        Set<String> keys = RedisUtil.scanKeys(String.format(SELF_KEY, RedisUtil.ALL), 500);
        // ...
    }
    // 否则查数据库
    List<OrgDepartment> list = getBaseMapper().selectList(null);
    return list.stream().map(OrgDepartmentConvert.INSTANCE::toDTO).collect(Collectors.toList());
}
```

#### 详细分析

- **问题描述**: `listAll()` 方法加载所有部门数据，如果部门数量巨大（如 10 万+），可能导致内存溢出和 Redis 大 Key 问题
- **触发条件**: 超大规模组织架构（> 10000 部门）
- **潜在风险**: 内存溢出、Redis 大 Key、接口超时

#### 解决方案

**推荐修复**:
1. 添加最大加载限制（如 10000 条）
2. 分页加载 + 前端懒加载
3. 使用 Redis Stream 或分片存储大集合

```java
@Override
public List<OrgDepartmentDTO> listAll() {
    // 添加限制
    List<OrgDepartment> list = getBaseMapper().selectList(
        new LambdaQueryWrapper<OrgDepartment>().last("LIMIT 10000")
    );
    return list.stream().map(OrgDepartmentConvert.INSTANCE::toDTO).collect(Collectors.toList());
}
```

**长期建议**:
- 实现部门树虚拟滚动（前端）
- 添加部门数量监控告警

---

## 六、安全漏洞详情

### 问题 11: carlos-license 生产环境包含 license-generate 模块

**模块**: `carlos-license`
**文件**: `carlos-license/pom.xml`
**行号**: L20
**严重程度**: P0-Critical
**影响范围**: 模块级

#### 相关代码

```xml
<modules>
    <module>carlos-license-core</module>
    <module>carlos-spring-boot-starter-license-generate</module>  <!-- 生产环境严禁 -->
    <module>carlos-spring-boot-starter-license-verify</module>
</modules>
```

#### 详细分析

- **问题描述**: `carlos-spring-boot-starter-license-generate` 模块包含私钥生成和证书签发功能，生产环境包含该模块会导致私钥泄露风险
- **违反规范**: AGENTS.md 安全红线明确禁止生产环境包含 carlos-spring-boot-starter-license-generate
- **触发条件**: 生产部署时
- **潜在风险**: 私钥泄露、证书伪造、授权绕过

#### 解决方案

**推荐修复**:
1. 从 `carlos-license/pom.xml` 中移除 `carlos-spring-boot-starter-license-generate` 模块
2. 创建独立的 `carlos-license-generate` 项目，仅用于开发环境
3. 在 CI/CD 中检查生产包是否包含该模块

```xml
<!-- 生产环境正确的配置 -->
<modules>
    <module>carlos-license-core</module>
    <module>carlos-spring-boot-starter-license-verify</module>
</modules>
```

**短期规避**:
在部署脚本中排除该模块：
```bash
mvn clean install -pl !carlos-spring-boot-starter-license-generate
```

**长期建议**:
- 将 license-generate 拆分为独立仓库
- 添加 Maven Enforcer 插件检查生产包合规性

---

### 问题 12: carlos-auth 登录接口缺少方法级权限控制

**模块**: `carlos-auth-service`
**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/web/*.java`
**严重程度**: P1-High
**影响范围**: 模块级

#### 详细分析

- **问题描述**: `carlos-auth-service` 的 Controller 层（除 MFA 控制器外）缺少 `@PreAuthorize` 注解，方法级安全控制不足
- **违反规范**: 安全规范要求敏感操作必须有方法级权限控制
- **触发条件**: 任何认证相关请求
- **潜在风险**: 未授权访问、权限提升

#### 解决方案

**推荐修复**:
在敏感接口上添加权限注解：
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/password/reset")
public Result<Void> resetPassword(@RequestBody ResetPasswordParam param) {
    // ...
}
```

---

## 七、功能未完善详情

### 问题 13: carlos-auth 缺少 Refresh Token 轮换机制

**模块**: `carlos-auth-service`
**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2TokenConfig.java`
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 详细分析

- **问题描述**: OAuth2 Token 配置中未实现 Refresh Token 轮换（Rotation），长期 Refresh Token 被盗用后无法撤销
- **触发条件**: Refresh Token 泄露
- **潜在风险**: 会话劫持、长期未授权访问

#### 解决方案

**推荐修复**:
1. 配置 Refresh Token 轮换策略
2. 每次刷新 Access Token 时生成新的 Refresh Token
3. 旧的 Refresh Token 加入黑名单

```java
@Bean
public OAuth2TokenGenerator<?> tokenGenerator(JwtEncoder jwtEncoder) {
    JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);
    jwtGenerator.setRefreshTokenGenerator(new OAuth2RefreshTokenGenerator());
    // 启用轮换
    return new DelegatingOAuth2TokenGenerator(jwtGenerator, new OAuth2RefreshTokenGenerator());
}
```

---

### 问题 14: carlos-system 区域管理缓存失效策略不完善

**模块**: `carlos-system-bus`
**文件**: `carlos-system-bus/src/main/java/com/carlos/system/region/manager/impl/SysRegionManagerImpl.java`
**行号**: L417, L423
**严重程度**: P2-Medium
**影响范围**: 类级

#### 相关代码

```java
// FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况
// FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容
```

#### 详细分析

- **问题描述**: 区域管理修改时未处理缓存失效，可能导致脏数据
- **触发条件**: 修改区域数据后
- **潜在风险**: 缓存数据与数据库不一致

#### 解决方案

**推荐修复**:
1. 在修改操作后清除相关缓存
2. 使用 Redis 发布订阅通知其他节点

```java
@Override
public boolean modify(SysRegionDTO dto) {
    boolean success = super.modify(dto);
    if (success) {
        // 清除缓存
        RedisUtil.del("region:" + dto.getId());
        RedisUtil.del("region:tree");
    }
    return success;
}
```

---

## 八、技术债务详情

### 8.1 模块间依赖合规性

| 模块 | 违规依赖 | 规范要求 | 建议操作 |
|----|------|------|------|
| carlos-license | 包含 license-generate | 生产环境严禁 | 立即移除 |
| carlos-audit | 使用旧 Health API | 使用 Spring Boot 3.x API | 升级迁移 |

### 8.2 已弃用 API 使用

| 文件 | 行号 | 已弃用 API | 替代方案 |
|----|----|---------|------|
| `carlos-audit-bus/.../ClickHouseHealthIndicator.java` | L5-L6 | `org.springframework.boot.health.contributor.HealthIndicator` | `org.springframework.boot.actuate.health.HealthIndicator` |
| `carlos-audit-bus/.../AuditAutoConfiguration.java` | L7 | `org.springframework.boot.health.contributor.HealthIndicator` | `org.springframework.boot.actuate.health.HealthIndicator` |

### 8.3 TODO/FIXME 遗留

| 文件 | 行号 | 标记内容 | 建议 |
|----|----|--------|------|
| `carlos-audit-bus/.../AuditLogQueryService.java` | L48 | TODO: 实现 ClickHouse 查询 | 立即实现 |
| `carlos-audit-bus/.../AuditLogQueryService.java` | L69 | TODO: 实现查询 | 立即实现 |
| `carlos-audit-bus/.../AuditLogQueryService.java` | L83 | TODO: 从物化视图查询统计 | 排期实现 |
| `carlos-audit-bus/.../AuditLogQueryService.java` | L117 | TODO: 实现查询 | 立即实现 |
| `carlos-system-bus/.../SysRegionManagerImpl.java` | L417 | FIXME: 缓存失效 | 本周修复 |
| `carlos-system-bus/.../SysRegionManagerImpl.java` | L423 | FIXME: 层级变更 | 本周修复 |
| `carlos-system-bus/.../SysRegionService.java` | L356 | FIXME: 方式优化 | 排期优化 |
| `carlos-system-bus/.../SysConfigService.java` | L130 | TODO: 文件类型转换 | 排期实现 |
| `carlos-message-bus/.../MessageSendApiImpl.java` | L84 | 批量发送功能待实现 | 立即实现或移除 |
| `carlos-auth-service/.../KeyPairManager.java` | L211 | TODO: 引入 BouncyCastle | 排期实现 |
| `carlos-auth-service/.../CaptchaController.java` | L156 | TODO: 自动注册 | 排期实现 |
| `carlos-license/.../LicenseCreatorController.java` | L32 | TODO: 接口替代 | 排期实现 |
| `carlos-tools` | 多处 | 大量 TODO | 统一清理 |

### 8.4 方法级安全缺失统计

| 模块 | 控制器数量 | 有 @PreAuthorize 数量 | 缺失率 |
|-----|----------|---------------------|-------|
| carlos-auth | 5 | 1 | 80% |
| carlos-org | 4 | 0 | 100% |
| carlos-system | 6 | 0 | 100% |
| carlos-message | 3 | 0 | 100% |
| carlos-audit | 2 | 0 | 100% |

> 注：MFA 控制器有 5 个 `@PreAuthorize("isAuthenticated()")`，但其他业务操作接口普遍缺失。

---

## 九、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|-----|--------|--------|--------|--------|--------|---------|
| carlos-auth | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 3.0/5.0 |
| carlos-gateway | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.6/5.0 |
| carlos-audit | ⭐⭐☆☆☆ | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | 2.4/5.0 |
| carlos-message | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 2.8/5.0 |
| carlos-org | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.2/5.0 |
| carlos-system | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 2.8/5.0 |
| carlos-license | ⭐⭐☆☆☆ | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | 2.4/5.0 |
| carlos-tools | ⭐⭐☆☆☆ | ⭐⭐☆☆☆ | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | 2.2/5.0 |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改

---

## 十、待办事项与修复计划

### 10.1 今日新增待办

- [ ] [P0] carlos-license: 从 `carlos-license/pom.xml` 移除 `carlos-spring-boot-starter-license-generate` 模块
- [ ] [P0] carlos-audit: 升级 `ClickHouseHealthIndicator` 使用 Spring Boot 3.x `actuate.health` API
- [ ] [P1] carlos-auth: 恢复 `BaseAuthenticationProvider` 代码或删除被注释文件
- [ ] [P1] carlos-audit: 实现 `AuditLogQueryService` 中的 4 个 TODO 查询方法
- [ ] [P1] carlos-message: 实现批量发送功能或移除未实现的 API
- [ ] [P1] carlos-message: 替换 `BeanUtils.copyProperties` 为 MapStruct Convert
- [ ] [P1] carlos-auth: 将设备指纹 MD5 升级为 SHA-256
- [ ] [P2] carlos-system: 修复区域管理缓存失效 FIXME
- [ ] [P2] carlos-auth: 实现 Refresh Token 轮换机制
- [ ] [P2] carlos-org: 为部门树全量加载添加数量限制
- [ ] [P3] 所有业务模块: 补充 README.md 和 CHANGELOG.md
- [ ] [P3] 所有业务模块: 补充 Feign 接口 JavaDoc 和 Swagger 注解

### 10.2 历史遗留跟踪

- [ ] [P0] carlos-license: 生产环境包含 license-generate 模块 ⏳ 待修复
- [ ] [P1] carlos-auth: 设备指纹使用 MD5 ⏳ 待修复
- [ ] [P1] carlos-audit: 审计查询功能未实现 ⏳ 待修复

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
