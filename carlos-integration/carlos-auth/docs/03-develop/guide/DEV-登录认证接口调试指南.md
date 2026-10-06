# Carlos Auth 登录认证接口调试指南

> **文档说明**：本文档详细梳理了 `/auth/user/login` 接口的完整调用链路、依赖的 Spring Security / Spring Authorization
> Server 内部机制，以及关键断点位置，供认证接口调试和问题排查使用。

---

## 一、为什么 `/auth/user/login` 需要 clientId？

### 1.1 核心原因：复用了 OAuth2 Authorization Server 的 Token 生成机制

`/auth/user/login` 是一个**自定义 REST 登录接口**（不是标准 OAuth2 的 `/oauth2/token` 端点），但 `UserLoginService`
在登录成功后，内部调用了 `buildLoginResponse()`，而这个方法**完全复用了 Spring Authorization Server 的 Token 生成基础设施
**：

- `OAuth2TokenGenerator.generate(OAuth2TokenContext)`
- `OAuth2AuthorizationService.save(OAuth2Authorization)`

Spring Authorization Server 的设计要求：**任何 Token 的生成都必须关联一个 `RegisteredClient`**。`RegisteredClient` 中包含了：

- Token 有效期（`accessTokenTimeToLive`、`refreshTokenTimeToLive`）
- 授权作用域（`scopes`）
- 支持的授权类型（`authorizationGrantTypes`）
- 客户端认证方式（`clientAuthenticationMethod`）
- 其他客户端级配置（PKCE、Consent 等）

因此，生成 JWT 时框架必须知道“这是哪个客户端发起的请求”，否则 `tokenGenerator.generate(context)` 无法工作。

### 1.2 当前代码中的 clientId 来源

```java
// UserLoginService.java
// 通过 OAuth2Properties.getLogin().getDefaultClientId() 获取
```

在 `buildLoginResponse()` 中：

```java
RegisteredClient registeredClient = registeredClientRepository.findByClientId(defaultClientId);
```

**重要澄清**：虽然 `LoginRequest` DTO 里有一个 `clientId` 字段，但当前 `UserLoginService.login()` **完全没有读取它**
，而是固定使用配置中的 `defaultClientId`。

也就是说：**目前前端传不传 `clientId` 都不影响逻辑**，除非你将 `buildLoginResponse` 改为 `loginRequest.getClientId()` 优先。

### 1.3 如果没有 RegisteredClient 会怎样？

如果 `registeredClientRepository.findByClientId(defaultClientId)` 返回 `null`，会直接抛异常：

```java
throw AuthErrorCode.AUTH_CLIENT_NOT_FOUND.exception("默认客户端配置不存在: %s", defaultClientId);
```

所以 `carlos-client` 必须在 `RegisteredClientRepository` 中存在（当前由 `OAuth2AuthorizationServerConfig` 在内存中默认创建）。

---

## 二、完整方法调用链路

### 链路 A：HTTP 请求入口 → Controller → Service

```
HTTP POST /auth/user/login
  │
  ▼
【DispatcherServlet.doDispatch()】
  │
  ▼
UserAuthController.login(LoginRequest, HttpServletRequest)
  │   📍 断点 1：验证请求参数、检查前置拦截
  │
  ├─ RateLimitUtil.tryAcquire("auth:rate:ip:" + clientIp, ...)
  ├─ RateLimitUtil.tryAcquire("auth:rate:login:" + username, ...)
  ├─ ipBlockManager.isIpBlocked(request)
  ├─ loginService.isAccountLocked(username)
  │     └─ LoginAttemptManager.isAccountLocked(username)
  │
  └─ UserLoginService.login(LoginRequest)
        │   📍 断点 2：进入核心登录逻辑
        │
        ├─ new UsernamePasswordAuthenticationToken(username, password)
        ├─ AuthenticationManager.authenticate(token)
        │     │   📍 断点 3：进入 Spring Security 认证链路（见链路 B）
        │     └─ 返回 Authentication（已认证成功）
        │
        ├─ userProvider.loadUserByIdentifier(username)
        │     │   📍 断点 4：加载业务用户信息
        │     └─ DefaultUserProvider.loadUserByIdentifier()
        │           └─ ConcurrentHashMap 查询（内存中的 admin/user/test）
        │
        ├─ buildLoginResponse(user, authentication)
        │     │   📍 断点 5：Token 生成核心（见链路 C）
        │     └─ 返回 LoginResponse
        │
        ├─ recordLoginSuccess(username)   // 重置失败计数
        ├─ recordLoginAudit(user, "LOGIN", "SUCCESS", null)  // 异步审计
        └─ 返回 LoginResponse
  │
  ▼
Result.success(loginResponse, "登录成功")
```

