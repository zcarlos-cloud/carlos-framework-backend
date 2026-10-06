# Carlos Gateway 安全与性能问题清单

> 扫描日期：2026-06-06
> 扫描范围：carlos-gateway 模块全部 GlobalFilter、GatewayFilterFactory、配置类和 application.yml
> 问题总数：28 项（安全 14 项 + 性能 14 项）
>
> 修复时请逐项打勾，并在"修复日期"列记录时间。

---

## 一、P0 - 立即修复（不修复会导致严重故障或安全漏洞）

| # | 类型 | 文件                                                                                                      | 问题描述                                                                                                                                                           | 修复建议                                                                                                 | 状态  | 修复人 | 修复日期 |
|---|----|---------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------|-----|-----|------|
| 1 | 安全 | `application.yml`                                                                                       | CORS `allowedOrigins: "*"` 与 `allowCredentials: true` 同时使用，严重违反 CORS 安全规范，可能导致凭证泄露给任意恶意域名                                                                      | 将 `allowedOrigins: "*"` 改为具体域名白名单列表，如 `https://admin.zcarlos.com`                                    | [ ] |     |      |
| 2 | 安全 | `WafFilter.java`<br>`ReplayProtectionFilter.java`<br>`CarlosKeyResolver.java`<br>`AccessLogFilter.java` | 所有获取客户端 IP 的地方直接使用 `getRemoteAddress()`，未读取 `X-Forwarded-For` 等代理头。在反向代理（Nginx/K8s Ingress）部署模式下，返回的是代理服务器 IP 而非真实客户端 IP，导致 WAF、限流、日志全部基于错误 IP                 | 新增 `ClientIpUtil` 工具类，优先读取 `X-Forwarded-For`、`X-Real-IP` 等可信代理头，并验证代理 IP 是否来自可信网段                    | [ ] |     |      |
| 3 | 性能 | `OAuth2AuthorizationFilter.java:204`                                                                    | `loadUserPermissions()` 中调用 `permissionProvider.getUserPermissions(userId).block()` 阻塞 Netty EventLoop。这是 WebFlux 致命反模式，权限缓存未命中时每个请求都会阻塞 I/O 线程，导致整个网关停止处理任何请求 | 将 Caffeine `LoadingCache` 改为 `AsyncLoadingCache`，异步加载权限；或在 filter 链中使用 `Mono.deferContextual` 替代同步加载 | [ ] |     |      |

---

## 二、P1 - 高优先级（不修复可被绕过或导致资源泄漏）

