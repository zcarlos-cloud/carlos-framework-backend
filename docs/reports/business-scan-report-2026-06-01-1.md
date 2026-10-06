# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-01 08:00
**扫描 Commit**: 0d0dd51b `docs: 框架核心代码扫描报告 2026-06-01`
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）
**发现问题总数**: 16 个（P0: 0 个, P1: 2 个, P2: 8 个, P3: 6 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 成功（已是最新）
- Commit: 0d0dd51b
- 上一个代码变更 Commit: cedb8898 `feat(auth): 添加前端兼容的令牌刷新和用户信息接口`
- 今日新增代码: Auth 模块新增 `/auth/user/refreshToken` 端点和 `UserInfoResponse` DTO，前端 `auth.ts` 适配 SoybeanAdmin
- 变更文件数: 7 个（UserAuthController, UserLoginService, LoginResponse, UserInfoResponse, CommonErrorCode, auth.ts, pwd-login.vue）

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 1 | 4 | 5 |
| 需求与设计缺陷 | 0 | 0 | 1 | 0 | 1 |
| 代码规范违规 | 0 | 0 | 1 | 0 | 1 |
| 性能风险 | 0 | 0 | 1 | 0 | 1 |
| 安全漏洞 | 0 | 1 | 1 | 0 | 2 |
| 功能未完善 | 0 | 1 | 2 | 0 | 3 |
| 技术债务 | 0 | 0 | 1 | 2 | 3 |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-gateway | 0 | 0 | 0 | 0 | 0 |
| carlos-auth | 0 | 1 | 3 | 1 | 5 |
| carlos-audit | 0 | 1 | 2 | 0 | 3 |
| carlos-org | 0 | 0 | 0 | 0 | 0 |
| carlos-system | 0 | 0 | 1 | 1 | 2 |
| carlos-license | 0 | 0 | 0 | 2 | 2 |
| carlos-message | 0 | 0 | 1 | 1 | 2 |
| carlos-tools | 0 | 0 | 0 | 1 | 1 |

### 1.4 今日重点关注
- [TOP 1] `carlos-auth-service`: `refreshAccessToken` 未撤销旧刷新令牌，存在 Refresh Token 重放攻击风险（P1，安全漏洞）
- [TOP 2] `carlos-audit-bus`: `AuditLogQueryService` ClickHouse 查询功能全部 TODO 未实现（P1，历史遗留，仍未修复）
- [TOP 3] `carlos-auth-service`: `getCurrentUser` 接口 `buttons` 字段硬编码为空列表，前端权限控制失效（P2，功能缺陷）

---

## 二、安全漏洞详情

### 问题 1: refreshAccessToken 未撤销旧刷新令牌 — Refresh Token 重放攻击风险

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/login/UserLoginService.java`
**行号**: L419-L420
**严重程度**: P1-High
**影响范围**: 模块级
**问题类型**: 安全漏洞
**CWE**: CWE-384（Session Fixation）/ CWE-287（Improper Authentication）

#### 相关代码

```java
// carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/login/UserLoginService.java L419-L420
// 6. 可选：撤销旧的授权（单用刷新令牌策略）
// authorizationService.remove(authorization);

