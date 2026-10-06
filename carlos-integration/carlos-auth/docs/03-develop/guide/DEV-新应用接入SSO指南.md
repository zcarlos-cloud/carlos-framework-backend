# 新应用接入 Carlos SSO 指南

> **适用场景**：基于 Carlos Framework 新建业务应用（前端或后端），需要接入统一认证中心。
> **前提条件**：carlos-auth 已部署并启用 SSO（OIDC 授权码模式 + PKCE）。
> **日期**：2026-07-02

---

## 一、架构概览

```
┌──────────────────────────┐     ┌─────────────────────────────┐
│  你的新应用（RP）          │     │  carlos-auth（IdP）            │
│                          │     │                             │
│  1. 用户访问受保护页面     │     │  /oauth2/authorize          │
│  2. 检测无 Token          │     │  /login（统一登录页）        │
│  3. 重定向到 IdP ─────────┼────►│  /oauth2/token              │
│                          │     │  /auth/user/me              │
│  6. 拿 code 换 Token      │     │  /oauth2/revoke             │
│  7. 存 Token，正常使用     │◄────┼  5. 302 回 redirect_uri?code │
└──────────────────────────┘     └─────────────────────────────┘
```

**你不需要实现任何登录页面。** 统一登录页在 carlos-auth 上，支持账号密码、短信、扫码、Passkey、第三方 SSO。

---

## 二、5 分钟接入步骤

### Step 1：在 carlos-auth 注册你的客户端

在 `application-auth.yml`（或数据库 `oauth2_registered_client` 表）注册新 Client：

```yaml
carlos:
  auth:
    clients:
      - client-id: my-new-app
        client-secret: ${MY_APP_SECRET:my-app-secret-change-me}
        authorization-grant-types:
          - authorization_code
          - refresh_token
        redirect-uris:
          - http://localhost:3100/oauth2/callback
          - https://my-app.carlos.com/oauth2/callback
        scopes:
          - openid
          - profile
          - read
        client-settings:
          require-authorization-consent: false
          require-proof-key: true       # SPA 必须启用 PKCE
        token-settings:
          access-token-format: self-contained
          reuse-refresh-tokens: false   # Refresh Rotation
          id-token-signature-algorithm: RS256
```

### Step 2：前端实现授权码流程

#### 2.1 发起授权（跳转到 IdP）

```typescript
function redirectToLogin() {
  const idpUrl = 'https://auth.carlos.com'; // IdP 地址
  const clientId = 'my-new-app';
  const redirectUri = `${location.origin}/oauth2/callback`;
  const state = generateRandomString(32);
  const verifier = generateRandomString(64);
  const challenge = await sha256Base64url(verifier);

  // 暂存 PKCE verifier
  sessionStorage.setItem('pkce_verifier', verifier);
  sessionStorage.setItem('oauth_state', state);

  const params = new URLSearchParams({
    response_type: 'code',
    client_id: clientId,
    redirect_uri: redirectUri,
    scope: 'openid profile',
    state,
    code_challenge: challenge,
    code_challenge_method: 'S256'
  });

  window.location.href = `${idpUrl}/oauth2/authorize?${params}`;
}
```

#### 2.2 处理回调（/oauth2/callback 页面）

```typescript
// 从 URL 取 code 和 state
const code = new URLSearchParams(location.search).get('code');
const state = new URLSearchParams(location.search).get('state');

// 校验 state
if (state !== sessionStorage.getItem('oauth_state')) {
  throw new Error('state mismatch - possible CSRF');
}

// 用 code + PKCE verifier 换 Token
const verifier = sessionStorage.getItem('pkce_verifier');
const response = await fetch('https://auth.carlos.com/oauth2/token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: `${location.origin}/oauth2/callback`,
    client_id: 'my-new-app',
    code_verifier: verifier
  })
});

const tokenData = await response.json();
// { access_token, refresh_token, expires_in, token_type, id_token }

// 存储 Token
localStorage.setItem('access_token', tokenData.access_token);
localStorage.setItem('refresh_token', tokenData.refresh_token);

// 清理 PKCE 临时数据
sessionStorage.removeItem('pkce_verifier');
sessionStorage.removeItem('oauth_state');

// 跳转到应用首页
window.location.href = '/';
```

#### 2.3 后续 API 调用

```typescript
// 每个请求携带 Bearer Token
fetch('/api/your-resource', {
  headers: {
    'Authorization': `Bearer ${localStorage.getItem('access_token')}`
  }
});
```

#### 2.4 Token 续期

```typescript
// Access Token 15分钟过期，用 Refresh Token 续期
const response = await fetch('https://auth.carlos.com/oauth2/token', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams({
    grant_type: 'refresh_token',
    refresh_token: localStorage.getItem('refresh_token'),
    client_id: 'my-new-app'
  })
});

const newTokens = await response.json();
// 重要：Refresh Rotation，新 refresh_token 替换旧的
localStorage.setItem('access_token', newTokens.access_token);
localStorage.setItem('refresh_token', newTokens.refresh_token);
```

#### 2.5 登出

```typescript
// 1. 撤销 Token
await fetch('https://auth.carlos.com/oauth2/revoke', {
  method: 'POST',
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams({
    token: localStorage.getItem('access_token'),
    client_id: 'my-new-app'
  })
});

// 2. 清理本地 Token
localStorage.removeItem('access_token');
localStorage.removeItem('refresh_token');

// 3. 可选：跳到 IdP 登出页，让所有系统都登出
window.location.href = 'https://auth.carlos.com/auth/user/logout?redirect_url=' + 
  encodeURIComponent(location.origin);
```

---

## 三、后端服务接入（Resource Server）

