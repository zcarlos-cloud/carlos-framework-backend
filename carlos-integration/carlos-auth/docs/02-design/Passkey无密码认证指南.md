# Passkey / WebAuthn 无密码认证指南

> 本文档说明 Carlos Framework 中 Passkey（WebAuthn/FIDO2）无密码认证的使用条件、触发方式、完整登录流程，以及如何从当前的骨架实现落地为生产可用。

---

## 一、概念速览

**Passkey** 是基于 W3C WebAuthn 标准和 FIDO2 协议的无密码认证方案。用户使用设备内置的生物识别（指纹 / Face ID / Windows Hello / Android 指纹）或硬件安全密钥（YubiKey）完成登录，无需输入密码。

**核心特性：**

| 特性   | 说明                         |
|------|----------------------------|
| 抗钓鱼  | 凭据与域名（RP ID）绑定，伪造域名无法获取签名  |
| 抗重放  | 每次认证使用一次性 challenge，签名不可复用 |
| 抗撞库  | 无密码存储，不存在密码泄露面             |
| 离线友好 | 设备本地存储私钥，服务器只存公钥           |

**相关组件：**

- **RP（Relying Party）**：依赖方，即本服务（carlos-auth）
- **Authenticator**：认证器，即用户的设备（手机/电脑/安全密钥）
- **Credential**：凭据，由认证器生成，包含公钥和凭据 ID

---

## 二、当前实现状态

Carlos Framework 已完成 **Passkey 生产可用实现**（v3.0.0）：

| 组件                                      | 状态                                                       | 位置                                                                     |
|-----------------------------------------|----------------------------------------------------------|------------------------------------------------------------------------|
| `PasskeyController`                     | ✅ 已实现（6 个端点，返回 typed DTO）                                | `carlos-auth-service/.../passkey/PasskeyController.java`               |
| `PasskeyService`                        | ✅ 基于 Yubico 库的完整密码学验证                                    | `carlos-auth-service/.../passkey/PasskeyService.java`                  |
| `PasskeyCredentialRepository`           | ✅ Yubico CredentialRepository 实现                         | `carlos-auth-service/.../passkey/PasskeyCredentialRepository.java`     |
| `PasskeyAutoConfiguration`              | ✅ `@ConditionalOnProperty` 条件装配 RelyingParty             | `carlos-auth-service/.../passkey/config/PasskeyAutoConfiguration.java` |
| 登录页 Passkey Tab                         | ✅ 已实现（含 JS 调用 WebAuthn API）                              | `carlos-auth-service/.../templates/login.html`                         |
| `LoginMethodValidator.isPasskeyReady()` | ✅ 校验 rpId/origins/RelyingParty Bean                      | `carlos-auth-service/.../oauth2/web/LoginMethodValidator.java`         |
| 配置开关 + RP 配置                            | ✅ `carlos.auth.login.methods.passkey` + `passkey-config` | `OAuth2Properties.Methods`                                             |
| WebAuthn 库依赖                            | ✅ `com.yubico:webauthn-server-core:2.9.0`                | `carlos-dependencies/pom.xml`                                          |
| 凭据存储 + 实体/Mapper/Manager                | ✅ `auth_passkey_credential` 表 + `PasskeyCredential`      | `auth_postgresql.sql`                                                  |
| Token 签发复用                              | ✅ `UserLoginService.issueTokenForUser()`                 | `carlos-auth-service/.../login/UserLoginService.java`                  |
| Passkey 注册 UI                           | ⚠️ 需在用户设置页新增绑定入口                                         | `carlos-ui` 或 `carlos-auth` 个人中心                                       |

**结论：** 配置 `carlos.auth.login.methods.passkey=true` + `passkey-config.rp-id` + `passkey-config.origins` 后，登录页将自动渲染 Passkey Tab，功能完整可用。注册 UI 需在用户登录后的设置页面新增。

---

## 三、使用条件

### 3.1 必须满足的前提

要让 Passkey 在 Carlos Framework 中真正可用，必须同时满足：

#### 1. 基础环境条件

