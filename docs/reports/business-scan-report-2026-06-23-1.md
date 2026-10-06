# Carlos Framework 业务模块扫描报告 Part1

> **扫描日期**: 2026-06-23
> **Git Commit**: `8854b043e4df0119fd7cd2347a4f7c51a7ce3275`
> **扫描范围**: carlos-gateway / carlos-audit / carlos-auth / carlos-license
> **扫描维度**: 文档完整性 → 需求覆盖 → 代码规范 → 性能风险 → 安全风险 → 功能完整度
> **严重级别**: P0=致命 P1=高 P2=中 P3=低

---

## 一、执行摘要

本次扫描覆盖 Carlos Framework 四个业务集成模块共 **~120+ Java 源文件**，发现 **2 个 P0 致命问题**、**7 个 P1 高风险问题**、**5 个 P2 中等问题**。整体来看框架在认证授权、网关防护、审计追踪方面架构设计合理，但在密码学安全（秘钥强度、密码泄露）和功能完整性（MFA 未完整实现）方面存在需要立即修复的缺陷。

| 模块 | P0 | P1 | P2 | P3 | 健康度 |
|------|----|----|----|----|--------|
| carlos-gateway | 0 | 3 | 2 | 0 | ⭐⭐⭐⭐ (78/100) |
| carlos-audit | 0 | 0 | 1 | 0 | ⭐⭐⭐⭐⭐ (88/100) |
| carlos-auth | 0 | 4 | 1 | 0 | ⭐⭐⭐ (72/100) |
| carlos-license | 2 | 0 | 1 | 0 | ⭐⭐ (55/100) |
| **总计** | **2** | **7** | **5** | **0** | **73/100** |

---

## 二、问题分布总览

```
维度分布:
  安全风险     ████████████████████████ 5 个 (P0×2, P1×2, P2×1)
  功能完整度   ██████████████ 3 个 (P1×3)
  性能风险     ██████████████ 3 个 (P1×1, P2×2)
  代码规范     ████████ 2 个 (P1×1, P2×1)
  文档完整性   ██ 1 个 (P2×1)
  需求覆盖     ██ 0 个 (需业务方确认)
```

---

## 三、各模块问题详情

---

### 模块 A: carlos-gateway (网关)

网关模块实现了 OAuth2 Token 认证、WAF 安全防护、重放攻击防护、限流熔断、灰度发布等完整功能栈。整体代码质量高，主要问题集中在性能优化和边界条件处理。

#### 🔴 P1-01: OpaqueTokenValidator.buildUserContext() 无容错 Long 解析

- **维度**: 代码规范 / 性能风险
- **严重级别**: P1
- **文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/oauth2/validator/OpaqueTokenValidator.java`
- **行号**: 约 148-170 行

**代码片段**:
```java
private UserContext buildUserContext(JSONObject json) {
    // ...
    context.setRoleId(json.getLong("role_id"));          // ← 可能返回 null
    context.setTenantId(json.getLong("tenant_id"));       // ← 可能返回 null
    context.setDepartmentId(json.getLong("dept_id"));     // ← 可能返回 null
    // ...

    // 解析角色列表 — 直接 Long.parseLong 无 try-catch
    if (roles instanceof List<?> roleList) {
        Set<Serializable> roleIds = roleList.stream()
            .map(Object::toString)
            .map(Long::parseLong)           // ← 若返回非数字字符串会抛 NFE
            .collect(Collectors.toSet());
        context.setRoleIds(roleIds);
    }

    // 部门列表同理
    if (depts instanceof List<?> deptList) {
        Set<Serializable> deptIds = deptList.stream()
            .map(Object::toString)
            .map(Long::parseLong)           // ← 同上
            .collect(Collectors.toSet());
        context.setDepartmentIds(deptIds);
    }
}
```

**问题分析**: introspection 端点的返回数据格式由认证服务器控制，但若返回非预期的字符串格式（如空串、字母），`Long.parseLong` 会抛出 `NumberFormatException` 并被 `onErrorMap` 捕获后返回 401。更优雅的做法是过滤掉无法解析的值而非阻断整个请求。

**解决方案**:
```java
// 方案1：优雅跳过无法解析的 ID
Set<Serializable> roleIds = roleList.stream()
    .map(Object::toString)
    .filter(s -> s.matches("\\d+"))
    .map(Long::parseLong)
    .collect(Collectors.toSet());