return response;
```

完整上下文（L368-L421）：

```java
public LoginResponse refreshAccessToken(String refreshTokenValue) {
    if (!StringUtils.hasText(refreshTokenValue)) {
        throw AuthErrorCode.AUTH_TOKEN_INVALID.exception("刷新令牌不能为空");
    }

    // 1. 查找刷新令牌对应的授权信息
    OAuth2Authorization authorization = authorizationService.findByToken(
        refreshTokenValue, OAuth2TokenType.REFRESH_TOKEN);

    if (authorization == null) {
        log.warn("Refresh token not found or expired");
        throw AuthErrorCode.AUTH_TOKEN_INVALID.exception("刷新令牌无效或已过期");
    }

    // ... 省略中间步骤 ...

    // 5. 生成新的令牌
    log.info("Refreshing token for user: {}", principalName);
    LoginResponse response = buildLoginResponse(user, authentication, registeredClient.getClientId());

    // 6. 可选：撤销旧的授权（单用刷新令牌策略）
    // authorizationService.remove(authorization);

    return response;
}
```

#### 详细分析

- **问题描述**: `refreshAccessToken` 方法在生成新的访问令牌和刷新令牌后，**未撤销旧的 OAuth2 授权记录**。`authorizationService.remove(authorization)` 被注释掉，且注释标记为"可选"。这违反了 OAuth 2.1 / Security Best Practice 中的 **Refresh Token Rotation** 原则。
- **攻击场景**:
  1. 用户 A 正常登录，获得 access_token_A 和 refresh_token_A
  2. 攻击者窃取 refresh_token_A（通过 XSS、日志泄露、网络嗅探等）
  3. 用户 A 在前端正常刷新令牌，获得 access_token_B 和 refresh_token_B
  4. 旧的 refresh_token_A 仍然有效，攻击者可以继续使用它获取新的访问令牌
  5. 只要 refresh_token_A 未过期（通常 7-30 天），攻击者可以持续获得有效的访问令牌
  6. 即使撤销了 access_token_A，攻击者通过 refresh_token_A 仍可获取新的访问令牌
- **触发条件**: 任何使用 `/auth/user/refreshToken` 端点刷新令牌的场景
- **潜在风险**:
  - 刷新令牌一旦泄露，无法在用户侧通过正常刷新操作使其失效
  - 攻击者可持续获得有效访问令牌，直到原始刷新令牌自然过期
  - 与 "单用刷新令牌策略"（Single-Use Refresh Token）安全最佳实践相违背
  - `buildLoginResponse` 中调用 `saveAuthorization` 会创建新的授权记录（以新的 access token value 为 ID），但旧的授权记录仍然完整存在于 Redis 中（包含旧的 access token、refresh token、state、code 索引）

#### 解决方案

**推荐修复**:

```java
public LoginResponse refreshAccessToken(String refreshTokenValue) {
    // ... 省略验证步骤 ...

    // 5. 生成新的令牌
    log.info("Refreshing token for user: {}", principalName);
    LoginResponse response = buildLoginResponse(user, authentication, registeredClient.getClientId());

    // 6. 撤销旧的授权（单用刷新令牌策略 — 必须执行）
    // 旧的 refresh_token 使用后立即失效，防止重放攻击
    try {
        authorizationService.remove(authorization);
        log.debug("Revoked old authorization after refresh: authId={}", authorization.getId());
    } catch (Exception e) {
        log.error("Failed to revoke old authorization after refresh: authId={}", authorization.getId(), e);
        // 撤销失败属于安全关键错误，应抛出异常阻止新令牌颁发
        throw AuthErrorCode.AUTH_TOKEN_REVOKE_FAILED.exception("令牌刷新失败，请重试");
    }

    return response;
}
```

**短期规避**:
- 缩短刷新令牌有效期（如从 30 天改为 7 天），减少泄露窗口
- 在网关层增加刷新令牌使用频率监控，异常频率触发告警
- 启用 IP 绑定：记录刷新令牌首次使用 IP，后续使用 IP 不一致时拒绝刷新（需扩展 `RedisOAuth2AuthorizationService`）

**长期建议**:
- 实现完整的 Refresh Token Rotation：每次刷新后颁发新的 refresh token，旧的立即失效
- 考虑引入 Refresh Token 家族（Refresh Token Family）机制，检测令牌重用并撤销整个家族
- 在 `OAuth2Properties` 中增加 `refreshToken.singleUse=true` 配置开关，默认启用
- 参考 [OAuth 2.1 Security Best Current Practice § 4.13.2](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics-19#section-4.13.2)

---

## 三、功能未完善详情

### 问题 2: AuditLogQueryService ClickHouse 查询功能全部 TODO 未实现

**模块**: `carlos-audit-bus`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`
**行号**: L45-L120
**严重程度**: P1-High
**影响范围**: 模块级
**问题类型**: 功能缺失
**状态**: ⏳ 历史遗留（2026-05-31 报告 P1-001），仍未修复

#### 相关代码

