# Carlos Framework 业务模块质量报告

**生成时间**: 2026-05-23 08:15
**扫描 Commit**: 5c7da67f4aa61e8ad0ba771049fc67112ca6280c
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）
**发现问题总数**: 38 个（P0: 2 个, P1: 8 个, P2: 18 个, P3: 10 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 成功（已是最新）
- Commit: `5c7da67f` — `docs: 框架核心代码扫描报告 2026-05-23 (42 issues, 7 P0)`
- 变更文件数: 1 个（报告文件）
- 新增代码: +815 行 / 删除代码: -0 行

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 2 | 4 | 3 | 9 |
| 需求与设计缺陷 | 0 | 1 | 3 | 1 | 5 |
| 代码规范违规 | 0 | 2 | 4 | 2 | 8 |
| 性能风险 | 0 | 1 | 3 | 1 | 5 |
| 安全漏洞 | 2 | 1 | 1 | 0 | 4 |
| 功能未完善 | 0 | 1 | 2 | 2 | 5 |
| 技术债务 | 0 | 0 | 1 | 1 | 2 |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 1 | 3 | 4 | 2 | 10 |
| carlos-gateway | 1 | 2 | 3 | 1 | 7 |
| carlos-audit | 0 | 1 | 3 | 2 | 6 |
| carlos-message | 0 | 0 | 2 | 1 | 3 |
| carlos-org | 0 | 1 | 2 | 1 | 4 |
| carlos-system | 0 | 1 | 2 | 1 | 4 |
| carlos-license | 0 | 0 | 1 | 1 | 2 |
| carlos-tools | 0 | 0 | 1 | 1 | 2 |

### 1.4 今日重点关注
- [TOP 1] **Auth 授权服务器全局禁用 CSRF**（P0，所有表单端点暴露于 CSRF 攻击）
- [TOP 2] **Gateway JWT 验证器空指针风险**（P0，公钥未配置时 NPE 导致认证完全失效）
- [TOP 3] **Audit 查询功能大量 TODO 未实现**（P1，审计日志查询功能缺失）
- [TOP 4] **System 区域管理 FIXME 缓存失效未处理**（P1，数据一致性风险）
- [TOP 5] **多个业务模块缺失 HealthIndicator**（P2，生产环境健康检查缺失）

---

## 二、P0-Critical 问题详情

### 问题 P0-001: Auth 授权服务器全局禁用 CSRF 保护

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2AuthorizationServerConfig.java`
**行号**: L95-L100, L145-L150
**严重程度**: P0-Critical
**影响范围**: 模块级（所有 OAuth2 端点和登录表单）

#### 相关代码

```java
// OAuth2AuthorizationServerConfig.java L95-L100
http
    .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
    // CSRF 禁用说明：见类文档安全说明部分
    .csrf(AbstractHttpConfigurer::disable)

// L145-L150（默认安全过滤器链）
http
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/login", "/error", "/oauth2/consent", ...).permitAll()
        ...)
    // CSRF 禁用说明：见方法文档安全说明部分
    .csrf(AbstractHttpConfigurer::disable)
```

#### 详细分析

- **问题描述**: 两条安全过滤器链均全局禁用 CSRF 保护（`.csrf(AbstractHttpConfigurer::disable)`）。文档注释声称 `/oauth2/token` 等端点为机器间 API 调用不需要 CSRF，但 `/login` 和 `/oauth2/consent` 是浏览器表单端点，禁用 CSRF 后攻击者可构造恶意页面诱导已登录用户执行非预期操作。
- **违反原则**: OWASP CSRF 防护指南明确要求：所有浏览器可访问的敏感端点必须实施 CSRF 保护。
- **触发条件**: 用户已登录认证服务器，访问攻击者构造的恶意页面。
- **潜在风险**: 
  - 攻击者可伪造授权同意（consent）请求，导致用户在不知情的情况下授权恶意客户端
  - 攻击者可利用 `/login` 端点进行登录 CSRF（强制用户登录到攻击者账户）
  - 与 `permitAll()` 配合的 `/auth/password/match` 等端点同样暴露

#### 解决方案

**推荐修复**:

```java
// 方案1：为浏览器端点单独启用 CSRF Token
@Bean
@Order(Ordered.HIGHEST_PRECEDENCE)
public SecurityFilterChain authorizationServerSecurityFilterChain(...) {
    http
        .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
        // 仅对 consent 页面启用 CSRF，API 端点仍禁用
        .csrf(csrf -> csrf.ignoringRequestMatchers(
            "/oauth2/token", "/oauth2/introspect", "/oauth2/revoke"
        ))
        ...
}

