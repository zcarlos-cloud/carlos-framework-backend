# carlos-spring-cloud-starter

## 模块简介

`carlos-spring-cloud-starter` 是 Carlos Framework 的 Spring Cloud Alibaba
集成模块，提供了微服务架构所需的核心组件集成，包括服务发现、配置管理、服务调用、负载均衡、熔断降级和分布式事务等功能。基于
Spring Cloud Alibaba 2025.0.0.0 构建，支持 Spring Boot 3.5.9+。

## 主要功能

### 1. FallbackFactory 自动注册（新特性）

模块提供 **FallbackFactory 自动扫描注册** 功能，无需手动为每个 FallbackFactory 创建 Bean 注册方法。

#### 自动注册说明

当应用中存在 `FallbackFactory` 实现类时，starter 会自动扫描并将其注册为 Spring Bean，无需手动在配置类中声明 `@Bean` 方法。

**默认行为**：

- 自动扫描 `com.carlos` 包及其子包下的所有 `FallbackFactory` 实现类
- 自动注册为 Spring Bean，Bean 名称为类名首字母小写
- 已手动注册的 FallbackFactory 不会被重复注册
- 抽象类和接口不会被注册

#### 配置示例

```yaml
carlos:
  feign:
    fallback:
      enabled: true                    # 启用自动注册（默认 true）
      scan-packages:                   # 自定义扫描包路径
        - com.carlos.system.fallback
        - com.carlos.org.fallback
```

#### 使用方式对比

**传统方式（需要手动注册）**：

```java
@Configuration
public class SystemFeignConfig {
    
    @Bean
    public FeignDictFallbackFactory dictFallbackFactory() {
        return new FeignDictFallbackFactory();
    }
    
    @Bean
    public FeignMenuFallbackFactory menuFallbackFactory() {
        return new FeignMenuFallbackFactory();
    }
    
    // 每增加一个 FallbackFactory 都需要手动添加 @Bean 方法
}
```

**自动注册方式（推荐）**：

```java
// 只需要实现 FallbackFactory 接口，无需任何配置
@Slf4j
public class FeignDictFallbackFactory implements FallbackFactory<ApiDict> {
    @Override
    public ApiDict create(Throwable throwable) {
        log.error("字典服务调用失败", throwable);
        return new ApiDict() {
            @Override
            public Result<Dict> getById(String id) {
                return Result.error();
            }
        };
    }
}
```

#### 注意事项

1. **扫描路径**：确保 FallbackFactory 实现类位于扫描路径下（默认 `com.carlos`）
2. **Bean 名称**：自动注册的 Bean 名称为类名首字母小写（如 `FeignDictFallbackFactory` → `feignDictFallbackFactory`）
3. **手动注册优先**：如果已通过 `@Bean` 或 `@Component` 注册，自动注册会跳过该类
4. **Feign 注解**：`@FeignClient` 的 `fallbackFactory` 属性需指向 FallbackFactory 类，而非 Bean 名称

```java
@FeignClient(
    name = "system-service",
    fallbackFactory = FeignDictFallbackFactory.class  // 指向类，starter 会自动找到对应的 Bean
)
public interface ApiDict {
    // ...
}
```

### 2. 一站式启动注解

**@SpringCloudApplication** 注解简化微服务启动配置：

```java
@SpringCloudApplication
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
```

**注解包含**：

- `@EnableDiscoveryClient`: 启用服务发现
- `@EnableFeignClients("com.carlos")`: 启用 Feign 客户端
- `@SpringBootApplication`: Spring Boot 应用

### 3. OpenFeign 服务调用

#### Feign 配置

```java
// 配置 Feign 客户端
@FeignClient(name = "user-service", path = "/api/user")
public interface UserServiceClient {

    @GetMapping("/{id}")
   Result<User> getUser(@PathVariable("id") Long id);

    @PostMapping
   Result<User> create(@RequestBody UserDTO user);

    @GetMapping("/list")
   Result<List<User>> list(@SpringQueryMap UserQuery query);
}
```

#### Feign 配置属性

