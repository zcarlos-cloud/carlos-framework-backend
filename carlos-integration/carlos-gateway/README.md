# Carlos Gateway 网关服务

基于 **Spring Cloud Gateway 4.x** 的微服务统一网关，提供路由转发、OAuth2 鉴权、限流熔断、灰度发布、安全防护等企业级网关能力。

## 核心特性

- **智能路由** - 支持路径重写、前缀剥离、负载均衡
- **OAuth2 鉴权** - 集成 carlos-auth 的统一认证授权
- **限流熔断** - 基于 Redis 的分布式限流 + Resilience4j 熔断降级
- **灰度发布** - 支持按用户、按比例的灰度路由策略
- **安全防护** - WAF 防火墙、防重放攻击、统一白名单管理
- **可观测性** - 链路追踪、指标收集、访问日志

## 模块结构

```
carlos-gateway/
├── src/main/java/com/carlos/gateway/
│   ├── config/          # 网关配置（含 GlobalFilterOrder、全局异常处理器）
│   ├── filter/          # 全局过滤器与 GatewayFilterFactory
│   ├── oauth2/          # OAuth2 认证鉴权
│   ├── security/        # 安全防护（WAF、防重放、白名单属性）
│   ├── ratelimit/       # 限流
│   ├── circuitbreaker/  # 熔断降级
│   ├── gray/            # 灰度发布
│   ├── transform/       # 请求转换
│   ├── cache/           # 响应缓存
│   ├── observability/   # 链路追踪与指标
│   └── exception/       # 网关异常定义
└── docs/                # 文档中心
```

## 过滤器执行顺序

所有自定义 `GlobalFilter` 的顺序统一在 `GlobalFilterOrder` 中定义，严禁在过滤器类中硬编码数字。过滤器按功能分为 9 个阶段：

### 9 阶段 GlobalFilter 一览

| 阶段     | Order     | 过滤器                          | 核心作用                                                      | 关键技术                                        |
|--------|-----------|------------------------------|-----------------------------------------------------------|---------------------------------------------|
| 可观测性基础 | **-9000** | `RequestTracingFilter`       | 生成/透传 RequestId、TraceId；注入 B3/SkyWalking 头                | Brave Tracer + B3 传播                        |
| 可观测性基础 | **-8500** | `MetricsFilter`              | 记录 Counter/Timer/Gauge；按状态码分类；降低标签基数                      | Caffeine 缓存指标实例 + 路径模式提取                    |
| 准入控制   | **-8000** | `UnifiedWhitelistFilter`     | 一次 Ant 路径匹配，产出 6 类白名单结果供后续共享                              | Caffeine 缓存匹配结果                             |
| 准入控制   | **-7900** | `IdentityHeaderSanitizeFilter` | 无条件剔除客户端自带的身份头，保证下游只见到网关写入的身份                   | 单次 mutate + 头名白名单                          |
| 安全防护   | **-7000** | `WafFilter`                  | SQL 注入、XSS、路径遍历、敏感文件、命令注入、CSRF 检测                         | BitSet 预检 + 预编译 Pattern                     |
| 安全防护   | **-6500** | `ReplayProtectionFilter`     | 时间戳容差、Nonce 去重、HMAC-SHA256 签名验证                           | Redis + HexFormat                           |
| 认证授权   | **-5000** | `OAuth2AuthenticationFilter` | JWT/Opaque Token 认证；构建 UserContext                        | TokenValidator 抽象                           |
| 认证授权   | **-4500** | `OAuth2AuthorizationFilter`  | RBAC/ABAC 权限校验                                            | Caffeine 双级缓存权限                             |
| 路由准备   | **-3000** | `GrayReleaseFilter`          | 按用户、IP、权重、请求头灰度；设置灰度标记头                                   | MurmurHash3 + ThreadLocalRandom             |
| 路由准备   | **-1000** | `RequestTransformFilter`     | API 版本控制、路径重写、Header 转换                                   | 预编译 Pattern + 单次 mutate                     |
| 上下文传递  | **-450**  | `IdentityAssertionSignFilter` | 用 HMAC-SHA256 把身份签成 `X-User-Context` 断言，供下游验签                    | 共享密钥 + 紧凑 JSON 载荷（Nacos 共读）                   |
| 响应优化   | **100**   | `ResponseCacheFilter`        | Caffeine L1 + Redis L2 缓存 GET/HEAD 响应                     | `ServerHttpResponseDecorator` + MurmurHash3 |
| 访问日志   | **1000**  | `AccessLogFilter`            | 记录 method/path/status/duration/requestId/traceId/clientIp | `doFinally` + 独立 Logger                     |

> **说明**：`AccessLogFilter` 虽然 order=1000，但使用 `doFinally` 模式，实际日志输出发生在整条过滤链（含后端调用）完成后，因此能记录最终状态码与完整耗时。

### 与 Spring Cloud Gateway 内置过滤器的相对位置

```
自定义过滤器范围: -10000 ~ 10000

NettyWriteResponseFilter:         -1
ResponseCacheFilter:              100
AccessLogFilter:                  1000
RouteToRequestUrlFilter:          10000
ReactiveLoadBalancerClientFilter: 10150
NettyRoutingFilter:               Integer.MAX_VALUE
ForwardRoutingFilter:             Integer.MAX_VALUE
```

所有自定义全局过滤器均安排在 `-10000 ~ 10000` 之间，确保在 SCG 内置路由过滤器之前完成安全、认证、路由准备等操作，同时处于日志记录范围内。

### 顺序设计原则

1. **分层原则**：可观测性 → 准入控制 → 安全防护 → 流量控制 → 认证授权 → 路由准备 → 上下文传递 → 响应优化 → 访问日志
2. **依赖优先原则**：被依赖的过滤器必须先执行
    - `UnifiedWhitelistFilter` → `WafFilter` / `ReplayProtectionFilter` / `OAuth2AuthenticationFilter`
    - `OAuth2AuthenticationFilter` → `OAuth2AuthorizationFilter` / `IdentityAssertionSignFilter`
3. **开销递减原则**：越先执行的过滤器开销越小（或拦截价值越高）
4. **拦截前置原则**：拒绝类过滤器（WAF、防重放、限流、认证、鉴权）尽量前置，越早拒绝越省资源

## WebFilter（路由前统一处理）

### PrefixWebFilter（统一前缀剥离 WebFilter）

- **源码位置**：`com.carlos.gateway.filter.PrefixWebFilter`
- **类型**：`WebFilter`
- **顺序**：`Ordered.HIGHEST_PRECEDENCE`
- **自动配置**：`FilterAutoConfiguration`
- **启用条件**：`carlos.gateway.prefix-enabled=true`（默认启用）

**作用**：

1. 在 Spring Cloud Gateway 路由匹配之前执行，提前剥离统一前缀（如 `/manage-api`）
2. 使路由 predicate 可以基于剥离后的路径编写，无需在每条路由中重复前缀
3. 白名单路径（如 actuator、swagger）跳过剥离，保持原有访问方式

**为什么用 WebFilter 而不是 GlobalFilter**：

Spring Cloud Gateway 的处理顺序是：

```
HTTP 请求
  ↓ WebFilter 链（PrefixWebFilter 在这里执行）
  ↓ DispatcherHandler
  ↓ RoutePredicateHandlerMapping（路由匹配）
  ↓ FilteringWebHandler
  ↓ GlobalFilter 链（UnifiedWhitelistFilter / WAF / 认证 / ...）
```

`GlobalFilter` 在路由匹配之后执行，因此无法在路由匹配前改变路径。`WebFilter` 在 `DispatcherHandler` 之前执行，可以修改
`ServerHttpRequest` 的路径，从而让后续路由匹配看到剥离后的路径。