- ✅ **HTTPS 环境**：WebAuthn 强制要求安全上下文（`https://` 或 `localhost`）。生产环境必须通过 HTTPS 部署 carlos-auth。
- ✅ **浏览器支持**：用户浏览器需支持 WebAuthn（Chrome 67+、Firefox 60+、Safari 13+、Edge 18+）。
- ✅ **认证器可用**：用户设备需具备平台认证器（Touch ID / Face ID / Windows Hello）或外接安全密钥（YubiKey）。

#### 2. 框架配置条件

- ✅ **carlos-auth 服务已部署**：Passkey 是 carlos-auth（IdP）的能力，必须启动该服务。
- ✅ **配置项已开启**：`carlos.auth.login.methods.passkey=true`。
- ✅ **RP 配置完整**：`rpId`、`rpName`、`origins` 三项必须配置（见下文）。
- ✅ **Redis 可用**：challenge 需暂存到 Redis（TTL ~300s），用于 finish 阶段校验。
- ✅ **数据库表已建**：`auth_passkey_credential` 表存在且可读写。

#### 3. 落地实现条件

- ✅ **引入 WebAuthn 库**：`com.yubico:webauthn-server-core`
- ✅ **实现 `CredentialRepository`**：读写凭据表
- ✅ **配置 `RelyingParty` Bean**：RP ID / RP Name / Origins
- ✅ **完善 `PasskeyService`**：用 Yubico 库替换 mock 实现
- ✅ **修改 `LoginMethodValidator.isPasskeyReady()`**：校验上述配置齐全后返回 `true`
- ✅ **新增注册 UI**：用户登录后在设置页注册 Passkey

### 3.2 RP 配置说明

RP（Relying Party）配置是 Passkey 的核心，决定了凭据绑定的域名：

| 配置项       | 含义                          | 示例                        | 注意事项                                                          |
|-----------|-----------------------------|---------------------------|---------------------------------------------------------------|
| `rpId`    | 依赖方域名，凭据与此域名绑定              | `carlos.com`              | 必须是访问域名的**可注册父域**。如服务在 `auth.carlos.com`，rpId 可为 `carlos.com` |
| `rpName`  | 依赖方显示名称                     | `Carlos Framework`        | 展示在设备生物识别弹窗中                                                  |
| `origins` | 允许的来源（scheme + host + port） | `https://auth.carlos.com` | 必须与浏览器地址栏完全一致，可配置多个                                           |

> **⚠️ 域名一致性要求**：`rpId` 必须是 `origins` 中所有 URL 的父域，否则浏览器会拒绝 WebAuthn 调用。

---

## 四、落地实现步骤

### 4.1 添加 WebAuthn 依赖

**在 `carlos-dependencies/pom.xml` 的 `<properties>` 中添加版本：**

```xml
<webauthn.version>2.5.0</webauthn.version>
```

**在 `<dependencyManagement>` 中添加：**

```xml
<!-- WebAuthn / Passkey -->
<dependency>
    <groupId>com.yubico</groupId>
    <artifactId>webauthn-server-core</artifactId>
    <version>${webauthn.version}</version>
</dependency>
```

**在 `carlos-auth-service/pom.xml` 中添加：**

```xml
<dependency>
    <groupId>com.yubico</groupId>
    <artifactId>webauthn-server-core</artifactId>
</dependency>
```

### 4.2 扩展配置属性

在 `OAuth2Properties.Methods` 中补充 Passkey RP 配置：

```java
/**
 * Passkey/WebAuthn Relying Party 配置
 */
@NestedConfigurationProperty
private Passkey passkeyConfig = new PasskeyConfig();

@Data
public static class PasskeyConfig {
    /** RP ID，如 carlos.com */
    private String rpId;
    /** RP 显示名称 */
    private String rpName = "Carlos Framework";
    /** 允许的 Origins，如 https://auth.carlos.com */
    private List<String> origins = new ArrayList<>();
}
```

对应 YAML：

```yaml
carlos:
  auth:
    login:
      methods:
        passkey: true
        passkey-config:
          rp-id: carlos.com
          rp-name: Carlos Framework
          origins:
            - https://auth.carlos.com
            - http://localhost:9511   # 开发环境
```