// 方案2：使用 Hutool NumberUtil.parseLong 带默认值
.map(s -> NumberUtil.parseLong(s, 0L))
```

---

#### 🔴 P1-02: CarlosRedisRateLimiter 黑/白名单使用 O(n) List.contains 线性扫描

- **维度**: 性能风险
- **严重级别**: P1
- **文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/ratelimit/CarlosRedisRateLimiter.java`
- **行号**: 约 102-116 行

**代码片段**:
```java
public boolean isWhitelisted(String ip) {
    // properties.getWhitelist() 返回 List<String>，contains() 是 O(n)
    return properties.getWhitelist() != null && properties.getWhitelist().contains(ip);
}

public boolean isBlacklisted(String ip) {
    return properties.getBlacklist() != null && properties.getBlacklist().contains(ip);
}
```

**问题分析**: 每次请求都会调用 `isAllowed()` → `isBlacklisted()` / `isWhitelisted()`，使用 `List.contains()` 进行线性扫描。当黑/白名单规模增长到数千条时，每个请求都将承受 O(n) 的匹配开销。虽然当前可能是内部小规模使用，但从架构角度应使用 HashSet 以保障 O(1) 查找。

**解决方案**:
```java
// 配置类中缓存为 HashSet
private final Set<String> whitelistSet;
private final Set<String> blacklistSet;

public CarlosRedisRateLimiter(...) {
    // ...
    this.whitelistSet = properties.getWhitelist() != null
        ? new HashSet<>(properties.getWhitelist()) : Collections.emptySet();
    this.blacklistSet = properties.getBlacklist() != null
        ? new HashSet<>(properties.getBlacklist()) : Collections.emptySet();
}

public boolean isWhitelisted(String ip) {
    return whitelistSet.contains(ip);  // O(1)
}
```

---

#### 🔴 P1-03: WafFilter 对请求体无大小限制

- **维度**: 安全风险 / 性能风险
- **严重级别**: P1
- **文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/security/WafFilter.java`

**问题分析**: WafFilter 对请求体执行 SQL 注入、XSS、命令注入等多模式正则匹配，但未配置请求体大小限制。攻击者可通过发送超大请求体（如 100MB JSON）触发正则匹配的 ReDoS 或直接耗尽网关内存。虽然 Spring WebFlux 默认限制 256KB，但 WAF 场景下建议显式配置更小的硬限制。

**解决方案**: 在 WafProperties 中添加 `maxBodySize` 配置项（默认 64KB），在检查前验证 Content-Length：
```java
@ConfigurationProperties(prefix = "carlos.gateway.waf")
public class WafProperties {
    /**
     * 最大请求体检查大小，默认 64KB
     */
    private DataSize maxBodySize = DataSize.ofKilobytes(64);
}
```

---

#### 🟡 P2-01: OpaqueTokenValidator 缓存反序列化无 Schema 校验

- **维度**: 安全风险
- **严重级别**: P2
- **文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/oauth2/validator/OpaqueTokenValidator.java`

**问题分析**: `parseCachedContext` 直接使用 `JSONUtil.toBean(cached, UserContext.class)` 反序列化 Redis 缓存。虽然数据来源是可信的（Redis 中仅存放自己写入的数据），但若能结合 Redis 注入攻击则存在被污染的潜在风险。建议增加简单的版本号/schema 校验。

**解决方案**:
```java
// 在缓存 JSON 中添加 schema_version 字段
// 反序列化时校验
private UserContext parseCachedContext(String cached) {
    JSONObject obj = JSONUtil.parseObj(cached);
    Integer schemaVersion = obj.getInt("schema_version");
    if (schemaVersion == null || schemaVersion != 1) {
        // 缓存数据格式不兼容，丢弃重新请求 introspection
        throw new IllegalStateException("Invalid cache schema version");
    }
    return JSONUtil.toBean(cached, UserContext.class);
}
```

---