**转换示例**：

```
外部请求：/manage-api/auth/v3/api-docs
  ↓ PrefixWebFilter 剥离 /manage-api
路由匹配路径：/auth/v3/api-docs
  ↓ 命中 Path=/auth/**
  ↓ SelectStripPrefix=1 剥离 /auth
转发到后端：/v3/api-docs
```

**可配置项**（`GatewayProperties` / `UnifiedWhitelistProperties`）：

| 属性                                            | 类型          | 默认值                  | 说明       |
|-----------------------------------------------|-------------|----------------------|----------|
| `carlos.gateway.prefix`                       | String      | `/manage-api`        | API 统一前缀 |
| `carlos.gateway.prefix-enabled`               | boolean     | `true`               | 是否启用前缀剥离 |
| `carlos.gateway.whitelist`                    | Set<String> | 含 swagger、actuator 等 | 全局白名单    |
| `carlos.gateway.whitelist.prefix-strip-paths` | Set<String> | 继承 global            | 前缀剥离白名单  |

**配置示例**：

```yaml
carlos:
  gateway:
    prefix: /manage-api
    prefix-enabled: true
    whitelist:
      - /custom/public/**
```

**注意事项**：

- 前缀只在一处定义，路由配置无需重复写前缀
- 白名单路径（如 `/actuator/**`、`/swagger-ui/**`）跳过剥离
- 路径不以前缀开头时记录日志但放行，可改为返回错误
- 因为执行顺序最早，日志/追踪中记录的路径可能是剥离后的路径

---

## 各过滤器详细说明

### 阶段一：可观测性基础

#### 1. RequestTracingFilter（链路追踪过滤器）

- **源码位置**：`com.carlos.gateway.observability.RequestTracingFilter`
- **Order**：`-9000`
- **自动配置**：`ObservabilityAutoConfiguration`
- **启用条件**：`carlos.gateway.tracing.enabled=true`（默认启用）且容器中存在 `Tracer`、`Propagator` Bean。这两个 Bean 由
  `spring-boot-micrometer-tracing-brave` 自动装配

**作用**：

1. 从请求头提取或生成 Request ID（业务追踪）
2. 从 B3 头提取或新建 Trace ID（分布式链路追踪）
3. 将 Request ID、Trace ID、Span ID 传递到下游请求头
4. 在响应头中返回这些 ID
5. 将追踪信息写入 Reactor Context，供后续过滤器使用

**注入的请求头**：

| Header         | 说明                             |
|----------------|--------------------------------|
| `X-Request-Id` | 业务请求 ID                        |
| `X-Trace-Id`   | 分布式链路 Trace ID                 |
| `X-Span-Id`    | 当前 Span ID                     |
| `b3`           | B3 单头格式：`{traceId}-{spanId}-1` |
| `X-B3-TraceId` | B3 多头部格式 Trace ID              |
| `X-B3-SpanId`  | B3 多头部格式 Span ID               |
| `X-B3-Sampled` | 是否采样                           |

**B3 头处理逻辑**：

`RequestTracingFilter` 作为网关入口的第一个过滤器，负责确保整条链路都有可用的追踪上下文。B3 头的处理分两种情况：

1. **上游已传入 B3 头**（网关不是请求开端，例如上游是 Nginx、Ingress、Sidecar 或其他微服务）
    - 优先读取上游传入的 `b3` 单头或 `X-B3-*` 多头部
    - 解析出 traceId、spanId，调用 `tracer.nextSpan(extracted)` 继续已有链路
    - 往下游转发时，**traceId 保持不变，spanId 更新为当前 Span 的 ID**

2. **上游未传入 B3 头**（网关是请求开端，例如浏览器、App、第三方客户端直接请求网关）
    - `extractB3Context()` 返回 null
    - 调用 `tracer.nextSpan()` 创建全新的 Span，生成新的 traceId 和 spanId
    - 在转发给下游之前，主动注入 B3 头，使网关成为整条链路的追踪起点

因此，**无论上游是否传入 B3 头，网关都会保证往下游注入 B3 头**。当前实现中 `X-B3-Sampled` 固定为 `1`，表示默认全采样。

**可配置项**（`TracingProperties`）：

| 属性                                                  | 类型       | 默认值                                    | 说明             |
|-----------------------------------------------------|----------|----------------------------------------|----------------|
| `carlos.gateway.tracing.enabled`                    | boolean  | `true`                                 | 是否启用链路追踪       |
| `carlos.gateway.tracing.sampling-rate`              | double   | `1.0`                                  | 采样率（0.0 - 1.0） |
| `carlos.gateway.tracing.record-request-body`        | boolean  | `false`                                | 是否记录请求体        |
| `carlos.gateway.tracing.record-response-body`       | boolean  | `false`                                | 是否记录响应体        |
| `carlos.gateway.tracing.ignore-patterns`            | String[] | `/actuator/**`、`/health`、`/prometheus` | 忽略追踪的 URL 前缀   |
| `carlos.gateway.tracing.request-id.header-name`     | String   | `X-Request-Id`                         | Request ID 头名称 |
| `carlos.gateway.tracing.request-id.auto-generate`   | boolean  | `true`                                 | 请求未提供时是否自动生成   |
| `carlos.gateway.tracing.request-id.add-to-response` | boolean  | `true`                                 | 是否添加到响应头       |

**配置示例**：

```yaml
carlos:
  gateway:
    tracing:
      enabled: true
      sampling-rate: 1.0
      ignore-patterns:
        - /actuator/**
        - /health
      request-id:
        header-name: X-Request-Id
        auto-generate: true
        add-to-response: true
```

**注意事项**：

- 必须最先执行，否则被拒绝的请求无法追踪
- 基于 Brave Tracer，支持 B3 传播协议
- 如果 SkyWalking 头 `sw8` 存在，会原样透传
- 当前 B3 实现默认全采样（`X-B3-Sampled=1`）。若需接入外部采样策略，建议通过 Brave 的 `Sampler` Bean 统一控制，而不是在网关中硬编码
- 必须引入 `spring-boot-micrometer-tracing-brave` 依赖，`Tracer` / `Propagator` Bean 才会自动装配，`RequestTracingFilter`
  才能注册
- `spring-boot-starter-zipkin` 用于 Zipkin 上报，配合 `management.zipkin.tracing.endpoint` 使用
- `spring-boot-starter-actuator` 仅负责暴露 `/actuator/prometheus` 等端点，**不会**自动创建 `Tracer` / `Propagator`

---

#### 2. MetricsFilter（指标收集过滤器）

- **源码位置**：`com.carlos.gateway.observability.MetricsFilter`
- **Order**：`-8500`
- **自动配置**：`ObservabilityAutoConfiguration`
- **启用条件**：`carlos.gateway.metrics.enabled=true`（默认启用）且容器中存在 `MeterRegistry`

**作用**：

1. 基于 Micrometer 记录请求 Counter、Latency Timer、Active Requests Gauge
2. 按状态码分类统计（2xx/4xx/5xx/限流/拦截）
3. 支持路径模式提取，降低 Prometheus 标签基数

**核心指标**：

| 指标名                             | 类型      | 说明                               |
|---------------------------------|---------|----------------------------------|
| `gateway.requests.total`        | Counter | 请求总数（按 route/path/method/status） |
| `gateway.requests.latency`      | Timer   | 请求延迟（按 route/method）             |
| `gateway.requests.active`       | Gauge   | 当前活跃请求数                          |
| `gateway.requests.success`      | Counter | 2xx 成功请求数                        |
| `gateway.requests.client_error` | Counter | 4xx 错误请求数                        |
| `gateway.requests.server_error` | Counter | 5xx 错误请求数                        |
| `gateway.requests.rate_limited` | Counter | 被限流请求数                           |
| `gateway.requests.blocked`      | Counter | 被拦截请求数                           |

