# Spring Cloud Gateway 4.x 过滤器详解

> 本文档详细分析 Spring Cloud Gateway 4.x 版本的过滤器类型、使用方式及适用场景。
>
> **版本信息**：Spring Cloud Gateway 4.x（基于 Spring Boot 3.x 和 Spring Framework 6.x）

---

## 目录

1. [过滤器概述](#一过滤器概述)
2. [GatewayFilter 过滤器](#二gatewayfilter-过滤器)
3. [GlobalFilter 全局过滤器](#三globalfilter-全局过滤器)
4. [过滤器工厂详解](#四过滤器工厂详解)
5. [自定义过滤器](#五自定义过滤器)
6. [过滤器执行顺序](#六过滤器执行顺序)
7. [典型应用场景](#七典型应用场景)

---

## 一、过滤器概述

### 1.1 什么是 Gateway Filter

Spring Cloud Gateway 的过滤器用于处理进入网关的请求和从网关返回的响应。过滤器可以在请求被路由之前或之后执行特定的逻辑，实现各种横切关注点，如：

- 认证授权
- 限流熔断
- 日志记录
- 请求/响应修改
- 路径重写
- 重试机制

### 1.2 过滤器分类

```
Spring Cloud Gateway 过滤器
├── GatewayFilter（路由级别）
│   ├── 内置 GatewayFilter Factory
│   │   ├── 修改请求类（AddRequestHeader, SetPath 等）
│   │   ├── 修改响应类（AddResponseHeader, SetStatus 等）
│   │   ├── 路径操作类（RewritePath, StripPrefix 等）
│   │   ├── 重试限流类（Retry, RequestRateLimiter 等）
│   │   └── 安全类（SetRequestHeader 脱敏等）
│   └── 自定义 GatewayFilter
│
└── GlobalFilter（全局级别）
    ├── 内置 GlobalFilter
    │   ├── ReactiveLoadBalancerClientFilter（负载均衡）
    │   ├── NettyRoutingFilter（Netty 路由）
    │   ├── NettyWriteResponseFilter（响应写入）
    │   ├── RouteToRequestUrlFilter（URL 转换）
    │   ├── WebsocketRoutingFilter（WebSocket 路由）
    │   └── ForwardRoutingFilter（本地转发）
    └── 自定义 GlobalFilter
```

---

## 二、GatewayFilter 过滤器

### 2.1 配置方式

#### 2.1.1 快捷配置（Shortcut Configuration）

适用于简单参数的场景：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - StripPrefix=1                    # 去掉第1个路径段
            - AddRequestHeader=X-Request-From,Gateway  # 添加请求头
            - AddResponseHeader=X-Response-From,Gateway # 添加响应头
```

#### 2.1.2 完整参数配置（Fully Expanded Arguments）

适用于复杂参数或需要明确指定参数名的场景：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
          filters:
            - name: Retry                     # 过滤器名称
              args:
                retries: 3                    # 重试次数
                statuses: BAD_GATEWAY         # 重试的HTTP状态码
                methods: GET,POST             # 重试的HTTP方法
                backoff:
                  firstBackoff: 50ms
                  maxBackoff: 500ms
                  factor: 2
                  basedOnPreviousValue: false
```

#### 2.1.3 Java DSL 配置

```java
@Configuration
public class GatewayConfig {
    
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("user-service", r -> r
                .path("/api/user/**")
                .filters(f -> f
                    .stripPrefix(1)
                    .addRequestHeader("X-Request-From", "Gateway")
                    .circuitBreaker(config -> config
                        .setName("userServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback"))
                )
                .uri("lb://user-service"))
            .build();
    }
}
```

#### 2.1.4 代码方式（编程式）

```java
@Component
public class CustomGatewayFilterFactory extends 
    AbstractGatewayFilterFactory<CustomGatewayFilterFactory.Config> {
    
    public CustomGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // 前置处理
            ServerHttpRequest request = exchange.getRequest();
            log.info("Custom filter processing: {}", request.getURI());
            
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                // 后置处理
                ServerHttpResponse response = exchange.getResponse();
                log.info("Custom filter post-processing: {}", response.getStatusCode());
            }));
        };
    }
    
    public static class Config {
        private String param;
        // getter/setter
    }
}
```

---

## 三、GlobalFilter 全局过滤器

### 3.1 特点

- **全局生效**：不需要在路由配置中显式声明，对所有路由都生效
- **执行顺序**：通过 `@Order` 注解或 `Ordered` 接口控制执行顺序
- **优先级**：数字越小优先级越高（先执行）

### 3.2 内置 GlobalFilter

| 过滤器                                       | 顺序          | 说明           |
|-------------------------------------------|-------------|--------------|
| `RemoveCachedBodyFilter`                  | -2147483648 | 移除缓存的请求体     |
| `AdaptCachedBodyGlobalFilter`             | -2147482648 | 适配缓存的请求体     |
| `NettyWriteResponseFilter`                | -1          | Netty 响应写入   |
| `ForwardPathFilter`                       | 0           | 转发路径处理       |
| `GatewayMetricsFilter`                    | 0           | 网关指标收集       |
| `RouteToRequestUrlFilter`                 | 10000       | 路由URL转换      |
| `ReactiveLoadBalancerClientFilter`        | 10150       | 负载均衡客户端      |
| `LoadBalancerServiceInstanceCookieFilter` | 10151       | 负载均衡Cookie处理 |
| `WebsocketRoutingFilter`                  | 2147483646  | WebSocket 路由 |
| `NettyRoutingFilter`                      | 2147483647  | Netty 路由转发   |
| `ForwardRoutingFilter`                    | 2147483647  | 本地转发路由       |

### 3.3 自定义 GlobalFilter

#### 3.3.1 基础实现

```java
@Component
@Order(-1)  // 数字越小越先执行
public class AuthGlobalFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst("Authorization");
        
        // 前置处理：认证检查
        if (StringUtils.isEmpty(token)) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            
            String body = "{\"code\":401,\"message\":\"Unauthorized\"}";
            DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        }
        
        // 继续过滤器链
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            // 后置处理：日志记录
            log.info("Request completed: {}", request.getURI());
        }));
    }
}
```

#### 3.3.2 带顺序的多个过滤器

```java
/**
 * 日志记录过滤器（最先执行）
 */
@Component
@Order(-2)
public class LoggingGlobalFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("{} {} - {}ms", 
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI(),
                duration);
        }));
    }
}

/**
 * 认证过滤器（第二个执行）
 */
@Component
@Order(-1)
public class AuthenticationGlobalFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 认证逻辑...
        return chain.filter(exchange);
    }
}

/**
 * 鉴权过滤器（第三个执行）
 */
@Component
@Order(0)
public class AuthorizationGlobalFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 鉴权逻辑...
        return chain.filter(exchange);
    }
}
```

---

## 四、过滤器工厂详解

### 4.1 请求修改类

#### 4.1.1 AddRequestHeader

**功能**：添加请求头

```yaml
filters:
  # 快捷方式
  - AddRequestHeader=X-Request-Id, 12345
  
  # 支持URI变量
  - AddRequestHeader=X-Request-From, {segment}
```

**场景**：

- 传递追踪ID（如 SkyWalking、Zipkin）
- 标记请求来源
- 传递灰度标识

#### 4.1.2 SetRequestHeader

**功能**：设置请求头（覆盖已有值）

```yaml
filters:
  - SetRequestHeader=X-Request-Id, new-value
```

**场景**：

- 替换敏感信息（脱敏处理）
- 强制指定版本号

#### 4.1.3 RemoveRequestHeader

**功能**：移除请求头

```yaml
filters:
  - RemoveRequestHeader=X-Internal-Token
```

**场景**：

- 移除内部使用的Token，防止泄露到下游服务
- 清理无用请求头

#### 4.1.4 AddRequestParameter

**功能**：添加请求参数

```yaml
filters:
  - AddRequestParameter=version, v2
  - AddRequestParameter=source, {segment}  # 支持路径变量
```

**场景**：

- 统一添加版本参数
- 添加来源标识

#### 4.1.5 SetPath

**功能**：设置请求路径（覆盖）

```yaml
filters:
  # 将 /api/user/123 改为 /user/123
  - SetPath=/user/{segment}
```

**场景**：

- 路径标准化
- 版本控制

### 4.2 响应修改类

#### 4.2.1 AddResponseHeader

**功能**：添加响应头

```yaml
filters:
  - AddResponseHeader=X-Response-Time, {datetime}
  - AddResponseHeader=X-Processed-By, Gateway
```

**场景**：

- 添加处理时间
- 标识处理节点

#### 4.2.2 SetResponseHeader

**功能**：设置响应头（覆盖）

```yaml
filters:
  - SetResponseHeader=X-Frame-Options, DENY
  - SetResponseHeader=Content-Security-Policy, default-src 'self'
```

**场景**：

- 设置安全响应头
- 修改 Content-Type

#### 4.2.3 RemoveResponseHeader

**功能**：移除响应头

```yaml
filters:
  - RemoveResponseHeader=X-Internal-Version
```

**场景**：

- 隐藏内部版本信息
- 移除敏感头信息

#### 4.2.4 SetStatus

**功能**：设置响应状态码

```yaml
filters:
  # 快捷方式
  - SetStatus=401
  
  # 完整配置
  - name: SetStatus
    args:
      status: 401
      message: Unauthorized
```

**场景**：

- 统一错误码
- Mock 测试

### 4.3 路径操作类

#### 4.3.1 StripPrefix

**功能**：去掉路径前缀

```yaml
filters:
  # 去掉第1个路径段
  # /api/user/list -> /user/list
  - StripPrefix=1
  
  # 去掉前2个路径段
  # /api/v1/user/list -> /user/list
  - StripPrefix=2
```

**场景**：

- API 版本剥离
- 服务前缀剥离

#### 4.3.2 PrefixPath

**功能**：添加路径前缀

```yaml
filters:
  # /user/list -> /api/user/list
  - PrefixPath=/api
```

**场景**：

- 统一添加 API 前缀
- 版本控制

#### 4.3.3 RewritePath

**功能**：正则重写路径

```yaml
filters:
  # 将 /api/user/(?<segment>.*) 重写为 /user/$\{segment}
  # /api/user/list -> /user/list
  - RewritePath=/api/user/(?<segment>.*), /user/$\{segment}
  
  # 去掉所有 /api 前缀
  - RewritePath=/api/(?<path>.*), /$\{path}
```

**场景**：

- 复杂路径映射
- 路径规范化

#### 4.3.4 SetPath

**功能**：完全替换路径

```yaml
filters:
  # 将任意路径改为 /health
  - SetPath=/health
```

#### 4.3.5 RedirectTo

**功能**：重定向

```yaml
filters:
  # 302 重定向
  - RedirectTo=302, https://example.com
  
  # 301 永久重定向
  - RedirectTo=301, https://new-domain.com{uri}
```

**场景**：

- 域名迁移
- HTTP 转 HTTPS

### 4.4 限流熔断类

#### 4.4.1 RequestRateLimiter

**功能**：请求限流

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rate_limited_route
          uri: lb://user-service
          predicates:
            - Path=/api/**
          filters:
            - name: RequestRateLimiter
              args:
                # Redis RateLimiter 所需的 Bean 名称
                rate-limiter: redisRateLimiter
                # 限流 Key 解析器 Bean 名称
                key-resolver: "#{@userKeyResolver}"
                # 每秒允许处理的请求数（REPLENISH_RATE）
                redis-rate-limiter.replenishRate: 10
                # 每秒最大请求数（BURST_CAPACITY）
                redis-rate-limiter.burstCapacity: 20
                # 每个请求需要多少个令牌
                redis-rate-limiter.requestedTokens: 1
```

**Java 配置 KeyResolver：**

```java
@Configuration
public class RateLimiterConfig {
    
    /**
     * 按用户限流（根据 Authorization header）
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getHeaders().getFirst("Authorization")
        );
    }
    
    /**
     * 按 IP 限流
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
    
    /**
     * 按接口路径限流
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getPath().value()
        );
    }
}
```

**场景**：

- API 防刷
- 流量控制
- 保护下游服务

#### 4.4.2 CircuitBreaker

**功能**：熔断降级（Spring Cloud Circuit Breaker）

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: circuit_breaker_route
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            - name: CircuitBreaker
              args:
                name: userServiceCircuitBreaker
                fallbackUri: forward:/fallback
                # Resilience4j 配置
                statusCodes:
                  - 500
                  - 502
                  - 503
```

**Java DSL 配置：**

```java
@Bean
public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("circuit_breaker_route", r -> r
            .path("/api/user/**")
            .filters(f -> f
                .circuitBreaker(config -> config
                    .setName("userServiceCb")
                    .setFallbackUri("forward:/fallback")
                    .addStatusCode("500")
                    .addStatusCode("503"))
            )
            .uri("lb://user-service"))
        .build();
}
```

**Fallback 处理器：**

```java
@RestController
public class FallbackController {
    
    @RequestMapping("/fallback")
    public ResponseEntity<Map<String, Object>> fallback(ServerWebExchange exchange) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("message", "服务暂时不可用，请稍后重试");
        result.put("path", exchange.getRequest().getPath().value());
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(result);
    }
}
```

**场景**：

- 服务熔断保护
- 降级处理
- 防止雪崩效应

#### 4.4.3 Retry

**功能**：请求重试

```yaml
filters:
  - name: Retry
    args:
      # 重试次数
      retries: 3
      # 需要重试的 HTTP 状态码
      statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
      # 需要重试的 HTTP 方法
      methods: GET,POST
      # 异常类型
      exceptions: java.io.IOException,java.util.concurrent.TimeoutException
      # 退避配置
      backoff:
        firstBackoff: 50ms
        maxBackoff: 500ms
        factor: 2
        basedOnPreviousValue: false
```

**场景**：

- 网络抖动恢复
- 临时故障重试
- 幂等请求重试

### 4.5 安全类

#### 4.5.1 DedupeResponseHeader

**功能**：去重响应头

```yaml
filters:
  # 移除重复的 Access-Control-Allow-Credentials 和 Access-Control-Allow-Origin
  - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin, RETAIN_FIRST
```

**策略**：

- `RETAIN_FIRST`：保留第一个值
- `RETAIN_LAST`：保留最后一个值
- `RETAIN_UNIQUE`：保留唯一值

**场景**：

- CORS 头去重
- 防止重复响应头

#### 4.5.2 PreserveHostHeader

**功能**：保留原始 Host 头

```yaml
filters:
  - PreserveHostHeader
```

**场景**：

- 多租户场景
- Host 头敏感的应用

#### 4.5.3 SetRequestHostHeader

**功能**：设置请求 Host 头

```yaml
filters:
  - SetRequestHostHeader=api.example.com
```

**场景**：

- 虚拟主机路由
- Host 头重写

### 4.6 其他实用过滤器

#### 4.6.1 ModifyRequestBody

**功能**：修改请求体

```java
@Bean
public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("modify_request", r -> r.path("/api/**")
            .filters(f -> f.modifyRequestBody(
                String.class,           // 原始类型
                String.class,           // 目标类型
                (exchange, body) -> {   // 转换函数
                    // 修改请求体
                    String modified = body.replace("old", "new");
                    return Mono.just(modified);
                }
            ))
            .uri("lb://user-service"))
        .build();
}
```

**场景**：

- 请求体脱敏
- 数据转换
- 加密解密

#### 4.6.2 ModifyResponseBody

**功能**：修改响应体

```java
@Bean
public RouteLocator routes(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("modify_response", r -> r.path("/api/**")
            .filters(f -> f.modifyResponseBody(
                String.class,
                String.class,
                (exchange, body) -> {
                    // 包装统一响应格式
                    String wrapped = "{\"data\":" + body + "}";
                    return Mono.just(wrapped);
                }
            ))
            .uri("lb://user-service"))
        .build();
}
```

**场景**：

- 响应体包装
- 数据脱敏
- 格式转换

#### 4.6.3 TokenRelay

**功能**：传递 OAuth2 Token

```yaml
filters:
  - TokenRelay=
```

**场景**：

- OAuth2 认证场景
- Token 透传

#### 4.6.4 SaveSession

**功能**：保存 WebSession

```yaml
filters:
  - SaveSession
```

**场景**：

- WebFlux 会话管理
- Spring Session 集成

---

## 五、自定义过滤器

### 5.1 自定义 GatewayFilter Factory

#### 5.1.1 实现步骤

```java
/**
 * 自定义日志过滤器工厂
 * 命名规则：必须以 GatewayFilterFactory 结尾
 * 配置时使用 LogRequest（去掉 GatewayFilterFactory 后缀）
 */
@Component
public class LogRequestGatewayFilterFactory extends 
    AbstractGatewayFilterFactory<LogRequestGatewayFilterFactory.Config> {
    
    private static final Logger log = LoggerFactory.getLogger(LogRequestGatewayFilterFactory.class);
    
    // 定义配置参数顺序（快捷配置时使用）
    private static final List<String> SHORTCUT_FIELD_ORDER = Arrays.asList("enabled", "level");
    
    public LogRequestGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public List<String> shortcutFieldOrder() {
        return SHORTCUT_FIELD_ORDER;
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }
            
            ServerHttpRequest request = exchange.getRequest();
            String method = request.getMethodValue();
            String uri = request.getURI().toString();
            String clientIp = getClientIp(request);
            
            // 根据级别记录不同信息
            if ("BASIC".equals(config.getLevel())) {
                log.info("[Request] {} {}", method, uri);
            } else if ("DETAIL".equals(config.getLevel())) {
                log.info("[Request] {} {} from {} headers: {}", 
                    method, uri, clientIp, request.getHeaders());
            }
            
            long startTime = System.currentTimeMillis();
            
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                long duration = System.currentTimeMillis() - startTime;
                ServerHttpResponse response = exchange.getResponse();
                log.info("[Response] {} {} - {} in {}ms", 
                    method, uri, response.getStatusCode(), duration);
            }));
        };
    }
    
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.isEmpty(ip)) {
            ip = request.getRemoteAddress().getAddress().getHostAddress();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 配置类
     */
    @Data
    public static class Config {
        private boolean enabled = true;
        private String level = "BASIC";  // BASIC / DETAIL
    }
}
```

#### 5.1.2 使用方式

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            # 快捷配置
            - LogRequest=true,DETAIL
            
            # 完整配置
            - name: LogRequest
              args:
                enabled: true
                level: DETAIL
```

### 5.2 自定义 GlobalFilter

#### 5.2.1 认证过滤器

```java
@Component
@Order(-100)  // 高优先级，尽早执行
public class AuthenticationGlobalFilter implements GlobalFilter {
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private WhiteListProperties whiteList;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        
        // 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }
        
        // 获取 Token
        String token = extractToken(request);
        if (StringUtils.isEmpty(token)) {
            return unauthorized(exchange, "Missing token");
        }
        
        // 验证 Token
        try {
            Claims claims = tokenProvider.validateToken(token);
            
            // 将用户信息添加到请求头传递给下游服务
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Roles", claims.get("roles", String.class))
                .build();
            
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
            
        } catch (JwtException e) {
            return unauthorized(exchange, "Invalid token: " + e.getMessage());
        }
    }
    
    private String extractToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
    
    private boolean isWhiteListed(String path) {
        return whiteList.getPaths().stream()
            .anyMatch(pattern -> antMatch(pattern, path));
    }
}
```

#### 5.2.2 签名校验过滤器

```java
@Component
@Order(-50)
public class SignatureGlobalFilter implements GlobalFilter {
    
    @Autowired
    private SignatureProperties signatureProps;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!signatureProps.isEnabled()) {
            return chain.filter(exchange);
        }
        
        ServerHttpRequest request = exchange.getRequest();
        String timestamp = request.getHeaders().getFirst("X-Timestamp");
        String signature = request.getHeaders().getFirst("X-Signature");
        String appId = request.getHeaders().getFirst("X-App-Id");
        
        // 参数校验
        if (StringUtils.isEmpty(timestamp) || StringUtils.isEmpty(signature) 
                || StringUtils.isEmpty(appId)) {
            return badRequest(exchange, "Missing signature headers");
        }
        
        // 时间戳校验（防重放）
        long ts = Long.parseLong(timestamp);
        long now = System.currentTimeMillis();
        if (Math.abs(now - ts) > signatureProps.getMaxTimeDiff()) {
            return badRequest(exchange, "Request expired");
        }
        
        // 获取 AppSecret
        String appSecret = signatureProps.getAppSecret(appId);
        if (appSecret == null) {
            return badRequest(exchange, "Invalid appId");
        }
        
        // 获取请求体并计算签名
        return DataBufferUtils.join(request.getBody())
            .flatMap(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                
                String body = new String(bytes, StandardCharsets.UTF_8);
                String computedSign = computeSignature(appSecret, timestamp, body);
                
                if (!computedSign.equalsIgnoreCase(signature)) {
                    return badRequest(exchange, "Invalid signature");
                }
                
                // 重新包装请求体（因为已经被消费）
                DataBuffer newBuffer = exchange.getResponse().bufferFactory()
                    .wrap(bytes);
                ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(request) {
                    @Override
                    public Flux<DataBuffer> getBody() {
                        return Flux.just(newBuffer);
                    }
                };
                
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            });
    }
    
    private String computeSignature(String secret, String timestamp, String body) {
        String content = timestamp + body + secret;
        return DigestUtils.sha256Hex(content);
    }
    
    private Mono<Void> badRequest(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        
        String body = String.format("{\"code\":400,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }
}
```

---

## 六、过滤器执行顺序

### 6.1 执行链流程

```
请求进入
    ↓
[GlobalFilter] Ordered.HIGHEST_PRECEDENCE (数字最小)
    ↓
... 其他 GlobalFilter 按 order 排序执行
    ↓
[Route Predicate 匹配]
    ↓
[GatewayFilter] 路由级别过滤器（按配置顺序）
    ↓
[NettyRoutingFilter / ForwardRoutingFilter] 实际转发
    ↓
下游服务处理
    ↓
响应返回
    ↓
[GatewayFilter] 后置处理（逆序）
    ↓
[GlobalFilter] 后置处理（逆序）
    ↓
响应返回客户端
```

### 6.2 顺序控制

```java
// 方式1：@Order 注解
@Component
@Order(-100)
public class MyGlobalFilter implements GlobalFilter {
}

// 方式2：实现 Ordered 接口
@Component
public class MyGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public int getOrder() {
        return -100;
    }
}

// 方式3：GatewayFilter 的顺序
// 在 GatewayFilterFactory 中通过 OrderedGatewayFilter 包装
@Override
public GatewayFilter apply(Config config) {
    GatewayFilter filter = (exchange, chain) -> {
        // 过滤器逻辑
        return chain.filter(exchange);
    };
    // 设置顺序
    return new OrderedGatewayFilter(filter, 100);
}
```

### 6.3 内置过滤器顺序参考

| 顺序值               | 过滤器                              | 说明        |
|-------------------|----------------------------------|-----------|
| Integer.MIN_VALUE | RemoveCachedBodyFilter           | 最先执行      |
| -2147482648       | AdaptCachedBodyGlobalFilter      | 缓存适配      |
| -100              | 自定义高优先级过滤器                       | 建议认证、限流等  |
| -1                | NettyWriteResponseFilter         | 响应写入      |
| 0                 | ForwardPathFilter                | 转发路径      |
| 100               | RouteToRequestUrlFilter          | URL 转换    |
| 10150             | ReactiveLoadBalancerClientFilter | 负载均衡      |
| 2147483646        | WebsocketRoutingFilter           | WebSocket |
| 2147483647        | NettyRoutingFilter               | Netty 路由  |

---

## 七、典型应用场景

### 7.1 微服务网关完整配置示例

```yaml
spring:
  cloud:
    gateway:
      # 默认过滤器（对所有路由生效）
      default-filters:
        # 添加追踪ID
        - AddRequestHeader=X-Request-Id, ${uuid}
        # 添加服务来源标识
        - AddRequestHeader=X-Source, Gateway
        # 移除内部头
        - RemoveRequestHeader=X-Internal-Token
        # 响应头去重
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
      
      # 全局 CORS 配置
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
      
      routes:
        # 用户服务
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/user/**
          filters:
            # 路径剥离
            - StripPrefix=1
            # 限流：每秒10个请求
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
                key-resolver: "#{@ipKeyResolver}"
            # 熔断降级
            - name: CircuitBreaker
              args:
                name: userServiceCb
                fallbackUri: forward:/fallback/user
            # 重试
            - name: Retry
              args:
                retries: 3
                statuses: SERVICE_UNAVAILABLE
                methods: GET
                backoff:
                  firstBackoff: 100ms
                  maxBackoff: 500ms

        # 订单服务（需要认证）
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/order/**
          filters:
            - StripPrefix=1
            # 传递 Token
            - TokenRelay=

        # 文件服务（重写路径）
        - id: file-service
          uri: lb://file-service
          predicates:
            - Path=/api/v1/files/**
          filters:
            # 路径重写：/api/v1/files/upload -> /upload
            - RewritePath=/api/v1/files/(?<segment>.*), /$\{segment}

        # 管理后台（IP 限制）
        - id: admin-service
          uri: lb://admin-service
          predicates:
            - Path=/admin/**
            # 只允许内网IP
            - RemoteAddr=192.168.0.0/16,10.0.0.0/8
          filters:
            - StripPrefix=1
```

### 7.2 安全加固场景

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: secure-route
          uri: lb://backend-service
          predicates:
            - Path=/api/**
          filters:
            # 1. 添加安全响应头
            - AddResponseHeader=X-Content-Type-Options, nosniff
            - AddResponseHeader=X-XSS-Protection, 1; mode=block
            - AddResponseHeader=X-Frame-Options, DENY
            - AddResponseHeader=Strict-Transport-Security, max-age=31536000; includeSubDomains
            
            # 2. 移除敏感信息头
            - RemoveResponseHeader=X-Powered-By
            - RemoveResponseHeader=Server
            
            # 3. 设置内容安全策略
            - SetResponseHeader=Content-Security-Policy, default-src 'self'
```

### 7.3 灰度发布场景

```java
/**
 * 灰度发布过滤器
 */
@Component
@Order(0)
public class GrayReleaseGlobalFilter implements GlobalFilter {
    
    @Autowired
    private GrayReleaseProperties grayProps;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 根据用户ID或Cookie判断是否走灰度版本
        String userId = request.getHeaders().getFirst("X-User-Id");
        String grayVersion = determineGrayVersion(userId);
        
        if (StringUtils.hasText(grayVersion)) {
            // 添加灰度标识到请求头
            ServerHttpRequest mutatedRequest = request.mutate()
                .header("X-Gray-Version", grayVersion)
                .build();
            
            // 修改目标URI到灰度服务
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (route != null) {
                URI originalUri = route.getUri();
                URI grayUri = URI.create(originalUri.toString().replace(
                    "-service", "-service-" + grayVersion));
                
                Route grayRoute = Route.async()
                    .asyncPredicate(route.getPredicate())
                    .filters(route.getFilters())
                    .id(route.getId())
                    .uri(grayUri)
                    .order(route.getOrder())
                    .build();
                
                exchange.getAttributes().put(
                    ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, grayRoute);
            }
            
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }
        
        return chain.filter(exchange);
    }
    
    private String determineGrayVersion(String userId) {
        if (StringUtils.isEmpty(userId)) {
            return null;
        }
        // 根据用户ID哈希或白名单判断
        if (grayProps.getWhiteList().contains(userId)) {
            return "v2";
        }
        // 按百分比灰度
        int hash = Math.abs(userId.hashCode()) % 100;
        if (hash < grayProps.getPercentage()) {
            return "v2";
        }
        return null;
    }
}
```

### 7.4 日志追踪场景

```java
@Component
@Order(-200)
public class TracingGlobalFilter implements GlobalFilter {
    
    private static final Logger log = LoggerFactory.getLogger(TracingGlobalFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 生成或获取追踪ID
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (StringUtils.isEmpty(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        
        String spanId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        
        // 存储到 exchange 属性
        exchange.getAttributes().put("traceId", traceId);
        exchange.getAttributes().put("spanId", spanId);
        exchange.getAttributes().put("startTime", startTime);
        
        // 添加追踪头到请求
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
            .header("X-Trace-Id", traceId)
            .header("X-Span-Id", spanId)
            .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate()
            .request(mutatedRequest)
            .build();
        
        // 记录请求日志
        log.info("[{}|{}] {} {} from {}", 
            traceId, spanId,
            exchange.getRequest().getMethod(),
            exchange.getRequest().getURI(),
            getClientIp(exchange.getRequest()));
        
        return chain.filter(mutatedExchange).doFinally(signalType -> {
            // 记录响应日志
            long duration = System.currentTimeMillis() - startTime;
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
            log.info("[{}|{}] Completed {} in {}ms status {}",
                traceId, spanId,
                exchange.getRequest().getURI(),
                duration,
                statusCode);
        });
    }
    
    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.isEmpty(ip)) {
            ip = request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
```

---

## 八、性能优化建议

### 8.1 过滤器优化

1. **避免阻塞操作**：使用 Reactive 编程，避免在过滤器中执行阻塞IO
2. **合理使用缓存**：对重复计算的结果进行缓存
3. **减少过滤器数量**：合并功能相似的过滤器
4. **异步处理**：日志记录等可以使用异步方式

### 8.2 配置优化

```yaml
spring:
  cloud:
    gateway:
      # 启用响应缓存
      httpclient:
        pool:
          type: elastic
          max-connections: 1000
          max-life-time: 900s
      # 启用 HTTPS
      httpserver:
        wiretap: true  # 开发调试时使用
```

---

## 附录：常用过滤器速查表

| 过滤器                  | 类型            | 用途              |
|----------------------|---------------|-----------------|
| AddRequestHeader     | GatewayFilter | 添加请求头           |
| SetRequestHeader     | GatewayFilter | 设置请求头           |
| RemoveRequestHeader  | GatewayFilter | 移除请求头           |
| AddResponseHeader    | GatewayFilter | 添加响应头           |
| SetResponseHeader    | GatewayFilter | 设置响应头           |
| RemoveResponseHeader | GatewayFilter | 移除响应头           |
| StripPrefix          | GatewayFilter | 剥离路径前缀          |
| PrefixPath           | GatewayFilter | 添加路径前缀          |
| RewritePath          | GatewayFilter | 正则重写路径          |
| SetPath              | GatewayFilter | 设置完整路径          |
| RedirectTo           | GatewayFilter | 重定向             |
| SetStatus            | GatewayFilter | 设置响应状态          |
| Retry                | GatewayFilter | 请求重试            |
| RequestRateLimiter   | GatewayFilter | 限流              |
| CircuitBreaker       | GatewayFilter | 熔断降级            |
| Hystrix              | GatewayFilter | Hystrix 熔断（已废弃） |
| ModifyRequestBody    | GatewayFilter | 修改请求体           |
| ModifyResponseBody   | GatewayFilter | 修改响应体           |
| PreserveHostHeader   | GatewayFilter | 保留 Host 头       |
| SetRequestHostHeader | GatewayFilter | 设置请求 Host       |
| DedupeResponseHeader | GatewayFilter | 响应头去重           |
| TokenRelay           | GatewayFilter | OAuth2 Token 传递 |
| SaveSession          | GatewayFilter | 保存 Session      |

---

**参考文档：**

- [Spring Cloud Gateway 官方文档](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Spring Cloud Circuit Breaker](https://docs.spring.io/spring-cloud-circuitbreaker/docs/current/reference/html/)