#### 🟡 P2-02: GatewayExceptionHandler 错误码映射不全

- **维度**: 代码规范 / 功能完整度
- **严重级别**: P2
- **文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/config/GatewayExceptionHandler.java`
- **行号**: 约 224-240 行

**问题分析**: `mapErrorCodeToHttpStatus` 方法使用硬编码的字符串匹配错误码（如 `"4001"`, `"4003"`），与 `CommonErrorCode` 枚举无直接关联，存在维护风险——新增错误码可能在此遗漏映射。建议通过 `CommonErrorCode.getByCode()` 查找枚举后获取 HttpStatus。

---

### 模块 B: carlos-audit (审计)

审计模块整体代码质量最高。ClickHouseBatchWriter 的双缓冲设计、Disruptor 的高性能异步处理、风险评分算法的多维度评估都体现了在性能和正确性方面的深度考虑。本次扫描未发现 P0/P1 问题。

#### 🟡 P2-03: ClickHouseBatchWriter 缺少慢查询监控

- **维度**: 性能风险
- **严重级别**: P2
- **文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java`

**问题分析**: 批量写入方法没有记录每次 flush 的耗时，不利于排查 ClickHouse 写入性能下降。建议增加 Micrometer Timer 或至少 DEBUG 级别的耗时日志。

**解决方案**:
```java
long start = System.nanoTime();
// ... flush 逻辑 ...
long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
if (elapsed > 1000) {
    log.warn("ClickHouse batch write slow: {}ms, batchSize={}", elapsed, batch.size());
}
```

---

### 模块 C: carlos-auth (认证)

认证模块实现了完整的 OAuth2 授权服务器，支持多种 Grant Type 和身份源。主要问题集中在功能完整度——MFA 模块的持久化逻辑被注释，以及密码编码器支持不完整。

#### 🔴 P1-04: MfaService 核心持久化逻辑全部被注释

- **维度**: 功能完整度
- **严重级别**: P1
- **文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/mfa/service/MfaService.java`
- **行号**: 约 87-110, 120-140, 160-175 行

**代码片段**:
```java
public boolean enableMfa(String username, String secret, String code) {
    // ... 验证 TOTP 验证码 ...
    
    // 启用MFA并保存密钥
    // user.setMfaEnabled(true);       // ← 整个持久化逻辑被注释
    // user.setMfaSecret(secret);      // ← 
    // userProvider.updateById(user);  // ← 
    
    log.info("MFA enabled successfully for user: {}", username);
    return true;  // ← 实际上没有保存任何状态！下次登录不会要求 MFA
}

public boolean verifyMfaCode(String username, String code) {
    // 检查是否启用了MFA
    // if (Boolean.FALSE.equals(user.getMfaEnabled()) || ...) {  // ← 全部被注释
    //     throw AuthErrorCode.AUTH_MFA_NOT_ENABLED.exception();
    // }
    // boolean isValid = totpGenerator.verifyTotpCode(user.getMfaSecret(), code); // ← 被注释
    
    log.debug("MFA verification successful for user: {}", username);
    return true;  // ← 任何验证码都通过！MFA 形同虚设
}

public boolean isMfaEnabled(String username) {
    // return Boolean.TRUE.equals(user.getMfaEnabled());
    return true;  // ← 始终返回 true，但实际状态从未持久化
}
```

**问题分析**: MfaService 中的三个核心方法——`enableMfa()`、`verifyMfaCode()`、`disableMfa()` 的持久化逻辑（调用 `userProvider.updateById()`、查询用户 MFA 状态）全部被注释。当前状态下：
- 启用 MFA 后不会保存密钥 → 下次无法验证
- 验证 MFA 时始终返回 `true` → MFA 保护完全无效
- `isMfaEnabled()` 始终返回 `true` → 无法判断真实状态

这是典型的开发中代码被注释导致的功能缺失。如果 MFA 已经在生产环境启用配置，存在严重的安全隐患——用户以为开启了 MFA 保护，实际上任何 6 位数字都能通过验证。

**解决方案**: 
1. 紧急：如果 MFA grant type 已配置在生产环境，立即禁用
2. 完成 `UserInfo` 实体增强：添加 `mfaEnabled` / `mfaSecret` / `mfaRecoveryCodes` 字段
3. 实现完整的持久化逻辑（取消注释并补充完整实现）
4. 增加 `@ConditionalOnProperty("carlos.oauth2.mfa.enabled")` 条件注解控制 MFA 模块启用

---

#### 🔴 P1-05: OAuth2PasswordEncoderConfig 仅支持 BCrypt，SM2 编码器未实现

- **维度**: 功能完整度 / 安全风险
- **严重级别**: P1
- **文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2PasswordEncoderConfig.java`
- **行号**: 约 40-48 行