| #  | 类型 | 文件                               | 问题描述                                                                                                                                                                              | 修复建议                                                                                                         | 状态  | 修复人 | 修复日期 |
|----|----|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|-----|-----|------|
| 4  | 安全 | `WafFilter.java:316-378`         | 请求体超过 `maxBodySize` 时直接放行（跳过了安全检查）；请求体检查异常时也直接放行。攻击者可发送超大请求体或构造畸形请求体绕过 WAF                                                                                                        | 超大请求体返回 HTTP 413 Payload Too Large；异常时返回 403 拦截，绝不放行                                                         | [ ] |     |      |
| 5  | 安全 | `WafFilter.java:382-414`         | CSRF 检查存在多处缺陷：(1) 获取了 `X-CSRF-Token` 头但从未使用验证；(2) `startsWith` 检查 Origin/Referer 可被绕过（如 `http://localhost:3000.attacker.com` 匹配 `http://localhost:3000`）；(3) 无 SameSite Cookie 支持 | 实现双重提交 Cookie 模式的 CSRF Token 验证；使用精确域名列表或 `URI.getHost()` 匹配替代 `startsWith`；推荐下游服务设置 `SameSite=Strict`       | [ ] |     |      |
| 6  | 安全 | `ResponseCacheFilter.java`       | 缓存键仅使用 method+path+query，未区分用户身份。带 `Authorization` 头的请求也被缓存，导致用户 A 可能获取到用户 B 的缓存响应（如个人资料、订单详情）                                                                                    | (1) 带 `Authorization` 头的请求直接跳过缓存；(2) 缓存键加入用户标识（如 userId 哈希）；(3) 缓存响应前移除 `Set-Cookie` 等敏感头                    | [ ] |     |      |
| 7  | 安全 | `JwtTokenValidator.java:107-149` | JWT Token 验证检查了签名、issuer、audience，但未验证 `exp`（过期时间）和 `nbf`（生效时间）。已过期的 Token 仍可通过验证                                                                                                 | 显式验证 `exp` 和 `nbf` 字段：`exp != null && exp < System.currentTimeMillis()/1000` 则拒绝                             | [ ] |     |      |
| 8  | 性能 | `ResponseCacheFilter.java:279`   | Redis 二级缓存写入使用 `.subscribe()` fire-and-forget 模式：(1) 写入失败完全静默无感知；(2) 无背压控制，高并发下堆积大量未完成的写入操作；(3) 响应已返回后才执行，网关崩溃时缓存数据丢失                                                             | 将 Redis 写入与响应返回绑定：`ReactiveRedisUtil.setValue(...).then(super.writeWith(...))`，并添加 `onErrorResume` 保证主流程不受影响 | [ ] |     |      |
| 9  | 性能 | `WafFilter.java:86-100`          | 5 个静态 `ThreadLocal<Matcher>`（SQL_MATCHER、XSS_MATCHER 等）从未调用 `.remove()`。Netty EventLoop 线程长期复用，Matcher 内部持有 CharSequence 引用无法被 GC                                                 | 移除 ThreadLocal 机制，直接使用 `pattern.matcher(input).find()`。现代 JVM 中 `Pattern.matcher()` 创建开销极低，无需缓存              | [ ] |     |      |
| 10 | 性能 | `ReplayProtectionFilter.java:58` | `ThreadLocal<Mac>` 实例级别缓存，密钥轮换后旧 Mac 实例仍留在 ThreadLocal 中无法释放。在 WebFlux 异步非阻塞模型中，同一线程可能处理多个请求的后续操作，Mac（非线程安全）状态可能被并发修改                                                             | 移除 ThreadLocal，每次创建新 Mac 实例。Mac 创建开销已通过 `Mac.getInstance()` 缓存（JVM 内部），实际性能影响可接受                             | [ ] |     |      |
| 11 | 性能 | `MetricsFilter.java:60-62`       | 三个 `ConcurrentHashMap`（counterCache、timerCache、statusCounterCache）无大小上限。缓存键基于 routeId+pathPattern+method+status，如果路径模式提取失败或路由 ID 动态变化，缓存将无限增长                                     | 使用 `Caffeine` 替代 `ConcurrentHashMap`，设置 `maximumSize(5000)` 和 `expireAfterWrite(1h)` 自动淘汰                    | [ ] |     |      |

---

## 三、P2 - 中优先级（存在风险但影响可控）