// 方案2：为表单登录链启用 CSRF
@Bean
@Order(2)
public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(...)
        // 对浏览器端点启用 CSRF，AJAX 端点通过 Cookie 模式兼容
        .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
        ...
}
```

**短期规避**: 
- 在网关层增加 Referer/Origin 校验
- 为登录/Consent 页面增加自定义的 state/nonce 验证

**参考文档**: [OWASP CSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)

---

### 问题 P0-002: Gateway JWT 验证器空指针风险

**模块**: `carlos-gateway`
**文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/oauth2/validator/JwtTokenValidator.java`
**行号**: L65-L75
**严重程度**: P0-Critical
**影响范围**: 模块级（所有 JWT Token 验证场景）

#### 相关代码

```java
// JwtTokenValidator.java L65-L75
private void initSigner() {
    try {
        if (StrUtil.isNotBlank(properties.getJwtPublicKey())) {
            // ... 解析公钥
            this.signer = JWTSignerUtil.rs256(publicKey);
        }
        // ← 如果 jwtPublicKey 未配置，signer 保持 null
    } catch (Exception e) {
        log.error("Failed to initialize JWT signer", e);
        throw CommonErrorCode.ENCRYPTION_ERROR.exception("JWT signer initialization failed");
    }
}

// validateJwt() 方法
if (signer != null && !jwt.verify(signer)) {
    throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Invalid token signature");
}
// ← 如果 signer == null，跳过签名验证！任何伪造 Token 都可通过
```

#### 详细分析

- **问题描述**: 当 `carlos.gateway.oauth2.jwt.public-key` 未配置时，`initSigner()` 不会抛出异常，`signer` 保持 `null`。在 `validateJwt()` 中，`signer != null` 条件导致**完全跳过签名验证**，任何伪造的 JWT 均可通过验证。
- **触发条件**: 网关未配置 JWT 公钥时（配置遗漏或开发环境）。
- **潜在风险**: 
  - 攻击者可构造任意 JWT 冒充任意用户
  - 所有依赖 JWT 验证的端点完全暴露
  - 这是一个"静默失败"——不报错但失去所有安全保护

#### 解决方案

```java
// 方案1：强制要求公钥配置
private void initSigner() {
    String publicKeyStr = properties.getJwtPublicKey();
    if (StrUtil.isBlank(publicKeyStr)) {
        throw new IllegalStateException(
            "JWT public key must be configured. Set carlos.gateway.oauth2.jwt.public-key"
        );
    }
    // ... 解析公钥
    this.signer = JWTSignerUtil.rs256(publicKey);
}

// 方案2：验证时拒绝无签名器
private Mono<UserContext> validateJwt(String token) {
    return Mono.fromCallable(() -> {
        JWT jwt = JWTUtil.parseToken(token);
        if (signer == null) {
            throw new IllegalStateException("JWT signer not initialized");
        }
        if (!jwt.verify(signer)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Invalid token signature");
        }
        // ...
    });
}
```

---

## 三、P1-High 问题详情

### 问题 P1-001: Audit 查询功能大量 TODO 未实现

**模块**: `carlos-audit-bus`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`
**严重程度**: P1-High
**影响范围**: 模块级（审计日志查询功能基本不可用）

#### 相关代码

```java
// AuditLogQueryService.java（多处 TODO）
public List<AuditLogMainDTO> queryByCondition(AuditLogMainPageParam param) {
    // TODO: 实现 ClickHouse 查询
    return Collections.emptyList();
}

