# Redis 分布式限流使用指南

基于 Redisson RRateLimiter 实现的分布式限流工具，支持注解驱动和编程式两种方式。

## 选型说明

**选用 Redisson RateLimiter 而非 Bucket4j 的原因：**

1. **零额外依赖** - 项目已集成 Redisson，无需引入 Bucket4j 及其 Redis 适配器
2. **技术栈统一** - 与现有分布式锁基础设施共享 Redis 连接和配置
3. **功能完备** - 支持令牌桶算法、分布式限流、多种时间单位
4. **维护成本低** - 减少版本冲突和依赖管理复杂度

## 快速开始

### 1. 配置属性

```yaml
carlos:
  redis:
    rate-limit:
      enabled: true                    # 是否启用限流，默认 true
      prefix: "framework:ratelimit:"    # 限流键前缀
      default-rate: 100                # 默认速率
      default-rate-interval: 1         # 默认速率间隔
      default-rate-interval-unit: SECONDS
      default-capacity: 100            # 默认桶容量
      default-max-wait-time: 5000      # 默认最大等待时间（毫秒）
      metrics-enabled: true            # 是否启用监控
      rate-limiter-expire-minutes: 60  # 限流器过期时间
```

### 2. 注解方式使用

#### 2.1 全局限流

```java
@RestController
@RequestMapping("/api")
public class OrderController {

    /**
     * 全局限流：每秒最多100个请求
     */
    @RateLimit(rate = 100, rateInterval = 1, rateIntervalUnit = TimeUnit.SECONDS)
    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderParam param) {
        return orderService.create(param);
    }
}
```

#### 2.2 用户级限流

```java
/**
 * 用户级限流：每个用户每分钟最多10次
 * 需要从请求上下文获取用户ID（通过 @RequestAttribute 或请求头）
 */
@RateLimit(
    type = RateLimitType.USER,
    key = "order:submit",
    rate = 10,
    rateInterval = 1,
    rateIntervalUnit = TimeUnit.MINUTES
)
@PostMapping("/orders/submit")
public Result submitOrder() {
    // 业务逻辑
}
```

#### 2.3 IP 限流

```java
/**
 * IP限流：每个IP每小时最多1000次请求
 * 适用于防止恶意攻击、爬虫
 */
@RateLimit(
    type = RateLimitType.IP,
    key = "api:sensitive",
    rate = 1000,
    rateInterval = 1,
    rateIntervalUnit = TimeUnit.HOURS
)
@GetMapping("/sensitive-data")
public SensitiveData getSensitiveData() {
    // 业务逻辑
}
```

#### 2.4 自定义限流

```java
/**
 * 自定义限流：基于方法参数
 * 使用 SpEL 表达式定义限流维度
 */
@RateLimit(
    type = RateLimitType.CUSTOM,
    key = "report:generate",
    customKeyExpression = "#userId",
    rate = 5,
    rateInterval = 10,
    rateIntervalUnit = TimeUnit.MINUTES
)
@GetMapping("/reports/{userId}")
public Report generateReport(@PathVariable String userId) {
    // 每个用户10分钟内最多生成5次报告
}

/**
 * 组合参数的自定义限流
 */
@RateLimit(
    type = RateLimitType.CUSTOM,
    key = "api:call",
    customKeyExpression = "#appId + ':' + #apiCode",
    rate = 1000,
    rateInterval = 1,
    rateIntervalUnit = TimeUnit.HOURS
)
@PostMapping("/openapi/call")
public ApiResult callApi(@RequestParam String appId, @RequestParam String apiCode) {
    // 每个应用+API组合每小时限流1000次
}
```

#### 2.5 阻塞等待策略

```java
/**
 * 阻塞等待：最多等待5秒获取令牌
 * 适用于重要业务，愿意等待的场景
 */
@RateLimit(
    rate = 10,
    strategy = RateLimitStrategy.BLOCK,
    maxWaitTime = 5000
)
@PostMapping("/important-task")
public Result importantTask() {
    // 业务逻辑
}
```

#### 2.6 降级处理策略

```java
/**
 * 降级策略：限流时执行 fallback 方法
 */
@RateLimit(
    rate = 100,
    strategy = RateLimitStrategy.FALLBACK,
    fallbackMethod = "getUserFallback"
)
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userService.getById(id);
}

/**
 * 降级方法：方法签名必须与原方法相同
 */
public User getUserFallback(Long id) {
    // 返回缓存数据或默认值
    return UserCache.get(id);
}
```

#### 2.7 带突发容量的限流

```java
/**
 * 令牌桶算法：rate=100, capacity=200
 * 每秒产生100个令牌，但最多可累积200个令牌应对突发流量
 */
@RateLimit(
    rate = 100,
    capacity = 200,
    rateInterval = 1,
    rateIntervalUnit = TimeUnit.SECONDS
)
@GetMapping("/burst-api")
public Result burstApi() {
    // 可应对短时间的突发流量
}
```

### 3. 编程式使用

#### 3.1 基础限流检查

```java
@Service
public class OrderService {

    public void createOrder(OrderParam param) {
        // 限流检查：每秒最多10个请求
        boolean allowed = RateLimitUtil.tryAcquire(
            "order:submit",
            10,
            1,
            TimeUnit.SECONDS
        );

        if (!allowed) {
            throw new BusinessException("提交订单过于频繁，请稍后再试");
        }

        // 执行业务逻辑
    }
}
```

#### 3.2 带突发容量的限流