#### 本链路常见异常点

| 异常                                       | 触发位置                                   | 现象                        |
|------------------------------------------|----------------------------------------|---------------------------|
| `BadCredentialsException`                | `AuthenticationManager.authenticate()` | 用户名或密码错误                  |
| `InternalAuthenticationServiceException` | `AuthenticationManager.authenticate()` | 认证服务内部错误                  |
| `AuthErrorCode.AUTH_CLIENT_NOT_FOUND`    | `buildLoginResponse()`                 | 默认客户端 `carlos-client` 未注册 |

---

### 链路 B：AuthenticationManager 密码认证（Spring Security 内部）

```
AuthenticationManager.authenticate(UsernamePasswordAuthenticationToken)
  │
  ▼
ProviderManager.authenticate(authentication)
  │   📍 断点 6：查看 providers 列表
  │
  ├─ 遍历所有 AuthenticationProvider，调用 supports(Class)
  │     ├─ ExtendAuthenticationProvider.supports() → false（只支持 ExtendAuthenticationToken）
  │     ├─ SmsAuthenticationProvider.supports() → false
  │     └─ DaoAuthenticationProvider.supports() → true ✅
  │
  ▼
DaoAuthenticationProvider.authenticate(authentication)
  │   📍 断点 7：Spring Security 核心密码认证
  │
  ├─ retrieveUser(username, authenticationToken)
  │     │
  │     ▼
  │     DefaultExtendUserDetailsService.loadUserByUsername(username)
  │       │   📍 断点 8：自定义用户详情加载
  │       ├─ UserProvider.loadUserByIdentifier(username)
  │       │     └─ DefaultUserProvider 内存查询
  │       └─ 构建 Spring Security UserDetails
  │             User.builder()
  │               .username(userInfo.getUsername())
  │               .password(userInfo.getPassword())   // BCrypt 密文
  │               .roles(...)
  │               .accountLocked(...)
  │               .disabled(...)
  │               .build()
  │
  ├─ preAuthenticationChecks.check(userDetails)
  │     ├─ isAccountNonLocked() → 抛 LockedException
  │     ├─ isEnabled() → 抛 DisabledException
  │     └─ isAccountNonExpired()
  │
  ├─ additionalAuthenticationChecks(userDetails, token)
  │     │
  │     ▼
  │     PasswordEncoder.matches(rawPassword, encodedPassword)
  │       │   📍 断点 9：密码校验核心
  │       └─ 当前默认：BCryptPasswordEncoder.matches()
  │           或配置为 Sm4PasswordEncoder
  │
  ├─ postAuthenticationChecks.check(userDetails)
  │     └─ isCredentialsNonExpired()
  │
  └─ createSuccessAuthentication(principal, token, userDetails)
        │
        ▼
        new UsernamePasswordAuthenticationToken(
            userDetails,           // principal
            null,                  // credentials 擦除
            userDetails.getAuthorities()
        )
```

#### 本链路常见异常点

| 异常                          | 触发位置                               | 现象         |
|-----------------------------|------------------------------------|------------|
| `UsernameNotFoundException` | `retrieveUser()`                   | 用户不存在      |
| `BadCredentialsException`   | `additionalAuthenticationChecks()` | 密码不匹配（最常见） |
| `LockedException`           | `preAuthenticationChecks`          | 账号被锁定      |
| `DisabledException`         | `preAuthenticationChecks`          | 账号被禁用      |

