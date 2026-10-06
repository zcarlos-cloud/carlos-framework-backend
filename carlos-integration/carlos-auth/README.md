# Carlos OAuth2 统一认证模块

Carlos OAuth2 是基于 Spring Authorization Server 1.x 构建的统一认证授权模块，兼容 Spring Boot 3.x，提供完整的 OAuth2.1 和
OpenID Connect 1.0 支持。

## 功能特性

- **授权服务器** - 支持 OAuth2.1 和 OpenID Connect 1.0 协议
- **资源服务器** - JWT Token 验证和权限提取
- **多种授权模式** - 授权码模式、客户端凭证模式、扩展的密码模式、短信验证码模式
- **第三方登录** - 支持微信、钉钉、短信等第三方登录方式
- **国密支持** - 支持 SM4 国密算法加密
- **多租户支持** - 内置租户/客户端隔离能力
- **可扩展设计** - 提供丰富的扩展点，兼容大多数业务场景

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-auth-service</artifactId>
</dependency>
```

### 2. 基础配置

#### 授权服务器配置（认证中心）

```yaml
carlos:
  oauth2:
    enabled: true
    
    # 授权服务器配置
    authorization-server:
      enabled: true
      # Token 有效期
      access-token-time-to-live: 2h
      refresh-token-time-to-live: 7d
      # 每次刷新都生成新令牌（更安全）
      reuse-refresh-tokens: false
    
    # JWT 配置
    jwt:
      issuer: https://auth.example.com
      key-id: auth-key-1
      include-user-info: true
    
    # 注册客户端
    clients:
      - client-id: web-app
        client-secret: web-secret
        client-name: Web Application
        authorization-grant-types:
          - authorization_code
          - refresh_token
        redirect-uris:
          - http://localhost:8080/login/oauth2/code/carlos
        scopes:
          - read
          - write
        require-authorization-consent: false