### 4.3 建表：auth_passkey_credential

在 `carlos-auth/docs/02-design/database/auth_postgresql.sql` 追加：

```sql
CREATE TABLE auth_passkey_credential (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    username        VARCHAR(64)  NOT NULL,
    credential_id   VARCHAR(255) NOT NULL,
    public_key_cose TEXT         NOT NULL,
    sign_count      BIGINT       NOT NULL DEFAULT 0,
    rp_id           VARCHAR(255) NOT NULL,
    user_handle     VARCHAR(255) NOT NULL,
    device_name     VARCHAR(128),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at    TIMESTAMP,
    is_deleted      SMALLINT     NOT NULL DEFAULT 0,
    CONSTRAINT uk_passkey_cred_id UNIQUE (credential_id)
);
CREATE INDEX idx_passkey_user_id ON auth_passkey_credential (user_id);
CREATE INDEX idx_passkey_username ON auth_passkey_credential (username);
```

### 4.4 配置 RelyingParty Bean

新建 `PasskeyConfig.java`：

```java
@Configuration
public class PasskeyConfig {

    @Bean
    public RelyingParty relyingParty(OAuth2Properties properties,
                                     CredentialRepository credentialRepository) {
        OAuth2Properties.Methods.PasskeyConfig cfg = properties.getLogin().getMethods().getPasskeyConfig();
        RelyingPartyIdentity rpId = RelyingPartyIdentity.builder()
                .id(cfg.getRpId())
                .name(cfg.getRpName())
                .build();
        return RelyingParty.builder()
                .identity(rpId)
                .credentialRepository(credentialRepository)
                .origins(new HashSet<>(cfg.getOrigins()))
                .allowOriginPort(false)
                .allowOriginSubdomain(false)
                .build();
    }
}
```

### 4.5 实现 CredentialRepository

```java
@Component
@RequiredArgsConstructor
public class JpaCredentialRepository implements CredentialRepository {

    private final PasskeyCredentialManager credentialManager;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        // 查询该用户所有 credential_id
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        // 返回用户的 userHandle
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        // 反查 username
    }
}
```

### 4.6 完善 PasskeyService

用 Yubico 库替换 mock：

```java
@Service
@RequiredArgsConstructor
public class PasskeyService {

    private final RelyingParty relyingParty;
    private final RedissonClient redissonClient;

    public PublicKeyCredentialCreationOptions beginRegistration(String username) {
        UserIdentity userIdentity = UserIdentity.builder()
                .name(username)
                .displayName(username)
                .id(generateUserHandle())
                .build();
        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(userIdentity)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                        .userVerification(UserVerificationRequirement.PREFERRED)
                        .residentKey(ResidentKeyRequirement.PREFERRED)
                        .build())
                .build();
        PublicKeyCredentialCreationOptions rpOptions = relyingParty.startRegistration(options);
        // challenge 存 Redis，TTL 300s
        redissonClient.getBucket("auth:passkey:challenge:" + username)
                .set(rpOptions.getChallenge(), Duration.ofSeconds(300));
        return rpOptions;
    }

    public boolean finishRegistration(String username, Map<String, Object> body) {
        ByteArray challenge = getAndDeleteChallenge(username);
        RegistrationRegistrationRequest req = ...; // 解析 body
        RegistrationResult result = relyingParty.finishRegistration(req);
        // 保存公钥到 auth_passkey_credential
        return true;
    }

    public AssertionRequest beginAuthentication(String username) {
        StartAssertionOptions options = StartAssertionOptions.builder()
                .username(username)  // 可为 null，支持 discoverable
                .userVerification(UserVerificationRequirement.PREFERRED)
                .build();
        AssertionRequest request = relyingParty.startAssertion(options);
        // challenge 存 Redis
        return request;
    }

    public Map<String, Object> finishAuthentication(Map<String, Object> body, HttpServletRequest request) {
        ByteArray challenge = getAndDeleteChallenge(...);
        FinishAssertionOptions options = FinishAssertionOptions.builder()
                .request(parseAssertionRequest(body))
                .build();
        AssertionResult result = relyingParty.finishAssertion(options);
        if (!result.isSuccess()) return null;
        // 更新 sign count
        // 签发 Token（复用 ExtendAuthenticationProvider 或 FormLoginSuccessHandler）
        return buildTokenResponse(result.getUsername());
    }
}
```