---

### 链路 C：OAuth2 Token 生成与存储（buildLoginResponse 内部）

```
buildLoginResponse(UserInfo, Authentication)
  │
  ├─ registeredClientRepository.findByClientId("carlos-client")
  │     │   📍 断点 10：确认 RegisteredClient 存在
  │     └─ InMemoryRegisteredClientRepository.findByClientId()
  │           └─ 返回默认客户端（OAuth2AuthorizationServerConfig 中注册的）
  │
  ├─ 构建 authorizedScopes（默认取 registeredClient.getScopes()）
  │
  ├─ DefaultOAuth2TokenContext.builder()
  │     .registeredClient(registeredClient)     ← 必须有 clientId 的原因
  │     .principal(authentication)
  │     .authorizedScopes(authorizedScopes)
  │     .authorizationGrant(authentication)
  │     .tokenType(OAuth2TokenType.ACCESS_TOKEN)
  │     .build()
  │       📍 断点 11：查看 TokenContext 内容
  │
  ├─ tokenGenerator.generate(tokenContext)
  │     │
  │     ▼
  │     DelegatingOAuth2TokenGenerator.generate()
  │       │
  │       ├─ CustomizeUserOAuth2AccessTokenGenerator.generate()
  │       │     │   📍 断点 12：用户访问令牌生成
  │       │     ├─ 检查 authorizationGrantType（null 或直接登录也放行）
  │       │     ├─ 构建 JWT Claims：
  │       │     │     sub = username
  │       │     │     aud = [registeredClient.getClientId()]  ← clientId 写入 JWT
  │       │     │     iat, exp, nbf, jti = UUID
  │       │     │     scope = authorizedScopes
  │       │     │
  │       │     ├─ 触发 accessTokenCustomizer.customize(context)
  │       │     │     │
  │       │     │     ▼
  │       │     │     CustomizeUserOAuth2TokenCustomizer.customize()
  │       │     │       │   📍 断点 13：JWT 声明增强
  │       │     │       ├─ 加载 LoginUserInfo（ExtendUserDetailsService.loadLoginUserInfo）
  │       │     │       ├─ 添加 user_id, tenant_id, dept_id, role_ids
  │       │     │       ├─ 添加 authorities, email, phone, mfa_enabled
  │       │     │       └─ 返回增强后的 claims
  │       │     │
  │       │     └─ 生成 tokenValue = IdUtil.randomUUID()
  │       │           组装成 OAuth2AccessTokenClaims
  │       │
  │       └─ 返回 OAuth2AccessToken
  │
  ├─ 构建 OAuth2AccessToken（Bearer 类型）
  │
  ├─ 若支持 REFRESH_TOKEN，再次生成 RefreshToken
  │     └─ tokenGenerator.generate(refreshTokenContext)
  │           └─ OAuth2RefreshTokenGenerator.generate()  （Spring 内置）
  │
  ├─ 构建 OAuth2Authorization
  │     .id(accessToken.getTokenValue())
  │     .accessToken(accessToken)
  │     .refreshToken(refreshToken)
  │     .principalName(authentication.getName())
  │
  ├─ authorizationService.save(authorization)
  │     │   📍 断点 14：Token 持久化
  │     └─ RedisOAuth2AuthorizationService.save()
  │           └─ 写入 Redis（Key 通常为 tokenValue）
  │
  └─ 组装 LoginResponse
        ├─ accessToken
        ├─ refreshToken
        ├─ tokenType = "Bearer"
        ├─ expiresIn
        └─ userInfo (id, username, email, phone)
```

#### 本链路常见异常点