```java
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogMainManager auditLogMainManager;

    public Paging<AuditLogMainDTO> pageQuery(String tenantId, int pageNum, int pageSize,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             String logType, String logStatus) {
        // TODO: 实现 ClickHouse 查询
        return new Paging<>();
    }

    public AuditLogMainDTO getById(Long id) {
        // TODO: 实现查询
        return null;
    }

    public AuditLogStatsVO getRealtimeStats(String tenantId, LocalDate date) {
        // TODO: 从物化视图查询统计
        return new AuditLogStatsVO();
    }

    public List<AuditLogMainDTO> listRiskEvents(String tenantId, int minRiskLevel,
                                                LocalDateTime startTime, LocalDateTime endTime) {
        // TODO: 实现查询
        return Collections.emptyList();
    }
}
```

#### 详细分析

- **问题描述**: `AuditLogQueryService` 是审计模块的核心查询服务，但所有 4 个查询方法均为 TODO 空实现。审计模块写入功能完善（ClickHouseBatchWriter 设计优秀），但查询功能完全不可用，形成"只写不读"的半残模块。
- **影响功能**: 审计日志分页查询、单条详情、实时统计、风险事件查询全部不可用
- **触发条件**: 任何调用审计查询接口的场景
- **潜在风险**: Dashboard 审计数据为空、安全运营无法查看风险事件、合规审计无法通过

#### 解决方案

**推荐修复**: 使用 ClickHouse JDBC 实现查询，引入物化视图优化统计查询。参考昨日报告中的修复建议。

**短期规避**: 提供 MySQL 兜底查询（如果审计数据同时写入 MySQL）

**参考**: 昨日报告 P1-001 详细方案

---

### 问题 3: getCurrentUser 接口 buttons 字段硬编码为空，前端权限控制失效

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/login/UserAuthController.java`
**行号**: L297-L316（变更后）
**严重程度**: P2-Medium
**影响范围**: 模块级
**问题类型**: 功能缺陷

#### 相关代码

```java
@GetMapping("/me")
public Result<UserInfoResponse> getCurrentUser() {
    // ... 省略认证检查 ...

    if (principal instanceof com.carlos.auth.security.SecurityUser securityUser) {
        List<String> roles = securityUser.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .toList();

        UserInfoResponse userInfoResponse = UserInfoResponse.builder()
            .userId(String.valueOf(securityUser.getId()))
            .userName(securityUser.getAccount())
            .roles(roles)
            .buttons(Collections.emptyList())  // <-- 硬编码为空
            .build();

        return Result.success(userInfoResponse);
    }

    // 降级处理：兼容标准 UserDetails
    if (principal instanceof UserDetails userDetails) {
        // ... 同样 .buttons(Collections.emptyList())
    }
}
```

#### 详细分析

- **问题描述**: `UserInfoResponse.buttons` 字段被硬编码为 `Collections.emptyList()`。该字段适配 SoybeanAdmin 前端框架的 `Api.Auth.UserInfo` 类型中的 `buttons`（按钮权限列表）。SoybeanAdmin 前端路由守卫和按钮级权限控制依赖此字段判断用户是否有权访问特定页面/按钮。
- **影响**: 所有通过 `/auth/user/me` 获取用户信息的用户，前端显示为"无任何按钮权限"，导致：
  - 所有需要按钮权限判断的 UI 元素（新增、编辑、删除按钮）被隐藏
  - 前端路由守卫可能误判用户无权访问某些页面
- **触发条件**: 登录后前端调用 `/auth/user/me` 获取用户信息
- **潜在风险**: 前端权限控制完全失效，用户界面功能按钮全部消失

#### 解决方案

**推荐修复**:

```java
// 从 SecurityUser 或 UserDetailsService 加载用户完整权限（包括按钮权限）
UserInfoResponse userInfoResponse = UserInfoResponse.builder()
    .userId(String.valueOf(securityUser.getId()))
    .userName(securityUser.getAccount())
    .roles(roles)
    .buttons(loadButtonPermissions(securityUser.getId()))  // 从权限服务加载
    .build();