**路径模式提取示例**：

- `/api/users/12345` → `/api/users/{id}`
- `/api/orders/550e8400-e29b-41d4-a716-446655440000` → `/api/orders/{id}`

**可配置项**：

| 属性                               | 类型      | 默认值    | 说明       |
|----------------------------------|---------|--------|----------|
| `carlos.gateway.metrics.enabled` | boolean | `true` | 是否启用指标收集 |

**配置示例**：

```yaml
carlos:
  gateway:
    metrics:
      enabled: true
```

**注意事项**：

- 使用 `doFinally` 模式，无论请求被哪个后续过滤器拒绝，指标都会被完整记录
- Counter/Timer 实例使用 Caffeine 缓存，防止内存泄漏
- 路径模式提取避免 UUID 数字 ID 导致标签基数爆炸

**指标使用方法**：

`MetricsFilter` 基于 Micrometer 注册指标，Spring Boot 会自动通过 `/actuator/prometheus` 端点暴露。使用流程为：

1. **确认指标暴露端点已开启**

   默认 `application.yml` 中已配置：

   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: health,info,metrics,prometheus,gateway
   ```

2. **Prometheus 抓取配置**

   在 `prometheus.yml` 中添加网关 job：

   ```yaml
   scrape_configs:
     - job_name: 'carlos-gateway'
       metrics_path: '/actuator/prometheus'
       static_configs:
         - targets: ['localhost:9510']
   ```

3. **常用 PromQL 查询示例**

   **QPS（按状态码）**：
   ```promql
   sum(rate(gateway_requests_total[1m])) by (status)
   ```

   **P99 延迟**：
   ```promql
   histogram_quantile(0.99, sum(rate(gateway_requests_duration_bucket[1m])) by (le, route))
   ```

   **按路由统计错误率**：
   ```promql
   sum(rate(gateway_requests_client_error_total[1m])) by (route) / sum(rate(gateway_requests_total[1m])) by (route)
   ```

   **当前活跃请求数**：
   ```promql
   gateway_requests_active
   ```

   **限流触发次数**：
   ```promql
   sum(rate(gateway_requests_rate_limited_total[1m])) by (route)
   ```

   **被拦截请求数（WAF/防重放等）**：
   ```promql
   sum(rate(gateway_requests_blocked_total[1m])) by (route)
   ```

4. **Grafana 大盘建议面板**

   | 面板 | PromQL |
         |---|---|
   | 总 QPS | `sum(rate(gateway_requests_total[1m]))` |
   | 状态码分布 | `sum(rate(gateway_requests_total[1m])) by (status)` |
   | P50/P95/P99 延迟 | `histogram_quantile(0.5/0.95/0.99, sum(rate(gateway_requests_duration_bucket[1m])) by (le, route))` |
   | 路由 Top 错误 | `topk(10, sum(rate(gateway_requests_client_error_total[1m])) by (route))` |
   | 活跃连接数 | `gateway_requests_active` |
   | 限流/拦截趋势 | `sum(rate(gateway_requests_rate_limited_total[1m]))` + `sum(rate(gateway_requests_blocked_total[1m]))` |

5. **告警规则示例**

   ```yaml
   groups:
     - name: gateway-alerts
       rules:
         - alert: GatewayHighErrorRate
           expr: sum(rate(gateway_requests_server_error_total[5m])) / sum(rate(gateway_requests_total[5m])) > 0.05
           for: 2m
           labels:
             severity: warning
           annotations:
             summary: "Gateway 5xx 错误率过高"

         - alert: GatewayHighLatency
           expr: histogram_quantile(0.99, sum(rate(gateway_requests_duration_bucket[5m])) by (le)) > 2
           for: 2m
           labels:
             severity: warning
           annotations:
             summary: "Gateway P99 延迟超过 2 秒"

         - alert: GatewayRateLimitSpike
           expr: sum(rate(gateway_requests_rate_limited_total[5m])) > 100
           for: 1m
           labels:
             severity: info
           annotations:
             summary: "Gateway 限流触发量突增"
   ```

6. **本地调试查看指标**

   启动网关后直接访问：

   ```bash
   curl http://localhost:9510/actuator/prometheus | grep gateway_requests
   ```

   或在 `/actuator/metrics` 中查看指标列表：

   ```bash
   curl http://localhost:9510/actuator/metrics/gateway.requests.total
   ```

---

### 阶段二：准入控制

#### 3. UnifiedWhitelistFilter（统一白名单过滤器）

- **源码位置**：`com.carlos.gateway.whitelist.UnifiedWhitelistFilter`
- **Order**：`-8000`
- **自动配置**：`FilterAutoConfiguration`
- **启用条件**：`carlos.gateway.whitelist.enabled=true`（默认启用）

**作用**：

1. 统一检查全局、认证、WAF、防重放、限流、路径前缀剥离等各类白名单
2. 将匹配结果存入 `Exchange Attributes`，供后续过滤器直接读取
3. 使用 Caffeine 缓存匹配结果，避免重复的 Ant 路径匹配

**白名单类型**：

| 类型                 | 用途                  | 默认继承                          |
|--------------------|---------------------|-------------------------------|
| `globalPaths`      | 全局白名单（适用于所有过滤器）     | 默认含 actuator、swagger、health 等 |
| `authPaths`        | 认证/鉴权白名单（OAuth2 跳过） | 默认含 OAuth2 端点、登录接口、public     |
| `wafPaths`         | WAF 白名单（安全防护跳过）     | 继承 globalPaths                |
| `replayPaths`      | 防重放白名单              | 继承 globalPaths                |
| `rateLimitPaths`   | 限流白名单               | 继承 globalPaths                |
| `prefixStripPaths` | 路径前缀剥离白名单           | 继承 globalPaths                |

**可配置项**（`UnifiedWhitelistProperties`）：

| 属性                                            | 类型          | 默认值                      | 说明        |
|-----------------------------------------------|-------------|--------------------------|-----------|
| `carlos.gateway.whitelist.enabled`            | boolean     | `true`                   | 是否启用统一白名单 |
| `carlos.gateway.whitelist.global-paths`       | Set<String> | 见源码 DEFAULT_GLOBAL_PATHS | 全局白名单     |
| `carlos.gateway.whitelist.auth-paths`         | Set<String> | 见源码 DEFAULT_AUTH_PATHS   | 认证白名单     |
| `carlos.gateway.whitelist.waf-paths`          | Set<String> | 空                        | WAF 白名单   |
| `carlos.gateway.whitelist.replay-paths`       | Set<String> | 继承 global                | 防重放白名单    |
| `carlos.gateway.whitelist.rate-limit-paths`   | Set<String> | 继承 global                | 限流白名单     |
| `carlos.gateway.whitelist.prefix-strip-paths` | Set<String> | 继承 global                | 前缀剥离白名单   |

**配置示例**：

```yaml
carlos:
  gateway:
    whitelist:
      enabled: true
      global-paths:
        - /actuator/**
        - /health
      auth-paths:
        - /oauth2/**
        - /public/**
        - /api/public/**
```

**注意事项**：

- 使用 `PathMatchUtil.antMatchAny` 进行 Ant 风格匹配
- 匹配结果缓存 10 分钟，最多 10000 条
- 后续过滤器通过 `WhitelistContext` 读取结果，避免重复匹配

---

### 阶段三：安全防护

#### 4. WafFilter（Web 应用防火墙过滤器）

- **源码位置**：`com.carlos.gateway.security.WafFilter`
- **Order**：`-7000`
- **自动配置**：`SecurityGatewayAutoConfiguration`
- **启用条件**：`carlos.gateway.waf.enabled=true`（默认启用）

**作用**：

1. SQL 注入检测（预编译 Pattern + BitSet 快速字符检查）
2. XSS 攻击检测
3. 路径遍历检测
4. 敏感文件访问检测
5. 命令注入检测
6. CSRF Token 验证

**防护类型**：

| 类型     | 规则说明                                            |
|--------|-------------------------------------------------|
| SQL 注入 | `'"%-#*/;` 快速字符检查 + 预编译正则                       |
| XSS    | `<script`、`javascript:`、事件处理器等                  |
| 路径遍历   | `../`、编码形式的路径遍历                                 |
| 敏感文件   | `.git`、`.env`、`.htaccess`、actuator、swagger-ui 等 |
| 命令注入   | `;`、`                                           |`、`&&`、反引号及常见命令关键字 |
| CSRF   | 基于 Origin/Referer 精确域名匹配                        |

**可配置项**（`WafProperties`）：

| 属性                                                | 类型      | 默认值                | 说明              |
|---------------------------------------------------|---------|--------------------|-----------------|
| `carlos.gateway.waf.enabled`                      | boolean | `true`             | 是否启用 WAF        |
| `carlos.gateway.waf.sql-injection-protection`     | boolean | `true`             | SQL 注入防护        |
| `carlos.gateway.waf.xss-protection`               | boolean | `true`             | XSS 防护          |
| `carlos.gateway.waf.path-traversal-protection`    | boolean | `true`             | 路径遍历防护          |
| `carlos.gateway.waf.sensitive-file-protection`    | boolean | `true`             | 敏感文件访问防护        |
| `carlos.gateway.waf.command-injection-protection` | boolean | `true`             | 命令注入防护          |
| `carlos.gateway.waf.check-body`                   | boolean | `true`             | 是否检查请求体         |
| `carlos.gateway.waf.csrf-protection`              | boolean | `true`             | CSRF 防护         |
| `carlos.gateway.waf.allowed-origin`               | String  | `http://localhost` | CSRF 允许的 Origin |
| `carlos.gateway.waf.max-body-size`                | int     | `1048576`（1MB）     | 最大请求体检查大小       |