| 异常                           | 触发位置                        | 现象                                                                                                                |
|------------------------------|-----------------------------|-------------------------------------------------------------------------------------------------------------------|
| `AUTH_CLIENT_NOT_FOUND`      | `findByClientId()`          | `carlos-client` 未注册                                                                                               |
| `AUTH_TOKEN_GENERATE_FAILED` | `tokenGenerator.generate()` | `CustomizeUserOAuth2AccessTokenGenerator` 的 grantType 判断不匹配导致返回 null，然后 `DelegatingOAuth2TokenGenerator` 也返回 null |

---

## 三、关键 Bean 与配置速查表

| 组件                | 类路径                                            | 作用                                       | 注册位置                                                           |
|-------------------|------------------------------------------------|------------------------------------------|----------------------------------------------------------------|
| **认证入口**          | `UserAuthController`                           | 接收 `/auth/user/login` HTTP 请求            | Controller Bean                                                |
| **登录服务**          | `UserLoginService`                             | 编排认证 + Token 生成                          | Service Bean                                                   |
| **认证管理器**         | `ProviderManager`                              | Spring Security 核心，调度 Providers          | `AuthenticationConfiguration` 默认注入                             |
| **密码认证 Provider** | `DaoAuthenticationProvider`                    | 处理 `UsernamePasswordAuthenticationToken` | Spring Security 默认注入                                           |
| **用户详情服务**        | `DefaultExtendUserDetailsService`              | 加载 UserDetails 和 LoginUserInfo           | `OAuth2AuthorizationServerConfig.extendUserDetailsService()`   |
| **用户数据源**         | `DefaultUserProvider`                          | 内存中维护 admin/user/test 三个测试用户             | `OAuth2AuthorizationServerConfig.userProvider()`               |
| **密码编码器**         | `BCryptPasswordEncoder` / `Sm4PasswordEncoder` | 密码加密与校验                                  | `OAuth2AuthorizationServerConfig.passwordEncoder()`            |
| **客户端仓库**         | `InMemoryRegisteredClientRepository`           | 存储 `carlos-client` 等客户端信息                | `OAuth2AuthorizationServerConfig.registeredClientRepository()` |
| **Token 生成器**     | `DelegatingOAuth2TokenGenerator`               | 组合多个生成器                                  | `OAuth2AuthorizationServerConfig.oAuth2TokenGenerator()`       |
| **用户 Token 生成器**  | `CustomizeUserOAuth2AccessTokenGenerator`      | 生成 Access Token（UUID 形式）                 | 上一步组合注入                                                        |
| **Token 增强器**     | `CustomizeUserOAuth2TokenCustomizer`           | 向 JWT 中添加 user_id、role_ids 等             | `OAuth2AuthorizationServerConfig.jwtTokenCustomizer()`         |
| **Token 存储**      | `RedisOAuth2AuthorizationService`              | 保存 OAuth2Authorization 到 Redis           | `OAuth2AuthorizationServerConfig.authorizationService()`       |
| **JWT 签名密钥**      | `JWKSource<SecurityContext>`                   | RSA 密钥对，用于 JWT 签名                        | `OAuth2AuthorizationServerConfig.jwkSource()`                  |

---

## 四、关键配置项（application.yml）

```yaml
carlos:
  oauth2:
    login:
      default-client-id: carlos-client   # 默认客户端，决定用哪个 RegisteredClient 生成 Token
      access-token-ttl: 7200             # 访问令牌有效期（秒）
      refresh-token-ttl: 604800          # 刷新令牌有效期（秒）
    authorization-server:
      enabled: true
      access-token-time-to-live: 2h
      refresh-token-time-to-live: 7d
    jwt:
      issuer: http://localhost:8080
      include-user-info: true            # 是否向 JWT 中注入用户详细信息
```

---

## 五、断点调试指南