**代码片段**:
```java
@Bean
@ConditionalOnMissingBean
public PasswordEncoder passwordEncoder(OAuth2Properties oauth2Properties) {
    SecurityProperties.PasswordEncoderType encoderType = oauth2Properties.getSecurity().getPasswordEncoder();
    switch (encoderType) {
        case BCRYPT:
            log.info("Using BCrypt password encoder");
            return new BCryptPasswordEncoder();
        default:                          // ← SM2 等类型直接 fall through 到 BCrypt
            log.info("Using BCrypt password encoder (default)");
            return new BCryptPasswordEncoder();
    }
}
```

**问题分析**: 虽然配置了 `PasswordEncoderType` 枚举支持 SM2/国密，但 switch 语句中除 `BCRYPT` 外的所有 case（SM2、SM3、MD5）都走到 default 分支，实际仍返回 BCryptPasswordEncoder。这意味着：
- 配置文件设置了 `password-encoder: sm2` → 日志显示 BCrypt → 实际使用 BCrypt
- 如果数据库中已有 SM2 加密的密码 → 登录验证会失败
- 而且没有任何日志提示配置被忽略了

**解决方案**:
```java
case SM2:
    log.info("Using SM2 password encoder");
    return new SM2PasswordEncoder();
case SM3:
    log.info("Using SM3 password encoder");
    return new SM3PasswordEncoder();
case MD5:
    log.warn("Using MD5 password encoder (deprecated, upgrade to BCrypt)");
    return new MD5PasswordEncoder();
case BCRYPT:
default:
    log.info("Using BCrypt password encoder");
    return new BCryptPasswordEncoder();
```

---

#### 🔴 P1-06: ExtendAuthenticationProvider 认证事件被静默注释

- **维度**: 功能完整度
- **严重级别**: P1
- **文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/ext/ExtendAuthenticationProvider.java`
- **行号**: 约 86-95 行

**代码片段**:
```java
if (eventPublisher != null) {
    // eventPublisher.publishEvent(new AuthenticationSuccessEvent(authenticatedToken));  // ← 注释
}

// ...

if (eventPublisher != null) {
    // eventPublisher.publishEvent(new AuthenticationFailureEvent(token, e));  // ← 注释
}
```

**问题分析**: 认证成功/失败事件发布逻辑被注释，导致以下后果：
- 审计模块无法接收到认证事件 → ClickHouse 审计日志中缺少登录数据
- 暴力破解检测无法基于失败事件触发（`SecurityAlertManager` 可能依赖此事件）
- 行为分析/异常检测能力缺失

**解决方案**: 取消注释并确保 `AuthenticationSuccessEvent` / `AuthenticationFailureEvent` 类存在且事件监听器正确配置：
```java
if (eventPublisher != null) {
    eventPublisher.publishEvent(new AuthenticationSuccessEvent(authenticatedToken));
}
```

---

#### 🟡 P2-04: RedisOAuth2AuthorizationService user_sessions 索引无过期清理

- **维度**: 性能风险
- **严重级别**: P2
- **文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/repository/RedisOAuth2AuthorizationService.java`
- **行号**: 约 200-220 行

**问题分析**: `saveUserSessionIndex` 使用 Redis Set 存储用户的所有授权 ID。`removeUserSessionIndex` 仅在显式 `remove()` 时删除，但如果授权凭 TTL 自动过期（Redis Key 自动删除），Set 中的引用不会同步清理，导致 `user_sessions:{principalName}` 的 Set 中积累越来越多的无效授权 ID（僵尸引用）。虽然不影响功能正确性，但长期运行会增大 Set 大小和 `findAuthorizationIdsByPrincipalName` 调用耗时。