**配置示例**：

```yaml
carlos:
  gateway:
    waf:
      enabled: true
      sql-injection-protection: true
      xss-protection: true
      path-traversal-protection: true
      sensitive-file-protection: true
      command-injection-protection: true
      check-body: true
      csrf-protection: true
      allowed-origin: "http://localhost:3000"
      max-body-size: 1048576
```

**注意事项**：

- WAF 优先使用统一白名单结果，白名单路径跳过检查
- 使用 BitSet 进行快速字符预检，减少正则调用
- 请求体检查会读取并重新包装 DataBuffer，注意大文件上传场景
- CSRF 使用精确 host + port 匹配，避免 `startsWith` 绕过

---

#### 5. ReplayProtectionFilter（防重放攻击过滤器）

- **源码位置**：`com.carlos.gateway.security.ReplayProtectionFilter`
- **Order**：`-6500`
- **自动配置**：`SecurityGatewayAutoConfiguration`
- **启用条件**：`carlos.gateway.replay.enabled=true`（默认启用）

**作用**：

1. 验证请求时间戳（默认 60 秒容差）
2. 检查 Nonce 唯一性（Redis 存储）
3. 验证 HMAC-SHA256 请求签名

**签名构建字符串**：

```
{METHOD}|{PATH}|{QUERY}|{TIMESTAMP}|{NONCE}
```

**所需请求头**：

| Header        | 说明                     |
|---------------|------------------------|
| `X-Timestamp` | 请求时间戳（秒级或毫秒级 Unix 时间戳） |
| `X-Nonce`     | 唯一请求标识                 |
| `X-Signature` | HMAC-SHA256 签名（Hex 格式） |

**可配置项**（`ReplayProtectionProperties`）：

| 属性                                  | 类型          | 默认值     | 说明                |
|-------------------------------------|-------------|---------|-------------------|
| `carlos.gateway.replay.enabled`     | boolean     | `true`  | 是否启用防重放           |
| `carlos.gateway.replay.strict-mode` | boolean     | `false` | 严格模式（GET 也强制验证签名） |
| `carlos.gateway.replay.secret-key`  | String      | 无       | 签名密钥（必须配置）        |
| `carlos.gateway.replay.whitelist`   | Set<String> | 无       | 白名单路径             |

**配置示例**：

```yaml
carlos:
  gateway:
    replay:
      enabled: true
      strict-mode: false
      secret-key: ${REPLAY_SECRET_KEY}
      whitelist:
        - /public/**
```

**注意事项**：

- 时间戳容差为 60 秒，Nonce 在 Redis 中保存 2 倍容差时间
- 非严格模式下，GET 请求只需验证时间戳
- 必须使用统一白名单结果，避免重复匹配
- 每次创建新的 Mac 实例，避免 ThreadLocal 在 EventLoop 线程复用下的状态污染

---

### 阶段四：流量控制

#### 6. CarlosRateLimiterGatewayFilterFactory（限流过滤器工厂）

- **源码位置**：`com.carlos.gateway.ratelimit.CarlosRateLimiterGatewayFilterFactory`
- **类型**：`GatewayFilterFactory`（路由级，非 GlobalFilter）
- **自动配置**：`CarlosRateLimiterAutoConfiguration`
- **启用条件**：`carlos.gateway.rate-limiter.enabled=true`（默认启用）且存在 `ReactiveStringRedisTemplate`

**作用**：

1. 基于 Redis 令牌桶算法实现分布式限流
2. 支持 IP/USER/API/METHOD_API/IP_API/IP_USER/SERVICE 多种 KeyResolver 策略
3. 支持黑名单直接拒绝、白名单直接放行
4. 提供统一 ErrorResponse 格式的限流响应

**KeyResolver 策略**（`CarlosKeyResolver`）：

| 策略           | 说明                            |
|--------------|-------------------------------|
| `IP`         | 基于客户端 IP 限流                   |
| `USER`       | 基于用户 ID 限流（从 `X-User-Id` 头获取） |
| `API`        | 基于请求路径限流                      |
| `METHOD_API` | 基于请求方法 + 路径限流                 |
| `IP_API`     | 基于 IP + 路径限流                  |
| `IP_USER`    | 基于 IP + 用户限流                  |
| `SERVICE`    | 基于服务 ID 限流                    |

**可配置项**（`CarlosRateLimiterProperties`）：

| 属性                                                            | 类型                | 默认值     | 说明            |
|---------------------------------------------------------------|-------------------|---------|---------------|
| `carlos.gateway.rate-limiter.enabled`                         | boolean           | `true`  | 是否启用限流        |
| `carlos.gateway.rate-limiter.fail-open`                       | boolean           | `false` | Redis 故障时是否放行 |
| `carlos.gateway.rate-limiter.default-config.replenish-rate`   | int               | `10`    | 每秒补充令牌数       |
| `carlos.gateway.rate-limiter.default-config.burst-capacity`   | int               | `20`    | 令牌桶容量         |
| `carlos.gateway.rate-limiter.default-config.requested-tokens` | int               | `1`     | 每次请求消耗令牌数     |
| `carlos.gateway.rate-limiter.default-config.key-resolver`     | CarlosKeyResolver | `IP`    | 默认 Key 解析策略   |
| `carlos.gateway.rate-limiter.default-config.status-code`      | int               | `429`   | 限流响应状态码       |
| `carlos.gateway.rate-limiter.routes`                          | List<RouteConfig> | 空       | 路由特定配置        |
| `carlos.gateway.rate-limiter.whitelist`                       | Set<String>       | 空       | 白名单 IP        |
| `carlos.gateway.rate-limiter.blacklist`                       | Set<String>       | 空       | 黑名单 IP        |
| `carlos.gateway.rate-limiter.response.include-headers`        | boolean           | `true`  | 包含限流响应头       |
| `carlos.gateway.rate-limiter.response.uniform-error-response` | boolean           | `true`  | 统一错误响应格式      |
| `carlos.gateway.rate-limiter.response.retry-after`            | int               | `60`    | 建议重试时间        |
| `carlos.gateway.rate-limiter.metrics.enabled`                 | boolean           | `true`  | 是否启用限流指标      |
| `carlos.gateway.rate-limiter.metrics.publish-events`          | boolean           | `true`  | 是否发布限流事件      |
| `carlos.gateway.rate-limiter.metrics.sample-rate`             | double            | `1.0`   | 事件采样率         |