```

#### 资源服务器配置（业务服务）

```yaml
carlos:
  oauth2:
    enabled: true
    
    # 资源服务器配置
    resource-server:
      enabled: true
      # 从认证中心获取公钥
      jwk-set-uri: http://auth-server:9000/oauth2/jwks
      # 或
      # issuer-uri: http://auth-server:9000
      
      # 白名单路径
      permit-all-paths:
        - /api/public/**
        - /actuator/health
```

### 3. 实现用户服务

生产环境必须实现 `ExtendUserDetailsService` 接口：

```java
@Service
@Primary
public class MyUserDetailsService implements ExtendUserDetailsService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        // 2. 查询权限
        List<GrantedAuthority> authorities = loadAuthorities(user.getId());
        
        // 3. 构建 UserDetails
        return User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(authorities)
            .build();
    }
    
    @Override
    public LoginUserInfo loadLoginUserInfo(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return null;
        }
        
        LoginUserInfo info = new LoginUserInfo();
        info.setId(user.getId());
        info.setAccount(user.getUsername());
        info.setRoleIds(loadRoleIds(user.getId()));
        info.setClientId(user.getTenantId()); // 多租户
        
        return info;
    }
}
```

## 配置详解

### OAuth2 总开关

```yaml
carlos:
  oauth2:
    enabled: true  # 是否启用 OAuth2 功能，默认 true
```

### 授权服务器配置

```yaml
carlos:
  oauth2:
    authorization-server:
      enabled: false                      # 是否启用授权服务器
      authorization-endpoint: /oauth2/authorize      # 授权端点
      token-endpoint: /oauth2/token                  # Token 端点
      token-revocation-endpoint: /oauth2/revoke      # 撤销端点
      token-introspection-endpoint: /oauth2/introspect  # 自省端点
      jwk-set-endpoint: /oauth2/jwks                 # JWK Set 端点
      oidc-user-info-endpoint: /userinfo             # 用户信息端点
      
      # Token 有效期
      authorization-code-time-to-live: 5m   # 授权码有效期
      access-token-time-to-live: 2h         # 访问令牌有效期
      refresh-token-time-to-live: 7d        # 刷新令牌有效期
      device-code-time-to-live: 5m          # 设备码有效期
      
      reuse-refresh-tokens: false           # 是否重用刷新令牌
      oidc-enabled: true                    # 是否启用 OIDC
```

### 资源服务器配置

```yaml
carlos:
  oauth2:
    resource-server:
      enabled: false                      # 是否启用资源服务器
      jwk-set-uri:                        # JWK Set URI（推荐）
      issuer-uri:                         # Issuer URI（自动发现）
      permit-all-paths:                   # 白名单路径
        - /api/public/**
```

**注意**：`jwk-set-uri` 和 `issuer-uri` 至少配置一个。

### JWT 配置

```yaml
carlos:
  oauth2:
    jwt:
      algorithm: RS256                    # 签名算法（RS256/ES256/HS256）
      private-key-path:                   # 私钥路径（可选）
      public-key-path:                    # 公钥路径（可选）
      key-id: carlos-key                  # 密钥 ID
      issuer: http://localhost:8080       # Issuer 标识
      include-user-info: true             # 是否包含用户信息
      custom-claims:                      # 自定义声明
        app-version: 3.0.0
        env: production
```

### 客户端配置

```yaml
carlos:
  oauth2:
    clients:
      - client-id: web-app                # 客户端 ID
        client-secret: secret             # 客户端密钥
        client-name: Web Application      # 客户端名称
        authorization-grant-types:        # 授权类型
          - authorization_code
          - refresh_token
        redirect-uris:                    # 重定向 URI
          - http://localhost:8080/callback
        scopes:                           # 作用域
          - read
          - write
        require-authorization-consent: false   # 是否需要授权确认
        require-proof-key: false              # 是否需要 PKCE
        access-token-time-to-live: 1h       # 客户端特定的访问令牌有效期
        refresh-token-time-to-live: 30d     # 客户端特定的刷新令牌有效期
```

### 安全配置

```yaml
carlos:
  oauth2:
    security:
      password-encoder: bcrypt            # 密码编码器（bcrypt/sm4）
      token-storage: memory               # Token 存储（memory/redis/jdbc）
      login-limit:                        # 登录限制
        enabled: true
        max-attempts: 5                   # 最大失败次数
        lock-duration: 30m                # 锁定时间
```

## 第三方登录

支持微信、钉钉、短信验证码等第三方登录方式。

### 1. 启用第三方登录

```yaml
carlos:
  oauth2:
    third-party:
      enabled: true       # 总开关
      wechat: true        # 启用微信登录
      dingtalk: true      # 启用钉钉登录
      sms: true           # 启用短信登录
      
      # 微信配置
      wechat-config:
        app-id: wx_xxx
        app-secret: xxx
        redirect-uri: http://localhost:8080/callback/wechat
      
      # 钉钉配置
      dingtalk-config:
        app-id: ding_xxx
        app-secret: xxx
        redirect-uri: http://localhost:8080/callback/dingtalk
      
      # 短信配置
      sms-config:
        code-expire-minutes: 5
        daily-limit: 10
        provider: aliyun  # aliyun / tencent
```

### 2. 客户端配置

```yaml
carlos:
  oauth2:
    clients:
      - client-id: mobile-app
        authorization-grant-types:
          - wx_code      # 微信登录
          - dt_code      # 钉钉登录
          - sms_code     # 短信登录
```

### 3. 实现登录服务

#### 微信登录示例

```java
@Service("wechatLoginService")
@Primary
public class WechatLoginServiceImpl implements ThirdPartyLoginService {
    
    @Autowired
    private WxMpService wxMpService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public LoginUserInfo login(String code) {
        try {
            // 1. 用 code 换取 access_token 和 openid
            WxMpOAuth2AccessToken token = wxMpService.oauth2getAccessToken(code);
            String openid = token.getOpenId();
            
            // 2. 获取用户信息
            WxMpUser wxUser = wxMpService.oauth2getUserInfo(token, null);
            
            // 3. 查找或创建本地用户
            User user = userMapper.findByWechatOpenId(openid);
            if (user == null) {
                // 创建新用户
                user = new User();
                user.setAccount("wx_" + openid.substring(0, 8));
                user.setWechatOpenId(openid);
                user.setNickname(wxUser.getNickname());
                user.setAvatar(wxUser.getHeadImgUrl());
                userMapper.insert(user);
            }
            
            // 4. 返回登录信息
            LoginUserInfo info = new LoginUserInfo();
            info.setId(user.getId());
            info.setAccount(user.getAccount());
            info.setEnable(user.getStatus() == 1);
            info.setRoleIds(Collections.singleton(2L)); // 普通用户角色
            
            return info;
            
        } catch (WxErrorException e) {
            log.error("微信登录失败", e);
            throw new ThirdPartyLoginException("微信登录失败: " + e.getMessage(), ThirdPartyType.WECHAT);
        }
    }
    
    @Override
    public ThirdPartyType getType() {
        return ThirdPartyType.WECHAT;
    }
}
```

#### 钉钉登录示例

```java
@Service("dingtalkLoginService")
public class DingtalkLoginServiceImpl implements ThirdPartyLoginService {
    
    @Autowired
    private DingTalkClient dingTalkClient;
    
    @Override
    public LoginUserInfo login(String code) {
        // 1. 获取用户信息
        OapiUserGetuserinfoResponse response = dingTalkClient.getUserInfo(code);
        
        if (!response.isSuccess()) {
            throw new ThirdPartyLoginException("钉钉登录失败", ThirdPartyType.DINGTALK);
        }
        
        String userid = response.getUserid();
        
        // 2. 获取用户详情
        OapiUserGetResponse userDetail = dingTalkClient.getUserDetail(userid);
        
        // 3. 查找或创建本地用户
        User user = userMapper.findByDingtalkUserId(userid);
        if (user == null) {
            user = createUserFromDingtalk(userDetail);
        }
        
        // 4. 返回登录信息
        return convertToLoginUserInfo(user);
    }
    
    @Override
    public ThirdPartyType getType() {
        return ThirdPartyType.DINGTALK;
    }
}
```

#### 短信登录示例

```java
@Service("smsLoginService")
public class SmsLoginServiceImpl implements ThirdPartyLoginService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private UserMapper userMapper;
    
    @Override
    public LoginUserInfo login(String authCode) {
        // authCode 格式: mobile:code
        String[] parts = authCode.split(":");
        if (parts.length != 2) {
            throw new ThirdPartyLoginException("验证码格式错误", ThirdPartyType.SMS);
        }
        
        String mobile = parts[0];
        String code = parts[1];
        
        // 1. 验证验证码
        String cacheKey = "sms:code:" + mobile;
        String cacheCode = redisTemplate.opsForValue().get(cacheKey);
        
        if (cacheCode == null || !cacheCode.equals(code)) {
            throw new ThirdPartyLoginException("验证码错误或已过期", ThirdPartyType.SMS);
        }
        
        // 2. 验证通过后删除验证码
        redisTemplate.delete(cacheKey);
        
        // 3. 查找或创建用户
        User user = userMapper.findByMobile(mobile);
        if (user == null) {
            user = createUserByMobile(mobile);
        }
        
        // 4. 返回登录信息
        return convertToLoginUserInfo(user);
    }
    
    @Override
    public ThirdPartyType getType() {
        return ThirdPartyType.SMS;
    }
}
```

### 4. 使用第三方登录

#### 微信登录请求

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "client-id:client-secret" \
  -d "grant_type=wx_code" \
  -d "code=WECHAT_AUTH_CODE"
```

#### 钉钉登录请求

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "client-id:client-secret" \
  -d "grant_type=dt_code" \
  -d "code=DINGTALK_AUTH_CODE"
```

#### 短信登录请求

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "client-id:client-secret" \
  -d "grant_type=sms_code" \
  -d "mobile=13800138000" \
  -d "code=123456"
```

## 扩展点

### 1. 自定义用户详情服务

```java
@Service
@Primary
public class CustomUserDetailsService implements ExtendUserDetailsService {
    // 实现方法...
}
```

### 2. 自定义 Token 增强

```java
@Bean
public OAuth2TokenCustomizer<JwtEncodingContext> customTokenCustomizer() {
    return context -> {
        context.getClaims().claim("custom_key", "custom_value");
    };
}
```

### 3. 自定义客户端存储

```java
@Service
@Primary
public class JdbcRegisteredClientRepository implements RegisteredClientRepository {
    // 实现方法...
}
```

### 4. 自定义授权存储

```java
@Service
@Primary
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {
    // 实现方法...
}
```

### 5. 自定义第三方登录

```java
// 1. 创建 Token
public class CustomAuthenticationToken extends BaseAuthenticationToken {
    // ...
}

// 2. 创建 Converter
public class CustomAuthenticationConverter extends BaseAuthenticationConverter<CustomAuthenticationToken> {
    @Override
    public boolean support(String grantType) {
        return "custom".equals(grantType);
    }
    // ...
}

// 3. 实现登录服务
@Service("customLoginService")
public class CustomLoginServiceImpl implements ThirdPartyLoginService {
    @Override
    public LoginUserInfo login(String authCode) {
        // 自定义登录逻辑
    }
    
    @Override
    public ThirdPartyType getType() {
        return ThirdPartyType.CUSTOM;
    }
}
```

## 端点说明

| 端点       | 路径                                | 说明           |
|----------|-----------------------------------|--------------|
| 授权端点     | /oauth2/authorize                 | 获取授权码（授权码模式） |
| Token 端点 | /oauth2/token                     | 获取访问令牌       |
| 撤销端点     | /oauth2/revoke                    | 撤销令牌         |
| 自省端点     | /oauth2/introspect                | 查询令牌信息       |
| JWK Set  | /oauth2/jwks                      | 获取公钥         |
| 用户信息     | /userinfo                         | OIDC 用户信息    |
| OIDC 配置  | /.well-known/openid-configuration | OIDC 发现端点    |

## 使用示例

### 获取 Token（客户端凭证模式）

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "client-id:client-secret" \
  -d "grant_type=client_credentials" \
  -d "scope=read"
```

### 获取 Token（授权码模式）

```bash
# 1. 获取授权码（浏览器访问）
http://localhost:8080/oauth2/authorize?response_type=code&client_id=web-app&redirect_uri=http://localhost:8080/callback&scope=read

# 2. 用授权码换取 Token
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "client-id:client-secret" \
  -d "grant_type=authorization_code" \
  -d "code=AUTHORIZATION_CODE" \
  -d "redirect_uri=http://localhost:8080/callback"
```

### 刷新 Token

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "client-id:client-secret" \
  -d "grant_type=refresh_token" \
  -d "refresh_token=REFRESH_TOKEN"
```

### 访问受保护资源

```bash
curl -X GET http://localhost:8081/api/protected \
  -H "Authorization: Bearer ACCESS_TOKEN"
```

## 方法级安全

```java
@RestController
public class OrderController {
    
    @PreAuthorize("hasAuthority('SCOPE_read')")
    @GetMapping("/orders")
    public List<Order> list() {
        // ...
    }
    
    @PreAuthorize("hasRole('1')")  // role_id 为 1 的角色
    @PostMapping("/orders")
    public Order create(@RequestBody Order order) {
        // ...
    }
    
    @PreAuthorize("#userId == authentication.principal.claims['user_id']")
    @GetMapping("/users/{userId}")
    public User get(@PathVariable Long userId) {
        // 只能查看自己的信息
    }
}
```

## 注意事项

1. **生产环境必须实现自定义用户服务** - 默认实现仅用于开发测试
2. **生产环境建议使用 Redis 存储 Token** - 默认内存存储重启后失效
3. **建议使用 JWK Set URI 验证 Token** - 支持密钥轮换
4. **客户端密钥必须强度足够** - 建议使用随机生成的复杂字符串
5. **配置 HTTPS** - 生产环境必须使用 HTTPS
6. **第三方登录需要实现对应的登录服务** - 默认实现仅打印日志

## 版本兼容性

- Spring Boot: 3.5.9+
- Spring Security: 6.x
- Spring Authorization Server: 1.x
- JDK: 17+

## 迁移指南

### 从 Spring Boot 2.x / SAS 0.4.x 迁移

1. 更新依赖版本
2. 修改配置属性前缀：`yj.oauth` → `carlos.oauth2`
3. 检查 SecurityConfig 中的 deprecated API
4. 更新 JWT 解码器配置

### 从 carlos-spring-boot-starter-oauth2 迁移

1. 移除旧依赖
2. 添加 carlos-auth-service 依赖
3. 配置属性完全兼容，无需修改