### 4.7 修改 LoginMethodValidator

```java
public boolean isPasskeyReady() {
    if (!oauth2Properties.getLogin().getMethods().isPasskey()) {
        return false;
    }
    PasskeyConfig cfg = oauth2Properties.getLogin().getMethods().getPasskeyConfig();
    if (StrUtil.hasBlank(cfg.getRpId()) || cfg.getOrigins().isEmpty()) {
        log.warn("Passkey 已启用但 RP 配置不完整（rpId/origins 缺失），自动降级");
        return false;
    }
    return true;
}
```

### 4.8 新增注册 UI

在用户登录后，提供 Passkey 注册入口（可在 `carlos-ui` 用户设置页或 `carlos-auth` 个人中心页新增）：

```html
<button onclick="doPasskeyRegister()">绑定 Passkey</button>
```

```javascript
async function doPasskeyRegister() {
    const resp = await fetch('/auth/passkey/register/begin', { method: 'POST' });
    const beginResult = await resp.json();
    const options = beginResult.data;
    const credential = await navigator.credentials.create({ publicKey: parseCreationOptions(options) });
    await fetch('/auth/passkey/register/finish', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(serializeCredential(credential))
    });
    alert('Passkey 绑定成功');
}
```

---

## 五、如何触发 Passkey

### 5.1 配置触发

**在 `carlos-auth` 的 `application.yml` 中：**

```yaml
carlos:
  auth:
    login:
      methods:
        passkey: true                              # 开启开关
        passkey-config:
          rp-id: carlos.com
          rp-name: Carlos Framework
          origins:
            - https://auth.carlos.com
```

**条件满足后，`LoginMethodValidator.isPasskeyReady()` 返回 `true`，`LoginController` 将 `passkeyEnabled=true` 传入 `login.html`，登录页渲染 Passkey Tab。**

### 5.2 用户触发流程

**首次使用（注册 Passkey）：**

1. 用户通过其他方式（密码/短信）登录 carlos-auth
2. 进入用户设置页 → 点击「绑定 Passkey」
3. 浏览器调用 `navigator.credentials.create()` → 设备弹生物识别
4. 注册成功，公钥存入 `auth_passkey_credential` 表

**日常登录（Passkey 登录）：**

1. 用户访问门户 → 重定向到 carlos-auth 的 `/login`
2. 登录页展示 Passkey Tab → 点击「🔑 使用 Passkey 登录」
3. 浏览器调用 `navigator.credentials.get()` → 设备弹生物识别
4. 验证通过 → 签发 Token → 跳回业务页

---

## 六、Passkey 登录完整流程

### 6.1 时序图（文字版）

```
浏览器/设备                  carlos-auth (IdP)                Authenticator
    │                              │                                │
    │  1. 用户点击 Passkey 登录      │                                │
    ├─────────────────────────────>│                                │
    │                              │                                │
    │  2. POST /auth/passkey/      │                                │
    │     authenticate/begin       │                                │
    ├─────────────────────────────>│                                │
    │                              │  生成 challenge                 │
    │                              │  challenge 存 Redis (TTL 300s) │
    │  3. 返回 PublicKeyCredential  │                                │
    │     RequestOptions（含 challenge、rpId）                       │
    │<─────────────────────────────┤                                │
    │                              │                                │
    │  4. navigator.credentials.get(publicKey)                      │
    │───────────────────────────────────────────────────────────────>│
    │                              │      5. 设备弹生物识别           │
    │                              │         用户触摸指纹/Face ID     │
    │                              │         私钥对 challenge 签名    │
    │  6. 返回 Assertion（含 signature、authenticatorData）          │
    │<───────────────────────────────────────────────────────────────┤
    │                              │                                │
    │  7. POST /auth/passkey/       │                                │
    │     authenticate/finish       │                                │
    │     （body: assertion response）│                                │
    ├─────────────────────────────>│                                │
    │                              │  从 Redis 取出 challenge 校验    │
    │                              │  用公钥验证 signature           │
    │                              │  校验 origin、rpId、sign count  │
    │                              │  更新 sign count               │
    │                              │  签发 OAuth2 Token             │
    │  8. 返回 Token（access_token + refresh_token）                  │
    │<─────────────────────────────┤                                │
    │                              │                                │
    │  9. 携带 Token 访问业务 API   │                                │
    │     → Gateway → 后端微服务    │                                │
```