**配置示例**：

```yaml
carlos:
  gateway:
    rate-limiter:
      enabled: true
      fail-open: false
      default-config:
        replenish-rate: 100
        burst-capacity: 200
        requested-tokens: 1
        key-resolver: IP
      routes:
        - route-id: carlos-org
          replenish-rate: 100
          burst-capacity: 200
          key-resolver: IP
          inherit-default: false
        - route-id: order-service
          replenish-rate: 50
          burst-capacity: 100
          key-resolver: USER
      whitelist:
        - "127.0.0.1"
      blacklist:
        - "192.168.1.100"
      response:
        include-headers: true
        uniform-error-response: true
        retry-after: 60
      metrics:
        enabled: true
        publish-events: true
        sample-rate: 1.0
```

**路由使用方式**：

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
            - CarlosRateLimiter=100,200,IP,1
```

**注意事项**：

- 基于 Redis Lua 脚本实现原子化令牌桶
- 默认 fail-closed，Redis 故障时拒绝请求更安全
- 限流放在认证之前，可保护认证接口免受暴力破解

---

### 阶段五：认证授权

#### 7. OAuth2AuthenticationFilter（OAuth2 认证过滤器）

- **源码位置**：`com.carlos.gateway.oauth2.OAuth2AuthenticationFilter`
- **Order**：`-5000`
- **自动配置**：`OAuth2GatewayAutoConfiguration`
- **启用条件**：`carlos.gateway.oauth2.enabled=true`（默认启用）

**作用**：

1. 统一处理 JWT 和 Opaque Token 认证
2. 从 Authorization 头、自定义 Token 头、Query 参数提取 Token
3. 验证 Token 有效性，构建 UserContext
4. 将用户上下文注入请求头和 Exchange Attributes

**Token 提取顺序**：

1. `Authorization: Bearer {token}`
2. 自定义 Token 头（`AuthConstant.TOKEN_HEADER`）
3. Query 参数 `access_token`（用于 WebSocket 等场景）

**注入的请求头**：

| Header           | 说明             |
|------------------|----------------|
| `X-User-Token`   | 用户 Token       |
| `X-User-Account` | 用户账号           |
| `X-User-Id`      | 用户 ID          |
| `X-Dept-Id`      | 部门 ID          |
| `X-Role-Id`      | 角色 ID          |
| `X-Tenant-Id`    | 租户 ID          |
| `X-User-Phone`   | 手机号            |
| `X-Role-Ids`     | 角色 ID 列表（逗号分隔） |
| `X-Dept-Ids`     | 部门 ID 列表（逗号分隔） |

**可配置项**（`OAuth2GatewayProperties`）：

| 属性                                                   | 类型          | 默认值                                    | 说明                  |
|------------------------------------------------------|-------------|----------------------------------------|---------------------|
| `carlos.gateway.oauth2.enabled`                      | boolean     | `true`                                 | 是否启用 OAuth2 认证      |
| `carlos.gateway.oauth2.token-type`                   | JWT/OPAQUE  | `JWT`                                  | Token 类型            |
| `carlos.gateway.oauth2.jwt-public-key`               | String      | 无                                      | JWT 公钥              |
| `carlos.gateway.oauth2.jwt-issuer`                   | String      | `carlos-auth`                          | JWT 发行者             |
| `carlos.gateway.oauth2.jwt-audience`                 | String      | `carlos-gateway`                       | JWT 受众              |
| `carlos.gateway.oauth2.introspection-uri`            | String      | `http://carlos-auth/oauth2/introspect` | Opaque Token 内省地址   |
| `carlos.gateway.oauth2.client-id`                    | String      | 无                                      | Opaque Token 客户端 ID |
| `carlos.gateway.oauth2.client-secret`                | String      | 无                                      | Opaque Token 客户端密钥  |
| `carlos.gateway.oauth2.introspection-cache-duration` | Duration    | `5m`                                   | Token 内省结果缓存时间      |
| `carlos.gateway.oauth2.authorization-enabled`        | boolean     | `true`                                 | 是否启用授权校验            |
| `carlos.gateway.oauth2.authorization-mode`           | RBAC/ABAC   | `RBAC`                                 | 授权模式                |
| `carlos.gateway.oauth2.pkce-enabled`                 | boolean     | `true`                                 | 是否启用 PKCE           |
| `carlos.gateway.oauth2.whitelist`                    | Set<String> | 见源码 DEFAULT_WHITELIST                  | 认证白名单               |

**配置示例**：

```yaml
carlos:
  gateway:
    oauth2:
      enabled: true
      token-type: OPAQUE
      introspection-uri: http://carlos-auth/oauth2/introspect
      client-id: gateway-client
      client-secret: gateway-secret
      introspection-cache-duration: 5m
      authorization-enabled: true
      authorization-mode: RBAC
      whitelist:
        - /public/**
        - /static/**
```

**注意事项**：

- 优先使用统一白名单结果，白名单路径跳过认证
- OPTIONS 预检请求直接放行
- Token 验证失败返回 401，禁止穿透到下游服务

---

#### 8. OAuth2AuthorizationFilter（OAuth2 鉴权过滤器）

- **源码位置**：`com.carlos.gateway.oauth2.OAuth2AuthorizationFilter`
- **Order**：`-4500`
- **自动配置**：`OAuth2GatewayAutoConfiguration`
- **启用条件**：`carlos.gateway.oauth2.enabled=true` 且 `carlos.gateway.oauth2.authorization-enabled=true`

**作用**：

1. 基于 RBAC（角色）或 ABAC（属性）进行权限控制
2. 使用 Caffeine 缓存用户权限（5 分钟过期）
3. 缓存权限匹配结果（1 分钟过期）

**权限格式**：

```
{METHOD}:{PATH}
```

例如：`GET:/api/users`、`*:/api/orders/**`

**可配置项**：与 `OAuth2GatewayProperties` 相同，关键配置：

| 属性                                            | 类型        | 默认值    | 说明     |
|-----------------------------------------------|-----------|--------|--------|
| `carlos.gateway.oauth2.authorization-enabled` | boolean   | `true` | 是否启用授权 |
| `carlos.gateway.oauth2.authorization-mode`    | RBAC/ABAC | `RBAC` | 授权模式   |

**配置示例**：

```yaml
carlos:
  gateway:
    oauth2:
      authorization-enabled: true
      authorization-mode: RBAC
```

**注意事项**：

- 必须在认证过滤器之后执行，依赖 `exchange.getAttribute(AuthConstant.USER_CONTEXT)`
- 使用 `AsyncLoadingCache` 异步加载权限，避免阻塞 EventLoop
- 支持权限刷新：调用 `invalidatePermissionCache(userId)` 清除缓存

---

### 阶段六：路由准备

#### 9. GrayReleaseFilter（灰度发布过滤器）