```yaml
carlos:
  feign:
    log:
      enable: true          # 启用 Feign 日志
      level: FULL           # 日志级别: NONE, BASIC, HEADERS, FULL
    
    retry:
      enabled: true         # 启用重试
      period: 100           # 重试间隔(ms)
      max-period: 1000      # 最大重试间隔(ms)
      max-attempts: 3       # 最大重试次数
    
    header:
      pass-authorization: true   # 传递 Authorization
      pass-request-id: true      # 传递 X-Request-Id
      pass-tenant-id: true       # 传递 X-Tenant-Id
      pass-user-id: true         # 传递 X-User-Id
      pass-headers:              # 额外传递的请求头
        - X-Custom-Header
    
    pool:
      enabled: true         # 启用连接池
      max-idle: 50          # 最大空闲连接
      keep-alive-duration: 5  # 连接保持时长(分钟)
      connect-timeout: 5000   # 连接超时(ms)
      read-timeout: 10000     # 读取超时(ms)
    
    fallback:
      enabled: true         # 启用 FallbackFactory 自动注册（默认 true）
      scan-packages:        # 扫描包路径列表（默认 com.carlos）
        - com.carlos.system.fallback
        - com.carlos.org.fallback

# Feign 客户端默认配置
feign:
  client:
    config:
      default:
        connect-timeout: 5000
        read-timeout: 10000
  okhttp:
    enabled: true
```

#### Feign 上下文传递（异步场景）

```java
// 在异步调用前设置上下文
Map<String, String> context = new HashMap<>();
context.put("X-Tenant-Id", "tenant_001");
context.put("X-User-Id", "user_123");
FeignRequestInterceptor.setContext(context);

// 执行 Feign 调用
userServiceClient.getUser(1L);

// 清理上下文
FeignRequestInterceptor.clearContext();
```

### 4. Nacos 服务发现与配置

#### 服务注册与发现

```yaml
spring:
  application:
    name: user-service    # 服务名称

  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}  # Nacos 服务器地址
        namespace: ${NACOS_NAMESPACE:public}         # 命名空间
        group: ${NACOS_GROUP:DEFAULT_GROUP}          # 分组
        metadata:
          version: 1.0.0                             # 服务版本
          region: beijing                            # 区域

      config:
        server-addr: ${NACOS_SERVER:localhost:8848}  # 配置中心地址
        file-extension: yaml                          # 配置文件格式
        refresh-enabled: true                         # 启用配置刷新
        shared-configs:                               # 共享配置
          - data-id: common-config.yaml
            group: DEFAULT_GROUP
            refresh: true
```

#### Nacos 扩展配置

```yaml
carlos:
  cloud:
    nacos:
      enabled: true              # 启用扩展功能
      version: 1.0.0             # 服务版本
      region: default            # 服务区域
      weight: 1.0                # 服务权重
      metadata:                  # 自定义元数据
        team: backend
        project: user-center
      heartbeat:
        interval: 5000           # 心跳间隔(ms)
        timeout: 15000           # 心跳超时(ms)
        delete-timeout: 30000    # 服务删除超时(ms)
      subscription:
        enabled: true            # 启用服务订阅监听
        log-change: true         # 服务变更时打印日志
```

### 5. Sentinel 熔断降级

#### 流量控制与熔断

```yaml
spring:
  cloud:
    sentinel:
      enabled: true
      transport:
        dashboard: ${SENTINEL_DASHBOARD:localhost:8080}  # Sentinel 控制台
        port: 8719                                       # 客户端端口
      filter:
        enabled: true                                    # 启用过滤器
      datasource:
        flow:
          nacos:
            server-addr: ${NACOS_SERVER:localhost:8848}
            data-id: ${spring.application.name}-flow-rules
            group-id: SENTINEL_GROUP
            rule-type: flow
```

#### 熔断规则配置