### 6.2 接口调用细节

#### 步骤 2：开始认证

```
POST /auth/passkey/authenticate/begin
Content-Type: application/json
（无需认证，可带可选 username）

响应：
{
  "code": 200,
  "data": {
    "challenge": "<base64url 32 字节随机串>",
    "rpId": "carlos.com",
    "timeout": 60000,
    "userVerification": "preferred",
    "allowCredentials": [           // 若带了 username，返回该用户已绑定的 credentialId
      { "type": "public-key", "id": "<base64url>" }
    ]
  }
}
```

#### 步骤 4-6：浏览器与设备交互（纯前端）

```javascript
const credential = await navigator.credentials.get({
    publicKey: {
        challenge: base64urlToBuffer(options.challenge),
        rpId: options.rpId,
        timeout: options.timeout,
        userVerification: options.userVerification,
        allowCredentials: (options.allowCredentials || []).map(c => ({
            type: 'public-key',
            id: base64urlToBuffer(c.id)
        }))
    }
});
// credential 含 authenticatorData、clientDataJSON、signature
```

#### 步骤 7：完成认证

```
POST /auth/passkey/authenticate/finish
Content-Type: application/json
（无需认证）

请求体：
{
  "id": "<credentialId base64url>",
  "rawId": "<credentialId base64url>",
  "response": {
    "authenticatorData": "<base64url>",
    "clientDataJSON": "<base64url>",
    "signature": "<base64url>",
    "userHandle": "<base64url 或 null>"
  },
  "type": "public-key"
}

响应（成功）：
{
  "code": 200,
  "data": {
    "access_token": "...",
    "refresh_token": "...",
    "token_type": "Bearer",
    "expires_in": 900
  }
}
```

### 6.3 注册流程（绑定 Passkey）

```
浏览器                         carlos-auth                    Authenticator
  │  1. POST /auth/passkey/register/begin（需登录态）             │
  ├──────────────────────────>│                                │
  │  2. 返回 PublicKeyCredentialCreationOptions                │
  │<──────────────────────────┤                                │
  │  3. navigator.credentials.create(publicKey)                │
  │────────────────────────────────────────────────────────────>│
  │  4. 用户生物识别 → 生成密钥对，私钥存设备，公钥回传            │
  │<────────────────────────────────────────────────────────────┤
  │  5. POST /auth/passkey/register/finish（body: attestation） │
  ├──────────────────────────>│                                │
  │                            │  验证 attestation              │
  │                            │  保存公钥到 auth_passkey_credential │
  │  6. 返回注册成功                                            │
  │<──────────────────────────┤                                │
```

### 6.4 与 OAuth2 SSO 的集成

当前 Passkey 是 carlos-auth 内部登录方式。在 SSO 场景下：

```
carlos-framework-portal (前端)
    │  访问需登录的资源
    ↓  重定向
carlos-auth /oauth2/authorize
    │  未登录
    ↓  重定向
carlos-auth /login
    │  用户选择 Passkey Tab → 走上述 Passkey 登录流程
    │  认证成功后 Spring Security 写入 SecurityContext
    ↓  FormLoginSuccessHandler 恢复原始请求
carlos-auth /oauth2/authorize → 签发 code
    ↓  回调到 carlos-framework-portal
carlos-framework-portal /oauth2/callback → 换取 Token
```

**关键点：** Passkey 认证成功后，`finishAuthentication` 内部需将用户身份写入 Spring Security Context（或签发一次性 code），后续 OAuth2 流程自动接管。无需改动 OAuth2 端点本身。

