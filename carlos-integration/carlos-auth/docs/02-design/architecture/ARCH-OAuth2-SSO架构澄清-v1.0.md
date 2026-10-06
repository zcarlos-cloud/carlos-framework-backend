# Carlos Framework 认证体系与 SSO 架构澄清文档

> 本文档梳理 Carlos Framework 中 `carlos-auth`、`carlos-org`、`carlos-gateway` 及前端在认证流程中的职责边界，明确 OAuth2
> 端点作用、SSO 实现条件及演进路线。

---

## 一、关键术语对照表

| 术语                  | 中文         | 在 Carlos Framework 中对应                     | 一句话解释                          |
|---------------------|------------|--------------------------------------------|--------------------------------|
| **IdP**             | 身份提供商      | `carlos-auth`                              | **唯一知道用户密码的地方**，负责验证"你是谁"并签发凭证 |
| **RP**              | 依赖方 / 客户端  | `carlos-ui`、ERP 系统、第三方 SaaS                | 需要用户身份才能提供服务的系统                |
| **Resource Server** | 资源服务器      | `carlos-org`、所有业务微服务                       | 持有业务数据，只认 Token 不认密码           |
| **Client**          | OAuth2 客户端 | 每个接入系统在 `carlos-auth` 中的注册记录               | 代表一个应用向 IdP 申请 Token 的身份       |
| **Grant Type**      | 授权类型       | `authorization_code`、`password`、`sms_code` | OAuth2 规定的"以什么方式换 Token"       |
| **Session Cookie**  | 会话 Cookie  | 浏览器在 `carlos-auth` 域名下保存的 Cookie           | **SSO 的核心**，让 IdP 记住用户已登录      |

---