- **源码位置**：`com.carlos.gateway.gray.GrayReleaseFilter`
- **Order**：`-3000`
- **自动配置**：`GrayReleaseAutoConfiguration`
- **启用条件**：`carlos.gateway.gray.enabled=true`（默认关闭）

**作用**：

1. 支持基于用户、IP、权重、请求头的灰度策略
2. 使用 MurmurHash3 计算哈希，选择灰度实例
3. 设置灰度标记头传递给下游

**灰度策略类型**（`GrayStrategy`）：

| 策略         | 说明                                |
|------------|-----------------------------------|
| `weight`   | 基于权重（0-100）                       |
| `userIds`  | 基于用户 ID 集合                        |
| `ipRanges` | 基于 IP 前缀                          |
| `headers`  | 基于请求头键值对                          |
| `version`  | 基于服务版本元数据                         |
| `hashKey`  | 权重模式下的哈希键类型：IP/USER/HEADER/RANDOM |

**可配置项**（`GrayReleaseProperties`）：

| 属性                               | 类型                        | 默认值     | 说明       |
|----------------------------------|---------------------------|---------|----------|
| `carlos.gateway.gray.enabled`    | boolean                   | `false` | 是否启用灰度发布 |
| `carlos.gateway.gray.strategies` | Map<String, GrayStrategy> | 空       | 服务级灰度策略  |

**GrayStrategy 配置**：

| 属性           | 类型                    | 默认值     | 说明            |
|--------------|-----------------------|---------|---------------|
| `enabled`    | boolean               | `false` | 策略是否启用        |
| `weight`     | int                   | `0`     | 权重百分比         |
| `userIds`    | Set<String>           | 空       | 灰度用户 ID 集合    |
| `ipRanges`   | Set<String>           | 空       | 灰度 IP 前缀集合    |
| `headers`    | Map<String, String>   | 空       | 灰度请求头匹配       |
| `version`    | String                | 空       | 目标版本号         |
| `hashKey`    | IP/USER/HEADER/RANDOM | `IP`    | 权重哈希键         |
| `headerName` | String                | 空       | 自定义 Header 名称 |

**配置示例**：

```yaml
carlos:
  gateway:
    gray:
      enabled: true
      strategies:
        user-service:
          enabled: true
          weight: 10
          version: "2.0.0"
          hash-key: USER
        order-service:
          enabled: true
          user-ids:
            - "10001"
            - "10002"
```

**注意事项**：

- 必须在路由决策之前执行，否则无法改变目标实例
- 优先选择带 `gray=true` 元数据的实例
- 如果没有灰度实例，使用版本匹配的实例

---

#### 10. RequestTransformFilter（请求转换过滤器）

- **源码位置**：`com.carlos.gateway.transform.RequestTransformFilter`
- **Order**：`-1000`
- **自动配置**：`TransformAutoConfiguration`
- **启用条件**：`carlos.gateway.transform.enabled=true`（默认启用）

**作用**：

1. API 版本控制（从 Header 或 URL 路径提取版本）
2. 路径重写（预编译 Pattern 缓存）
3. Header 转换（添加/替换/删除）
4. 添加网关标识和转发时间戳

**标准注入头**：

| Header                | 说明            |
|-----------------------|---------------|
| `X-Gateway-Id`        | 网关标识          |
| `X-Gateway-Timestamp` | 网关转发时间戳       |
| `X-Request-Id`        | 请求 ID（未提供时生成） |
| `X-Routed-Version`    | API 版本        |

**可配置项**（`TransformProperties`）：

| 属性                                             | 类型                    | 默认值              | 说明                        |
|------------------------------------------------|-----------------------|------------------|---------------------------|
| `carlos.gateway.transform.enabled`             | boolean               | `true`           | 是否启用转换                    |
| `carlos.gateway.transform.gateway-id`          | String                | `carlos-gateway` | 网关标识                      |
| `carlos.gateway.transform.api-version-enabled` | boolean               | `true`           | 是否启用 API 版本控制             |
| `carlos.gateway.transform.path-rewrites`       | Map<String, String>   | 空                | 路径重写规则（Key: 正则，Value: 替换） |
| `carlos.gateway.transform.header-transforms`   | List<HeaderTransform> | 空                | Header 转换规则               |

**HeaderTransform 配置**：

| 属性          | 类型                  | 默认值   | 说明           |
|-------------|---------------------|-------|--------------|
| `source`    | String              | 无     | 源 Header 名称  |
| `target`    | String              | 无     | 目标 Header 名称 |
| `operation` | ADD/REPLACE/REMOVE  | `ADD` | 操作类型         |
| `prefix`    | String              | 无     | 前缀           |
| `suffix`    | String              | 无     | 后缀           |
| `mapping`   | Map<String, String> | 空     | 值映射          |

**配置示例**：

```yaml
carlos:
  gateway:
    transform:
      enabled: true
      gateway-id: carlos-gateway
      api-version-enabled: true
      path-rewrites:
        "/api/v1/(.*)": "/v1/$1"
      header-transforms:
        - source: X-Old-Header
          target: X-New-Header
          operation: REPLACE
        - source: X-Internal-Token
          operation: REMOVE
```

**注意事项**：

- 在 PrefixWebFilter 之后执行，基于已剥离前缀的路径编写规则
- 使用单次 `request.mutate()` 合并所有修改
- 路径重写规则在 `@PostConstruct` 中预编译

**典型使用场景**：

`RequestTransformFilter` 用于在网关层统一处理入口协议差异，使下游服务无感知地接入。常见场景包括：

1. **API 版本平滑演进**

   外部客户端使用 `/api/v2/users`，但下游服务仍只支持 `/api/v1/users`。网关负责把 v2 路径重写为 v1，逐步灰度升级。

   ```yaml
   carlos:
     gateway:
       transform:
         path-rewrites:
           "/api/v2/(.*)": "/api/v1/$1"
   ```

2. **多客户端统一入口**

   移动端使用 `/m/users`，Web 端使用 `/web/users`，但后端只有一个 `/users` 服务。网关在转发前把路径归一化。

   ```yaml
   carlos:
     gateway:
       transform:
         path-rewrites:
           "/m/(.*)": "/$1"
           "/web/(.*)": "/$1"
   ```

3. **遗留系统路径兼容**

   老系统路径为 `/old/service/order`，新系统路径为 `/order-service/orders`。网关在不改动下游的情况下兼容老路径。

   ```yaml
   carlos:
     gateway:
       transform:
         path-rewrites:
           "/old/service/(.*)": "/$1-service/$1"
   ```

4. **Header 标准化/脱敏**

   把外部传入的 `X-App-Token` 替换为内部约定的 `X-Internal-Token`；移除不应继续透传的内部 Header。

   ```yaml
   carlos:
     gateway:
       transform:
         header-transforms:
           - source: X-App-Token
             target: X-Internal-Token
             operation: REPLACE
             prefix: "Bearer "
           - source: X-App-Secret
             operation: REMOVE
   ```

5. **多租户/多环境标识透传**

   根据请求头中的环境标识，为下游补充固定的内部标识头，避免每个服务重复解析。

   ```yaml
   carlos:
     gateway:
       transform:
         header-transforms:
           - source: X-Env
             target: X-Gateway-Env
             operation: REPLACE
             mapping:
               prod: production
               dev: development
   ```

6. **网关统一埋点**

   无论业务路由如何，都自动注入 `X-Gateway-Id`、`X-Gateway-Timestamp`（由 `RequestTransformFilter` 写入，
   精度为 epoch 毫秒），以及追踪系列头（`X-Trace-Id` / B3 / sw8，由 `RequestTracingFilter` 写入）。

---

### 阶段七：上下文传递

#### 12. IdentityHeaderSanitizeFilter（身份头净化，order = -7900）与 IdentityAssertionSignFilter（身份断言签发，order = -450）