如果你的新应用是 Spring Boot 微服务，需要校验 Token：

### 3.1 添加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### 3.2 配置

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.carlos.com
          # 或直接指定 JWK Set URI
          jwk-set-uri: https://auth.carlos.com/oauth2/jwks
```

### 3.3 SecurityFilterChain

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/public/**").permitAll()
            .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(Customizer.withDefaults())
        );
    return http.build();
}
```

### 3.4 获取当前用户信息

```java
@GetMapping("/api/me")
public Map<String, Object> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
    return Map.of(
        "userId", jwt.getClaim("user_id"),
        "username", jwt.getClaim("username"),
        "roles", jwt.getClaim("role_ids"),
        "tenantId", jwt.getClaim("tenant_id")
    );
}
```

---

## 四、SSO 效果验证

### 验证步骤

1. 打开应用 A（如 carlos-ui），未登录 → 跳转到 IdP `/login` 统一登录页
2. 在统一登录页输入密码登录 → 回调回应用 A → 拿到 Token → 正常使用
3. **打开应用 B（你的新应用）**，未登录 → 跳转到 IdP `/oauth2/authorize`
4. **关键：IdP 检测到 Session Cookie 已存在（步骤 2 种下的）→ 直接下发 code**
5. 回调回应用 B → 拿到 Token → 正常使用 → **无需再次输入密码**

### 验证 SSO 登出

1. 在应用 A 调用 `/auth/user/logout` → IdP Session 失效
2. 刷新应用 B → Token 已过期（或 back-channel logout 已通知）→ 跳转重新登录

---

## 五、高级场景

### 5.1 服务间调用（client_credentials）

不需要用户参与的后端服务间调用：

```yaml
# 注册一个 client_credentials 客户端
carlos:
  auth:
    clients:
      - client-id: my-backend-service
        client-secret: ${SERVICE_SECRET}
        authorization-grant-types:
          - client_credentials
        scopes:
          - server
```

```bash
# 获取 Token
curl -X POST https://auth.carlos.com/oauth2/token \
  -d "grant_type=client_credentials&client_id=my-backend-service&client_secret=xxx&scope=server"
```

### 5.2 多租户（Home Realm Discovery）

如果你的应用是多租户的，可以在授权请求中传递 `login_hint` 让 IdP 识别租户：

```
/oauth2/authorize?...&login_hint=tenant:acme
```

IdP 登录页根据 `login_hint` 决定是否跳转到租户自己的 IdP（SAML/LDAP）。

### 5.3 第三方系统接入（非 Carlos 框架）

任何支持 OAuth2/OIDC 的系统都可以接入：

| 平台 | 配置方式 |
|------|----------|
| Spring Security | `spring.security.oauth2.client.registration` |
| Next.js (NextAuth) | provider 配置 authorization/token/userinfo URL |
| Django | django-allauth OIDC provider |
| Go (golang.org/x/oauth2) | oauth2.Config + PKCE |
| .NET (IdentityModel) | OIDC Client 配置 |

标准 OIDC Discovery 端点：
```
https://auth.carlos.com/.well-known/openid-configuration
```

---

## 六、安全检查清单

接入前确认以下安全要求：

- [ ] SPA（纯前端）客户端启用 PKCE（`require-proof-key: true`）
- [ ] SPA 不使用 client_secret（公开客户端）
- [ ] 回调地址 `redirect_uri` 精确匹配（不支持通配符）
- [ ] state 参数校验（防 CSRF）
- [ ] Token 存储在 HttpOnly Cookie（BFF 模式）或 localStorage（SPA 模式）
- [ ] Refresh Token 使用 Rotation（`reuse-refresh-tokens: false`）
- [ ] Access Token TTL ≤ 15 分钟
- [ ] 登出时撤销 Token + 清理本地存储
- [ ] 后端 Resource Server 验证 JWT 签名 + issuer + audience

---

## 七、常见问题

### Q: PKCE 是什么？为什么 SPA 必须用？

**A**: PKCE（Proof Key for Code Exchange）是 OAuth2.1 强制要求 SPA 使用的安全机制。SPA 无法安全存储 `client_secret`（源码可查看），PKCE 用一次性 `code_verifier`/`code_challenge` 对来证明"发起授权的和换 Token 的是同一个客户端"，防止授权码劫持。

### Q: 我的应用和 carlos-auth 不同域，Cookie 怎么共享？

**A**: 不需要共享 Cookie。SSO 的 Session Cookie 只存在 IdP 域下（`auth.carlos.com`）。你的应用跳到 IdP 时，浏览器自动携带 IdP 域下的 Cookie。这就是 OIDC 的设计精髓——**不需要同域**。

### Q: 如果 IdP 挂了，我的应用还能用吗？

**A**: 已登录的用户不受影响（Token 验签是本地操作，只需 JWK 公钥缓存）。但新用户登录会失败。建议 IdP 做高可用部署。

### Q: 我不想每次都跳 IdP，能保持静默登录吗？

**A**: 可以。在 `<iframe>` 中请求 `/oauth2/authorize?prompt=none`，如果 IdP Session 有效会静默返回 code，无效返回 `login_required` 错误。这是 OIDC 的 Silent Authentication 机制。

---

> **参考文档**：
> - [ARCH-20260701-统一登录体系架构设计-v1.0.md](../02-design/architecture/ARCH-20260701-统一登录体系架构设计-v1.0.md)
> - [DEV-前端登录全流程对接指南.md](./DEV-前端登录全流程对接指南.md)
> - [ARCH-OAuth2-SSO架构澄清-v1.0.md](../02-design/architecture/ARCH-OAuth2-SSO架构澄清-v1.0.md)
