# carlos-spring-boot-starter-apm

## 模块简介

`carlos-spring-boot-starter-apm` 是 Carlos 框架的应用性能监控（APM）集成模块，提供了对 SkyWalking 和 Spring Cloud Sleuth（基于
Micrometer Tracing + Brave）的集成支持。该模块实现了分布式追踪、链路监控、性能指标收集等功能，帮助开发者和运维人员监控微服务系统的运行状态。

## 主要功能

### 1. 分布式追踪支持

集成 Spring Cloud Sleuth（基于 Micrometer Tracing + Brave）提供分布式追踪能力，自动为每个请求生成唯一的 Trace ID 和 Span
ID：

```java
import com.carlos.apm.TraceUtil;

// 获取当前请求的 Trace ID（优先 Sleuth，其次 SkyWalking）
String traceId = TraceUtil.getUnifiedTraceId();

// 获取 Sleuth Trace ID
String sleuthTraceId = TraceUtil.getTraceId();

// 获取 SkyWalking Trace ID
String swTraceId = TraceUtil.getSkyWalkingTraceId();

// 获取当前 Span 的 ID
String spanId = TraceUtil.getSpanId();

// 在日志中记录追踪信息
log.info("[TraceId: {}] 用户操作记录", traceId);
```

### 2. SkyWalking 集成

集成 Apache SkyWalking Toolkit，提供完整的 APM 功能：

- **分布式追踪**：跨服务的调用链路追踪
- **服务拓扑图**：可视化展示服务间依赖关系
- **性能指标**：响应时间、吞吐量、错误率等
- **日志关联**：日志与追踪信息自动关联
- **告警监控**：基于规则的异常告警

```java
import com.carlos.apm.skywalking.util.SkyWalkingUtil;

// 获取 SkyWalking Trace ID
String traceId = SkyWalkingUtil.getTraceId();

// 检查是否在追踪上下文中
if (SkyWalkingUtil.inTraceContext()) {
    // 添加自定义标签
    SkyWalkingUtil.setTag("user.id", "12345");
    SkyWalkingUtil.setTag("business.module", "ORDER");
    
    // 记录事件
    SkyWalkingUtil.logInfo("订单创建成功");
    
    // 记录错误
    SkyWalkingUtil.setError("订单处理异常");
}

// 获取完整上下文信息
String fullContext = SkyWalkingUtil.getFullContext();
// 输出: traceId|segmentId|spanId
```

### 3. MDC 日志上下文

自动将 Trace ID 注入到 MDC（Mapped Diagnostic Context），方便在日志中统一输出：

```java
import com.carlos.apm.mdc.MdcUtil;

// 手动设置 Trace ID 到 MDC
MdcUtil.setTraceId();
MdcUtil.setSkyWalkingTraceId();

// 获取 MDC 中的 Trace ID
String traceId = MdcUtil.getTraceId();

// 设置自定义值
MdcUtil.put("requestId", requestId);
String value = MdcUtil.get("requestId");

// 清理 MDC（请求结束时自动清理）
MdcUtil.clear();
```

**logback-spring.xml 配置示例**：

```xml
<configuration>
    <!-- 日志格式，包含 Trace ID -->
    <property name="LOG_PATTERN" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%thread] %-5level %logger{36} - %msg%n" />

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

日志输出示例：
```
2024-12-09 14:30:25.123 [a1b2c3d4e5f6g7h8] [http-nio-8080-exec-1] INFO  c.c.service.UserService - 用户查询成功
```

### 4. @TraceTag 注解

通过注解方式在方法上添加自定义追踪标签：

```java
import com.carlos.apm.annotation.TraceTag;

@Service
public class OrderService {

    // 简单的标签添加
    @TraceTag(key = "order.action", value = "'create'")
    public Order createOrder(OrderParam param) {
        // 业务逻辑
        return order;
    }

    // 使用 SpEL 表达式引用参数
    @TraceTag(key = "user.id", value = "#userId")
    @TraceTag(key = "order.amount", value = "#param.amount")
    public Order createOrder(Long userId, OrderParam param) {
        // 业务逻辑
        return order;
    }

    // 引用返回值
    @TraceTag(key = "order.id", value = "#result.id")
    @TraceTag(key = "order.status", value = "#result.status")
    public Order processOrder(OrderParam param) {
        // 业务逻辑
        return order;
    }

    // 带条件的标签（满足条件时才添加）
    @TraceTag(key = "order.vip", value = "'true'", condition = "#userId > 10000")
    public Order createVipOrder(Long userId, OrderParam param) {
        // 业务逻辑
        return order;
    }
}
```

### 5. 追踪上下文工具类

`TraceUtil` 提供统一的追踪信息获取接口：

```java
import com.carlos.apm.TraceUtil;

