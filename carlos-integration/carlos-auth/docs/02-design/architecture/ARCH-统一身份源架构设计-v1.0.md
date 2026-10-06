# Carlos 统一身份源架构设计方案

> **文档说明**：本文档阐述 `carlos-auth` + `carlos-org` 双模块下的统一身份认证架构，旨在解决多身份源（本地密码、短信、钉钉、企微、飞书、LDAP/AD、SAML
> 等）的扩展与接入问题。

---

## 一、设计背景与目标

### 1.1 核心挑战

随着业务发展，系统需要陆续接入多种身份源：

- **本地凭证**：用户名密码、短信验证码、邮箱验证码
- **OAuth2/OIDC 社交/企业登录**：钉钉、企微、飞书、微信
- **企业 SSO（SAML/Ws-Fed）**：ADFS、Okta、OneLogin
- **企业内部互信**：LDAP/AD、IP 白名单、JWT 透传

不同身份源的认证协议、交互方式、回调路径差异巨大，如果每种都硬编码一条特殊流程，会导致 `carlos-auth` 代码膨胀、维护困难。

### 1.2 设计目标

1. **统一入口**：前端始终调用 `carlos-org` 的 `/org/auth/login`（或 BFF 聚合接口），无需关心后端具体走哪种身份源。
2. **协议分层**：`carlos-auth` 作为纯 OAuth2 Authorization Server，只处理 Token 协议；`carlos-org` 作为用户主数据模块，负责用户校验与绑定。
3. **插件化扩展**：新增一种身份源只需新增一个 `IdentityProvider` 适配器，不改动核心认证代码。
4. **数据归一化**：无论用户从哪个身份源进来，最终都绑定到 `org_user` 主账号体系。

---

## 二、按协议形态分类登录方式

将各类登录方式按**认证协议**分层，便于后续路由：

| 类别                      | 代表                 | 交互方式                              | 关键特征                                    |
|-------------------------|--------------------|-----------------------------------|-----------------------------------------|
| **本地凭证**                | 用户名密码、短信、邮箱        | 后端直接校验                            | 一次请求，即时返回 Token                         |
| **OAuth2/OIDC 社交/企业**   | 钉钉、企微、飞书、微信        | 浏览器重定向 `authorize → code → token` | 需要回调地址（callback），走 `authorization_code` |
| **企业 SSO（SAML/Ws-Fed）** | ADFS、Okta、OneLogin | 浏览器重定向到 IdP 登录页                   | 需要 SAML Assertion 解析和 SP-Initiated 流程   |
| **企业 LDAP/AD**          | 域账号                | 后端直接绑定 LDAP Server                | 一次请求，auth 直连 LDAP 校验                    |
| **企业内部互信**              | 某系统的 JWT 透传、IP 白名单 | 后端直接校验                            | 需要专门的 Token 解析器                         |

**设计原则**：

- **浏览器重定向类**（OAuth2/SAML）必须走标准协议端点，因为它们依赖 `callback`。
- **后端直连类**（LDAP/内部互信/手机号一键登录）可以走自定义 Grant Type 或 BFF 代理。

---

## 三、整体架构设计

```
┌─────────────────────────────────────────────────────────────┐
│  前端（Web/App/小程序）                                       │
│  统一调用 /org/auth/login（JSON，由 carlos-org 暴露）         │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  carlos-org（BFF 聚合层）                                    │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ OrgAuthController                                      │  │
│  │ - 接收前端 JSON 请求                                    │  │
│  │ - 根据 loginType 路由到不同处理器                        │  │
│  └───────────────────────────────────────────────────────┘  │
│                              │                              │
│              ┌───────────────┼───────────────┐              │
│              ▼               ▼               ▼              │
│        ┌─────────┐    ┌──────────┐    ┌──────────┐         │
│        │本地登录  │    │OAuth2代理 │    │SAML代理  │         │
│        │处理器   │    │处理器     │    │处理器     │         │
│        └────┬────┘    └────┬─────┘    └────┬─────┘         │
│             │              │               │                │
│             ▼              ▼               ▼                │
│        Feign Auth    302 重定向        302 重定向            │
│        /oauth2/token 到钉钉/企微      到企业 ADFS             │
│                        callback         callback             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  carlos-auth（OAuth2 Authorization Server）                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ /oauth2/token（标准端点）                               │  │
│  │ - password（用户名密码）                                │  │
│  │ - sms_code（短信验证码）                                │  │
│  │ - email_code（邮箱验证码）                              │  │
│  │ - dingtalk（钉钉授权码）                                │  │
│  │ - wecom（企微授权码）                                   │  │
│  │ - ldap（LDAP 绑定认证）                                 │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ /oauth2/authorize（标准端点）                           │  │
│  │ 用于浏览器重定向类的授权码流程                            │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ callback 端点（自定义）                                 │  │
│  │ - /auth/callback/dingtalk                              │  │
│  │ - /auth/callback/wecom                                 │  │
│  │ - /auth/callback/saml/{idpId}                          │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  统一身份源适配层（Auth 内部）                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ DingTalkIdP │  │  WeComIdP   │  │  LdapIdP    │         │
│  │   Adapter   │  │   Adapter   │  │   Adapter   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│         ...            ...            ...                    │
│              都实现 IdentityProvider 接口                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 四、核心抽象：统一身份源接口 `IdentityProvider`

### 4.1 接口定义

```java
public interface IdentityProvider {
    