```java
public void apiCall(String appId) {
    // 每分钟100次，桶容量200（允许突发）
    boolean allowed = RateLimitUtil.tryAcquire(
        "api:call:" + appId,
        100,      // 速率
        200,      // 容量
        1,
        TimeUnit.MINUTES
    );

    if (!allowed) {
        throw new RateLimitException("api:call:" + appId);
    }
}
```

#### 3.3 阻塞等待获取许可

```java
public Report generateReport(String userId) {
    // 尝试获取许可，最多等待3秒
    boolean acquired = RateLimitUtil.tryAcquire(
        "report:generate:" + userId,
        1,
        1,
        TimeUnit.MINUTES,
        3000  // 等待3秒
    );

    if (!acquired) {
        throw new RateLimitException("report:generate:" + userId, 3000);
    }

    return doGenerateReport(userId);
}
```

#### 3.4 模板方法（推荐）

```java
// 方式1：快速失败模式
public Order createOrder(Long userId, OrderParam param) {
    return RateLimitUtil.executeWithRateLimit(
        "order:create:" + userId,
        10,           // 速率
        10,           // 容量
        1,
        TimeUnit.MINUTES,
        () -> {
            // 执行业务逻辑
            return orderRepository.save(param);
        }
    );
}

// 方式2：带降级处理
public Data fetchExternalData(String key) {
    return RateLimitUtil.executeWithFallback(
        "external:api:" + key,
        100,
        1,
        TimeUnit.SECONDS,
        () -> externalApi.call(key),    // 主逻辑
        () -> localCache.get(key)       // 降级逻辑
    );
}

// 方式3：阻塞等待模式
public ImportantResult processImportantTask(String taskId) {
    return RateLimitUtil.executeWithBlock(
        "task:important:" + taskId,
        5,
        5,
        1,
        TimeUnit.MINUTES,
        10000,  // 最多等待10秒
        () -> taskService.process(taskId)
    );
}
```

#### 3.5 限流器管理

```java
@Component
public class RateLimitManager {

    /**
     * 删除限流器
     */
    public void deleteRateLimiter(String key) {
        RateLimitUtil.deleteRateLimiter(key);
    }

    /**
     * 检查限流器是否存在
     */
    public boolean checkExists(String key) {
        return RateLimitUtil.isExists(key);
    }

    /**
     * 获取限流器可用许可数
     */
    public long getAvailablePermits(String key) {
        return RateLimitUtil.availablePermits(key);
    }
}
```

## 限流策略对比

| 策略            | 说明                      | 适用场景               |
|---------------|-------------------------|--------------------|
| **FAIL_FAST** | 立即抛出 RateLimitException | API 接口、用户操作，需要快速响应 |
| **BLOCK**     | 等待令牌可用（带超时）             | 重要任务、队列处理，可接受等待    |
| **FALLBACK**  | 执行降级逻辑                  | 有备用方案的业务，如读缓存      |

## 限流类型对比

| 类型         | 说明              | 示例            |
|------------|-----------------|---------------|
| **GLOBAL** | 全局限流，所有请求共享     | 保护下游服务、系统负载保护 |
| **USER**   | 用户级限流，每个用户独立    | 防止刷单、API 滥用   |
| **IP**     | IP 级限流，每个 IP 独立 | 防止攻击、爬虫限制     |
| **CUSTOM** | 自定义维度，SpEL 表达式  | 复杂业务场景        |

## SpEL 表达式支持

在 `customKeyExpression` 中支持以下表达式：

```java
// 使用参数名
@RateLimit(type = CUSTOM, customKeyExpression = "#userId")
public void method(String userId) {}

// 使用参数索引
@RateLimit(type = CUSTOM, customKeyExpression = "#p0")
public void method(String userId) {}

// 使用对象属性
@RateLimit(type = CUSTOM, customKeyExpression = "#user.id")
public void method(User user) {}

// 组合多个参数
@RateLimit(type = CUSTOM, customKeyExpression = "#appId + ':' + #apiCode")
public void method(String appId, String apiCode) {}
```

## 异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitException(RateLimitException e) {
        String message = e.getWaitTimeMillis() > 0
            ? String.format("请求过于频繁，请在 %d 秒后重试", e.getWaitTimeMillis() / 1000)
            : "请求过于频繁，请稍后重试";

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.fail("429", message));
    }
}
```

## 注意事项

1. **限流键设计** - 确保限流键的唯一性，避免不同业务冲突
2. **容量设置** - 合理设置 capacity，过小会导致频繁限流，过大失去保护作用
3. **异常处理** - 限流异常时默认允许请求通过，避免误杀
4. **Redis 性能** - 高并发场景下，每个限流键会产生 Redis 操作，合理规划键数量
5. **时钟同步** - 分布式限流依赖 Redis 服务器时钟，确保集群时钟同步

## 与 Bucket4j 功能对比

| 特性      | Redisson RateLimiter | Bucket4j        |
|---------|----------------------|-----------------|
| 令牌桶算法   | ✅                    | ✅               |
| 分布式支持   | ✅（基于 Redis）          | ✅（需额外适配器）       |
| 本地+远程多级 | ❌                    | ✅               |
| 项目依赖    | 0（已集成）               | 2+（核心+Redis适配器） |
| 复杂度     | 低                    | 中               |
| 企业级需求覆盖 | 95%                  | 100%            |

**结论**：对于大多数企业级应用，Redisson RateLimiter 已完全满足需求，且维护成本更低。