// 辅助方法
private List<String> loadButtonPermissions(Serializable userId) {
    // 方式1：从 SecurityUser 的 authorities 中过滤出按钮权限（通常以 "btn:" 前缀）
    // 方式2：调用权限服务查询用户的按钮权限列表
    // 方式3：从 Redis 缓存的用户权限树中提取
    return securityUser.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(auth -> auth.startsWith("btn:"))
        .collect(Collectors.toList());
}
```

**短期规避**: 如果当前系统不依赖按钮级权限，可暂时忽略；但必须在 `UserInfoResponse` JavaDoc 中注明 "buttons 字段暂未实现，前端按钮权限控制将失效"。

**长期建议**:
- 在 `SecurityUser` 中增加 `buttonPermissions` 字段，在认证时一并加载
- 或通过 `PermissionProvider` / `CachedPermissionProvider` 从 Redis 缓存中查询用户的按钮权限编码列表

---

## 四、代码规范违规详情

### 问题 4: UserAuthController.refreshToken 异常处理过于宽泛

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/login/UserAuthController.java`
**行号**: L198-L209
**严重程度**: P2-Medium
**影响范围**: 类级
**问题类型**: 代码规范违规 / 安全

#### 相关代码

```java
@PostMapping("/refreshToken")
public Result<LoginResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
    log.info("Received refresh token request");
    try {
        LoginResponse loginResponse = loginService.refreshAccessToken(refreshToken);
        log.info("Token refreshed successfully for user");
        return Result.success(loginResponse);
    } catch (Exception e) {
        log.error("Failed to refresh token", e);
        return Result.<LoginResponse>error(CommonErrorCode.AUTH_TOKEN_EXPIRED, "刷新令牌无效或已过期");
    }
}
```

#### 详细分析

- **问题描述**: `catch (Exception e)` 捕获所有异常（包括 `NullPointerException`、`OutOfMemoryError`、`DataAccessException` 等），统一返回"刷新令牌无效或已过期"。这违反了框架异常处理规范：应使用框架定义的异常类（`BusinessException`、`SystemException`）区分业务异常和系统异常。
- **风险**:
  - 内部系统错误（如 Redis 连接失败、数据库异常）被掩盖为"令牌过期"，误导运维排查
  - 攻击者可能通过构造异常输入触发内部错误，获取错误信息差
  - 无法区分"令牌确实无效"和"系统故障"

#### 解决方案

```java
@PostMapping("/refreshToken")
public Result<LoginResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
    log.info("Received refresh token request");
    try {
        LoginResponse loginResponse = loginService.refreshAccessToken(refreshToken);
        log.info("Token refreshed successfully");
        return Result.success(loginResponse);
    } catch (BusinessException e) {
        // 业务异常（令牌无效、用户不存在等）— 直接返回给客户端
        log.warn("Token refresh failed: {}", e.getMessage());
        return Result.<LoginResponse>error(e.getErrorCode(), e.getMessage());
    } catch (SystemException e) {
        // 系统异常 — 记录详细日志，返回通用错误
        log.error("System error during token refresh", e);
        return Result.<LoginResponse>error(CommonErrorCode.INTERNAL_ERROR, "令牌刷新服务暂时不可用");
    } catch (Exception e) {
        // 未知异常 — 兜底，但不应经常触发
        log.error("Unexpected error during token refresh", e);
        return Result.<LoginResponse>error(CommonErrorCode.INTERNAL_ERROR, "系统异常");
    }
}
```

---

## 五、性能风险详情

### 问题 5: AuditLogQueryService 统计查询性能风险

**模块**: `carlos-audit-bus`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`
**行号**: L80-L95
**严重程度**: P2-Medium
**影响范围**: 模块级
**问题类型**: 性能风险
**状态**: ⏳ 历史遗留（2026-05-31 报告 P1-002），仍未修复

#### 详细分析

- **问题描述**: `getRealtimeStats` 和 `listRiskEvents` 方法即使实现后，直接对 ClickHouse 的 `audit_log_main_local` 大表做聚合查询和范围扫描，性能风险高。
- **触发条件**: 审计日志量大时（日百万级），统计查询响应时间可能超过 10 秒
- **潜在风险**: 慢查询拖垮 ClickHouse 节点，影响审计日志写入性能

#### 解决方案

使用 ClickHouse 物化视图（Materialized View）预聚合统计数据。参考昨日报告中的 SQL 建议。

---

## 六、需求与设计缺陷详情

### 问题 6: CustomGrantTypes 密码授权类型与文档说明矛盾

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/grant/CustomGrantTypes.java`
**行号**: L25-L45
**严重程度**: P2-Medium
**影响范围**: 类级
**问题类型**: 设计缺陷
**状态**: ⏳ 历史遗留（2026-05-31 报告 P2-004），仍未修复