    /**
     * 身份源唯一标识
     */
    String getProviderId();
    
    /**
     * 身份源类型：oauth2, saml, ldap, local
     */
    String getProviderType();
    
    /**
     * 认证方式：password, sms_code, dingtalk, wecom, ldap
     */
    Set<String> getSupportedGrantTypes();
    
    /**
     * 后端直接校验类登录
     * @param request 认证请求上下文
     * @return 认证成功后的用户信息
     */
    UserIdentity authenticate(IdentityProviderRequest request);
    
    /**
     * 获取授权重定向 URL（仅 OAuth2/SAML 类需要）
     */
    String buildAuthorizeUrl(String state, String redirectUri);
    
    /**
     * 处理回调/授权码（仅 OAuth2/SAML 类需要）
     */
    UserIdentity handleCallback(Map<String, String> callbackParams);
}
```

### 4.2 领域模型

```java
@Data
public class UserIdentity {
    private String providerId;      // 来自哪个身份源
    private String providerUserId;  // 第三方系统的用户ID（如钉钉的 openId）
    private String username;        // 映射到本系统的用户名
    private String email;
    private String phone;
    private Set<String> roleCodes;
    private Map<String, Object> extraAttrs;  // 钉钉的 deptId 等扩展属性
    private boolean isNewUser;      // 是否需要自动注册
}
```

---

## 五、各类登录方式的具体接入路径

### 5.1 本地登录（密码 / 短信 / 邮箱）

**流程**：

1. 前端调用 `carlos-org` 的 `/org/auth/login`（JSON）
2. org BFF 层转发到 `carlos-auth` 的 `/oauth2/token`（`grant_type=password/sms_code/email_code`）
3. auth 的 `ExtendAuthenticationProvider` 路由到 `LocalIdentityProvider`
4. `LocalIdentityProvider` 通过 Feign 调用 org 的用户校验接口
5. 校验通过后，`UserBindingService` 做绑定/注册
6. auth 签发标准 OAuth2 Token，返回给 org
7. org 组装 `userInfo` 返回前端

### 5.2 OAuth2 类（钉钉 / 企微 / 飞书 / 微信）

**流程**：

1. 前端调用 `/org/auth/sso-url?provider=dingtalk`
2. org 调 auth 获取 state，auth 返回钉钉授权地址
3. 前端重定向到钉钉授权页
4. 用户授权后，钉钉回调 `carlos-auth` 的 `/auth/callback/dingtalk`
5. auth 的 `DingTalkIdentityProvider` 用 `code` 换取 access_token
6. 调钉钉 API 获取用户信息
7. `UserBindingService` 绑定/注册用户
8. auth 签发 Token，302 重定向回前端 callback（带 token）

**示例代码**：

```java
@Component
public class DingTalkIdentityProvider implements IdentityProvider {
    
    @Override
    public String getProviderId() { return "dingtalk"; }
    
    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        return "https://oapi.dingtalk.com/connect/oauth2/sns_authorize"
            + "?appid=" + appId
            + "&response_type=code"
            + "&scope=snsapi_auth"
            + "&state=" + state
            + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
    }
    
    @Override
    public UserIdentity handleCallback(Map<String, String> params) {
        String code = params.get("code");
        
        // 1. 用 code 获取 persistent_code 和 openid
        // 2. 用 persistent_code + openid 获取 sns_token
        // 3. 用 sns_token 获取用户信息
        DingTalkUserInfo dtUser = dingTalkApiClient.getUserInfo(snsToken);
        
        return UserIdentity.builder()
            .providerId("dingtalk")
            .providerUserId(dtUser.getOpenid())
            .username("dingtalk_" + dtUser.getOpenid())
            .phone(dtUser.getMobile())
            .extraAttrs(Map.of("deptId", dtUser.getDeptIdList()))
            .build();
    }
}
```

### 5.3 SAML 类（ADFS / Okta / OneLogin）

**流程**：

1. 前端调用 `/org/auth/sso/{idpId}`
2. org 调 auth 的 `/auth/saml/login/{idpId}`
3. auth 生成 SAML AuthnRequest，302 到企业 ADFS
4. ADFS 用户登录后，POST SAML Response 回 `/auth/saml/acs/{idpId}`
5. auth 解析 SAML Assertion，获取 NameID 和属性
6. `UserBindingService` 绑定/注册用户
7. 签发 Token，302 重定向回前端

**示例代码**：

```java
@Component
public class SamlIdentityProvider implements IdentityProvider {
    