> **本阶段曾由单个 `UserContextRelayFilter`（明文头中继，order `-500`）承担**，现已删除。原因见本节末尾。

**源码位置**：`com.carlos.gateway.identity.IdentityHeaderSanitizeFilter` / `com.carlos.gateway.identity.IdentityAssertionSignFilter`
**自动配置**：`IdentityGatewayAutoConfiguration`

##### 12.1 IdentityHeaderSanitizeFilter (-7900)：不做鉴权，只管净化

无条件剔除客户端自带的身份头，保证下游见到的身份头只可能由网关写入：

- `X-User-Context`（身份断言本身）
- `X-User-Id` / `X-User-Name` / `X-User-Roles` / `X-Tenant-Id` / `X-Dept-Id`

**为什么必须有这一层**：`OAuth2AuthenticationFilter` 在路径命中 oauth2 白名单时会**整个短路**（既不认证、
也不注入身份头），而 `ServerHttpRequest.Builder.header()` 只覆盖它自己写入的头 —— 于是客户端自带的
伪造身份头会原样穿透到下游。本过滤器**无开关、始终生效**。

##### 12.2 IdentityAssertionSignFilter (-450)：签发身份断言

1. 从 Exchange Attributes 读取 `userContext`（由 `OAuth2AuthenticationFilter` 设置）
2. 用共享密钥（HMAC-SHA256）把身份签成 `X-User-Context` 断言写入请求头

**断言格式**：`v1.<payloadBase64Url>.<signatureBase64Url>`

| 载荷字段  | 说明                    |
|-------|-----------------------|
| `uid` | 用户 ID（必填）             |
| `acc` | 登录账号                  |
| `roles` | 角色**编码**列表（如 `SUPER_ADMIN`） |
| `dept` / `ten` | 部门 / 租户 ID            |
| `iat` / `exp` | 签发与过期时间（epoch **秒**）   |

签名串为 `v1.<payload>` —— 签「编码后的字节」而非 JSON 语义，因此两侧不需要共享字段顺序/空白/转义等
规范化约定，也就没有「改了一侧忘了另一侧」导致验签静默失效的空间。

**为什么以 `X-` 开头**：`FeignRequestInterceptor` 只转发 `x-` 前缀的头，本头借此天然支持
「网关 → 服务 A → 服务 B」的多跳身份传递，无需为 Feign 单独适配。

**可配置项**（在 Nacos 共享配置 `common.yml` 中，网关与全部下游服务共读同一份）：

```yaml
carlos:
  identity-assertion:
    sign-enabled: true        # 网关是否签发；默认 false，需显式开启
    verify-enabled: true      # 下游是否接受；默认 true
    secret: <32 字节以上随机值>
    previous-secret: ""       # 密钥轮换宽限期（仅验签接受）
    ttl: 5m
    clock-skew: 60s
    max-lifetime: 10m
```

**注意事项**：

- 开关在**请求时读取**（非 `@ConditionalOnProperty`）—— 条件化 Bean 不随配置刷新重建，会让灰度期出现
  「改了配置不生效」的假象，也让单侧回滚失去即时性
- 签发失败只是本次不注入断言并留 ERROR 日志，**绝不抛异常**（不让一次身份故障演变成网关整体不可用）
- 白名单路径、未认证请求没有 userContext，自然不签发断言，下游降级为匿名（符合预期）

##### 12.3 为什么删掉了原来的明文头中继

`UserContextRelayFilter` 把身份降级成明文头，靠下游的 `carlos.resource-server.auth.trusted-ip-ranges`
（应用层 IP 白名单）证明其不可伪造。该方案有两个结构性问题：

1. **两头不靠**：网络可信时冗余，网络不可信时不够（同网段的伪造者照样能冒充）
2. **与内部调用天然冲突**：`FeignRequestInterceptor` 会转发所有 `x-` 前缀的头，身份头因此在内部调用中被
   多跳传播，而源 IP 变成**发起方服务**而非网关 —— 本机全部服务同 IP 时侥幸不暴雷，多主机部署下每次
   多跳调用都会丢身份

签名断言的完整性与调用方所在网段无关，已整体替代之，`trusted-ip-ranges` 随之删除。

---

### 阶段八：响应优化

#### 13. ResponseCacheFilter（响应缓存过滤器）

- **源码位置**：`com.carlos.gateway.cache.ResponseCacheFilter`
- **Order**：`100`
- **自动配置**：`CacheGatewayAutoConfiguration`
- **启用条件**：`carlos.gateway.cache.enabled=true`（默认启用）

**作用**：

1. 两级缓存：本地 Caffeine L1 + Redis L2
2. 仅缓存 GET/HEAD 请求的可缓存响应
3. 使用 MurmurHash3 快速生成缓存键

**缓存键生成**：

```
gateway:cache:{MurmurHash3(METHOD:PATH?QUERY)}
```

**可缓存状态码**：200 OK、304 Not Modified、300 Multiple Choices

**可配置项**（`CacheProperties`）：

| 属性                                   | 类型       | 默认值          | 说明            |
|--------------------------------------|----------|--------------|---------------|
| `carlos.gateway.cache.enabled`       | boolean  | `true`       | 是否启用缓存        |
| `carlos.gateway.cache.local-size`    | long     | `10000`      | 本地缓存大小        |
| `carlos.gateway.cache.local-expire`  | Duration | `5m`         | 本地缓存过期时间      |
| `carlos.gateway.cache.redis-enabled` | boolean  | `true`       | 是否启用 Redis 缓存 |
| `carlos.gateway.cache.redis-expire`  | Duration | `10m`        | Redis 缓存过期时间  |
| `carlos.gateway.cache.path-prefix`   | String   | `/api/cache` | 缓存路径前缀        |

**配置示例**：

```yaml
carlos:
  gateway:
    cache:
      enabled: true
      local-size: 10000
      local-expire: 5m
      redis-enabled: true
      redis-expire: 10m
      path-prefix: /api/cache
```

**注意事项**：

- 带 `Authorization` 头的请求跳过缓存，避免跨用户缓存泄露
- `Cache-Control: no-cache/no-store` 请求跳过缓存
- 使用 `ServerHttpResponseDecorator` 拦截响应体
- 敏感头（Set-Cookie、WWW-Authenticate 等）不会缓存

---

### 阶段九：访问日志

#### 14. AccessLogFilter（访问日志过滤器）

- **源码位置**：`com.carlos.gateway.observability.AccessLogFilter`
- **Order**：`1000`
- **自动配置**：`ObservabilityAutoConfiguration`
- **启用条件**：`carlos.gateway.access-log.enabled=true`（默认启用）

**作用**：

记录所有经过 Gateway 的请求信息：method、path、status、duration、requestId、traceId、clientIp。

**日志格式**（管道符分隔）：

```
{method}|{path}{query}|{status}|{duration}|{requestId}|{traceId}|{clientIp}
```

**可配置项**（`AccessLogProperties`）：

| 属性                                                 | 类型       | 默认值                                | 说明         |
|----------------------------------------------------|----------|------------------------------------|------------|
| `carlos.gateway.access-log.enabled`                | boolean  | `true`                             | 是否启用访问日志   |
| `carlos.gateway.access-log.log-request-body`       | boolean  | `false`                            | 是否记录请求体    |
| `carlos.gateway.access-log.log-response-body`      | boolean  | `false`                            | 是否记录响应体    |
| `carlos.gateway.access-log.slow-request-threshold` | long     | `1000`                             | 慢请求阈值（毫秒）  |
| `carlos.gateway.access-log.ignore-patterns`        | String[] | actuator、health、prometheus、favicon | 忽略的 URL 路径 |

**配置示例**：

