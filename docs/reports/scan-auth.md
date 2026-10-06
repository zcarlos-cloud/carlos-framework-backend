# Carlos Framework 业务模块扫描报告 — carlos-auth

> **扫描日期**: 2026-05-27  
> **扫描模块**: carlos-auth (api / boot / cloud / service)  
> **HEAD**: `e990618f`  
> **代码文件数**: 226  

---

## 一、执行摘要

| 指标 | 数值 |
|------|------|
| 扫描模块数 | 4 个（api / boot / cloud / service） |
| 安全漏洞 | **3 项（均为 P0）** |
| 设计缺陷 | **3 项（2 P1 / 1 P2）** |
| 功能缺失 | **3 项（均为 P1）** |
| 技术债务 | **2 项（均为 P3）** |
| 代码亮点 | **8 项** |

---

## 二、P0 级发现（立即修复）

### 🔴 AUTH-001: `SecurityUser` 安全状态检查全部硬编码返回 `true`

**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/security/SecurityUser.java`

**问题描述**:  
`SecurityUser` 实现 `UserDetails` 接口，但 `isAccountNonLocked()`、`isAccountNonExpired()`、`isCredentialsNonExpired()` 全部硬编码返回 `true`。

```java
@Override
public boolean isAccountNonLocked() {
    return true;  // ← 锁定状态永远返回未锁定
}

@Override
public boolean isAccountNonExpired() {
    return true;  // ← 账号过期永远返回未过期
}

@Override
public boolean isCredentialsNonExpired() {
    return true;  // ← 密码过期永远返回未过期
}
```

**风险**:  
- `LoginAttemptManager` 在 Redis 中锁定账号后，数据库状态也改为 `LOCKED`
- 但 `SecurityUser.isAccountNonLocked()` 永远返回 `true`，Spring Security 认证流程**永远不会识别账号锁定状态**
- 如果绕过了 `LoginAttemptManager` 的前置检查（如通过其他 grant type 或内部接口），被锁定的用户仍可通过认证
- 同样，密码过期策略也无法生效

**修复建议**:  
```java
@Override
public boolean isAccountNonLocked() {
    return this.enable; // 或使用独立字段 accountLocked
}
```
- 在 `SecurityUser` 中添加 `accountLocked`/`accountExpired`/`credentialsExpired` 字段
- 在构造 `SecurityUser` 时从 `LoginUserInfo` 或数据库同步状态
- 确保 `DefaultExtendUserDetailsService` 中 `accountLocked(userInfo.isAccountLocked())` 设置生效

---

## 三、P1 级发现（本周修复）

### 🟠 AUTH-002: `CaptchaController.verifySmsCaptcha()` 未完成实现

**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/captcha/CaptchaController.java` L156-L157

```java
// TODO: 查询用户，如果不存在则自动注册
// TODO: 生成并返回JWT令牌
```

**风险**: 短信验证码登录接口返回 `requiresRegistration=false` 但实际未完成 JWT 生成，前端集成后功能异常。

**修复建议**: 完成 TODO 或标记接口为 `@Deprecated` 直到实现完成。

---

### 🟠 AUTH-003: `SocialCallbackController` 未完成社交登录 JWT 生成

**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/idp/web/SocialCallbackController.java` L142

```java
// TODO: 此处应调用登录服务生成 JWT Token
```

**风险**: 社交登录回调后无法生成 Token，用户无法完成登录流程。

---

### 🟠 AUTH-004: `LoginAttemptManager` 使用硬编码状态字符串

**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/security/manager/LoginAttemptManager.java` L150, L179

```java
userProvider.updateUserStatus(userInfo.getUserId(), "LOCKED");
userProvider.updateUserStatus(userInfo.getUserId(), "ENABLE");
```

**风险**: 状态字符串硬编码，一旦枚举变更会导致状态不一致。应使用框架统一的枚举常量。

**修复建议**: 使用 `ClientStateEnum` 或统一的账号状态枚举替代字符串字面量。

---

## 四、P2 级发现（纳入下个迭代）