## 二、Carlos 中的认证端点地图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         carlos-auth (IdP)                              │
├─────────────────────────────────────────────────────────────────────────┤
│  端点                     │ 作用              │ 调用方                  │
├──────────────────────────┼──────────────────┼─────────────────────────┤
│  /oauth2/authorize       │ 授权端点          │ 浏览器直接跳转           │
│  /oauth2/token           │ 换 Token          │ BFF 后端、第三方应用     │
│  /oauth2/revoke          │ 撤销 Token        │ BFF 后端                │
│  /oauth2/jwks            │ 公钥获取          │ Gateway、Resource Server │
│  /userinfo               │ 获取用户信息(OIDC)│ Resource Server         │
│  /login                  │ IdP 登录页        │ 浏览器直接访问           │
│  /auth/user/login        │ 自定义 JSON 登录  │ 前端 AJAX (如 carlos-ui) │
└─────────────────────────────────────────────────────────────────────────┘
```

### 各端点详细说明

#### 1. `/oauth2/authorize` —— SSO 的入口

- **谁调用**：浏览器直接跳转（`window.location.href = ...`），**不是 AJAX**
- **干什么**：检查用户是否在 `carlos-auth` 登录过
    - 已登录 → 直接发 `code`，跳转回原系统
    - 未登录 → **302 跳转到 `/login`**（IdP 自己的登录页）
- **能否输密码/短信验证码**：**不能**，它只是把你带到登录页

#### 2. `/login` —— IdP 的登录页与认证提交入口

- **谁调用**：
    - **GET `/login`**：浏览器被 `/oauth2/authorize` 重定向过来，显示登录页
    - **POST `/login`**：用户在登录页输入账号密码后，表单提交到此地址进行认证
- **干什么**：
    - 显示登录表单（账号密码、短信验证码、扫码等）
    - 接收表单提交，调用 Spring Security 的 `UsernamePasswordAuthenticationFilter` 进行认证
    - 认证成功后，在 `carlos-auth` 域下**种下 Session Cookie**，并跳转回 `/oauth2/authorize`
- **标准性**：这不是 OAuth2 标准端点，是 IdP 内部实现，**你可以任意定制 UI 和认证方式**
- **Carlos 现状**：`OAuth2AuthorizationServerConfig` 的 `defaultSecurityFilterChain` 中**没有配置 `.formLogin()`**，所以
  `UsernamePasswordAuthenticationFilter` 未注册，**当前实际上不存在有效的 `POST /login` 处理器**

#### 3. `/oauth2/token` —— 真正的 Token 工厂

- **谁调用**：BFF 后端、第三方系统的后端
- **干什么**：用各种 `grant_type` 换取 `access_token`
- **关键参数**：
    - `grant_type=authorization_code` → 用 `/authorize` 给的 `code` 换 Token（**标准 SSO 流程**）
    - `grant_type=password` → 用用户名密码直接换 Token（**已废弃，不安全**）
    - `grant_type=client_credentials` → 服务间调用

#### 4. `/auth/user/login` —— Carlos 的自定义便利接口

- **谁调用**：前端 AJAX（如 `carlos-ui`）
- **干什么**：封装了密码校验 + Token 生成，返回 JSON 格式的 Token
- **与标准流程的区别**：
    - ✅ 用户体验好（前端不用跳转）
    - ❌ **不是 SSO**，因为不走浏览器重定向，`carlos-auth` 不会种下跨系统共享的 Session Cookie

---

## 三、三种登录流程对比

### 流程 A：Authorization Code + PKCE（标准 SSO，推荐）

```
┌─────────┐                                  ┌─────────────┐
│ 浏览器   │ ──1.访问系统A──────────────────► │  carlos-auth │
│ (用户)  │    (未登录，重定向到 /oauth2/authorize) │    (IdP)     │
└────┬────┘                                  └──────┬──────┘
     │                                              │
     │ ◄────────────2. 302 跳转到 /login───────────┘
     │         (IdP 检查未登录，要求输密码)
     │
     │ ──3.在 /login 页面输入账号密码，点击提交─────►
     │    POST /login (被 UsernamePasswordAuthenticationFilter 拦截)
     │         ├── 提取 username、password
     │         ├── 构造 UsernamePasswordAuthenticationToken
     │         ├── 调用 AuthenticationManager.authenticate()
     │         │       └── ExtendUserDetailsService.loadUserByUsername()
     │         │           └── UserProvider.loadUserByIdentifier()
     │         │               └── Feign 调 carlos-org 获取用户信息
     │         ├── 密码比对（BCrypt/SM4）
     │         ├── SecurityContextHolder 设置认证信息
     │         ├── 创建 HttpSession，种下 SESSION Cookie
     │         └── CustomAuthenticationSuccessHandler 处理跳转
     │                                              │
     │ ◄────────────4. 认证成功，302 跳转回 /oauth2/authorize
     │              (从 RequestCache 取出原始请求)
     │
     │ ──5.再次访问 /oauth2/authorize──────────────►
     │    (这次带着 Cookie，IdP 认出了用户)
     │
     │ ◄────────────6. 下发 code，跳转回系统A───────┘
     │
     │ ──7.系统A用 code 调 /oauth2/token────────────►
     │                                              │
     │ ◄────────────8. 返回 access_token────────────┘
     │
     │
     │ ──9.用户访问系统B────────────────────────────►
     │    (同样跳转到 /oauth2/authorize)
     │                                              │
     │ ◄───────────10. 直接下发 code（因为 Session Cookie 还在）
     │              【这就是 SSO：无需再次输密码】
```

**特点**：

- ✅ 真正的 SSO
- ✅ 安全（前端不碰密码，不存 Refresh Token）
- ⚠️ 需要浏览器重定向，前端改动较大

---

### 流程 B：Password Grant（当前 `carlos-org` 代理的方式，不推荐）

```
前端 (carlos-ui)
    │
    ▼
POST /org/auth/login  ──►  carlos-org  ──► Feign 调 /oauth2/token
(JSON 用户名密码)         (BFF 代理)         (grant_type=password)
                                              │
                                              ▼
                                         carlos-auth 返回 Token
                                              │
                                              ▼
                                         返回给 carlos-org
                                              │
                                              ▼
                                         返回给前端
```

**特点**：

- ❌ **不是 SSO**（没有 Session Cookie，系统 B 不认）
- ❌ `/oauth2/token` 缺少风控（IP 封禁、账号锁定）
- ❌ Feign 调用丢失真实 IP
- ❌ `password` grant 已被 OAuth2.1 移除

---

### 流程 C：自定义 JSON 登录（`/auth/user/login`，过渡期可用）

```
前端 (carlos-ui)
    │
    ▼
POST /auth/user/login ──►  carlos-auth
(JSON 用户名密码)          (UserAuthController)
                            │
                            ├── 校验密码（调 UserProvider → carlos-org）
                            ├── IP 限流 / 账号锁定 / 风控
                            └── 生成 Token 返回 JSON