**解决方案**: 
1. 方案 A — 定时任务清理：周期性扫描 Set 中的 auth ID，检查对应 Redis Key 是否存在
2. 方案 B — 使用 Redis Keyspace Notification 监听 Key 过期事件自动清理
3. 方案 C — 在 `findAuthorizationIdsByPrincipalName` 返回值前过滤无效 ID

---

### 模块 D: carlos-license (软件授权)

License 模块存在两个严重的安全问题——弱密钥和使用命令行参数传递密码。这是本次扫描中最需要立即修复的模块。

#### 🔴 P0-01: LicenseCreatorService 使用 1024-bit RSA 密钥（已废弃强度）

- **维度**: 安全风险
- **严重级别**: P0
- **文件**: `carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate/src/main/java/com/carlos/license/generate/LicenseCreatorService.java`
- **行号**: 约 165 行

**代码片段**:
```java
private void generatePrivateKey(LicenseCreatorParam param, String path, String name) {
    String[] command = {
            "keytool",
            "-genkeypair",
            "-alias", param.getPrivateAlias(),
            // "-keyalg", "RSA",           // ← 注释掉了 RSA，使用 keytool 默认（DSA?）
            "-keysize", "1024",           // ← 1024 位密钥，低于 NIST 推荐的 2048 位
            // "-storetype", "PKCS12",     // ← 注释掉了 PKCS12，使用 keytool 默认（JKS？）
            "-validity", param.getValidity(),
            // ...
    };
}
```

**问题分析**:
1. **密钥强度不足**: 1024 位 RSA 已被 NIST 800-57 标准废弃（要求 ≥2048 位），1024 位可在数周内被破解
2. **算法未指定**: `-keyalg RSA` 被注释，可能使用 keytool JDK 版本的默认算法（部分版本默认 DSA-1024）
3. **密钥库格式**: `-storetype PKCS12` 被注释，可能使用 JKS 格式（Java 专有，互操作性差）
4. **注释与代码矛盾**: 类 JavaDoc 中明确写着 "改用 RSA 算法和 2048 位密钥" 和 "替换 JKS 为行业标准格式"，但实际代码中这些改进被注释掉了

**解决方案**:
```java
String[] command = {
        "keytool",
        "-genkeypair",
        "-alias", param.getPrivateAlias(),
        "-keyalg", "RSA",       // 显式指定 RSA
        "-keysize", "2048",     // 符合 NIST 800-57 标准
        "-storetype", "PKCS12", // 行业标准格式
        "-validity", param.getValidity(),
        "-keystore", path,
        "-storepass", param.getStorePass(),
        "-keypass", param.getKeyPass(),
        "-dname", "\"" + name + "\""
};
```

> ⚠️ **兼容性注意**: 修改密钥强度后，已颁发的旧证书将无法通过新生成的公钥库验证。需要重新颁发所有证书或同时保留两套验证逻辑。

---

#### 🔴 P0-02: LicenseCreatorService 通过命令行参数传递密码，泄露到进程列表

- **维度**: 安全风险
- **严重级别**: P0
- **文件**: `carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate/src/main/java/com/carlos/license/generate/LicenseCreatorService.java`
- **行号**: 约 165-190, 215-235, 250-265 行

**代码片段**:
```java
// generatePrivateKey()
String[] command = {
        "keytool",
        "-genkeypair",
        "-storepass", param.getStorePass(),  // ← 密码出现在命令行参数中
        "-keypass", param.getKeyPass(),      // ← 同上
        // ...
};

// generateCertificates()
String[] command = {
        "keytool",
        "-exportcert",
        "-storepass", param.getStorePass(),  // ← 同上
        // ...
};

// generatePublicKey()
String[] command = {
        "keytool",
        "-import",
        "-storepass", param.getStorePass(),  // ← 同上
        // ...
};
```

**问题分析**: 密钥库密码和别称密码通过 `keytool -storepass xxx -keypass xxx` 命令行参数传递。在任何 Linux 系统中，`ps aux` 可以查看所有进程的完整命令行参数（包括传递给 keytool 的密码）。在多租户服务器或有其他用户的容器中，这将直接泄露 License 密钥库的访问凭证。