```yaml
carlos:
  gateway:
    access-log:
      enabled: true
      log-request-body: false
      log-response-body: false
      slow-request-threshold: 1000
      ignore-patterns:
        - /actuator/**
        - /health
        - /prometheus
```

**注意事项**：

- 使用 `doFinally` 模式，实际在整条链完成后记录
- 使用独立的 `gateway.access` Logger 输出到专用日志文件
- 需要在 `logback.xml` 中配置 `gateway.access` appender

---

## 非 GlobalFilter 组件

### CarlosRateLimiterGatewayFilterFactory

路由级限流 GatewayFilterFactory，详见「阶段四：流量控制」。

### Resilience4jCircuitBreakerFilter

- **源码位置**：`com.carlos.gateway.circuitbreaker.Resilience4jCircuitBreakerFilter`
- **类型**：`GatewayFilterFactory`
- **自动配置**：`CircuitBreakerAutoConfiguration`
- **启用条件**：`carlos.gateway.circuitbreaker.enabled=true`（默认启用）

**作用**：基于 Resilience4j 实现服务级熔断降级。

**默认熔断配置**：

| 属性      | 默认值  | 说明          |
|---------|------|-------------|
| 失败率阈值   | 50%  | 触发熔断的失败率    |
| 慢调用阈值   | 80%  | 触发熔断的慢调用比例  |
| 慢调用时间阈值 | 2s   | 慢调用判定时间     |
| 半开状态调用数 | 10   | 半开状态允许的调用数  |
| 滑动窗口大小  | 100  | 统计窗口        |
| 最小调用数   | 10   | 触发熔断的最小调用数  |
| 熔断持续时间  | 30s  | 熔断器打开状态持续时间 |
| 自动转换半开  | true | 是否自动从打开转到半开 |

**配置示例**：

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
            - name: Resilience4jCircuitBreaker
              args:
                name: user-service-cb
                fallbackUri: forward:/fallback
```

### SelectStripPrefixGatewayFilterFactory

- **源码位置**：`com.carlos.gateway.filter.SelectStripPrefixGatewayFilterFactory`
- **类型**：`GatewayFilterFactory`
- **自动配置**：`FilterAutoConfiguration`
- **启用条件**：无条件，始终注册

**作用**：选择性路径前缀剥离，用于 Swagger/Knife4j 文档路径。

**转换示例**（`parts=1`）：

```
/user-service/doc.html           -> /doc.html
/order-service/v3/api-docs       -> /v3/api-docs
/user-service/api/users          -> /user-service/api/users (不匹配白名单，不剥离)
```

**内置白名单路径**：

- `/doc.html`、`/swagger-ui.html`、`/favicon.ico`
- `/swagger-resources/**`、`/v3/api-docs/**`、`/v3/api-docs-ext`
- `/webjars/**`、`/csrf`

**配置示例**：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: knife4j-gateway
          uri: http://localhost:9510
          predicates:
            - Path=/manage-api/doc.html,/manage-api/webjars/**,/manage-api/swagger-resources/**,/manage-api/v3/api-docs/**
          filters:
            - SelectStripPrefix=1
```

### GatewayExceptionHandler

- **源码位置**：`com.carlos.gateway.config.GatewayExceptionHandler`
- **类型**：`ErrorWebExceptionHandler`
- **自动配置**：`InfrastructureAutoConfiguration`
- **Order**：`Ordered.HIGHEST_PRECEDENCE`

**作用**：统一处理网关层面的所有异常，返回标准化的 JSON 错误响应。

**可配置项**（`ExceptionProperties`）：

| 属性                                          | 类型      | 默认值     | 说明             |
|---------------------------------------------|---------|---------|----------------|
| `carlos.gateway.exception.dev-mode`         | boolean | `false` | 开发模式（显示详细错误信息） |
| `carlos.gateway.exception.show-stack-trace` | boolean | `false` | 是否显示异常堆栈       |

**支持的异常类型**：

| 异常类型                         | 处理方式                                  |
|------------------------------|---------------------------------------|
| `GatewayException`           | 提取 httpStatus、errorCode、message、extra |
| `GlobalException`            | 根据 errorCode 映射 HTTP 状态               |
| `NotFoundException`          | 服务未找到                                 |
| `ResponseStatusException`    | 使用原始状态码                               |
| `WebClientResponseException` | 下游服务错误，隐藏原始响应体                        |
| `TimeoutException`           | 504 Gateway Timeout                   |
| `ConnectException`           | 502 Bad Gateway                       |
| 其他异常                         | 500 Internal Server Error             |

**配置示例**：

```yaml
carlos:
  gateway:
    exception:
      dev-mode: false
      show-stack-trace: false
```

---

## 快速开始

### 1. 最小配置

```yaml
server:
  port: 9510

spring:
  application:
    name: carlos-gateway
  cloud:
    gateway:
      routes:
        - id: carlos-auth
          uri: lb://carlos-auth
          predicates:
            - Path=/auth/**,/oauth2/**
          filters:
            - SelectStripPrefix=1

carlos:
  gateway:
    prefix: /api/v1
    prefix-enabled: true
    whitelist:
      enabled: true
      auth-paths:
        - /oauth2/**
        - /api/public/**
    oauth2:
      enabled: true
      token-type: OPAQUE
      introspection-uri: http://carlos-auth/oauth2/introspect
      client-id: gateway-client
      client-secret: gateway-secret
```

### 2. 启动服务

```bash
mvn spring-boot:run
```

### 3. 查看已注册过滤器

```bash
# 查看所有已注册的 GlobalFilter
curl http://localhost:9510/actuator/gateway/globalfilters

# 查看路由详情（含 GatewayFilter）
curl http://localhost:9510/actuator/gateway/routes
```

---

## 完整配置参考

```yaml
carlos:
  gateway:
    prefix: /manage-api
    prefix-enabled: true
    whitelist:
      - /custom/public/**

    tracing:
      enabled: true
      sampling-rate: 1.0
      ignore-patterns:
        - /actuator/**
        - /health

    metrics:
      enabled: true

    whitelist:
      enabled: true
      global-paths:
        - /actuator/**
      auth-paths:
        - /oauth2/**
        - /public/**

    waf:
      enabled: true
      sql-injection-protection: true
      xss-protection: true
      csrf-protection: true
      allowed-origin: "http://localhost:3000"

    replay:
      enabled: true
      strict-mode: false
      secret-key: ${REPLAY_SECRET_KEY}

    oauth2:
      enabled: true
      token-type: OPAQUE
      introspection-uri: http://carlos-auth/oauth2/introspect
      client-id: gateway-client
      client-secret: gateway-secret
      authorization-enabled: true
      authorization-mode: RBAC

    rate-limiter:
      enabled: true
      default-config:
        replenish-rate: 100
        burst-capacity: 200
        key-resolver: IP

    gray:
      enabled: false
      strategies:
        user-service:
          enabled: true
          weight: 10

    transform:
      enabled: true
      api-version-enabled: true

    cache:
      enabled: true
      local-size: 10000
      redis-enabled: true

    access-log:
      enabled: true
      slow-request-threshold: 1000

    exception:
      dev-mode: false
      show-stack-trace: false

    circuitbreaker:
      enabled: true
```

---

## 文档

- [文档中心](./docs/README.md)
- [过滤器执行顺序说明](./docs/过滤器顺序说明.md)
- [Spring Cloud Gateway 4.x 过滤器详解](./docs/03-develop/guide/DEV-SpringCloudGateway4.x过滤器详解.md)
- [统一白名单优化方案](./docs/02-design/decision/ADR-001-统一白名单优化方案.md)
- [网关现代设计](./docs/02-design/architecture/ARCH-网关现代设计.md)

## License

Copyright (c) Carlos Team