#### 详细分析

- **问题描述**: 类文档注释明确说明"密码授权类型（password）已被移除"，但代码中仍定义了 `PASSWORD` 常量。
- **风险**: 密码授权类型在 OAuth 2.1 中已被弃用，继续使用违反安全最佳实践

#### 解决方案

将 `PASSWORD` 标记为 `@Deprecated` 并添加明确警告。参考昨日报告方案。

---

## 七、文档缺失详情

### 问题 7: carlos-audit 模块缺少 README.md

**模块**: `carlos-audit`
**严重程度**: P2-Medium
**影响范围**: 模块级
**状态**: ⏳ 历史遗留（2026-05-31 报告 P2-005），仍未修复

- **缺失文件**: `carlos-integration/carlos-audit/README.md`
- **应有内容**: 模块简介、ClickHouse 表结构、Disruptor 配置、写入/查询架构说明、部署依赖

### 问题 8: carlos-message 模块缺少设计文档

**模块**: `carlos-message`
**严重程度**: P3-Low
**影响范围**: 模块级
**状态**: ⏳ 历史遗留（2026-05-31 报告 P3-007），仍未修复

- **缺失文件**: `carlos-integration/carlos-message/docs/design/`
- **影响**: 消息模块的多渠道路由、模板渲染、限流防刷等核心设计未文档化

### 问题 9: carlos-gateway WafFilter 规则缺少维护文档

**模块**: `carlos-gateway`
**严重程度**: P3-Low
**影响范围**: 模块级
**状态**: ⏳ 历史遗留（2026-05-31 报告 P2-006），仍未修复

- **问题**: WafFilter 的正则规则缺乏维护文档，规则更新历史、误报处理流程未记录

### 问题 10: carlos-license 模块缺少 README

**模块**: `carlos-license`
**严重程度**: P3-Low
**影响范围**: 模块级

- **缺失文件**: `carlos-integration/carlos-license/README.md`
- **应有内容**: 证书生成流程、验证逻辑、公钥安全说明、生产环境注意事项

---

## 八、技术债务详情

### 8.1 TODO/FIXME 遗留

| 文件 | 行号 | 标记内容 | 严重程度 | 建议 |
|----|----|--------|---------|------|
| `carlos-system-bus/.../SysRegionManagerImpl.java` | L283 | `FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况` | P2 | 添加缓存失效兜底：从数据库重建 |
| `carlos-system-bus/.../SysRegionManagerImpl.java` | L295 | `FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容` | P2 | 层级变更时重建全部缓存关系 |
| `carlos-auth-service/.../LicenseCreatorController.java` | L67 | `TODO Carlos 2025/04/10` | P3 | 实现或移除 TODO |

**状态**: 3 处 FIXME/TODO 均未修复。其中 SysRegionManagerImpl 2 处 FIXME 已遗留 6 个月。

### 8.2 模块间依赖合规性

经扫描，所有业务模块的依赖方向合规：
- `-api` 模块不依赖 `-bus`
- `-cloud` 模块仅依赖 `-api`
- `-boot` 模块正确聚合子模块

### 8.3 已弃用 API 使用

未发现 `javax.*` 包残留或已弃用 Spring API 使用。

---

## 九、业务模块健康度评分

| 模块          | 文档完整  | 需求合理  | 规范落实  | 性能安全  | 功能完整  | 综合评分    |
|-------------|-------|-------|-------|-------|-------|---------|
| carlos-gateway | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.7/5.0 |
| carlos-auth | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-audit | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐☆☆☆ | 3.4/5.0 |
| carlos-org | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.7/5.0 |
| carlos-system | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.3/5.0 |
| carlos-license | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.5/5.0 |
| carlos-message | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-tools | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改
>
> **auth 模块评分下降原因**: 今日新增的 `refreshAccessToken` 存在安全漏洞（未撤销旧刷新令牌），`getCurrentUser` buttons 硬编码为空，异常处理过宽。安全维度从 4 星降至 3 星。

---

## 十、待办事项与修复计划