| 断点编号      | 目标方法                                                   | 调试目的                                |
|-----------|--------------------------------------------------------|-------------------------------------|
| **断点 1**  | `UserAuthController.login()`                           | 确认请求参数、IP 限流、封禁检查是否通过               |
| **断点 2**  | `UserLoginService.login()`                             | 确认进入核心登录逻辑，查看 `LoginRequest` 内容     |
| **断点 3**  | `AuthenticationManager.authenticate()`                 | 进入 Spring Security 认证链路             |
| **断点 4**  | `UserProvider.loadUserByIdentifier()`                  | 确认业务用户信息加载结果                        |
| **断点 5**  | `UserLoginService.buildLoginResponse()`                | 确认认证成功，准备生成 Token                   |
| **断点 6**  | `ProviderManager.authenticate()`                       | 查看 `providers` 列表，确认哪个 Provider 被选中 |
| **断点 7**  | `DaoAuthenticationProvider.authenticate()`             | Spring Security 核心密码认证入口            |
| **断点 8**  | `DefaultExtendUserDetailsService.loadUserByUsername()` | 自定义用户详情加载                           |
| **断点 9**  | `PasswordEncoder.matches()`                            | 密码校验核心，排查密码不匹配问题                    |
| **断点 10** | `registeredClientRepository.findByClientId()`          | 确认 `carlos-client` 是否存在             |
| **断点 11** | `DefaultOAuth2TokenContext.builder().build()`          | 查看 TokenContext 内容                  |
| **断点 12** | `CustomizeUserOAuth2AccessTokenGenerator.generate()`   | 用户访问令牌生成                            |
| **断点 13** | `CustomizeUserOAuth2TokenCustomizer.customize()`       | JWT 声明增强，确认 user_id、role_ids 等注入    |
| **断点 14** | `authorizationService.save()`                          | Token 持久化，确认 Redis 写入               |

---

## 六、常见问题排查

### 6.1 请求 `/auth/user/login` 返回 302

**原因**：Spring Security 未认证请求被 `LoginUrlAuthenticationEntryPoint` 拦截，重定向到 `/login` 页面。

**排查步骤**：

1. 检查 `OAuth2AuthorizationServerConfig.defaultSecurityFilterChain()` 是否已放行 `/auth/user/login`
2. 检查是否已将 `LoginUrlAuthenticationEntryPoint` 替换为 `UnauthorizedEntryPoint`（返回 401 JSON）
3. 检查 Gateway 层白名单是否正确转发（Gateway 白名单放行不代表服务内部也放行）

### 6.2 报错 `AUTH_CLIENT_NOT_FOUND`

**原因**：`registeredClientRepository.findByClientId(defaultClientId)` 返回 null。

**排查步骤**：

1. 确认 `application.yml` 中 `carlos.auth.login.default-client-id` 的值
2. 确认 `OAuth2AuthorizationServerConfig.registeredClientRepository()` 是否正确注册了该客户端
3. 如果自定义了 `RegisteredClientRepository` Bean，确保其中包含对应 clientId

### 6.3 报错 `AUTH_TOKEN_GENERATE_FAILED`

**原因**：`tokenGenerator.generate(context)` 返回 null。

**排查步骤**：

1. 在 **断点 12** 查看 `CustomizeUserOAuth2AccessTokenGenerator.generate()`
2. 检查 `context.getAuthorizationGrantType()` 是否为 null 或不被 `CustomGrantTypes.isCustomGrantType()` 识别
3. 检查 `context.getTokenType()` 是否为 `OAuth2TokenType.ACCESS_TOKEN`

### 6.4 JWT 中缺少 user_id / role_ids

**原因**：`CustomizeUserOAuth2TokenCustomizer` 加载用户信息失败或用户信息为空。

**排查步骤**：

1. 在 **断点 13** 查看 `loadLoginUserInfo(username)` 和 `loadUserInfo(username)` 的返回值
2. 确认 `ExtendUserDetailsService` 和 `UserProvider` 是否正确返回了业务数据
3. 检查 `carlos.auth.jwt.include-user-info` 是否为 `true`

---

> **文档版本**：3.0.0  
> **适用模块**：`carlos-integration/carlos-auth`  
> **最后更新**：2026-04-14