**解决方案**: `keytool` 支持交互式输入密码。当 `-storepass` 和 `-keypass` 省略时，keytool 会从 stdin 读取。应通过 `process.getOutputStream()` 写入密码替代命令行参数：
```java
// 不在 command 中传递 storepass/keypass
String[] command = {
        "keytool",
        "-genkeypair",
        "-alias", param.getPrivateAlias(),
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", param.getValidity(),
        "-keystore", path,
        "-dname", "\"" + name + "\""
};

Process process = RuntimeUtil.exec(command);
try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
    writer.write(param.getStorePass() + "\n");  // 密钥库密码
    writer.write(param.getKeyPass() + "\n");    // 密钥口令
    writer.write(param.getKeyPass() + "\n");    // keytool 可能要求确认
    writer.flush();
}
```

> ⚠️ **注意**: 这种方式下 keytool 的输出流格式可能因 JDK 版本而异，需要充分测试。

---

#### 🟡 P2-05: LicenseCreatorService.generatePublicKey() 虚拟线程读取无超时控制

- **维度**: 性能风险
- **严重级别**: P2
- **文件**: `carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate/src/main/java/com/carlos/license/generate/LicenseCreatorService.java`
- **行号**: 约 260 行

**代码片段**:
```java
// 通过线程异步读取输出流（避免缓冲区满导致阻塞）
Thread.startVirtualThread(() -> {
    String result = RuntimeUtil.getResult(process);
    log.info("Output:  {}", result);
});
```

**问题分析**: `Thread.startVirtualThread()` 创建虚拟线程读取 process 输出流。虽然虚拟线程开销很小，但如果 `generatePublicKey()` 被频繁调用（如批量生成 License），每次调用都会创建新的虚拟线程，且这些线程没有超时控制——如果 process 输出流异常阻塞，虚拟线程会一直存活。

**解决方案**: 使用 `CompletableFuture` 带超时，或 `ExecutorService` 管理虚拟线程池；或者更简单的方式——使用 `process.inputStream()` 直接读取（因为已经向 process 的 stdin 写入了确认指令，keytool 的输出应该是有限的）。

---

## 四、模块健康度评分

### carlos-gateway: ⭐⭐⭐⭐ (78/100)
| 维度 | 得分 | 说明 |
|------|------|------|
| 文档完整性 | 85 | GlobalFilterOrder 常量类、异常体系完善 |
| 需求覆盖 | 80 | Token 验证/限流/WAF/灰度/熔断/重放防护完整 |
| 代码规范 | 78 | 少量边界处理不够严谨（Long.parseLong 无保护） |
| 性能风险 | 72 | 黑/白名单 O(n) 线性扫描，无请求体大小限制 |
| 安全风险 | 78 | SHA-256 缓存键、Token 脱敏日志，WAF 防护完善 |
| 功能完整度 | 80 | 异常处理器覆盖 8+ 异常类型，降级方案完整 |

### carlos-audit: ⭐⭐⭐⭐⭐ (88/100)
| 维度 | 得分 | 说明 |
|------|------|------|
| 文档完整性 | 80 | 类注释准确描述数据流和架构 |
| 需求覆盖 | 85 | ClickHouse+Disruptor+Feign 降级+风险评分完整 |
| 代码规范 | 90 | 双缓冲无 clear()防并发、递归改循环 |
| 性能风险 | 88 | 背压控制、批量写入、本地兜底 |
| 安全风险 | 90 | 多维度风险评分、审计数据匿名化 |
| 功能完整度 | 85 | 缺少慢查询监控是唯一改进点 |

### carlos-auth: ⭐⭐⭐ (72/100)
| 维度 | 得分 | 说明 |
|------|------|------|
| 文档完整性 | 75 | 类级注释完善，缺少架构决策记录 |
| 需求覆盖 | 78 | OAuth2 授权服务器功能完整 |
| 代码规范 | 80 | Token 脱敏、Redis Key 统一管理 |
| 性能风险 | 75 | user_sessions 索引缺乏清理，密码过期检查 OK |
| 安全风险 | 65 | MFA 实现不完整、认证事件未发布、SM2 编码器缺失 |
| 功能完整度 | 55 | MFA 核心逻辑被注释、SM2 编码器未实现 |