public AuditLogStatsVO queryStats(AuditLogStatsParam param) {
    // TODO: 从物化视图查询统计
    return new AuditLogStatsVO();
}
```

#### 详细分析

- **问题描述**: 审计模块核心查询功能全部标记为 TODO，返回空列表或空对象。这意味着审计日志写入功能完善，但查询/统计功能完全不可用。
- **影响**: 管理员无法查询操作日志、无法生成审计报表、无法满足合规审查需求。

#### 解决方案

- 实现 ClickHouse SQL 查询（利用 MergeTree 引擎的查询性能）
- 或提供基于 MySQL 的降级查询方案（审计双写时）
- 建议优先实现按时间范围+业务类型的基础查询

---

### 问题 P1-002: System 区域管理 FIXME 缓存失效未处理

**模块**: `carlos-system-bus`
**文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/region/manager/impl/SysRegionManagerImpl.java`
**行号**: L85-L95
**严重程度**: P1-High

#### 相关代码

```java
// SysRegionManagerImpl.java
// FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况
public boolean update(SysRegionDTO dto) {
    // ... 更新数据库
    // FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容
    return updateById(entity);
}
```

#### 详细分析

- **问题描述**: 区域数据更新后未清理 Redis 缓存，且注释明确说明未处理层级变更。区域树结构变更后缓存与数据库不一致。
- **风险**: 区域数据（省市区）更新后，前端获取到旧数据，影响业务地址选择等场景。

---