| #  | 类型 | 文件                                                                  | 问题描述                                                                                                                                                     | 修复建议                                                                                                                                                           | 状态  | 修复人 | 修复日期 |
|----|----|---------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|-----|-----|------|
| 12 | 安全 | `UnifiedWhitelistProperties.java`<br>`OAuth2GatewayProperties.java` | 默认白名单包含过于宽泛的通配符：`/**/swagger-ui/**`、`/**/v3/api-docs/**`、`/**/doc.html`、`/**/public/**`、`/actuator/**`。`/**/` 匹配任意路径前缀，可能暴露内部服务的 Swagger 文档和 Actuator 端点 | 精确指定白名单路径，如 `/swagger-ui/**`、`/v3/api-docs/**`，移除 `/**/` 前缀通配符。Actuator 端点应单独配置 IP 白名单或 Basic Auth                                                             | [ ] |     |      |
| 13 | 安全 | `ReplayProtectionFilter.java:108-122`                               | (1) GET 请求非严格模式下时间戳为 null 直接放行；(2) 无密钥配置时签名验证直接返回 true（弱验证模式）；(3) 时间戳容差 300 秒过宽，重放窗口大                                                                    | GET 请求非严格模式下至少要求时间戳；无密钥时应在启动时拒绝启动或 WARN 级别报警；时间戳容差缩短至 60 秒                                                                                                     | [ ] |     |      |
| 14 | 安全 | `GatewayExceptionHandler.java:217-238`                              | `handleWebClientResponseException` 将下游服务的原始响应体直接放入 `extra.upstreamResponse` 返回给客户端。下游错误响应可能包含内部错误详情、数据库结构、堆栈跟踪等敏感信息                                      | 不直接暴露下游原始响应；如需传递错误信息，进行敏感信息过滤（正则移除 stacktrace、SQL 语句等）后再返回                                                                                                     | [ ] |     |      |
| 15 | 安全 | `WafFilter.java`（多处日志）<br>`OAuth2AuthenticationFilter.java:86`      | WAF 日志直接记录查询参数（`query.substring(0, 100)`）和 Header 值（含 Authorization Token）。`OAuth2AuthenticationFilter` 记录路径时也可能包含敏感 query 参数                            | 实现 `LogSanitizer` 工具类，对日志中的敏感字段（password、token、authorization、secret、key、cookie 等）进行掩码处理，如 `password=***`                                                       | [ ] |     |      |
| 16 | 安全 | `CarlosRateLimiterGatewayFilterFactory.java:133-137`                | Redis 限流检查异常时默认放行：`onErrorResume(e -> chain.filter(exchange))`。Redis 故障时限流保护完全失效，系统可能遭受 DDoS 攻击                                                          | 提供配置项 `rate-limit.fail-open=false`（默认 false），Redis 故障时返回 503 拒绝请求，而非默认放行                                                                                       | [ ] |     |      |
| 17 | 性能 | `UnifiedWhitelistFilter.java:112-119`                               | 缓存达到 10000 条时直接 `clearCache()` 清空全部缓存。清空后所有请求重新执行 Ant 路径匹配（CPU 密集型），导致周期性性能抖动                                                                            | 使用 `Caffeine` 替代 `ConcurrentHashMap`，利用其 LRU/W-TinyLFU 淘汰策略自动管理缓存，无需手动清空                                                                                       | [ ] |     |      |
| 18 | 性能 | `WafFilter.java:316-378`                                            | `DataBufferUtils.join()` 失败时（非 flatMap 内部异常），`onErrorResume` 中未释放 DataBuffer。Netty 堆外内存泄漏，最终触发 `OutOfDirectMemoryError`                                  | 确保所有异常路径都调用 `DataBufferUtils.release()`。考虑使用 `try-with-resources` 模式或 `doFinally` 释放                                                                           | [ ] |     |      |
| 19 | 性能 | `InfrastructureAutoConfiguration.java:49-52`                        | WebClient `HttpClient` 使用默认连接池，未配置最大连接数、空闲超时、生命周期。高并发下连接耗尽，下游服务响应慢时请求大量堆积                                                                                | 配置 `ConnectionProvider.builder("gateway-pool").maxConnections(500).maxIdleTime(30s).maxLifeTime(5min).pendingAcquireTimeout(10s).evictInBackground(30s)`       | [ ] |     |      |
| 20 | 性能 | `application.yml`                                                   | 缺少 Redis Lettuce 连接池配置（max-active、max-idle、min-idle、max-wait）和 Gateway HTTP 客户端连接池配置                                                                     | 添加 `spring.redis.lettuce.pool.max-active=100`、`spring.cloud.gateway.httpclient.pool.type=elastic`、`spring.cloud.gateway.httpclient.pool.max-connections=500` 等 | [ ] |     |      |

---

## 四、P3 - 低优先级（优化项，可延后处理）