// 检查是否在追踪上下文中
if (TraceUtil.isInTraceContext()) {
    // 获取统一 Trace ID（优先 Sleuth）
    String traceId = TraceUtil.getUnifiedTraceId();
    
    // 获取 Sleuth Trace ID
    String sleuthId = TraceUtil.getTraceId();
    
    // 获取 SkyWalking Trace ID
    String swId = TraceUtil.getSkyWalkingTraceId();
    
    // 获取完整追踪信息
    String traceInfo = TraceUtil.getTraceInfo();
    // 输出: TraceContext{sleuth={traceId='xxx', spanId='yyy', parentId='zzz'}, skywalking={...}}
}

// 添加标签到当前 Span
TraceUtil.tag("business.module", "PAYMENT");
TraceUtil.tag("order.id", orderId);

// 添加事件注解
TraceUtil.annotate("Payment completed");
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-apm</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 2. 配置 application.yml

```yaml
carlos:
  apm:
    # 是否启用 APM 模块
    enabled: true
    
    # Sleuth 配置
    sleuth:
      enabled: true
      # 采样率 (0.0 - 1.0)，生产环境建议 0.1
      probability: 1.0
      # 传播类型
      propagation-type:
        - B3
      # 是否启用 Web 追踪
      web-enabled: true
      # 跳过的 URL 路径
      skip-paths:
        - /actuator/**
        - /health
        - /favicon.ico
    
    # SkyWalking 配置
    skywalking:
      enabled: true
      # 是否启用日志上报
      log-report-enabled: true
    
    # MDC 配置
    mdc:
      enabled: true
      # Trace ID 在 MDC 中的 key
      trace-id-key: traceId
      # Span ID 在 MDC 中的 key
      span-id-key: spanId
      # 是否将 Trace ID 添加到响应头
      add-to-response: true
      # 响应头名称
      response-header-name: X-Trace-Id
```

### 3. SkyWalking Agent 配置

SkyWalking Agent 需要单独配置和启动：

```bash
# JVM 启动参数
-javaagent:/path/to/skywalking-agent.jar
-Dskywalking.agent.service_name=your-service-name
-Dskywalking.collector.backend_service=localhost:11800
```

或者使用环境变量：

```bash
# Linux/Mac
export SW_AGENT_NAME=your-service-name
export SW_AGENT_COLLECTOR_BACKEND_SERVICES=localhost:11800

# Windows
set SW_AGENT_NAME=your-service-name
set SW_AGENT_COLLECTOR_BACKEND_SERVICES=localhost:11800
```

## 配置项说明

### Sleuth 配置项

| 配置项                                  | 类型      | 默认值      | 说明             |
|--------------------------------------|---------|----------|----------------|
| `carlos.apm.sleuth.enabled`          | boolean | `true`   | 是否启用 Sleuth 追踪 |
| `carlos.apm.sleuth.probability`      | double  | `1.0`    | 采样率 (0.0-1.0)  |
| `carlos.apm.sleuth.propagation-type` | List    | `["B3"]` | 传播类型           |
| `carlos.apm.sleuth.web-enabled`      | boolean | `true`   | 是否启用 Web 追踪    |
| `carlos.apm.sleuth.async-enabled`    | boolean | `true`   | 是否启用异步追踪       |
| `carlos.apm.sleuth.skip-paths`       | List    | 见上方      | 跳过的 URL 路径     |

### SkyWalking 配置项

| 配置项                                        | 类型      | 默认值    | 说明                 |
|--------------------------------------------|---------|--------|--------------------|
| `carlos.apm.skywalking.enabled`            | boolean | `true` | 是否启用 SkyWalking 支持 |
| `carlos.apm.skywalking.log-report-enabled` | boolean | `true` | 是否启用日志上报           |

### MDC 配置项

| 配置项                                   | 类型      | 默认值          | 说明                    |
|---------------------------------------|---------|--------------|-----------------------|
| `carlos.apm.mdc.enabled`              | boolean | `true`       | 是否启用 MDC              |
| `carlos.apm.mdc.trace-id-key`         | String  | `traceId`    | Trace ID 在 MDC 中的 key |
| `carlos.apm.mdc.span-id-key`          | String  | `spanId`     | Span ID 在 MDC 中的 key  |
| `carlos.apm.mdc.add-to-response`      | boolean | `true`       | 是否添加到响应头              |
| `carlos.apm.mdc.response-header-name` | String  | `X-Trace-Id` | 响应头名称                 |

## 使用示例

### 在 Controller 中使用