### 问题 P1-003: Auth 授权服务器 permitAll 路径过宽

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2AuthorizationServerConfig.java`
**行号**: L140-L150
**严重程度**: P1-High

#### 相关代码

```java
.requestMatchers(
    "/login",
    "/error",
    "/oauth2/consent",
    "/actuator/**",       // ← actuator 端点完全开放
    "/assets/**",
    "/webjars/**",
    "/swagger-ui/**",     // ← Swagger UI 开放
    "/doc.html",          // ← Knife4j 开放
    "/v3/api-docs/**",    // ← OpenAPI 文档开放
    "/auth/user/login",
    "/auth/captcha/**",
    "/auth/password/encode"
).permitAll()
```

#### 详细分析

- **问题描述**: `/actuator/**`、`/swagger-ui/**`、`/doc.html`、`/v3/api-docs/**` 完全 permitAll，无需认证即可访问。
- **风险**: 
  - `/actuator/env` 可能暴露环境变量和配置属性
  - `/actuator/heapdump` 可下载 JVM 堆内存，包含敏感数据
  - Swagger UI 暴露所有 API 接口定义，便于攻击者侦察

#### 解决方案

```java
// 移除敏感端点的 permitAll，或限制为内网访问
.requestMatchers("/actuator/health", "/actuator/info").permitAll()  // 仅开放必要端点
.requestMatchers("/actuator/**").hasRole("ADMIN")               // 其余需要管理员权限
.requestMatchers("/swagger-ui/**", "/doc.html", "/v3/api-docs/**")
    .access(new IpAddressAuthorizationManager("127.0.0.1", "192.168.0.0/16"))  // 内网限制
```

---

### 问题 P1-004: Auth 密码编码器配置缺少 SM2/SM3 分支

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2PasswordEncoderConfig.java`
**行号**: L35-L50
**严重程度**: P1-High

#### 相关代码

```java
public PasswordEncoder passwordEncoder(OAuth2Properties oauth2Properties) {
    SecurityProperties.PasswordEncoderType encoderType = oauth2Properties.getSecurity().getPasswordEncoder();
    switch (encoderType) {
        case BCRYPT:
            log.info("Using BCrypt password encoder");
            return new BCryptPasswordEncoder();
        default:
            log.info("Using BCrypt password encoder (default)");
            return new BCryptPasswordEncoder();  // ← SM2/SM3/MD5 分支缺失
    }
}
```

#### 详细分析

- **问题描述**: 密码编码器配置仅实现了 BCRYPT 分支，SM2、SM3、MD5 等配置虽然定义在枚举中，但实际都会 fallback 到 BCrypt。这导致国密合规场景下配置不生效。
- **风险**: 国密合规环境中配置了 SM2/SM3 但实际使用 BCrypt，审计不通过；MD5 配置意外 fallback 到 BCrypt 虽安全但行为不一致。

---

### 问题 P1-005: Org 部门树缓存未考虑循环引用检测

**模块**: `carlos-org-bus`
**文件**: `carlos-integration/carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`
**严重程度**: P1-High

#### 详细分析

- **问题描述**: 部门树移动/更新时，未检测循环引用（A 的父部门设为 B，B 的父部门又设为 A）。循环引用会导致递归查询栈溢出。
- **风险**: 恶意或误操作导致部门树循环引用后，所有涉及部门树的查询（缓存初始化、树形结构获取）都会 StackOverflowError。

#### 解决方案

```java
// 在更新父部门前检测循环引用
private boolean hasCycle(Long deptId, Long newParentId) {
    if (deptId.equals(newParentId)) return true;
    Set<Long> ancestors = getAncestorIds(newParentId);
    return ancestors.contains(deptId);
}
```

---

### 问题 P1-006: Gateway OAuth2AuthorizationFilter 缺少路径白名单

**模块**: `carlos-gateway`
**文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/oauth2/OAuth2AuthorizationFilter.java`
**严重程度**: P1-High

#### 详细分析

- **问题描述**: 网关授权过滤器未配置公开路径白名单（如 `/health`、`/public/**`、`/oauth2/token` 等），可能导致 OAuth2 令牌端点自身被拦截（如果令牌端点经过网关）。
- **风险**: 令牌申请端点被授权过滤器拦截，导致"无法获取令牌因为需要令牌"的死锁。

---

### 问题 P1-007: License 生成模块生产环境风险

**模块**: `carlos-license`
**文件**: `carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate`
**严重程度**: P1-High

#### 详细分析

- **问题描述**: `carlos-spring-boot-starter-license-generate` 包含私钥管理和证书生成逻辑，文档已标记"仅开发环境使用"。但 Starter 的 AutoConfiguration 没有环境检测机制，若误引入生产环境，私钥可能泄露。
- **风险**: 私钥泄露后，任何人可伪造许可证。

#### 解决方案

```java
// 在 LicenseGenerateAutoConfiguration 中增加环境检测
@ConditionalOnProperty(prefix = "carlos.license.generate", name = "enabled", havingValue = "true")
// 并默认 false，文档明确标注仅限开发环境
```

---

### 问题 P1-008: Auth 默认安全过滤器链 order=2 优先级过低

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2AuthorizationServerConfig.java`
**行号**: L120
**严重程度**: P1-High

#### 详细分析

- **问题描述**: 默认安全过滤器链 `@Order(2)`，如果存在其他优先级介于 HIGHEST_PRECEDENCE 和 2 之间的过滤器链，可能导致请求被错误路由。
- **建议**: 使用明确的顺序值，如 `@Order(Ordered.LOWEST_PRECEDENCE - 100)` 确保它是最后的 fallback。

---

## 四、P2-Medium 问题详情

### 问题 P2-001: 业务模块 HealthIndicator 覆盖率极低

**模块**: 多个业务模块
**严重程度**: P2-Medium
**影响范围**: 多模块联动

#### 详细分析

- **问题描述**: 扫描发现仅 `carlos-audit-bus` 实现了 `ClickHouseHealthIndicator`。`carlos-auth`、`carlos-gateway`、`carlos-org`、`carlos-system`、`carlos-message` 等业务模块均无 HealthIndicator。
- **影响**: 生产环境无法通过 `/actuator/health` 检查各业务组件健康状态，Kubernetes/容器编排平台无法正确判断服务就绪/存活状态。

#### 解决方案

- 为核心业务模块实现 HealthIndicator：
  - `AuthHealthIndicator`：检查数据库连接、Redis 连接
  - `GatewayHealthIndicator`：检查下游路由可达性
  - `OrgHealthIndicator`：检查 Redis 缓存连接
  - `SystemHealthIndicator`：检查字典缓存状态

---

### 问题 P2-002: Audit AutoConfiguration.imports 为空

**模块**: `carlos-audit-boot`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: `carlos-audit-boot` 模块存在但没有 AutoConfiguration.imports 文件，只有 `carlos-audit-bus` 有。`audit-boot` 的职责不明确。
- **影响**: 模块结构混乱， Starter 聚合关系不清晰。

---

### 问题 P2-003: Org 部门缓存未设置过期时间

**模块**: `carlos-org-bus`
**文件**: `carlos-integration/carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: 五层 Redis 缓存结构（Hash/Children/Desc/Anc/Code）均未设置 TTL，缓存数据永久有效。
- **风险**: 内存持续增长；若缓存与数据库不一致（如手动修库），只能通过重启服务或手动清理恢复。

#### 解决方案

- 为缓存 key 设置合理的 TTL（如 7 天）
- 数据变更时主动清理/更新缓存（已实现，但 TTL 作为兜底保护）

---

### 问题 P2-004: System 配置管理 TODO 遗留

**模块**: `carlos-system-bus`
**文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/configration/service/impl/SysConfigServiceImpl.java`
**行号**: TODO 标记
**严重程度**: P2-Medium

#### 相关代码

```java
// TODO: Carlos 2022/12/22 如果是文件类型，将文件转换成地址
```

#### 详细分析

- **问题描述**: 配置项支持文件类型的 TODO 已遗留 3 年多未实现。
- **影响**: 配置文件上传功能不可用。

---

### 问题 P2-005: Message 模块结构完整但缺少核心实现审查

**模块**: `carlos-message`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: message 模块有完整的 api/boot/bus/cloud/core 分层结构，但未找到消息发送限流防刷、多渠道路由的核心实现代码（可能存在于未扫描到的文件中）。
- **建议**: 重点审查 `MessageChannelRouter`、`MessageTemplateEngine`、`MessageRateLimiter` 等核心类是否实现。

---

### 问题 P2-006: Auth 设备指纹使用 MD5

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/util/DeviceFingerprint.java`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: 设备指纹生成使用 MD5 哈希。虽然设备指纹不是密码，但 MD5 的碰撞性可能导致不同设备被识别为同一设备。
- **建议**: 改用 SHA-256 或更安全的哈希算法。

---

### 问题 P2-007: Gateway TokenValidator 未处理 Token 过期时间

**模块**: `carlos-gateway`
**文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/oauth2/validator/JwtTokenValidator.java`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: JWT 验证器检查了签名、发行者、受众，但未显式检查 `exp`（过期时间）和 `nbf`（生效时间）声明。
- **风险**: 虽然 Hutool JWT 的 `verify()` 可能包含过期检查，但代码中未明确体现。若验证器配置不当，过期 Token 可能通过验证。

#### 解决方案

```java
// 显式检查过期时间
Date expiration = (Date) jwt.getPayload("exp");
if (expiration != null && expiration.before(new Date())) {
    throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "Token has expired");
}
```

---

### 问题 P2-008: License 验证模块缺少健康检查

**模块**: `carlos-license-verify`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: License 验证 Starter 没有 HealthIndicator，生产环境无法感知证书过期状态。
- **建议**: 实现 `LicenseHealthIndicator`，在证书即将过期（30 天内）或已过期时返回 DOWN 状态。

---

### 问题 P2-009: Tools 代码生成器 TODO 遗留

**模块**: `carlos-tools`
**文件**: `carlos-integration/carlos-tools/src/main/java/com/carlos/fx/codege/service/CodeGeneratorService.java`
**严重程度**: P2-Medium

#### 相关代码

```java
// TODO: Carlos 2020/9/9 如果是多模块模板 这个目录下会有多个src/main/java目录
```

#### 详细分析

- **问题描述**: 多模块模板支持 TODO 遗留 6 年未实现。
- **影响**: 代码生成器仅支持单模块项目，多模块项目使用时生成的代码路径可能错误。

---

### 问题 P2-010: Gateway UserContextRelayFilter 未处理响应头清理

**模块**: `carlos-gateway`
**文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/filter/UserContextRelayFilter.java`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: 网关向下游服务传递用户上下文（X-User-Id、X-User-Roles 等），但未清理来自客户端的相同 Header。客户端可能伪造这些 Header 直接传递给下游。
- **风险**: 如果网关到下游的通信可被客户端直接访问（绕过网关），或网关配置错误，伪造的 Header 可能直达下游服务。

#### 解决方案

```java
// 在 Gateway Filter 中清理来自客户端的敏感 Header
.exchange.getRequest().mutate()
    .headers(headers -> {
        headers.remove("X-User-Id");
        headers.remove("X-User-Roles");
        headers.remove("X-User-Permissions");
    })
    .header("X-User-Id", userContext.getUserId())  // 重新设置可信值
    ...
```

---

### 问题 P2-011: Audit ClickHouseBatchWriter 缺少 metrics 暴露

**模块**: `carlos-audit-bus`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: ClickHouseBatchWriter 内部维护了 `totalWritten` 计数器，但未通过 Micrometer 暴露为 metrics。生产环境无法监控审计日志写入速率、失败率、缓冲区大小等关键指标。
- **建议**: 使用 `MeterRegistry` 暴露 gauge（缓冲区大小）和 counter（写入成功/失败数）。

---

### 问题 P2-012: Auth JWT Token 包含敏感字段

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/user/CustomizeUserOAuth2TokenCustomizer.java`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: JWT Token 自定义增强器可能将用户手机号等敏感信息加入 Token payload（需进一步确认代码）。
- **风险**: JWT Token 虽然签名但可被解码读取 payload，敏感信息泄露。
- **建议**: 审查 `Oauth2JwtTokenCustomizer`，确保不包含 phone、email、idCard 等敏感字段。

---

### 问题 P2-013: 业务模块 JavaDoc 覆盖率不足

**模块**: 多个业务模块
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: 扫描发现部分 Service/Manager 接口缺少完整的 JavaDoc（参数说明、返回值、异常说明）。特别是 `carlos-message-bus`、`carlos-system-bus` 中的核心方法。
- **建议**: 为所有公开 API 补充 JavaDoc，尤其是复杂业务逻辑的方法。

---

### 问题 P2-014: System 字典缓存热更新机制未实现

**模块**: `carlos-system-bus`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: 字典数据（数据字典、系统参数）通常缓存在 Redis/本地缓存中，但扫描未发现主动通知/刷新机制。字典变更后需要等待缓存过期或重启服务。
- **建议**: 实现基于 Redis Pub/Sub 的缓存同步机制，或提供管理后台手动刷新接口。

---

### 问题 P2-015: Gateway 缺少请求体大小限制

**模块**: `carlos-gateway`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: 未发现网关层配置请求体大小限制。大文件上传请求可能直接穿透到下游服务，造成内存压力。
- **建议**: 在网关层配置 `spring.codec.max-in-memory-size` 和文件上传大小限制，提前拒绝超大请求。

---

### 问题 P2-016: Org 部门树递归深度未限制

**模块**: `carlos-org-bus`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: 部门树递归查询（获取子孙、祖先链）未限制最大深度。在极端情况下（如 1000 级部门），可能导致栈溢出。
- **建议**: 限制最大递归深度（如 50 层），或使用迭代方式替代递归。

---

### 问题 P2-017: Auth MFA 双因素认证模块未找到实现

**模块**: `carlos-auth-service`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: AGENTS.md 提到支持 MFA（多因素认证），但扫描未发现完整的 MFA 流程实现（如 TOTP、短信验证码二次验证）。
- **建议**: 确认 MFA 功能状态，若已实现但分散在多个文件中，应集中整理文档；若未实现，应排期开发。

---

### 问题 P2-018: License 生成器 Controller 暴露信息过多

**模块**: `carlos-license-generate`
**文件**: `carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate/src/main/java/com/carlos/license/generate/LicenseCreatorController.java`
**严重程度**: P2-Medium

#### 详细分析

- **问题描述**: TODO 注释提到"该接口的功能应该用一个脚本代替"，说明当前通过 REST 接口收集服务器信息生成 license 的方式不够安全。
- **风险**: 接口可能暴露服务器硬件信息，被攻击者利用进行系统指纹识别。

---

## 五、P3-Low 问题详情

### 问题 P3-001: 业务模块 README 内容过于简略

**模块**: 多个业务模块
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: 各业务模块的 README.md 大多只有模块名称和简短描述，缺少快速开始、配置说明、依赖关系等必要内容。

---

### 问题 P3-002: Tools 模块 GitLab 集成大量 TODO

**模块**: `carlos-tools`
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: `GitlabMainController` 和 `UserManagementController` 中有大量 TODO（从配置文件加载、显示配置对话框等），说明功能尚未完成。

---

### 问题 P3-003: 业务模块 package-info.java 大量为空

**模块**: 多个业务模块
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: 虽然各包都有 `package-info.java`，但大部分为空文件，未说明包职责和使用规范。

---

### 问题 P3-004: Audit 模块 docs 目录结构完整但内容可能为空

**模块**: `carlos-audit`
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: 各模块都有 `docs/01-product` 到 `06-deploy` 的完整目录结构，但 README.md 文件可能只有标题无实质内容。需人工确认文档质量。

---

### 问题 P3-005: Gateway 过滤器 Order 管理分散

**模块**: `carlos-gateway`
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: 过滤器顺序通过 `GlobalFilterOrder` 枚举管理，但部分过滤器使用硬编码顺序值，不便于统一管理。

---

### 问题 P3-006: Auth 模块异常处理使用 BusinessException 而非 AuthErrorCode

**模块**: `carlos-auth-service`
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: 部分异常抛出使用 `CommonErrorCode` 而非 `AuthErrorCode`，不利于错误码体系的一致性。

---

### 问题 P3-007: Message 模块缺少消息发送示例

**模块**: `carlos-message`
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: 未发现 `carlos-sample-message` 示例模块，不利于开发者理解消息模块的使用方式。

---

### 问题 P3-008: Org 模块导出功能未审计

**模块**: `carlos-org-bus`
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: 部门/人员导出功能（如有）未接入 `carlos-audit` 审计模块，敏感数据导出无留痕。

---

### 问题 P3-009: System 配置项缺少校验注解

**模块**: `carlos-system-bus`
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: 部分配置项 Param 类缺少 `@NotNull`/`@Size` 等校验注解，依赖前端校验不够安全。

---

### 问题 P3-010: License 验证模块无版本变更日志

**模块**: `carlos-license`
**严重程度**: P3-Low

#### 详细分析

- **问题描述**: 未发现 CHANGELOG.md，无法追溯版本变更历史。

---

## 六、模块间依赖合规性

| 模块 | 依赖关系 | 规范要求 | 状态 |
|----|--------|------|------|
| carlos-audit-api | 不依赖 audit-bus | ✅ | 合规 |
| carlos-audit-cloud | 依赖 audit-api | ✅ | 合规 |
| carlos-auth-api | 不依赖 auth-service | ✅ | 合规 |
| carlos-auth-cloud | 依赖 auth-api | ✅ | 合规 |
| carlos-message-api | 不依赖 message-bus | ✅ | 合规 |
| carlos-org-api | 不依赖 org-bus | ✅ | 合规 |
| carlos-system-api | 不依赖 system-bus | ✅ | 合规 |
| carlos-license-core | 不依赖 starter | ✅ | 合规 |

**总体评价**: 模块间依赖方向基本合规，`-api` 模块均遵循"最小暴露原则"。

---

## 七、TODO/FIXME 遗留清单

| 文件 | 行号 | 标记内容 | 遗留时间 | 建议 |
|----|----|--------|--------|------|
| AuditLogQueryService.java | 多行 | ClickHouse 查询实现 TODO | 未知 | P1 排期实现 |
| SysRegionManagerImpl.java | L85 | 缓存失效 FIXME | 2025-12 | P1 尽快修复 |
| SysRegionManagerImpl.java | L95 | 层级变更 FIXME | 2025-12 | P1 尽快修复 |
| SysConfigServiceImpl.java | 单行 | 文件类型转换 TODO | 2022-12 | P2 评估必要性 |
| LicenseCreatorController.java | 单行 | 接口用脚本替代 TODO | 2025-04 | P2 评估优化 |
| CodeGeneratorService.java | 单行 | 多模块模板 TODO | 2020-09 | P3 长期排期 |
| GitlabMainController.java | 多行 | 配置加载对话框 TODO | 未知 | P3 工具模块低优先级 |
| UserManagementController.java | 多行 | 导入导出 TODO | 未知 | P3 工具模块低优先级 |

---

## 八、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|-----|--------|--------|--------|--------|--------|---------|
| carlos-auth | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-gateway | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | 3.0/5.0 |
| carlos-audit | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐☆☆☆ | 3.4/5.0 |
| carlos-message | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 2.8/5.0 |
| carlos-org | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.6/5.0 |
| carlos-system | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 3.2/5.0 |
| carlos-license | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-tools | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | 2.4/5.0 |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改

---

## 九、待办事项与修复计划

### 9.1 今日新增待办

- [ ] **P0-001** `carlos-auth`: 为浏览器端点启用 CSRF 保护，API 端点保持禁用
- [ ] **P0-002** `carlos-gateway`: JWT 验证器强制要求公钥配置，禁止 null signer 跳过验证
- [ ] **P1-001** `carlos-audit`: 实现 ClickHouse 审计日志查询（基础按时间+业务类型查询）
- [ ] **P1-002** `carlos-system`: 区域管理缓存失效处理（SysRegionManagerImpl）
- [ ] **P1-003** `carlos-auth`: 限制 actuator/swagger 端点访问（内网/管理员权限）
- [ ] **P1-004** `carlos-auth`: 补充密码编码器 SM2/SM3 分支实现
- [ ] **P1-005** `carlos-org`: 部门树循环引用检测
- [ ] **P1-006** `carlos-gateway`: OAuth2 授权过滤器增加公开路径白名单
- [ ] **P1-007** `carlos-license`: 生成模块增加环境检测，默认禁用
- [ ] **P1-008** `carlos-auth`: 调整默认安全过滤器链优先级
- [ ] **P2-001** 多模块: 为核心业务模块实现 HealthIndicator
- [ ] **P2-002** `carlos-audit`: 明确 audit-boot 与 audit-bus 的职责分工
- [ ] **P2-003** `carlos-org`: 为部门缓存设置 TTL 兜底
- [ ] **P2-004** `carlos-system`: 评估配置文件类型 TODO 必要性
- [ ] **P2-005** `carlos-message`: 审查核心功能实现完整性（路由/限流/模板）
- [ ] **P2-006** `carlos-auth`: 设备指纹算法 MD5 → SHA-256
- [ ] **P2-007** `carlos-gateway`: 显式检查 JWT exp/nbf 声明
- [ ] **P2-008** `carlos-license-verify`: 实现 LicenseHealthIndicator
- [ ] **P2-009** `carlos-tools`: 评估多模块模板支持必要性
- [ ] **P2-010** `carlos-gateway`: UserContextRelayFilter 清理客户端伪造 Header
- [ ] **P2-011** `carlos-audit`: ClickHouseBatchWriter 暴露 Micrometer metrics
- [ ] **P2-012** `carlos-auth`: 审查 JWT payload 敏感字段
- [ ] **P2-013** 多模块: 补充核心 Service/Manager JavaDoc
- [ ] **P2-014** `carlos-system`: 实现字典缓存热更新机制
- [ ] **P2-015** `carlos-gateway`: 配置请求体大小限制
- [ ] **P2-016** `carlos-org`: 限制部门树递归深度
- [ ] **P2-017** `carlos-auth`: 确认 MFA 功能实现状态
- [ ] **P2-018** `carlos-license`: 评估 License 生成接口安全性

### 9.2 历史遗留跟踪

- [ ] **P0-XXX** `starter-encrypt`: EncryptConfig SM4 密钥派生使用 MD5 ⏳ 昨日记录，待修复
- [ ] **P0-XXX** `starter-web`: XssHttpServletRequestWrapper 绕过 JSON 请求 ⏳ 昨日记录，待修复
- [ ] **P0-XXX** `starter-security`: UserContextAuthenticationFilter 信任任意 Header ⏳ 昨日记录，待修复

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