```

**特点**：

- ✅ 风控完整（IP 封禁、账号锁定都在）
- ✅ 前端体验好（AJAX，无跳转）
- ❌ **仍然不是 SSO**
- ⚠️ 只适合单一系统或过渡期

---

## 四、登录页 `/login` 的认证接口设计与流程

### 4.1 登录页提交后，调用什么接口？

**`POST /login`**

这是 Spring Security 表单登录的默认提交地址。当用户在登录页输入账号密码后，浏览器发起：

```http
POST /login
Content-Type: application/x-www-form-urlencoded

username=admin&password=123456
```

### 4.2 谁处理这个 POST 请求？

**`UsernamePasswordAuthenticationFilter`**

这是 Spring Security 内置的 Filter，默认拦截 `POST /login`，处理逻辑：

1. 从请求中提取 `username` 和 `password`
2. 构造 `UsernamePasswordAuthenticationToken`
3. 交给 `AuthenticationManager` 认证
4. 认证成功后：
    - `SecurityContextHolder` 设置认证信息
    - 调用 `SessionAuthenticationStrategy` 创建 HttpSession（种下 Cookie）
    - 调用 `AuthenticationSuccessHandler` 跳转回原始请求

### 4.3 Carlos 现状：缺少有效的 `POST /login` 处理器

`OAuth2AuthorizationServerConfig.defaultSecurityFilterChain()` 当前配置：

```java
http
    .authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/login", "/oauth2/**", "/auth/user/login", ...).permitAll()
        .anyRequest().authenticated()
    )
    .csrf(AbstractHttpConfigurer::disable)
    .exceptionHandling(exceptions ->
        exceptions.authenticationEntryPoint(new UnauthorizedEntryPoint())
    );
// 注意：这里没有 .formLogin()！
```

**结论**：`/login` 虽然被放行，但**没有 `UsernamePasswordAuthenticationFilter` 注册**，所以当前 `POST /login`
无法处理。框架实际可用的登录入口只有 `/auth/user/login`（JSON API）。

### 4.4 如何补齐 `/login` 的认证能力？

#### 配置层：启用 Spring Security 表单登录

修改 `OAuth2AuthorizationServerConfig.defaultSecurityFilterChain()`：

```java
@Bean
@Order(2)
public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers("/login", "/login/sms", "/oauth2/**", "/auth/captcha/**", "/error")
            .permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form
            .loginPage("/login")                    // GET /login 显示登录页
            .loginProcessingUrl("/login")           // POST /login 处理表单登录
            .usernameParameter("username")
            .passwordParameter("password")
            .successHandler(customAuthenticationSuccessHandler())
            .failureHandler(customAuthenticationFailureHandler())
        )
        .csrf(AbstractHttpConfigurer::disable);
    return http.build();
}
```

#### 页面层：提供登录页

最简单方式：在 `carlos-auth-service/src/main/resources/static/login.html` 放置静态页面：

```html
<!DOCTYPE html>
<html>
<head><title>Carlos 统一登录</title></head>
<body>
    <h2>统一身份认证</h2>
    <form action="/login" method="post">
        <input type="text" name="username" placeholder="用户名/手机号/邮箱" required />
        <input type="password" name="password" placeholder="密码" required />
        <button type="submit">登录</button>
    </form>
    <a href="/login?type=sms">短信验证码登录</a>
</body>
</html>
```

#### 认证扩展层：支持短信验证码登录

如果要支持短信验证码，需要添加自定义 Filter：

```java
public class SmsCodeAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    public SmsCodeAuthenticationFilter() {
        super(new AntPathRequestMatcher("/login/sms", "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        String phone = request.getParameter("phone");
        String smsCode = request.getParameter("sms_code");
        SmsAuthenticationToken authRequest = new SmsAuthenticationToken(phone, smsCode);
        return this.getAuthenticationManager().authenticate(authRequest);
    }
}
```

并在 `defaultSecurityFilterChain` 中注册：

```java
http.addFilterBefore(smsCodeAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

### 4.5 认证成功后如何回到 `/oauth2/authorize`？

Spring Security 的 `SavedRequestAwareAuthenticationSuccessHandler` 会自动处理：

1. 用户首次访问 `/oauth2/authorize` 时，Spring Security 将原始请求保存到 `RequestCache`（Session 中）
2. 用户被重定向到 `/login`
3. 认证成功后，`SavedRequestAwareAuthenticationSuccessHandler` 从 `RequestCache` 取出原始请求
4. 自动 302 重定向回 `/oauth2/authorize`
5. 此时用户已登录（Session Cookie 存在），`/oauth2/authorize` 直接下发 `code`

Carlos 已有的 `CustomAuthenticationSuccessHandler` 可以继承 `SavedRequestAwareAuthenticationSuccessHandler`：

```java
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
    public CustomAuthenticationSuccessHandler() {
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // 记录审计日志、重置失败次数
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
```

### 4.6 `/login` 表单登录与 `password` grant 的本质区别

| 维度                         | `/login` 表单登录                                  | `grant_type=password`                                               |
|----------------------------|------------------------------------------------|---------------------------------------------------------------------|
| **HTTP 方式**                | `POST /login`（浏览器表单）                           | `POST /oauth2/token`（API 调用）                                        |
| **Spring Security Filter** | `UsernamePasswordAuthenticationFilter`         | `OAuth2ClientAuthenticationFilter` + `ExtendAuthenticationProvider` |
| **认证成功后产生什么**              | **Session Cookie**（支持 SSO）                     | **Access Token**（不支持 SSO）                                           |
| **底层密码校验**                 | 复用 `ExtendUserDetailsService` / `UserProvider` | 复用 `ExtendUserDetailsService` / `UserProvider`                      |
| **OAuth2 标准状态**            | ✅ IdP 内部实现，合规                                  | ❌ OAuth2.1 已移除                                                      |
| **Carlos 框架注释**            | 推荐做法                                           | `CustomGrantTypes.PASSWORD` 明确标注"已被移除"                              |

**关键结论**：两者的密码校验底层复用同一套 `UserProvider` / `ExtendUserDetailsService`，但前者产生 **Session Cookie**（支持
SSO），后者只产生 **Token**（不支持 SSO）。

### 4.7 风控如何接入 `/login` 表单登录？

**前置拦截**（账号锁定检查）放在 `ExtendUserDetailsService` 中：

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (loginAttemptManager.isAccountLocked(username)) {
        throw new LockedException("账号已锁定，请15分钟后再试");
    }
    // ... 正常加载用户
}
```

**后置事件**（成功/失败计数）通过 Spring Security 事件解耦：

```java
@Component
public class AuthenticationSecurityEventListener {

    @Autowired
    private LoginAttemptManager loginAttemptManager;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        loginAttemptManager.recordLoginSuccess(event.getAuthentication().getName());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        loginAttemptManager.recordLoginFailure(event.getAuthentication().getName());
    }
}
```

---

## 五、Carlos 各模块的职责定位

| 模块                          | 应该做什么                            | 不应该做什么            |
|-----------------------------|----------------------------------|-------------------|
| **`carlos-auth`** (IdP)     | 用户认证、Token 签发、SSO 会话管理、登录风控、审计日志 | 直接操作用户业务表（如部门、岗位） |
| **`carlos-org`** (用户中心)     | 用户 CRUD、部门管理、角色权限分配、提供用户查询 API   | **代理登录**、签发 Token |
| **`carlos-gateway`** (统一入口) | Token 校验、用户上下文注入、IP 限流、白名单管理     | 认证逻辑、密码校验         |
| **`carlos-ui`** (前端)        | 调用登录接口、存储 Token、展示页面             | 知道用户密码（长期目标）      |

---

## 六、登录风控应该放在哪里？

| 风险类型                   | 最佳放置位置                                   | 原因                                  |
|------------------------|------------------------------------------|-------------------------------------|
| **IP 限流**（如 50次/分钟）    | `carlos-gateway`                         | 在入口处挡掉恶意流量，保护后端                     |
| **IP 封禁**（如 20次失败封1小时） | `carlos-auth` + Gateway 协同               | Gateway 可做粗粒度拦截，`carlos-auth` 做精准拦截 |
| **账号锁定**（如 5次失败锁15分钟）  | `carlos-auth` 的 `AuthenticationProvider` | 只有这里知道"密码是否输错"                      |
| **审计日志**               | `carlos-auth`                            | 登录事件统一在这里发生                         |
| **验证码/短信限流**           | `carlos-auth`                            | 与认证流程强耦合                            |

### 当前问题的根因与解法

**问题**：`carlos-org` 通过 Feign 代理 `/oauth2/token`，绕过了 `UserAuthController` 的风控，且 `/oauth2/token` 本身没有账号锁定逻辑。

**标准解法**：

1. **立即**：废弃 `carlos-org` 代理登录，让前端直接调 `carlos-auth` 的 `/auth/user/login`（恢复风控）。
2. **中期**：在 `carlos-auth` 的 `ExtendAuthenticationProvider` 中集成 `LoginAttemptManager`，确保 `/oauth2/token` 也受保护。
3. **长期**：迁移到 `/oauth2/authorize` 标准流程，补齐 `/login` 的 `.formLogin()` 配置和登录页，风控自然收敛到 IdP。

---

## 七、SSO 必须满足的条件

> **用户在 IdP 上登录一次后，访问其他系统时无需再次输入密码。**

要实现这一点，必须满足：

### 条件 1：浏览器在 IdP 域下有共享的 Session Cookie

- `/oauth2/authorize` 流程通过浏览器重定向，天然满足
- `/auth/user/login` 是 AJAX，**无法满足**（AJAX 不能跨域写 Cookie）

### 条件 2：所有系统都信任同一个 IdP

- 系统 A、B、C 都跳转 `carlos-auth` 的 `/oauth2/authorize`
- 不是各自写自己的登录接口

### 条件 3：Token 可以统一撤销

- 用户在系统 A 登出，系统 B 的 Token 也应失效
- 通过 `carlos-auth` 的 `/logout` + Token 黑名单实现

---

## 八、推荐演进路线

```
                    现在                          中期                          长期
                     │                             │                             │
    前端登录          │   POST /auth/user/login     │   POST /auth/user/login     │  跳转 /oauth2/authorize
    (carlos-ui)      │   (直接打到 carlos-auth)     │   (直接打到 carlos-auth)     │  (标准 OIDC)
                     │                             │                             │
    carlos-org       │   只做用户数据管理            │   只做用户数据管理            │  只做用户数据管理
                     │   ❌ 不再代理登录             │   ❌ 不再代理登录             │  ❌ 不再代理登录
                     │                             │                             │
    carlos-auth      │   ✅ 风控完整                 │   ✅ 补齐 /login 页面 +      │  ✅ 完整 SSO/IdP
                     │   /auth/user/login           │   /oauth2/token 风控         │   第三方系统可注册接入
                     │                             │                             │
    新系统接入        │   只能调 /auth/user/login    │   可选：自定义接口或 /authorize │  直接注册 OAuth2 Client
```

### 各阶段目标

| 阶段       | 时间     | 核心任务                                                                                                           | 产出                            |
|----------|--------|----------------------------------------------------------------------------------------------------------------|-------------------------------|
| **第一阶段** | 1-2 周  | 废弃 `carlos-org` 代理登录，前端直接调 `/auth/user/login`；`carlos-org` 回归用户管理                                              | 解决风控缺失和 IP 透传问题               |
| **第二阶段** | 1 个月   | 给 `carlos-auth` 添加 `.formLogin()` 配置和 `/login` 页；给 `/oauth2/token` 补齐 `LoginAttemptManager` + `IpBlockManager` | 支持标准授权码模式；`/oauth2/token` 也安全 |
| **第三阶段** | 2-3 个月 | `carlos-ui` 接入 `oidc-client-ts`；所有新系统走 `/oauth2/authorize`                                                     | 实现真正的 SSO                     |

---

## 九、常见误区澄清

| 误区                                 | 正解                                                                      |
|------------------------------------|-------------------------------------------------------------------------|
| "用 `/oauth2/token` 就是标准 OAuth2"    | `password` grant 虽然走 `/oauth2/token`，但已被 OAuth2.1 移除，不是安全标准             |
| "所有系统调同一个接口就是 SSO"                 | SSO 需要**浏览器重定向**和**IdP 共享 Session Cookie**，AJAX 接口无法实现                  |
| "`/oauth2/authorize` 不支持短信验证码"     | `/oauth2/authorize` 只负责跳转，**真正的认证在 `/login` 页**，你可以放任何登录方式              |
| "SSO 必须废弃 `/auth/user/login`"      | 短期可以保留作为内部系统入口，长期建议统一走 `/oauth2/authorize`                              |
| "Gateway 应该做账号锁定判断"                | Gateway 不知道密码对不对，**账号锁定必须在 `carlos-auth` 完成**                           |
| "`/login` 和 `password` grant 是一回事" | 底层密码校验复用同一套逻辑，但前者产生 **Session Cookie**（支持 SSO），后者只产生 **Token**（不支持 SSO） |

---

## 总结：一句话记住三件事

1. **`carlos-auth` 是唯一的 IdP**，所有认证最终必须 converge 到这里。
2. **要 SSO，必须走 `/oauth2/authorize` + 浏览器重定向**；`/auth/user/login` 只是过渡方案。
3. **风控必须在知道"密码对错"的地方做**，即 `carlos-auth` 的认证核心层。