```java
@RestController
@RequestMapping("/api/order")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{id}")
    public Result<Order> getOrder(@PathVariable Long id) {
        String traceId = TraceUtil.getUnifiedTraceId();
        log.info("[TraceId: {}] 查询订单，ID: {}", traceId, id);
        
        Order order = orderService.getOrder(id);
        return Result.success(order);
    }

    @PostMapping("/create")
    @TraceTag(key = "order.action", value = "'create'")
    @TraceTag(key = "order.userId", value = "#param.userId")
    public Result<Order> createOrder(@RequestBody @Valid OrderParam param) {
        String traceId = TraceUtil.getUnifiedTraceId();
        log.info("[TraceId: {}] 创建订单，用户: {}", traceId, param.getUserId());
        
        Order order = orderService.createOrder(param);
        
        // 添加业务标签
        TraceUtil.tag("order.id", order.getId().toString());
        TraceUtil.tag("order.amount", order.getAmount().toString());
        
        return Result.success(order);
    }
}
```

### 在 Service 中使用

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;

    @TraceTag(key = "order.query.id", value = "#id")
    public Order getOrder(Long id) {
        String traceId = TraceUtil.getUnifiedTraceId();
        log.debug("[TraceId: {}] 开始查询订单", traceId);
        
        Order order = orderMapper.selectById(id);
        
        if (order != null) {
            TraceUtil.tag("order.status", order.getStatus());
            TraceUtil.annotate("Order found");
        } else {
            TraceUtil.annotate("Order not found");
        }
        
        return order;
    }

    @Transactional
    @TraceTag(key = "order.created.id", value = "#result.id")
    @TraceTag(key = "order.created.amount", value = "#result.totalAmount")
    public Order createOrder(OrderParam param) {
        // 业务逻辑
        Order order = new Order();
        // ... 处理逻辑
        
        // SkyWalking 标签
        SkyWalkingUtil.setTag("order.type", param.getOrderType());
        SkyWalkingUtil.logInfo("订单创建成功: " + order.getId());
        
        return order;
    }
}
```

## 日志配置

### 标准 Logback 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 引入模块提供的默认配置（可选） -->
    <include resource="config/logback-skywalking.xml" optional="true"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-}] [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- SkyWalking GRPC 日志上报 -->
    <appender name="SKYWALKING" class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.log.GRPCLogClientAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.mdc.TraceIdMDCPatternLogbackLayout">
                <Pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{tid}] [%thread] %-5level %logger{50} - %msg%n</Pattern>
            </layout>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="SKYWALKING" />
    </root>
</configuration>
```

## 依赖项

| 依赖                                | 说明                          |
|-----------------------------------|-----------------------------|
| `carlos-spring-boot-core`         | Carlos 核心模块                 |
| `micrometer-tracing-bridge-brave` | Micrometer Tracing Brave 实现 |
| `spring-boot-starter-actuator`    | Actuator 指标监控（可选）           |
| `apm-toolkit-trace`               | SkyWalking 追踪工具包            |
| `apm-toolkit-logback-1.x`         | SkyWalking Logback 集成       |
| `apm-toolkit-webflux`             | SkyWalking WebFlux 支持（可选）   |
| `aspectjweaver`                   | AOP 切面支持                    |
| `spring-expression`               | SpEL 表达式解析                  |

## 注意事项

### 1. SkyWalking Agent 要求

- SkyWalking Toolkit 需要在 SkyWalking Agent 启动后才能正常工作
- 未接入 Agent 时，相关方法会返回默认值（如 "N/A"）
- 建议使用 `SkyWalkingUtil.inTraceContext()` 检查是否在追踪上下文中

### 2. 采样率配置

- 开发环境：建议全量采样（probability=1.0）
- 生产环境：建议适当降低采样率（如 0.1）
- 可以通过动态配置实时调整采样率

### 3. 性能考虑

- TraceTag 切面使用 SpEL 表达式解析，复杂表达式可能影响性能
- 建议在高频调用的方法上使用简单的标签
- 使用 condition 属性避免不必要的标签计算

### 4. MDC 线程安全

- MDC 是基于 ThreadLocal 的实现
- 在异步/线程池场景下需要手动传递 MDC 上下文
- 模块自动处理 Web 请求的 MDC 清理

## 版本要求

- JDK 17+
- Spring Boot 3.5.9+
- Spring Cloud 2025.0.1+
- SkyWalking Agent 9.5.0+
- Micrometer Tracing 1.2+

## 相关模块

- **carlos-spring-boot-core**：基础工具类
- **carlos-spring-boot-starter-log**：日志记录，与追踪信息结合
- **carlos-spring-cloud-starter**：微服务基础，分布式追踪依赖

## 更新日志

### 3.0.0-SNAPSHOT

- 升级至 Spring Boot 3.x
- 升级 Micrometer Tracing 1.2+
- 新增 @TraceTag 注解支持
- 新增 MDC 上下文自动管理
- 新增 SkyWalkingUtil 工具类
- 完善配置属性支持