### 10.1 今日新增待办

- [ ] **P1-011** `carlos-auth-service`: `refreshAccessToken` 启用 Refresh Token Rotation — 使用后必须撤销旧授权（`authorizationService.remove(authorization)`）
- [ ] **P2-012** `carlos-auth-service`: `UserAuthController.refreshToken` 精细化异常处理 — 区分 `BusinessException` / `SystemException` / 未知异常
- [ ] **P2-013** `carlos-auth-service`: `getCurrentUser` 实现 `buttons` 字段权限加载 — 从 `SecurityUser.authorities` 过滤 `btn:` 前缀权限，或调用权限服务查询
- [ ] **P2-014** `carlos-auth-service`: `refreshToken` 端点改用 `@RequestBody` 接收 refreshToken，避免 URL 参数泄露风险
- [ ] **P3-015** `carlos-auth-service`: `LoginResponse.UserInfo.phone` 增加脱敏处理

### 10.2 历史遗留跟踪

- [ ] **P1-001** `carlos-audit-bus`: 实现 `AuditLogQueryService` 的 ClickHouse 查询功能 ⏳ 未修复
- [ ] **P1-002** `carlos-audit-bus`: 设计 ClickHouse 物化视图用于统计查询 ⏳ 未修复
- [ ] **P2-003** `carlos-system-bus`: 修复 `SysRegionManagerImpl` 两处 FIXME ⏳ 未修复
- [ ] **P2-004** `carlos-auth-service`: 更新 `CustomGrantTypes` 文档注释或标记 `@Deprecated` ⏳ 未修复
- [ ] **P2-005** `carlos-audit`: 补充模块 README.md ⏳ 未修复
- [ ] **P2-006** `carlos-gateway`: 补充 WafFilter 规则维护文档 ⏳ 未修复
- [ ] **P3-007** `carlos-message`: 补充多渠道路由和模板渲染的设计文档 ⏳ 未修复

---

## 十一、扫描结论

**整体评价**: Carlos Framework 业务集成层整体架构设计**优秀**，安全基线**基本达标**，代码规范**良好**。但今日新增代码引入了安全漏洞，需要立即修复。

**关键结论**:
1. ❌ **新增安全风险**: `refreshAccessToken` 未撤销旧刷新令牌，存在 Refresh Token 重放攻击风险（P1-011，需立即修复）
2. ⚠️ **新增功能缺陷**: `getCurrentUser` 接口 `buttons` 硬编码为空列表，前端权限控制失效（P2-013）
3. ✅ Gateway 安全过滤器设计优秀：WafFilter（ThreadLocal Matcher + BitSet）、ReplayProtectionFilter（HMAC-SHA256 + ThreadLocal Mac）
4. ✅ Auth 模块安全配置完善：BCrypt 唯一编码器、CSRF 保护、RSA 密钥管理
5. ✅ Org 模块五层 Redis 缓存设计优秀，分页初始化 + Pipeline 批量操作
6. ✅ Audit 写入模块 ClickHouseBatchWriter 设计优秀：双缓冲 + VirtualThread + 防御性背压
7. ⚠️ Audit 查询模块 `AuditLogQueryService` 4 个方法全部 TODO 空实现，核心功能缺失（P1-001，历史遗留，仍未修复）
8. ⚠️ System 区域管理 2 处 FIXME 遗留 6 个月未处理（P2-003）
9. ⚠️ 多个业务模块缺少 README 和设计文档
10. ✅ License 模块 XMLDecoder 有白名单保护，生产环境不含 license-generate

**下一步建议**:
1. **立即修复** `refreshAccessToken` 的 Refresh Token Rotation（取消注释 `authorizationService.remove(authorization)`）
2. **本周内** 实现 `AuditLogQueryService` 的 ClickHouse 查询功能（P1-001）
3. **本周内** 修复 `getCurrentUser` 的 `buttons` 字段权限加载（P2-013）
4. **本月内** 清理 SysRegionManagerImpl FIXME（P2-003）和 CustomGrantTypes 文档矛盾（P2-004）
5. **持续跟踪** 补充各业务模块 README 和设计文档

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
*扫描工具: OpenClaw Agent / kimi-k2p6 / 2026-06-01*