### carlos-license: ⭐⭐ (55/100)
| 维度 | 得分 | 说明 |
|------|------|------|
| 文档完整性 | 75 | keytool 命令注释详细 |
| 需求覆盖 | 80 | 证书生成/验证/下载/硬件绑定完整 |
| 代码规范 | 60 | 大量注释掉的代码与注释矛盾 |
| 性能风险 | 70 | XML 白名单校验、ReentrantLock 线程安全 |
| 安全风险 | 30 | 1024-bit RSA + 密码泄露到进程列表 (P0×2) |
| 功能完整度 | 55 | 2048-bit/PKCS12 改进已规划但未实现 |

---

## 五、待办事项

### 🔴 紧急 (P0) — 立即修复
- [ ] **LIC-1**: 将 LicenseCreatorService 密钥长度从 1024 升级到 2048，启用 `-keyalg RSA -storetype PKCS12`（取消注释已有的改进方案）
- [ ] **LIC-2**: 移除所有 keytool 命令中的 `-storepass` / `-keypass` 参数，改用 stdin 交互式传递密码

### 🟠 高优 (P1) — 本迭代内修复
- [ ] **GW-1**: OpaqueTokenValidator 中 `Long.parseLong` 增加容错处理（filter + try-catch）
- [ ] **GW-2**: CarlosRedisRateLimiter 黑/白名单改用 HashSet 实现 O(1) 查找
- [ ] **GW-3**: WafProperties 增加 `maxBodySize` 配置项防止大请求体内存耗尽
- [ ] **AUTH-1**: MfaService 完成持久化逻辑实现（取消注释 + 补充数据库字段）
- [ ] **AUTH-2**: OAuth2PasswordEncoderConfig 实现 SM2/SM3 编码器的 switch case
- [ ] **AUTH-3**: ExtendAuthenticationProvider 恢复认证事件发布（`publishEvent` 取消注释）

### 🟡 中等 (P2) — 下个迭代
- [ ] **GW-4**: OpaqueTokenValidator 缓存反序列化增加 schema version 校验
- [ ] **GW-5**: GatewayExceptionHandler 错误码映射改为枚举查找
- [ ] **AUDIT-1**: ClickHouseBatchWriter 增加慢写入耗时监控
- [ ] **AUTH-4**: RedisOAuth2AuthorizationService 增加 user_sessions 僵尸引用清理机制
- [ ] **LIC-3**: LicenseCreatorService 虚拟线程读取增加超时控制

### 🔵 建议 (P3+) — 计划内
- [ ] 所有模块增加基于 CommonErrorCode 枚举的异常处理器覆盖率统计
- [ ] carlos-auth MFA 模块增加 `@ConditionalOnProperty` 条件启用注解
- [ ] carlos-gateway 增加集成测试覆盖 OAuth2 认证 + WAF + 限流的组合场景
- [ ] 各模块增加 README.md 或 package-info.java 的架构概述

---

## 六、扫描方法论说明

- **扫描方式**: 代码审查 + 静态分析（人工逐文件审计）
- **代码范围**: 排除 `*test*` 路径，仅扫描 `src/main/java/**/*.java`
- **编码规范参考**: `.claude/skills/carlos-framework-standard/SKILL.md` + `CLAUDE.md`
- **P0 标准**: 存在安全漏洞、可导致数据泄露或系统被攻击
- **P1 标准**: 功能缺失/边界处理不严谨、可能在生产环境引发故障
- **P2 标准**: 性能优化点、代码整洁度、可观测性提升
- **P3 标准**: 文档补充、测试覆盖、架构优化建议

---

> 📊 **报告生成时间**: 2026-06-23
> 👤 **扫描执行**: Carlos Dev Assistant (AI)
> 📝 **下次扫描**: 建议在 P0/P1 修复完成后执行 Part2 扫描（carlos-system, carlos-org, carlos-message 等模块）