### 6.5 可选：作为 OAuth2 Grant Type

若需 API 直连登录（非浏览器 SSO），可将 Passkey 注册为自定义 grant type：

1. 在 `CustomGrantTypes` 添加 `PASSKEY = new AuthorizationGrantType("passkey")`
2. 实现 `PasskeyGrantTypeHandler`（从请求参数提取 assertion）
3. 实现 `PasskeyIdentityProvider`（调用 `PasskeyService.finishAuthentication`）
4. 通过 `/oauth2/token?grant_type=passkey&assertion=...` 直接换 Token

参考 `DingTalkIdentityProvider` 的实现模式。

---

## 七、安全注意事项

1. **challenge 一次性**：finish 阶段必须从 Redis 删除 challenge，防止重放。
2. **sign count 递增校验**：每次认证后更新 sign count，若下次认证的 count 小于等于已存值，可能凭据被克隆，应拒绝。
3. **RP 域名严格匹配**：`origins` 必须与浏览器实际访问地址完全一致，否则浏览器拒绝。
4. **HTTPS 强制**：生产环境必须 HTTPS，`localhost` 开发例外。
5. **注册需登录态**：`/auth/passkey/register/**` 必须 `@PreAuthorize("isAuthenticated()")`，防止他人给受害者绑定凭据。
6. **凭据删除需二次确认**：删除 Passkey 应校验用户当前登录态，避免凭据被恶意清除后无法登录。
7. **多凭据支持**：一个用户可绑定多个 Passkey（手机+电脑+YubiKey），`auth_passkey_credential` 表按 user_id 关联多条。

---

## 八、验证清单

完成落地实现后，按以下步骤验证：

- [ ] `mvn clean install` 在 `carlos-auth` 模块通过
- [ ] carlos-auth 启动无报错，`RelyingParty` Bean 正常注入
- [ ] 登录页 `/login` 出现 Passkey Tab
- [ ] 登录后调用 `POST /auth/passkey/register/begin` → 设备弹生物识别 → `register/finish` 返回成功
- [ ] 数据库 `auth_passkey_credential` 表有新增记录
- [ ] 退出登录 → 在 Passkey Tab 触发登录 → 设备弹生物识别 → 返回 Token
- [ ] 用 Token 调用 `/userinfo` 验证身份正确
- [ ] 删除 Passkey 后，该凭据无法再登录
- [ ] HTTPS 生产环境验证（非 localhost）

---

## 九、相关文件索引

| 文件                                                                           | 作用                                      |
|------------------------------------------------------------------------------|-----------------------------------------|
| `carlos-auth-service/.../passkey/PasskeyController.java`                     | WebAuthn REST 端点                        |
| `carlos-auth-service/.../passkey/PasskeyService.java`                        | 注册/认证业务逻辑（待完善）                          |
| `carlos-auth-service/.../oauth2/web/LoginController.java`                    | 登录页渲染，控制 Passkey Tab 显隐                 |
| `carlos-auth-service/.../oauth2/web/LoginMethodValidator.java`               | `isPasskeyReady()` 降级判断                 |
| `carlos-auth-service/.../config/OAuth2Properties.java`                       | `carlos.auth.login.methods.passkey` 配置  |
| `carlos-auth-service/.../templates/login.html`                               | 登录页（含 Passkey Tab + JS）                 |
| `carlos-auth-service/.../oauth2/config/OAuth2AuthorizationServerConfig.java` | CSRF 放行 `/auth/passkey/authenticate/**` |
| `carlos-auth/docs/02-design/database/auth_postgresql.sql`                    | 待追加 `auth_passkey_credential` 表         |

---

## 十、参考标准

- [W3C Web Authentication Level 2](https://www.w3.org/TR/webauthn-2/)
- [FIDO2 Specification](https://fidoalliance.org/fido2/)
- [Yubico WebAuthn Server-Side Java Library](https://github.com/Yubico/java-webauthn-server)
- [passkeys.dev](https://passkeys.dev/)