### 🟡 AUTH-005: `JwtProperties` 默认密钥长度 2048，生产环境推荐 4096

**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/oauth2/server/JwtProperties.java` L50

```java
private Integer keySize = 2048;
```

**风险**: 2048 位 RSA 在当前计算能力下尚可，但 4096 位更安全。默认配置应引导用户使用更安全的选项。

**修复建议**: 
- 将默认值改为 4096
- 或在配置验证中增加警告日志："当前使用 2048 位密钥，建议生产环境使用 4096 位"

---

### 🟡 AUTH-006: `KeyPairManager` 证书链 TODO

**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/security/manager/KeyPairManager.java` L211

```java
// TODO: 如果需要证书链，可以引入 BouncyCastle 库
```

**影响**: 当前保存的 JKS 不包含证书链，某些严格验证场景可能受影响。

---

### 🟡 AUTH-007: `DefaultUserProvider` 仅 `@Profile("!prod")` 保护

**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/provider/DefaultUserProvider.java` L25

**风险**: 仅通过 Spring Profile 限制，缺少 `@ConditionalOnMissingBean` 兜底。如果生产环境意外激活 dev profile，默认用户（admin/123456）将可用。

**修复建议**: 同时添加 `@ConditionalOnMissingBean(UserProvider.class)` 确保自定义实现优先。

---

## 五、P3 级发现（技术债务）

| 编号 | 文件 | 问题 | 建议 |
|------|------|------|------|
| TD-001 | `carlos-auth-service` 全局 | JavaDoc 注释格式不统一 | 统一使用 `/**` 多行注释 |
| TD-002 | `carlos-auth-boot` | README.md 可能缺失 | 补充模块 README |

---

## 六、代码亮点

| 模块 | 亮点 | 说明 |
|------|------|------|
| **OAuth2 授权服务器** | CSRF 配置 | `CookieCsrfTokenRepository` + API 端点白名单 |
| **密码编码器** | BCrypt 强制 | 仅支持 BCrypt，无 MD5/SHA1 弱编码器 |
| **IpBlockManager** | IP 级防护 | Redis 计数 + 滑动窗口 + 可配置阈值 |
| **LoginAttemptManager** | 账号级防护 | Redis + DB 同步锁定，TTL 自动解锁 |
| **TokenController** | Token 掩码 | `maskToken()` 防止日志泄露完整 Token |
| **UserLoginService** | MFA 风险评估 | 异地登录 + 非工作时间检测 |
| **审计集成** | `@Async` 审计 | 登录/登出/全局登出均异步记录审计日志 |
| **Feign 降级** | FallbackFactory | `FeignAuthFallbackFactory` 完整实现 |

---

## 七、修复优先级路线图

```
本周内（P0+P1）:
  ├─ AUTH-001: SecurityUser 安全状态硬编码 — 添加 accountLocked/expired 字段
  ├─ AUTH-002: 完成 CaptchaController 验证码登录 JWT 生成
  ├─ AUTH-003: 完成 SocialCallbackController JWT 生成
  └─ AUTH-004: LoginAttemptManager 状态字符串改用枚举

下个迭代（P2）:
  ├─ AUTH-005: JwtProperties 默认密钥长度改为 4096
  ├─ AUTH-006: KeyPairManager 引入 BouncyCastle 证书链
  └─ AUTH-007: DefaultUserProvider 添加 @ConditionalOnMissingBean
```

---

## 八、文档完整性评估

| 文档类型 | 状态 | 说明 |
|----------|------|------|
| 需求文档 (01-product) | ✅ | BRD/PRD/roadmap 目录存在 |
| 设计文档 (02-design) | ✅ | architecture/api/database/flow 目录存在 |
| 开发文档 (03-develop) | ✅ | guide 目录存在 |
| 测试文档 (04-test) | ✅ | case/plan/report 目录存在 |
| 问题跟踪 (05-issue) | ✅ | bug/review/risk 目录存在 |
| 部署文档 (06-deploy) | ✅ | config/playbook 目录存在 |

---

> _扫描完成时间: 2026-05-27_  
> _扫描人: Carlos Framework 专属架构扫描代理_