    @Override
    public String buildAuthorizeUrl(String state, String redirectUri) {
        String samlRequest = generateAuthnRequest();
        return idpLoginUrl + "?SAMLRequest=" + Base64.encode(samlRequest);
    }
    
    @Override
    public UserIdentity handleCallback(Map<String, String> params) {
        String samlResponse = params.get("SAMLResponse");
        SamlAssertion assertion = samlParser.parse(samlResponse);
        
        return UserIdentity.builder()
            .providerId(idpId)
            .providerUserId(assertion.getNameId())
            .username(assertion.getNameId())
            .email(assertion.getAttribute("email"))
            .build();
    }
}
```

### 5.4 LDAP/AD 类

**流程**：

1. 前端调用 `/org/auth/login`（JSON，带 `loginType=ldap`）
2. org BFF 转发到 auth 的 `/oauth2/token`
3. auth 的 `LdapIdentityProvider` 直连 AD Server 绑定认证
4. 查询用户属性，做绑定/注册
5. 返回 Token

**示例代码**：

```java
@Component
public class LdapIdentityProvider implements IdentityProvider {
    
    @Override
    public UserIdentity authenticate(IdentityProviderRequest request) {
        String username = (String) request.getPrincipal();
        String password = (String) request.getCredentials();
        
        DirContext ctx = ldapTemplate.getContext(
            "uid=" + username + "," + baseDn, 
            password
        );
        
        Attributes attrs = ldapTemplate.lookup(
            "uid=" + username + "," + baseDn
        );
        
        return UserIdentity.builder()
            .providerId("ldap")
            .providerUserId(username)
            .username(username)
            .email(getAttr(attrs, "mail"))
            .phone(getAttr(attrs, "mobile"))
            .build();
    }
}
```

---

## 六、关键扩展点设计

### 6.1 身份源注册中心 `IdentityProviderRegistry`

```java
@Component
public class IdentityProviderRegistry {
    
    private final Map<String, IdentityProvider> providerMap = new ConcurrentHashMap<>();
    
    public void register(IdentityProvider provider) {
        providerMap.put(provider.getProviderId(), provider);
    }
    
    public IdentityProvider findByGrantType(String grantType) {
        return providerMap.values().stream()
            .filter(p -> p.getSupportedGrantTypes().contains(grantType))
            .findFirst()
            .orElseThrow(() -> new UnsupportedOperationException("不支持的登录方式"));
    }
    
    public IdentityProvider findByProviderId(String providerId) {
        return providerMap.get(providerId);
    }
}
```

### 6.2 用户绑定与自动注册 `UserBindingService`

```java
@Service
public class UserBindingService {
    
    private final ApiOrgAuth apiOrgAuth;
    
    public OrgUserAO bindOrCreateUser(UserIdentity identity) {
        // 1. 查询是否已绑定
        OrgUserAO existingUser = apiOrgAuth.findByProviderUserId(
            identity.getProviderId(), 
            identity.getProviderUserId()
        ).getData();
        
        if (existingUser != null) {
            return existingUser;
        }
        
        // 2. 根据手机号/邮箱查找已有用户
        if (StringUtils.hasText(identity.getPhone())) {
            existingUser = apiOrgAuth.findByPhone(identity.getPhone()).getData();
        }
        if (existingUser == null && StringUtils.hasText(identity.getEmail())) {
            existingUser = apiOrgAuth.findByEmail(identity.getEmail()).getData();
        }
        
        if (existingUser != null) {
            apiOrgAuth.bindIdentityProvider(existingUser.getId(), identity);
            return existingUser;
        }
        
        // 3. 自动注册新用户
        if (identity.isNewUser()) {
            return apiOrgAuth.createUserFromIdentity(identity).getData();
        }
        
        throw new BusinessException("用户未绑定，请先绑定账号");
    }
}
```

### 6.3 org 侧 BFF 统一入口 `OrgAuthController`

```java
@RestController
@RequestMapping("/org/auth")
@RequiredArgsConstructor
public class OrgAuthController {