```java
// 在 Controller 中使用 @SentinelResource
@RestController
@RequestMapping("/api/user")
public class UserController {

    @GetMapping("/{id}")
    @SentinelResource(
        value = "getUserById",
        blockHandler = "handleBlock",
        fallback = "handleFallback"
    )
    public Result<User> getUser(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    // 限流处理
    public Result<User> handleBlock(Long id, BlockException ex) {
        return Result.error("请求过于频繁，请稍后重试");
    }

    // 降级处理
    public Result<User> handleFallback(Long id, Throwable ex) {
        return Result.error("服务暂时不可用");
    }
}

// 在 Feign 客户端中使用 fallback
@FeignClient(
    name = "order-service",
    fallbackFactory = OrderServiceFallbackFactory.class
)
public interface OrderServiceClient {
    @GetMapping("/api/orders/user/{userId}")
   Result<List<Order>> getUserOrders(@PathVariable Long userId);
}

@Component
@Slf4j
public class OrderServiceFallbackFactory implements FallbackFactory<OrderServiceClient> {
    @Override
    public OrderServiceClient create(Throwable cause) {
        log.error("Order service fallback", cause);
        return userId ->Result.success(Collections.emptyList());
    }
}
```

### 6. Seata 分布式事务

#### 启用 Seata

```yaml
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default
  client:
    rm:
      async-commit-buffer-limit: 10000
      report-retry-count: 5
      table-meta-check-enable: false
      report-success-enable: false
      saga-branch-register-enable: false
    tm:
      commit-retry-count: 5
      rollback-retry-count: 5
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: ${NACOS_SERVER:localhost:8848}
      namespace: seata
      group: SEATA_GROUP
```

#### 使用分布式事务

```java
@Service
@Slf4j
public class OrderService {

    @Autowired
    private StockServiceClient stockService;

    @Autowired
    private AccountServiceClient accountService;

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 创建订单 - 全局事务
     */
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public Result<Order> createOrder(OrderDTO orderDTO) {
        // 1. 扣减库存
        stockService.deduct(orderDTO.getSkuId(), orderDTO.getQuantity());
        
        // 2. 扣减账户余额
        accountService.debit(orderDTO.getUserId(), orderDTO.getAmount());
        
        // 3. 创建订单
        Order order = new Order();
        BeanUtils.copyProperties(orderDTO, order);
        orderMapper.insert(order);
        
        log.info("创建订单成功, 订单ID: {}", order.getId());
        return Result.success(order);
    }
}
```

### 7. 负载均衡

#### 配置负载均衡策略

```yaml
carlos:
  cloud:
    loadbalancer:
      enabled: true
      strategy: roundrobin   # 轮询: roundrobin, 随机: random
      cache:
        enabled: true
        ttl: 30
        max-size: 1000
      health-check:
        enabled: true
        interval: 30000
        initial-delay: 0
```

### 8. 服务上下文传递

```java
// 设置上下文
ServiceContext.setTenantId("tenant_001");
ServiceContext.setUserId("user_123");
ServiceContext.setRequestId(UUID.randomUUID().toString());

// 获取上下文
String tenantId = ServiceContext.getTenantId();
String userId = ServiceContext.getUserId();

// 清除上下文
ServiceContext.clear();
```

### 9. 统一异常处理

模块已内置统一的 Feign 异常处理和 Sentinel 限流异常处理，返回标准响应格式：

```json
{
  "code": 429,
  "message": "访问过于频繁，请稍后重试",
  "data": null
}
```

### 10. 健康检查

模块自动集成 Spring Boot Actuator 健康检查：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

访问 `/actuator/health` 查看服务健康状态。

## 依赖引入

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-cloud-starter</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

## 依赖项

- **Spring Cloud OpenFeign**: 服务调用
- **Spring Cloud LoadBalancer**: 负载均衡
- **Spring Cloud Alibaba Nacos Discovery**: 服务发现
- **Spring Cloud Alibaba Nacos Config**: 配置管理
- **Spring Cloud Alibaba Sentinel**: 熔断降级
- **Spring Cloud Alibaba Seata**: 分布式事务
- **Feign OkHttp**: HTTP 客户端
- **Caffeine**: 缓存库
- **carlos-spring-boot-core**: 核心基础模块
- **carlos-spring-boot-starter-web**: Spring Boot 集成
- **SkyWalking Toolkit**: 链路追踪支持

## 使用指南

### 1. 快速开始

```java
// 1. 创建微服务应用
@SpringCloudApplication
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}

// 2. 配置 application.yml
// 3. 定义 Feign 客户端
// 4. 实现业务逻辑
```

### 2. 最佳实践

#### Feign 客户端定义规范