| #  | 类型 | 文件                                                                     | 问题描述                                                                                             | 修复建议                                                                                             | 状态  | 修复人 | 修复日期 |
|----|----|------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|-----|-----|------|
| 21 | 安全 | `application.yml:217`                                                  | 防重放密钥使用默认回退值 `${REPLAY_SECRET_KEY:your-secret-key-here}`。环境变量未设置时使用弱默认密钥，攻击者可构造合法签名              | 移除默认值，改为 `${REPLAY_SECRET_KEY}`。在 `ReplayProtectionFilter` 或配置类中添加 `@PostConstruct` 验证，密钥为空时拒绝启动 | [ ] |     |      |
| 22 | 安全 | `OpaqueTokenValidator.java:57`                                         | 使用原始 Token 明文作为 Redis 缓存键：`"token:introspection:" + token`。Token 以明文形式存储在 Redis 键中，日志、监控、备份中可能暴露 | 使用 `DigestUtils.sha256Hex(token)` 或 MurmurHash3 哈希值作为缓存键，避免明文 Token 出现在键名中                       | [ ] |     |      |
| 23 | 性能 | `GrayReleaseFilter.java:192-245`<br>`ResponseCacheFilter.java:178-230` | 两个类各自实现了完全相同的 MurmurHash3 算法（约 50 行重复代码），增加维护成本，修改时容易遗漏                                          | 提取公共工具类 `MurmurHash3Util`，两个过滤器统一调用                                                              | [ ] |     |      |
| 24 | 性能 | `CarlosRedisRateLimiter.java:186`                                      | 使用 `Math.random()`（全局同步种子），高并发下存在轻微竞争，虽然影响很小但可优化                                                 | 改为 `ThreadLocalRandom.current().nextDouble()`                                                    | [ ] |     |      |
| 25 | 性能 | `Resilience4jCircuitBreakerFilter.java:42`                             | `ConcurrentHashMap<String, CircuitBreaker>` 无上限。每个路由 ID 创建一个熔断器实例，动态路由场景下（如基于路径参数）无限增长导致内存泄漏     | 使用 `Caffeine` 替代，设置 `maximumSize(1000)` 和 `expireAfterAccess(1h)` 自动淘汰不活跃路由的熔断器                  | [ ] |     |      |
| 26 | 性能 | `AccessLogFilter.java:80-84`                                           | 每次请求都执行 `String.formatted()` 拼接（创建新字符串），极高 QPS 下消耗 CPU                                           | 当前实现已可接受（使用独立 Logger 异步输出）。极端场景（10万+QPS）可使用 `StringBuilder` 池化或批量日志写入                            | [ ] |     |      |

---

## 五、额外发现（非安全/性能问题，但值得修复）

| #  | 类型 | 文件                               | 问题描述                                                                                                                              | 修复建议                                                             | 状态  | 修复人 | 修复日期 |
|----|----|----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|-----|-----|------|
| 27 | 代码 | ~~`UserContextRelayFilter.java:41`~~ | 标注了 `@Component`，但其他过滤器均通过自动配置类注册。如果配置类中也定义了 Bean，可能导致重复注册                                                                        | 移除 `@Component` 注解，统一通过 `OAuth2GatewayAutoConfiguration` 注册 Bean | [x] | carlos | 2026-09-23（**该过滤器已整体删除**：明文头中继被 `identity/IdentityAssertionSignFilter` 的 HMAC 签名断言替代，问题随之消失） |     |      |
| 28 | 代码 | `RequestTransformFilter.java:32` | Javadoc 声称优化点包含"使用 IdUtils.fastSimpleUUID() 替代 UUID.randomUUID()"，但实际代码第 224 行仍使用 `UUID.randomUUID().toString().replace("-", "")` | 统一改为 `IdUtil.fastSimpleUUID()`，或修正 Javadoc 描述                    | [ ] |     |      |

---

## 六、修复优先级建议

```
第一轮（立即）：P0 全部 3 项
第二轮（本周）：P1 全部 8 项
第三轮（下周）：P2 全部 9 项
第四轮（排期）：P3 全部 6 项 + 额外发现 2 项
```

### 最致命的 3 个问题

1. **#3 OAuth2 `.block()`** - 高并发下网关完全不可用，所有路由受影响
2. **#1 CORS `*`** - 凭证可能泄露给任意恶意域名
3. **#2 客户端 IP 获取错误** - 反向代理部署时所有 IP 安全策略（WAF、限流、日志）全部失效

### 最危险的绕过方式

1. **#4 WAF 超大请求体放行** - 发送 10MB 请求体即可绕过 WAF 检查
2. **#6 响应缓存跨用户泄露** - 带 Token 的请求命中缓存后，其他用户可获取相同缓存响应
3. **#7 JWT 缺少过期验证** - 窃取的历史 Token 永久有效

---

*本文档由自动化扫描生成，修复后请更新状态列并记录修复日期。*