    private final OrgLoginService loginService;
    private final OrgSsoService ssoService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid OrgLoginRequest request) {
        return Result.success(loginService.login(request));
    }

    @GetMapping("/sso-url")
    public Result<String> getSsoUrl(
        @RequestParam("provider") String providerId,
        @RequestParam("redirectUri") String redirectUri) {
        return Result.success(ssoService.buildSsoUrl(providerId, redirectUri));
    }

    @GetMapping("/userinfo")
    public Result<OrgUserVO> getUserInfo(@RequestHeader("Authorization") String token) {
        // 解析 token，查 org 用户
        return Result.success(loginService.getUserInfo(token));
    }
}
```

---

## 七、数据表设计建议（carlos-org 模块）

### 7.1 身份源配置表

```sql
CREATE TABLE org_identity_provider (
    id BIGINT PRIMARY KEY,
    provider_id VARCHAR(64) NOT NULL COMMENT '唯一标识：dingtalk, wecom, ldap',
    provider_name VARCHAR(128) COMMENT '显示名称',
    provider_type VARCHAR(32) NOT NULL COMMENT '类型：oauth2, saml, ldap, local',
    enabled TINYINT DEFAULT 1,
    config_json TEXT COMMENT 'JSON 配置：appId, appSecret, ldapUrl 等',
    create_time DATETIME,
    update_time DATETIME
);
```

### 7.2 用户身份源绑定表

```sql
CREATE TABLE org_user_identity_bind (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT 'org_user 表主键',
    provider_id VARCHAR(64) NOT NULL,
    provider_user_id VARCHAR(256) NOT NULL COMMENT '第三方用户唯一标识',
    provider_username VARCHAR(256),
    extra_attrs TEXT COMMENT '扩展属性 JSON',
    bind_time DATETIME,
    UNIQUE KEY uk_provider_user (provider_id, provider_user_id)
);
```

---

## 八、模块职责边界

| 职责              | carlos-auth                            | carlos-org                               |
|-----------------|----------------------------------------|------------------------------------------|
| **Token 协议**    | 纯 OAuth2 Authorization Server，维护标准端点   | 不处理 Token 协议                             |
| **用户数据**        | 不持有用户主数据                               | 维护 `org_user` 主账号体系                      |
| **身份源适配**       | 实现 `IdentityProvider` 协议适配器            | 不直接对接第三方协议                               |
| **用户校验**        | 通过 Feign 调用 org 的校验接口                  | 提供用户校验、查询、注册接口                           |
| **前端入口**        | 提供 `/oauth2/token`、`/oauth2/authorize` | 提供 `/org/auth/login`、`/org/auth/sso-url` |
| **用户绑定**        | 调用 org 的绑定/注册接口                        | 维护 `org_user_identity_bind` 表            |
| **Callback 处理** | 接收 `/auth/callback/{provider}`         | 不参与                                      |

---

## 九、接入新身份源的成本评估

以**接入飞书**为例，所需工作量：

1. **新建适配器**：在 `carlos-auth` 中新建 `FeishuIdentityProvider implements IdentityProvider`（约 100 行）
2. **配置注册**：在 `org_identity_provider` 表插入一条配置（`provider_id=feishu`，`provider_type=oauth2`）
3. **前端适配**：前端无改动，直接调 `/org/auth/sso-url?provider=feishu`
4. **用户绑定**：复用 `UserBindingService`，无需额外开发

**时间估算**：1-2 天即可完成一个企业 OAuth2 身份源的接入。

---

## 十、演进路线图建议

### 阶段一：本地登录标准化（已完成 + 待完善）

- 将 `password`、`sms_code`、`email_code` 注册为 auth 的自定义 Grant Type
- org 建立 BFF 层 `/org/auth/login`

### 阶段二：引入统一身份源接口（当前优先级最高）

- 定义 `IdentityProvider` 接口
- 建立 `IdentityProviderRegistry`
- 建立 `UserBindingService`
- 改造 `ExtendAuthenticationProvider` 统一路由到 `IdentityProvider`

### 阶段三：接入第一个 OAuth2 身份源（如钉钉）

- 实现 `DingTalkIdentityProvider`
- 建立 `/auth/callback/dingtalk`
- 前端跑通完整授权码流程

### 阶段四：接入 SAML/LDAP

- 引入 SAML2 解析库（如 `spring-security-saml2-service-provider`）
- 实现 `SamlIdentityProvider`
- 实现 `LdapIdentityProvider`

### 阶段五：多身份源统一用户中心

- 支持用户在个人中心绑定/解绑多个身份源
- 支持管理员在后台配置和启停身份源
- 支持按租户配置不同的身份源策略

---

> **文档版本**：3.0.0  
> **适用模块**：`carlos-integration/carlos-auth`、`carlos-integration/carlos-org`  
> **最后更新**：2026-04-14