```java
// API 接口定义在 {service}-api 模块
@FeignClient(
    name = "order-service",
    path = "/api/order",
    fallbackFactory = OrderServiceClientFallbackFactory.class
)
public interface OrderServiceClient {
    
    @GetMapping("/{id}")
   Result<OrderAO> getById(@PathVariable("id") Long id);
    
    @PostMapping
   Result<OrderAO> create(@RequestBody OrderCreateParam param);
}

// 熔断降级实现在 {service}-bus 模块
@Component
@Slf4j
public class OrderServiceClientFallbackFactory implements FallbackFactory<OrderServiceClient> {
    @Override
    public OrderServiceClient create(Throwable cause) {
        return new OrderServiceClient() {
            @Override
            public Result<OrderAO> getById(Long id) {
                log.error("获取订单失败, id={}", id, cause);
                return Result.error("订单服务暂时不可用");
            }
            
            @Override
            public Result<OrderAO> create(OrderCreateParam param) {
                log.error("创建订单失败", cause);
                return Result.error("订单服务暂时不可用");
            }
        };
    }
}
```

#### 配置中心使用

```java
@RestController
@RefreshScope  // 支持配置动态刷新
public class ConfigController {

    @Value("${dynamic.feature.enabled:false}")
    private Boolean featureEnabled;

    @GetMapping("/config")
    public Result<ConfigInfo> getConfig() {
        return Result.success(new ConfigInfo(featureEnabled));
    }
}
```

## 配置参考

### 完整配置示例

```yaml
spring:
  application:
    name: user-service
  profiles:
    active: dev

  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
        namespace: dev
        group: SERVICES
        metadata:
          version: 1.0.0
      config:
        server-addr: localhost:8848
        namespace: dev
        file-extension: yaml
        refresh-enabled: true
        shared-configs:
          - data-id: common-config.yaml
            group: DEFAULT_GROUP
            refresh: true

    sentinel:
      enabled: true
      transport:
        dashboard: localhost:8080
        port: 8719
      datasource:
        flow:
          nacos:
            server-addr: localhost:8848
            data-id: ${spring.application.name}-flow-rules
            group-id: SENTINEL_GROUP
            rule-type: flow

  loadbalancer:
    enabled: true

seata:
  enabled: true
  tx-service-group: default_tx_group
  service:
    vgroup-mapping:
      default_tx_group: default

carlos:
  feign:
    log:
      enable: true
      level: FULL
    retry:
      enabled: true
      max-attempts: 3
    pool:
      enabled: true
      max-idle: 50
```

## 注意事项

1. **Nacos 版本兼容性**: 确保 Nacos 版本与 Spring Cloud Alibaba 版本兼容
2. **Sentinel 控制台**: 生产环境建议部署 Sentinel 控制台进行监控
3. **Seata TC 服务**: 使用分布式事务前确保 Seata TC 服务已启动
4. **配置刷新**: 使用 `@RefreshScope` 注解的 Bean 支持配置动态刷新
5. **服务发现延迟**: 服务注册和发现可能存在短暂延迟
6. **Feign 超时**: 根据网络情况合理配置 Feign 超时时间
7. **Sentinel 规则持久化**: 建议将 Sentinel 规则持久化到 Nacos 配置中心
8. **多环境支持**: 通过 namespace 和 group 支持多环境隔离
9. **链路追踪**: 结合 SkyWalking 进行服务链路追踪
10. **监控告警**: 配置服务健康监控和告警机制

## 版本要求

- **JDK**: 17+
- **Spring Boot**: 3.5.9+
- **Spring Cloud**: 2025.0.1+
- **Spring Cloud Alibaba**: 2025.0.0.0+
- **Nacos**: 2.2.3+
- **Sentinel**: 1.8.7+
- **Seata**: 2.5.0+
- **Maven**: 3.8+

## 相关模块

- `carlos-spring-boot-core`: 核心基础模块
- `carlos-spring-boot-starter-web`: Spring Boot 集成
- `carlos-spring-boot-starter-mybatis`: 数据访问
- `carlos-spring-boot-starter-redis`: 缓存服务
- `carlos-spring-boot-starter-oauth2`: 认证授权
